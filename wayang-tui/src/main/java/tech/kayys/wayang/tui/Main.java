package tech.kayys.wayang.tui;

import tech.kayys.wayang.sdk.agent.WayangAgent;
import tech.kayys.wayang.tui.config.Config;
import tech.kayys.wayang.sdk.provider.Provider;
import tech.kayys.wayang.tui.provider.ProviderFactory;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tui.ui.PanelUi;
import tech.kayys.wayang.tui.ui.ReplUi;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point: loads configuration, resolves the active profile (with
 * optional CLI overrides), builds the provider/tools/agent, and runs
 * the requested UI mode -- switching between REPL and panel modes if
 * the user requests it mid-session via /panel or /repl.
 */
public final class Main {

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            if (System.getenv("AGENTIC_TUI_DEBUG") != null) e.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(String[] args) throws IOException {
        CliArgs cli = CliArgs.parse(args);
        if (cli.help) {
            printHelp();
            return;
        }

        Path configPath = cli.configPath != null ? Paths.get(cli.configPath) : Config.defaultConfigPath();
        Config config = Config.load(configPath);

        if (cli.profileName != null) config.activeProfile = cli.profileName;
        Config.Profile profile = config.activeProfile();
        if (profile == null) {
            System.err.println("No profile configured. Edit " + configPath + " or delete it to regenerate defaults.");
            System.exit(1);
            return;
        }

        // CLI overrides apply only to this run; they don't get persisted to disk.
        if (cli.provider != null) profile.provider = cli.provider;
        if (cli.model != null) profile.model = cli.model;
        if (cli.uiMode != null) profile.uiMode = cli.uiMode;
        if (cli.agentMode != null) profile.agentMode = cli.agentMode;

        if (!config.providers.isEmpty() && config.provider(profile.provider) == null) {
            System.err.println("Profile '" + profile.name + "' references unknown provider '" + profile.provider + "'.");
            System.err.println("Configured providers: " + providerNames(config));
            System.exit(1);
            return;
        }

        // First run: write the default config to disk so the user has something to edit.
        if (!java.nio.file.Files.exists(configPath)) {
            config.save(configPath);
        }

        Provider provider = ProviderFactory.create(config, profile);

        List<Tool> allTools = List.of(
            new tech.kayys.wayang.tool.os.ReadFileTool(),
            new tech.kayys.wayang.tool.os.WriteFileTool(),
            new tech.kayys.wayang.tool.os.EditFileTool(),
            new tech.kayys.wayang.tool.os.ListDirTool(),
            new tech.kayys.wayang.tool.os.GrepTool(),
            new tech.kayys.wayang.tool.os.BashTool()
        );
        List<Tool> tools = new ArrayList<>();
        if (profile.agentMode == Config.AgentMode.AGENT) {
            if (profile.allowedTools == null || profile.allowedTools.isEmpty()) {
                tools.addAll(allTools);
            } else {
                for (Tool t : allTools) {
                    if (profile.allowedTools.contains(t.name())) {
                        tools.add(t);
                    }
                }
            }
        }

        WayangAgent agent = new WayangAgent(provider, tools, profile.systemPrompt,
                profile.temperature, profile.maxTokens, profile.autoApproveTools, java.nio.file.Path.of(System.getProperty("user.dir")));

        Config.ProviderConfig pc = config.provider(profile.provider);
        if (pc != null && needsApiKey(pc) && Config.resolveApiKey(pc) == null) {
            System.out.println("Note: no API key found for provider '" + profile.provider + "' " +
                    "(expected env var " + pc.apiKeyEnv + "). Try `--provider demo` to explore the UI without one.");
        }

        Config.UiMode mode = profile.uiMode;
        while (true) {
            String next;
            if (mode == Config.UiMode.PANEL) {
                next = new PanelUi(config, agent).run();
            } else {
                next = new ReplUi(config, agent, null, null).run();
            }
            if ("panel".equals(next)) { mode = Config.UiMode.PANEL; continue; }
            if ("repl".equals(next)) { mode = Config.UiMode.REPL; continue; }
            break; // quit
        }
    }

    private static boolean needsApiKey(Config.ProviderConfig pc) {
        return !"demo".equals(pc.type) && pc.apiKeyEnv != null;
    }

    private static String providerNames(Config config) {
        List<String> names = new ArrayList<>();
        for (Config.ProviderConfig pc : config.providers) names.add(pc.name);
        return String.join(", ", names);
    }

    private static void printHelp() {
        System.out.println("""
                agentic-tui -- a terminal AI coding assistant

                Usage: agentic-tui [options]

                Options:
                  --provider <name>     Override the provider (e.g. anthropic, openai, ollama, demo)
                  --model <name>        Override the model name
                  --mode <repl|panel>   UI layout (default: repl)
                  --agent <chat|agent>  chat-only or full coding-agent (with file/bash tools)
                  --profile <name>      Use a named profile from the config file
                  --config <path>       Path to config.json (default: ~/.agentic-tui/config.json)
                  --help                Show this help

                Config file: ~/.agentic-tui/config.json (created on first run)
                API keys: set via environment variables, e.g. ANTHROPIC_API_KEY, OPENAI_API_KEY

                Try it instantly with no API key: agentic-tui --provider demo
                """);
    }

    private static final class CliArgs {
        String provider, model, profileName, configPath;
        Config.UiMode uiMode;
        Config.AgentMode agentMode;
        boolean help;

        static CliArgs parse(String[] args) {
            CliArgs c = new CliArgs();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "--provider" -> c.provider = value(args, ++i);
                    case "--model" -> c.model = value(args, ++i);
                    case "--mode" -> c.uiMode = parseUiMode(value(args, ++i));
                    case "--agent" -> c.agentMode = parseAgentMode(value(args, ++i));
                    case "--profile" -> c.profileName = value(args, ++i);
                    case "--config" -> c.configPath = value(args, ++i);
                    case "--help", "-h" -> c.help = true;
                    default -> System.err.println("Unknown argument: " + a + " (use --help for usage)");
                }
            }
            return c;
        }

        private static String value(String[] args, int i) {
            return i < args.length ? args[i] : null;
        }

        private static Config.UiMode parseUiMode(String s) {
            if (s == null) return null;
            try { return Config.UiMode.valueOf(s.toUpperCase()); }
            catch (IllegalArgumentException e) { System.err.println("Invalid --mode: " + s); return null; }
        }

        private static Config.AgentMode parseAgentMode(String s) {
            if (s == null) return null;
            try { return Config.AgentMode.valueOf(s.toUpperCase()); }
            catch (IllegalArgumentException e) { System.err.println("Invalid --agent: " + s); return null; }
        }
    }
}
