package co.whale.cli;

import co.whale.Main;
import co.whale.context.ClientManager;
import co.whale.packet.OriginType;
import co.whale.packet.request.RequestFilePacket;
import co.whale.packet.request.RequestSpacePacket;
import co.whale.util.OptionBuilder;
import com.github.tomaslanger.chalk.Chalk;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Data
public class ClientCLI extends CLI<ClientManager> {

    private ClientManager clientManager;

    private Scanner scanner;
    private CommandLineParser parser;

    private Thread inputThread;

    private long lastSent = 0;
    private long timeout = 10 * 1000;
    private boolean waiting = false;

    private boolean forUpload = false;
    private boolean forDownload = false;

    private String lastName;
    private String lastPath;

    @Override
    public void listen(ClientManager manager) {

        this.clientManager = manager;

        this.scanner = new Scanner(System.in);
        this.parser = new DefaultParser();

        inputThread = new Thread(() -> {

            while(true) {

                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

                if(waiting) {
                    sleep(50);
                    continue;
                }

                forDownload = false;

                System.out.print("whale " + Chalk.on("(CLIENT)").magenta() + " >> ");
                String input = scanner.nextLine();

                String[] args = input.split(" ");

                if(args.length == 0 || args[0].isEmpty()) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid Command");
                    continue;

                }

                process(args);

            }

        });

        inputThread.start();

    }

    @SneakyThrows
    public void process(String[] args) {

        CommandLine cmd;

        switch (args[0]) {

            case "help": // HELP command for server instance

                cmd = OptionBuilder.builder()
                        .add("t", "test", false, false, "Test argument")
                        .cmd("execute", args);

                if(cmd.hasOption("t")) {
                    System.out.println("LEL");
                }

                System.out.println("PRINTING HELP");

                break;

            case "upload":

                if(args.length < 2) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " \"upload\" command requires 2 arguments or more");
                    return;

                }

                if(!args[1].startsWith("\"") || !args[args.length -1].endsWith("\"")) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid usage!");
                    return;

                }

                String pattern = "";
                String pathPattern = "";

                int position = 1;

                for(int i = 1; i < args.length; i++) {
                    position++;
                    pattern += (args[i].replaceAll("\"", "") + (i == args.length - 1 || args[i].endsWith("\"") ? "" : " "));
                    if(args[i].endsWith("\"")) {
                        break;
                    }
                }

                for(int i = position; i < args.length; i++) {
                    pathPattern += (args[i].replaceAll("\"", "") + (i == args.length - 1 || args[i].endsWith("\"") ? "" : " "));
                    if(args[i].endsWith("\"")) {
                        break;
                    }
                    position++;
                }

                if(pathPattern.contains("\\/")) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " \"upload\" command requires file legible name without \n" +
                            "| special characters or spaces");
                    return;

                }

                lastName = pathPattern;
                lastPath = pattern;

                forUpload = true;

                System.out.println("| " + Chalk.on("(INFO)").yellow() + " Finding space for \"" + lastPath + "\".\n");
                clientManager.getSocketClient().send(RequestSpacePacket.builder()
                        .required(100000)
                        .clientRequester(Main.getGeneratedNodeId())
                        .originType(OriginType.CLIENT)
                        .requested(new HashSet<>())
                        .build());

                waiting = true;
                break;

            case "download":

                if(args.length < 4) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " \"download\" command requires 4 arguments or more");
                    return;

                }

                if(!args[2].startsWith("\"") || !args[args.length -1].endsWith("\"")) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid find pattern, example of usage:\n" +
                            "| download sha1 \"5271593CA406362D7A2701E331408AB77D5B5B88\"");
                    return;

                }

                String upattern = "";
                String upathPattern = "";

                int uposition = 2;

                for(int i = 2; i < args.length; i++) {
                    uposition++;
                    upattern += (args[i].replaceAll("\"", "") + (i == args.length - 1 || args[i].endsWith("\"") ? "" : " "));
                    if(args[i].endsWith("\"")) {
                        break;
                    }
                }

                for(int i = uposition; i < args.length; i++) {
                    upathPattern += (args[i].replaceAll("\"", "") + (i == args.length - 1 || args[i].endsWith("\"") ? "" : " "));
                    if(args[i].endsWith("\"")) {
                        break;
                    }
                    uposition++;
                }

                lastPath = upathPattern;

                if(args[1].equalsIgnoreCase("sha1") || args[1].equalsIgnoreCase("hash")) {

                    forDownload = true;

                    System.out.println("| " + Chalk.on("(INFO)").yellow() + " Finding \"" + upattern + "\" in the whole ocean...\n");
                    clientManager.getSocketClient().send(RequestFilePacket.builder()
                            .fileName(upattern)
                            .clientRequester(Main.getGeneratedNodeId())
                            .originType(OriginType.CLIENT)
                            .requested(new HashSet<>())
                            .build());

                    waiting = true;
                    return;

                }

                if(args[1].equalsIgnoreCase("name")) {

                    forDownload = true;

                    System.out.println("| " + Chalk.on("(INFO)").yellow() + " Finding \"" + upattern + "\" in the whole ocean...\n");
                    clientManager.getSocketClient().send(RequestFilePacket.builder()
                            .fileName(upattern)
                            .clientRequester(Main.getGeneratedNodeId())
                            .originType(OriginType.CLIENT)
                            .requested(new HashSet<>())
                            .build());

                    waiting = true;
                    return;

                }

                System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid second argument (Check \n" +
                        "| that you are using SHA1 or NAME).");
                break;

            case "find":

                if(args.length < 3) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " \"find\" command requires 3 arguments or more");
                    return;

                }

                if(!args[2].startsWith("\"") || !args[args.length -1].endsWith("\"")) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid find pattern, example of usage:\n" +
                            "| find sha1 \"5271593CA406362D7A2701E331408AB77D5B5B88\"");
                    return;

                }

                String fpattern = "";

                for(int i = 2; i < args.length; i++)
                    fpattern += (args[i].replaceAll("\"", "") + (i == args.length - 1 ? "" : " "));

                if(args[1].equalsIgnoreCase("sha1") || args[1].equalsIgnoreCase("hash")) {

                    System.out.println("| " + Chalk.on("(INFO)").yellow() + " Finding \"" + fpattern + "\" in the whole ocean...\n");
                    clientManager.getSocketClient().send(RequestFilePacket.builder()
                            .fileName(fpattern)
                            .clientRequester(Main.getGeneratedNodeId())
                            .originType(OriginType.CLIENT)
                            .requested(new HashSet<>())
                            .build());

                    waiting = true;
                    return;

                }

                if(args[1].equalsIgnoreCase("name")) {

                    System.out.println("| " + Chalk.on("(INFO)").yellow() + " Finding \"" + fpattern + "\" in the whole ocean...\n");
                    clientManager.getSocketClient().send(RequestFilePacket.builder()
                            .fileName(fpattern)
                            .clientRequester(Main.getGeneratedNodeId())
                            .originType(OriginType.CLIENT)
                            .requested(new HashSet<>())
                            .build());

                    waiting = true;
                    return;

                }

                System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid second argument (Check \n" +
                        "| that you are using SHA1 or NAME).");
                break;

            case "exit":

                System.out.println("| " + Chalk.on("(SUCCESS)").green() + " Goodbye!");
                System.exit(-1);
                break;

            default:

                System.out.println("| " + Chalk.on("(ERROR)").red() + " Command \"" + args[0] + "\" not found");
                return;

        }

    }

    @SneakyThrows
    private void sleep(long timeout) {
        TimeUnit.MILLISECONDS.sleep(timeout);
    }

}
