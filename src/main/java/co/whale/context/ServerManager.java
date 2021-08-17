package co.whale.context;

import co.whale.Context;
import co.whale.Main;
import co.whale.cli.ServerCLI;
import co.whale.config.Configuration;
import co.whale.config.entry.ConfigServer;
import co.whale.context.collector.OceanFileCollector;
import co.whale.context.collector.WhaleSpaceCollector;
import co.whale.context.entry.OceanFile;
import co.whale.context.entry.WhaleFile;
import co.whale.context.entry.WhaleServer;
import co.whale.context.entry.WhaleSpace;
import co.whale.logger.VirtualLogger;
import co.whale.packet.OriginType;
import co.whale.packet.Packet;
import co.whale.packet.request.*;
import co.whale.packet.response.ResponseDownloadFilePacket;
import co.whale.packet.response.ResponseSpacePacket;
import co.whale.packet.response.ResponseStatusPacket;
import co.whale.packet.response.ResponseUploadFilePacket;
import co.whale.socket.SocketClient;
import co.whale.socket.SocketServer;
import co.whale.util.Crypto;

import co.whale.util.Net;
import com.github.tomaslanger.chalk.Chalk;
import lombok.Data;
import lombok.SneakyThrows;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

@Data
public class ServerManager extends Context {

    private static ServerManager instance;

    private Configuration configuration;
    private ServerCLI serverCLI;

    private Thread updater;

    private List<WhaleFile> files;
    private List<WhaleServer> servers;
    private List<TemporalCollector> collectors;

    private ScheduledExecutorService executorService;

    private long update = 0;
    private long discoverInterval = 0;

    private SocketServer socketServer;

    private String type = "PUBLIC";

    private String hostname = "127.0.0.1";
    private int port = -1;

    private int maxSize = -1;
    private VirtualLogger logger;

    @Override
    public void start() {

        instance = this;

        this.logger = new VirtualLogger("server-" + Main.getGeneratedNodeId());

        this.servers = new ArrayList<>();
        this.files = new ArrayList<>();
        this.collectors = new ArrayList<>();

        this.executorService = Executors.newScheduledThreadPool(20);
        this.configuration = Main.getConfiguration();

        this.serverCLI = new ServerCLI();

        type = configuration.getServerSetting("type").asString();
        hostname = configuration.getServerSetting("host").asString();

        if(hostname == null || hostname.isEmpty() || hostname.isBlank())
            hostname = Net.getIp();

        port = configuration.getServerSetting("port").asInt();
        maxSize = configuration.getServerSetting("size").asInt();

        discoverInterval = configuration.getServerSetting("discover-interval").asInt();

        makeServer();
        autoUpdate();

        serverCLI.listen(this);

    }

    public void makeServer() {

        logger.info("Initializing server in port (" + port + ")...");

        this.socketServer = new SocketServer(port);
        this.socketServer.connect();

        logger.info("Server initialized!");
        System.out.println("NODE ID = " + Main.getGeneratedNodeId());

        this.socketServer.listen(RequestStatusPacket.class, (fetcher, packet) -> {

            logger.net("Packet OK! (" + packet + ")");

            fetcher.send(ResponseStatusPacket.builder()
                    .id(Main.getGeneratedNodeId())
                    .type(type)
                    .maxSize(maxSize)
                    .size(getCurrentSize())
                    .hostname(hostname)
                    .port(port)
                    .build());

        });

        this.socketServer.listen(RequestFilePacket.class, ((fetcher, packet) -> {

            System.out.println("LOL 255");

            if(packet.getOriginType() == OriginType.CLIENT) {

                String searchId = UUID.randomUUID().toString();

                if(packet.getSearchId() == null || packet.getSearchId().isEmpty())
                    packet.setSearchId(searchId);
                else return;

                packet.setOriginServer(Main.getGeneratedNodeId());

                if(!packet.getRequested().contains(Main.getGeneratedNodeId())) {

                    packet.getRequested().add(Main.getGeneratedNodeId());

                    int online = getOnlineServers();

                    OceanFileCollector collector = new OceanFileCollector(packet, (list) -> {

                        Set<String> requested = new HashSet<>();

                        Set<OceanFile> otherFiles = new HashSet<>();
                        Set<OceanFile> hereFiles = find(packet.getFileName());

                        for(ResponseFilePacket responseFilePacket : list) {
                            requested.addAll(responseFilePacket.getRequested());
                            otherFiles.addAll(responseFilePacket.getFiles());
                        }

                        requested.add(Main.getGeneratedNodeId());
                        hereFiles.addAll(otherFiles);

                        fetcher.send(ResponseFilePacket.builder()
                                .fileName(packet.getFileName())
                                .originType(OriginType.SERVER)
                                .searchId(searchId)
                                .originServer(packet.getOriginServer())
                                .clientRequester(packet.getClientRequester())
                                .files(hereFiles)
                                .requested(requested)
                                .build());

                    });

                    collectors.add(collector);
                    collector.start(instance, online, 10 * 1000);

                    for(WhaleServer whaleServer : servers)
                        if(whaleServer.isOnline() && whaleServer.getSocketClient().isConnected()) {

                            send(whaleServer.getHostname(), whaleServer.getPort(), RequestFilePacket.builder()
                                    .serverHostname(hostname)
                                    .serverPort(port)
                                    .clientRequester(packet.getClientRequester())
                                    .originServer(packet.getOriginServer())
                                    .originType(OriginType.SERVER)
                                    .searchId(packet.getSearchId())
                                    .fileName(packet.getFileName())
                                    .requested(packet.getRequested())
                                    .build());

                        }

                    return;

                }

            } else {

                String searchId = packet.getSearchId();

                if(packet.getRequested().contains(Main.getGeneratedNodeId())) {

                    fetcher.send(ResponseFilePacket.builder()
                            .fileName(packet.getFileName())
                            .originType(OriginType.SERVER)
                            .searchId(searchId)
                            .originServer(packet.getOriginServer())
                            .clientRequester(packet.getClientRequester())
                            .files(new HashSet<>())
                            .requested(packet.getRequested())
                            .build());

                } else {

                    int online = getOnlineServers(packet.getRequested());

                    OceanFileCollector collector = new OceanFileCollector(packet, (list) -> {

                        Set<String> requested = new HashSet<>();

                        Set<OceanFile> otherFiles = new HashSet<>();
                        Set<OceanFile> hereFiles = find(packet.getFileName());

                        for(ResponseFilePacket responseFilePacket : list) {
                            requested.addAll(responseFilePacket.getRequested());
                            otherFiles.addAll(responseFilePacket.getFiles());
                        }

                        requested.add(Main.getGeneratedNodeId());
                        hereFiles.addAll(otherFiles);

                        send(packet.getServerHostname(), packet.getServerPort(), ResponseFilePacket.builder()
                                .fileName(packet.getFileName())
                                .originType(OriginType.SERVER)
                                .searchId(searchId)
                                .originServer(packet.getOriginServer())
                                .clientRequester(packet.getClientRequester())
                                .files(hereFiles)
                                .requested(requested)
                                .build());

                    });

                    collectors.add(collector);
                    collector.start(instance, online, 10 * 1000);

                    for(WhaleServer whaleServer : servers)
                        if(whaleServer.isOnline() && whaleServer.getSocketClient().isConnected() &&
                            !packet.getRequested().contains(whaleServer.getGeneratedId())) {

                            send(whaleServer.getHostname(), whaleServer.getPort(), RequestFilePacket.builder()
                                    .serverHostname(hostname)
                                    .serverPort(port)
                                    .clientRequester(packet.getClientRequester())
                                    .originServer(packet.getOriginServer())
                                    .originType(OriginType.SERVER)
                                    .searchId(packet.getSearchId())
                                    .fileName(packet.getFileName())
                                    .requested(packet.getRequested())
                                    .build());

                        }

                }

            }

        }));

        this.socketServer.listen(RequestSpacePacket.class, ((fetcher, packet) -> {

            if(packet.getOriginType() == OriginType.CLIENT) {

                String searchId = UUID.randomUUID().toString();

                if(packet.getSearchId() == null || packet.getSearchId().isEmpty())
                    packet.setSearchId(searchId);
                else return;

                packet.setOriginServer(Main.getGeneratedNodeId());

                if(!packet.getRequested().contains(Main.getGeneratedNodeId())) {

                    packet.getRequested().add(Main.getGeneratedNodeId());

                    int online = getOnlineServers();

                    WhaleSpaceCollector collector = new WhaleSpaceCollector(packet, (list) -> {

                        Set<String> requested = new HashSet<>();
                        Set<WhaleSpace> otherSpaces = new HashSet<>();

                        otherSpaces.add(WhaleSpace.builder()
                                .whaleHostname(hostname)
                                .whalePort(port)
                                .whaleId(Main.getGeneratedNodeId())
                                .size(getCurrentSize())
                                .maxSize(getMaxSize())
                                .build());

                        for(ResponseSpacePacket responseSpacePacket : list) {
                            requested.addAll(responseSpacePacket.getRequested());
                            otherSpaces.addAll(responseSpacePacket.getSpaces());
                        }

                        requested.add(Main.getGeneratedNodeId());

                        fetcher.send(ResponseSpacePacket.builder()
                                .originType(OriginType.SERVER)
                                .searchId(searchId)
                                .originServer(packet.getOriginServer())
                                .clientRequester(packet.getClientRequester())
                                .spaces(otherSpaces)
                                .requested(requested)
                                .build());

                    });

                    collectors.add(collector);
                    collector.start(instance, online, 10 * 1000);

                    for(WhaleServer whaleServer : servers)
                        if(whaleServer.isOnline() && whaleServer.getSocketClient().isConnected()) {

                            send(whaleServer.getHostname(), whaleServer.getPort(), RequestSpacePacket.builder()
                                    .serverHostname(hostname)
                                    .serverPort(port)
                                    .clientRequester(packet.getClientRequester())
                                    .originServer(packet.getOriginServer())
                                    .originType(OriginType.SERVER)
                                    .searchId(packet.getSearchId())
                                    .requested(packet.getRequested())
                                    .build());

                        }

                    return;

                }

            } else {

                String searchId = packet.getSearchId();

                if(packet.getRequested().contains(Main.getGeneratedNodeId())) {

                    fetcher.send(ResponseSpacePacket.builder()
                            .originType(OriginType.SERVER)
                            .searchId(searchId)
                            .originServer(packet.getOriginServer())
                            .clientRequester(packet.getClientRequester())
                            .spaces(new HashSet<>())
                            .requested(packet.getRequested())
                            .build());

                } else {

                    int online = getOnlineServers(packet.getRequested());

                    WhaleSpaceCollector collector = new WhaleSpaceCollector(packet, (list) -> {

                        Set<String> requested = new HashSet<>();
                        Set<WhaleSpace> otherSpaces = new HashSet<>();

                        otherSpaces.add(WhaleSpace.builder()
                                .whaleHostname(hostname)
                                .whalePort(port)
                                .whaleId(Main.getGeneratedNodeId())
                                .size(getCurrentSize())
                                .maxSize(getMaxSize())
                                .build());

                        for(ResponseSpacePacket responseSpacePacket : list) {
                            requested.addAll(responseSpacePacket.getRequested());
                            otherSpaces.addAll(responseSpacePacket.getSpaces());
                        }

                        requested.add(Main.getGeneratedNodeId());

                        send(packet.getServerHostname(), packet.getServerPort(), ResponseSpacePacket.builder()
                                .originType(OriginType.SERVER)
                                .searchId(searchId)
                                .originServer(packet.getOriginServer())
                                .clientRequester(packet.getClientRequester())
                                .spaces(otherSpaces)
                                .requested(requested)
                                .build());

                    });

                    collectors.add(collector);
                    collector.start(instance, online, 10 * 1000);

                    for(WhaleServer whaleServer : servers)
                        if(whaleServer.isOnline() && whaleServer.getSocketClient().isConnected() &&
                                !packet.getRequested().contains(whaleServer.getGeneratedId())) {

                            send(whaleServer.getHostname(), whaleServer.getPort(), RequestSpacePacket.builder()
                                    .serverHostname(hostname)
                                    .serverPort(port)
                                    .clientRequester(packet.getClientRequester())
                                    .originServer(packet.getOriginServer())
                                    .originType(OriginType.SERVER)
                                    .searchId(packet.getSearchId())
                                    .requested(packet.getRequested())
                                    .build());

                        }

                }

            }

        }));

        this.socketServer.listen(ResponseFilePacket.class, ((fetcher, packet) -> {

            Optional<TemporalCollector> optionalCollector = getCollector(packet);

            if(optionalCollector.isPresent()) {

                TemporalCollector collector = optionalCollector.get();
                collector.save(packet);

            }

        }));

        this.socketServer.listen(ResponseSpacePacket.class, ((fetcher, packet) -> {

            Optional<TemporalCollector> optionalCollector = getCollector(packet);

            if(optionalCollector.isPresent()) {

                TemporalCollector collector = optionalCollector.get();
                collector.save(packet);

            }

        }));

        this.socketServer.listen(RequestDownloadFilePacket.class, ((fetcher, packet) -> {

            for(WhaleFile file : files) {

                if(file.getSha1().equals(packet.getSha1())) {

                    try {

                        byte[] bytes = FileUtils.readFileToByteArray(file.getFile());

                        fetcher.send(ResponseDownloadFilePacket.builder()
                                .fileBytes(bytes)
                                .build());

                        return;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        }));

        this.socketServer.listen(RequestUploadFilePacket.class, ((fetcher, packet) -> {

            try {

                Files.write(Paths.get("./whale/" + packet.getFileName()), packet.getContent());
                fetcher.send(ResponseUploadFilePacket.builder().build());

            } catch (IOException e) {
                logger.error("Error ocurred writing the new file.");
            }

        }));

    }

    public void send(String hostname, int port, Packet packet) {

        SocketClient socketClient = new SocketClient(hostname, port);
        socketClient.connect();
        socketClient.send(packet);
        socketClient.getThread().stop();

    }

    public Optional<TemporalCollector> getCollector(Packet packet) {
        for(TemporalCollector temporalCollector : collectors) {
            if(temporalCollector.getPacketClass().equals(packet.getClass()) && temporalCollector.canCollect(packet)) {
                return Optional.of(temporalCollector);
            }
        }
        return Optional.empty();
    }

    public Set<OceanFile> find(String pattern) {

        Set<OceanFile> result = new HashSet<>();

        for(WhaleFile file : files)
            if(file.getFilename().startsWith(pattern) || file.getSha1().startsWith(pattern))
                result.add(OceanFile.builder()
                        .whaleId(Main.getGeneratedNodeId())
                        .whaleHostname(hostname)
                        .whalePort(port)
                        .filename(file.getFilename())
                        .sha1(file.getSha1())
                        .size(file.getSize())
                        .lastModified(file.getLastModified())
                        .build());

        return result;

    }

    public void autoUpdate() {

        updater = new Thread(new Runnable() {

            @SneakyThrows
            @Override
            public void run() {

                long current = new Date().getTime();

                if(current - update >= discoverInterval * 1000) {

                    // Reset timer
                    update = current;

                    // Discover Files

                    File whaleDirectory = new File("./whale");

                    if(!whaleDirectory.isDirectory())
                        whaleDirectory.mkdir();

                    files.clear();

                    for(File file : FileUtils.listFiles(whaleDirectory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {

                        files.add(WhaleFile.builder()
                                .file(file)
                                .filename(file.getName())
                                .sha1(Crypto.calcSHA1(file))
                                .lastModified(file.lastModified())
                                .size(file.length())
                                .build());

                    }

                    // Discover Nodes

                    cloop : for(ConfigServer configServer : configuration.getServerNodes()) {

                        wloop : for(WhaleServer whaleServer : servers) {
                            if(whaleServer.getHostname().equalsIgnoreCase(configServer.getHostname()) &&
                                    whaleServer.getPort() == configServer.getPort()) {

                                if(!whaleServer.getSocketClient().isConnected()) {

                                    whaleServer.setSocketClient(null);
                                    submit(() -> whaleServer.update());

                                } else if(whaleServer.getGeneratedId().equals(Main.getGeneratedNodeId())) {

                                    System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid node in this whale\n" +
                                    "| " + Chalk.on("EXIT CODE [605]").bgRed());
                                    System.exit(1);
                                    return;

                                }

                                if(!whaleServer.isOnline())
                                    submit(() -> whaleServer.update());

                                continue cloop;

                            }
                        }

                        WhaleServer whaleServer = new WhaleServer(configServer.getHostname(), configServer.getPort(), configServer.getKey(), instance);
                        servers.add(whaleServer);
                        submit(() -> whaleServer.update());

                    }

                }

                TimeUnit.SECONDS.sleep(1);
                run();

            }

        });

        updater.start();

    }

    public boolean addWhale(String hostname, int port, String key) {

        for(ConfigServer configServer : configuration.getServerNodes())
            if(configServer.getHostname().equals(hostname) && configServer.getPort() == port)
                return false;

        configuration.addServerNode(ConfigServer.builder()
                .hostname(hostname)
                .port(port)
                .key(key)
                .build());

        update = 0;
        return true;

    }

    public boolean removeWhale(String hostname, int port) {
        for(ConfigServer configServer : configuration.getServerNodes())
            if(configServer.getHostname().equals(hostname) && configServer.getPort() == port) {
                configuration.removeServerNode(ConfigServer.builder()
                        .hostname(hostname)
                        .port(port)
                        .build());
                return true;
            }
        return false;
    }

    public int getOnlineServers() {
        int online = 0;
        for(WhaleServer server : servers)
            if(server.isOnline())
                online++;
        return online;
    }

    public int getOnlineServers(Set<String> except) {
        int online = 0;
        for(WhaleServer server : servers)
            if(server.isOnline() && !except.contains(server.getGeneratedId()))
                online++;
        return online;
    }

    public long getCurrentSize() {
        long size = 0;
        for(WhaleFile whaleFile : files)
            size += whaleFile.getSize();
        return size;
    }

    public Future<?> submit(Runnable runnable, long timeout) {
        Future<?> future = executorService.submit(runnable);
        executorService.schedule(() -> {

            future.cancel(true);

        }, timeout, TimeUnit.SECONDS);
        return executorService.submit(runnable);
    }

    public Future<?> submit(Runnable runnable) {
        return executorService.submit(runnable);
    }

}
