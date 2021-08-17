package co.whale.context;

import co.whale.Context;
import co.whale.Main;
import co.whale.cli.ClientCLI;
import co.whale.config.Configuration;
import co.whale.context.entry.OceanFile;
import co.whale.context.entry.WhaleFile;
import co.whale.context.entry.WhaleSpace;
import co.whale.logger.VirtualLogger;
import co.whale.packet.Packet;
import co.whale.packet.request.RequestDownloadFilePacket;
import co.whale.packet.request.RequestUploadFilePacket;
import co.whale.packet.request.ResponseFilePacket;
import co.whale.packet.response.ResponseDownloadFilePacket;
import co.whale.packet.response.ResponseSpacePacket;
import co.whale.packet.response.ResponseUploadFilePacket;
import co.whale.socket.SocketClient;
import co.whale.util.Dates;
import co.whale.util.TableList;
import com.github.tomaslanger.chalk.Chalk;
import lombok.Data;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Data
public class ClientManager extends Context {

    private Configuration configuration;
    private ClientCLI clientCLI;

    private ScheduledExecutorService executorService;

    private SocketClient socketClient;
    private VirtualLogger logger;

    private String hostname;
    private int port;

    @Override
    public void start() {

        this.logger = new VirtualLogger("client-" + Main.getGeneratedNodeId());

        this.executorService = Executors.newScheduledThreadPool(20);
        this.configuration = Main.getConfiguration();

        this.clientCLI = new ClientCLI();

        makeClient();

        clientCLI.listen(this);

    }

    public void makeClient() {

        logger.info("Initializing client with main whale (" + hostname + ":" + port + ")...");

        this.socketClient = new SocketClient(hostname, port);

        try {

            this.socketClient.connect();

        } catch(Exception e) {

            System.out.println("| " + Chalk.on("(ERROR)").red() + " Connection reset (" + hostname + ":" + port + ")\n" +
                    "| " + Chalk.on("EXIT CODE [705]").bgRed());
            System.exit(-1);
            return;

        }

        logger.info("Client initialized!");

        socketClient.listen(ResponseFilePacket.class, (packet -> {

            // System.out.println("RESPONSE: " + packet);

            if(clientCLI.isForDownload()) {

                clientCLI.setForDownload(false);

                if(packet.getFiles().isEmpty()) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " File not found!");
                    clientCLI.setWaiting(false);

                    return;

                }

                int index = 1;
                Map<Integer, OceanFile> options = new HashMap<>();

                for(OceanFile file : packet.getFiles()) {

                    options.put(index, file);
                    System.out.println("| [" + index + "] \"" + file.getSha1() + "\" ~ \"" + file.getFilename() + "\"");

                    index++;

                }

                System.out.print("|\n| Please select an option (write the number): ");

                clientCLI.getScanner().reset();

                try {

                    String text = clientCLI.getScanner().nextLine();
                    int number = Integer.parseInt(text);

                    if(number >= 1 && number <= index) {

                        TableList filesTableList = new TableList(5, "Whale ID", "SHA-1", "Filename", "Size", "Date").sortBy(0).withUnicode(true);
                        OceanFile file = options.get(number);

                        filesTableList.addRow(
                                file.getWhaleId(),
                                file.getSha1(),
                                file.getFilename(),
                                "" + file.getSize(),
                                Dates.getFormatted(file.getLastModified()));

                        filesTableList.print();

                        String lastPath = clientCLI.getLastPath();

                        System.out.println("| " + Chalk.on("(INFO)").yellow() + " Preparing for download \"" + file.getSha1()+ "\"...\n");
                        System.out.println("| " + Chalk.on("(INFO)").yellow() + " Requesting downloading to local file \"" + lastPath + "\"...\n");

                        RequestDownloadFilePacket downloadPacket = RequestDownloadFilePacket.builder()
                                .clientRequester(Main.getGeneratedNodeId())
                                .localPath(lastPath)
                                .sha1(file.getSha1())
                                .build();

                        SocketClient socketClient = new SocketClient(file.getWhaleHostname(), file.getWhalePort());
                        socketClient.connect();
                        socketClient.send(downloadPacket);

                        socketClient.listen(ResponseDownloadFilePacket.class, (content -> {

                            try {

                                File clone = new File(lastPath);
                                clone.getParentFile().mkdirs();

                                clone.createNewFile();

                                FileUtils.writeByteArrayToFile(clone, content.getFileBytes());

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }));

                        clientCLI.setWaiting(false);

                    } else {

                        System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid selection!");
                        clientCLI.setWaiting(false);

                    }

                    return;

                } catch(Exception e) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " Bad input!");
                    clientCLI.setWaiting(false);

                    return;

                }

            }

            TableList filesTableList = new TableList(5, "Whale ID", "SHA-1", "Filename", "Size", "Date").sortBy(0).withUnicode(true);

            for(OceanFile file : packet.getFiles())
                filesTableList.addRow(
                        file.getWhaleId(),
                        file.getSha1(),
                        file.getFilename(),
                        "" + file.getSize(),
                        Dates.getFormatted(file.getLastModified()));

            filesTableList.print();
            clientCLI.setWaiting(false);

        }));

        socketClient.listen(ResponseSpacePacket.class, (packet -> {

            if(clientCLI.isForUpload()) {

                File clone = new File(clientCLI.getLastPath());

                if(packet.getSpaces().isEmpty()) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " File too large for our current capacity!");
                    clientCLI.setWaiting(false);

                    return;

                }

                for(WhaleSpace whaleSpace : packet.getSpaces()) {

                    if(whaleSpace.getSize() + clone.length() <= whaleSpace.getMaxSize()) {

                        System.out.println("| " + Chalk.on("(INFO)").yellow() + " Whale found (" + whaleSpace.getWhaleId() + ")!");
                        System.out.println("| " + Chalk.on("(INFO)").yellow() + " Preparing for upload \"" + clone.getAbsolutePath() + "\"...");
                        System.out.println("| " + Chalk.on("(INFO)").yellow() + " Requesting uploading from local file \"" + clone.getAbsolutePath() + "\"...");

                        try {

                            RequestUploadFilePacket downloadPacket = RequestUploadFilePacket.builder()
                                    .fileName(clientCLI.getLastName())
                                    .content(Files.readAllBytes(Paths.get(clone.getAbsolutePath())))
                                    .build();

                            SocketClient socketClient = new SocketClient(whaleSpace.getWhaleHostname(), whaleSpace.getWhalePort());
                            socketClient.connect();
                            socketClient.send(downloadPacket);

                            socketClient.listen(ResponseUploadFilePacket.class, (content -> {

                                logger.info("File upload completed!");

                            }));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        clientCLI.setWaiting(false);
                        return;


                    }

                }

                System.out.println("| " + Chalk.on("(ERROR)").red() + " No whale found for store your file!");
                clientCLI.setWaiting(false);

                return;

            }

        }));

    }

}
