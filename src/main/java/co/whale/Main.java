package co.whale;

import co.whale.config.Configuration;
import co.whale.context.ClientManager;
import co.whale.context.ServerManager;
import co.whale.util.OptionBuilder;
import com.github.tomaslanger.chalk.Chalk;
import lombok.SneakyThrows;
import org.apache.commons.cli.CommandLine;
import org.hjson.JsonValue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

public class Main {

    private static String generatedNodeId;
    private static Configuration configuration;

    private static ServerManager serverManager;
    private static ClientManager clientManager;

    public static void main(String...args) {

        CommandLine cmd = OptionBuilder.builder()
                .add("p", "port", true, false, "Port of the server")
                .add("h", "host", true, false, "Host of the server")
                .cmd("execute", args);

        if(args.length < 1) {

            System.out.println("| " + Chalk.on("(ERROR)").red() + " Not enough arguments (Pleas use SERVER or CLIENT to set\n" +
                    "| the instance type)\n" +
                    "| " + Chalk.on("EXIT CODE [600]").bgRed());
            return;

        }

        if(args[0].equalsIgnoreCase("SERVER")) {

            configuration = new Configuration("whale-config.hjson");
            generatedNodeId = UUID.randomUUID().toString().substring(0, 8);

            if(cmd.hasOption("p"))
                configuration.setServerSetting("port", JsonValue.valueOf(Integer.parseInt(cmd.getOptionValue("p"))));

            if(cmd.hasOption("h"))
                configuration.setServerSetting("host", JsonValue.valueOf(cmd.getOptionValue("h")));

            serverManager = new ServerManager();
            serverManager.start();

        } else if(args[0].equalsIgnoreCase("CLIENT")) {

            clientManager = new ClientManager();

            configuration = new Configuration("whale-config.hjson");
            generatedNodeId = UUID.randomUUID().toString().substring(0, 8);

            if (cmd.hasOption("p")) {
                clientManager.setPort(Integer.parseInt(cmd.getOptionValue("p")));
            } else {
                System.out.println("| " + Chalk.on("(ERROR)").red() + " Required port (-p) parameter not found\n" +
                        "| " + Chalk.on("EXIT CODE [701]").bgRed());
                return;
            }

            if (cmd.hasOption("p")) {
                clientManager.setHostname(cmd.getOptionValue("h"));
            } else {
                System.out.println("| " + Chalk.on("(ERROR)").red() + " Required hostname (-h) parameter not found\n" +
                        "| " + Chalk.on("EXIT CODE [702]").bgRed());
                return;
            }

            clientManager.start();

        } else {

            System.out.println("| " + Chalk.on("(ERROR)").red() + " Unrecognized instance type \"" + args[0] + "\"\n" +
                    "| " + Chalk.on("EXIT CODE [601]").bgRed());
            return;

        }


        // System.out.println(configuration.getServer().toString());



    }

    @SneakyThrows
    public static InputStream getResource(String resource) {
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(resource);
        return inputStream;
    }

    public static String getGeneratedNodeId() {
        return generatedNodeId;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

}
