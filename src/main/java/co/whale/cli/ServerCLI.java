package co.whale.cli;

import co.whale.Main;
import co.whale.context.ServerManager;
import co.whale.context.entry.WhaleFile;
import co.whale.context.entry.WhaleServer;
import co.whale.util.*;
import com.github.tomaslanger.chalk.Chalk;
import lombok.SneakyThrows;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

public class ServerCLI extends CLI<ServerManager> {

    private ServerManager serverManager;

    private Scanner scanner;
    private CommandLineParser parser;

    private Thread inputThread;

    @SneakyThrows
    @Override
    public void listen(ServerManager serverManager) {

        this.serverManager = serverManager;

        this.scanner = new Scanner(System.in);
        this.parser = new DefaultParser();

        inputThread = new Thread(() -> {

            while(true) {

                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

                System.out.print("whale " + Chalk.on("(SERVER)").cyan() + " >> ");
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

            case "exit":

                System.out.println("| " + Chalk.on("(SUCCESS)").green() + " Goodbye!");
                System.exit(-1);
                break;

            case "info":

                information();
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

                TableList fileTableList = new TableList(4, "SHA-1", "Filename", "Size", "Date").sortBy(0).withUnicode(true);
                String pattern = "";

                for(int i = 2; i < args.length; i++)
                    pattern += (args[i].replaceAll("\"", "") + (i == args.length - 1 ? "" : " "));

                if(args[1].equalsIgnoreCase("sha1") || args[1].equalsIgnoreCase("hash")) {

                    for(WhaleFile file : serverManager.getFiles())
                        if(file.getSha1().equals(pattern)) {
                            fileTableList.addRow(
                                    file.getSha1(),
                                    file.getFilename(),
                                    "" + file.getSize(),
                                    Dates.getFormatted(file.getLastModified()));
                            fileTableList.print();
                            return;
                        }

                    System.out.println("| " + Chalk.on("(INFO)").yellow() + " No file found with SHA1 \"" + pattern + "\"\n" +
                            "| in this whale");
                    return;

                }

                if(args[1].equalsIgnoreCase("name")) {

                    for(WhaleFile file : serverManager.getFiles())
                        if(file.getFilename().equals(pattern)) {
                            fileTableList.addRow(
                                    file.getSha1(),
                                    file.getFilename(),
                                    "" + file.getSize(),
                                    Dates.getFormatted(file.getLastModified()));
                            fileTableList.print();
                            return;
                        }

                    System.out.println("| " + Chalk.on("(INFO)").yellow() + " No file found with NAME \"" + pattern + "\"\n" +
                            "| in this whale");
                    return;

                }

                System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid second argument (Check \n" +
                        "| that you are using SHA1 or NAME).");
                break;

            case "files":

                TableList filesTableList = new TableList(4, "SHA-1", "Filename", "Size", "Date").sortBy(0).withUnicode(true);

                for(WhaleFile file : serverManager.getFiles())
                    filesTableList.addRow(
                            file.getSha1(),
                            file.getFilename(),
                            "" + file.getSize(),
                            Dates.getFormatted(file.getLastModified()));

                filesTableList.print();
                break;

            case "node":

                cmd = OptionBuilder.builder()
                        .add("k", "key", true, false, "Key argument for private server instances")
                        .cmd("execute", args);

                if(args.length == 2 && !args[1].equals("info")) {

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " \"node\" command with 1 arguments requires second to be \"info\"");
                    return;

                } else if(args.length >= 3) {

                    String[] nodeComposition = args[2].split(":");

                    if(nodeComposition.length < 2) {

                        System.out.println("| " + Chalk.on("(ERROR)").red() + " Bad node hostname and port (check that\n" +
                                "| you are using hostname:port format)");
                        return;

                    }

                    String hostname = nodeComposition[0];
                    String portStr = nodeComposition[1];

                    int port = -1;

                    try {

                        port = Integer.parseInt(portStr);

                    } catch(Exception e) {

                        System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid port (" + portStr + "), it must be numeric");
                        return;

                    }

                    if(port <= 0) {

                        System.out.println("| " + Chalk.on("(ERROR)").red() + " Invalid port (" + portStr + "), it must be greater or equals to one");
                        return;

                    }

                    if(args[1].equals("add")) {

                        String key = "none";

                        if(cmd.hasOption("k"))
                            key = cmd.getOptionValue("k");

                        if(!serverManager.addWhale(hostname, port, key)) {

                            System.out.println("| " + Chalk.on("(ERROR)").red() + " Duplicated entry (" + hostname + ":" + port + ")");
                            return;

                        }

                        System.out.println("| " + Chalk.on("(SUCCESS)").green() + " New node (" + hostname + ":" + port + ") added! Check it with \"nodes\" command");
                        return;

                    }

                    if(args[1].equals("remove")) {

                        if(!serverManager.removeWhale(hostname, port)) {

                            System.out.println("| " + Chalk.on("(ERROR)").red() + " Node (" + hostname + ":" + port + ") not exists!");
                            return;

                        }

                        System.out.println("| " + Chalk.on("(SUCCESS)").green() + " Node (" + hostname + ":" + port + ") enqueue to be removed!");
                        return;

                    }


                } else {

                    if(args[1].equals("info")) {

                        information();
                        return;

                    }

                    System.out.println("| " + Chalk.on("(ERROR)").red() + " \"node\" command requires 2 arguments or more");
                    return;

                }

                break;

            case "nodes":

                TableList nodesTableList = new TableList(4, "Generated ID", "Hostname", "Port", "Online").sortBy(0).withUnicode(true);

                for(WhaleServer whaleServer : serverManager.getServers())
                    nodesTableList.addRow(
                            whaleServer.getGeneratedId(),
                            whaleServer.getHostname(),
                            String.valueOf(whaleServer.getPort()),
                            String.valueOf(whaleServer.isOnline()));

                nodesTableList.print();
                break;

            case "logs":

                System.out.print(serverManager.getLogger().getBuffer().toString());
                serverManager.getLogger().setObserving(true);

                scanner.reset();

                while(System.in.available() == 0) {

                    while(!serverManager.getLogger().getLogs().isEmpty())
                        System.out.print(serverManager.getLogger().getLogs().remove());

                }

                serverManager.getLogger().setObserving(false);
                scanner.nextLine();
                break;

            default:

                System.out.println("| " + Chalk.on("(ERROR)").red() + " Command \"" + args[0] + "\" not found");
                return;

        }

    }

    private void information() {

        TableList infoTableList = new TableList(6, "Generated ID", "Hostname", "Port", "Current Size", "Max Size", "Files").sortBy(0).withUnicode(true);

        long totalSize = 0;
        long maxSize = serverManager.getConfiguration().getServerSetting("size").asLong();

        List<WhaleFile> wfiles = serverManager.getFiles();

        for(WhaleFile file : wfiles)
            totalSize += file.getSize();

        infoTableList.addRow(
                Main.getGeneratedNodeId(),
                Net.getIp(),
                "" + serverManager.getPort(),
                Sizes.humanReadableByteCountBin(totalSize),
                Sizes.humanReadableByteCountBin(maxSize),
                String.valueOf(wfiles.size()));

        infoTableList.print();

    }

}
