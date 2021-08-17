package co.whale.util;

import org.apache.commons.cli.*;

public class OptionBuilder {

    private Options options;

    public OptionBuilder() {
        this.options = new Options();
    }

    public OptionBuilder add(String one, String name, String description) {
        Option option = new Option(one, name, true, description);
        option.setRequired(true);
        options.addOption(option);
        return this;
    }

    public OptionBuilder add(String one, String name) {
        Option option = new Option(one, name, true, "dummy");
        option.setRequired(true);
        options.addOption(option);
        return this;
    }

    public OptionBuilder add(String one, String name, boolean hasArg, String description) {
        options.addOption(new Option(one, name, hasArg, description));
        return this;
    }

    public OptionBuilder add(String one, String name, boolean hasArg, boolean required, String description) {
        Option option = new Option(one, name, hasArg, description);
        option.setRequired(required);
        options.addOption(option);
        return this;
    }

    public CommandLine cmd(String cmd, String args) {
        return parse(options, cmd, args.split(" "));
    }

    public CommandLine cmd(String cmd, String[] args) {
        return parse(options, cmd, args);
    }

    public Options options() {
        return options;
    }

    public static OptionBuilder builder() {
        return new OptionBuilder();
    }

    public static CommandLine parse(Options options, String cmd, String args) {
        return parse(options, cmd, args.split(" "));
    }

    public static CommandLine parse(Options options, String cmdLabel, String[] args) {

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {

            cmd = parser.parse(options, args);

        } catch (ParseException e) {

            System.out.println(e.getMessage());
            formatter.printHelp(cmdLabel, options);

        }

        return cmd;

    }

}
