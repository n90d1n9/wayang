package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.config.GamelanConfigStore;
import tech.kayys.gamelan.util.AnsiPrinter;

/**
 * Configuration management subcommand.
 *
 * <pre>
 * Usage:
 *   gamelan config list              # Show all configuration
 *   gamelan config get model         # Get a config value
 *   gamelan config set model qwen2   # Set a config value
 *   gamelan config reset             # Reset to defaults
 * </pre>
 *
 * <p>Configuration is stored in {@code ~/.gamelan/config.yml}.
 */
@Command(
    name = "config",
    description = "View and edit Gamelan configuration",
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigCommand.ListCmd.class,
        ConfigCommand.GetCmd.class,
        ConfigCommand.SetCmd.class,
        ConfigCommand.ResetCmd.class
    }
)
public class ConfigCommand implements Runnable {

    @Override
    public void run() {
        new picocli.CommandLine(this).usage(System.out);
    }

    @Command(name = "list", description = "Show all configuration", aliases = {"ls", "show"})
    static class ListCmd implements Runnable {

        @Inject
        GamelanConfig config;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            printer.sectionHeader("Gamelan Configuration");
            printer.println("  model          : " + config.defaultModel());
            printer.println("  skills.dir     : " + config.skillsDir());
            printer.println("  history.size   : " + config.historySize());
            printer.println("  stream         : " + config.streamByDefault());
            printer.println("  temperature    : " + config.temperature());
            printer.println("  max.tokens     : " + config.maxTokens());
            printer.println("  engine.mode    : " + config.engineMode());
            printer.println("  remote.url     : " + config.remoteUrl());
        }
    }

    @Command(name = "get", description = "Get a configuration value")
    static class GetCmd implements Runnable {

        @Inject
        GamelanConfigStore store;

        @Parameters(index = "0", description = "Config key")
        String key;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            String value = store.get(key);
            if (value == null) {
                printer.warn("Key not found: " + key);
            } else {
                printer.println(key + "=" + value);
            }
        }
    }

    @Command(name = "set", description = "Set a configuration value")
    static class SetCmd implements Runnable {

        @Inject
        GamelanConfigStore store;

        @Parameters(index = "0", description = "Config key")
        String key;

        @Parameters(index = "1", description = "Config value")
        String value;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            try {
                store.set(key, value);
                printer.success("Set " + key + " = " + value);
            } catch (Exception e) {
                printer.error("Cannot set config: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Command(name = "reset", description = "Reset configuration to defaults")
    static class ResetCmd implements Runnable {

        @Inject
        GamelanConfigStore store;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            try {
                store.reset();
                printer.success("Configuration reset to defaults.");
            } catch (Exception e) {
                printer.error("Reset failed: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
