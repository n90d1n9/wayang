package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import tech.kayys.gollek.factory.GollekSdkFactory;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.session.ChatSession;
import tech.kayys.gollek.sdk.session.ChatSessionImpl;
import tech.kayys.gollek.spi.model.ModelInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Interactive coding-agent command in the style of Gemini CLI and Claude Code.
 *
 * <p>When invoked without a prompt, opens a REPL loop. When invoked with a
 * prompt, runs once and exits (unless {@code --interactive} is set).</p>
 *
 * <p>Slash commands available inside the REPL:
 * <ul>
 *   <li>{@code /exit}, {@code /quit} — exit the session</li>
 *   <li>{@code /reset} — clear conversation history</li>
 *   <li>{@code /models}, {@code /list} — list available local models</li>
 *   <li>{@code /model <model-id>} — switch model</li>
 *   <li>{@code /providers} — list available LLM providers</li>
 *   <li>{@code /provider <provider-id>} — switch provider</li>
 *   <li>{@code /info} — display system info</li>
 *   <li>{@code /status} — show platform status</li>
 *   <li>{@code /workspace} — show workspace snapshot</li>
 *   <li>{@code /harness} — show verification checks</li>
 *   <li>{@code /help} — show available slash commands</li>
 * </ul>
 * </p>
 */
@Command(
        name = "code",
        aliases = {"agent", "coder"},
        description = "Start an interactive coding-agent session (Gemini CLI / Claude Code style).",
        mixinStandardHelpOptions = true)
final class WayangCodeCommand implements Callable<Integer> {

    // ANSI colour constants
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";
    private static final String CYAN    = "\u001B[36m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String RED     = "\u001B[31m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String BLUE    = "\u001B[34m";

    // Surface / profile constants
    private static final String CODING_SURFACE  = "coding-agent";
    private static final String CODING_PROFILE  = "coding-agent";
    private static final int    DEFAULT_STEPS   = 12;

    @ParentCommand
    WayangGollekCli parent;

    @Parameters(index = "0", arity = "0..1",
            description = "Initial prompt or task. Omit to enter interactive REPL.")
    String prompt;

    @Option(names = {"--session", "-S"},
            description = "Session id for conversational continuity. Auto-generated when omitted.")
    String sessionId;

    @Option(names = {"--workspace", "-w"},
            arity = "0..1", fallbackValue = ".",
            description = "Workspace root path (default: current directory).")
    String workspacePath;

    @Option(names = {"--model", "-m"},
            description = "Model id or backend alias.")
    String modelId;

    @Option(names = {"--profile"},
            description = "Product profile id (default: coding-agent).",
            defaultValue = CODING_PROFILE)
    String profileId;

    @Option(names = {"--no-memory"},
            description = "Disable memory context for this session.")
    boolean noMemory;

    @Option(names = {"--harness"},
            description = "Attach harness verification checks to each run.")
    boolean harness;

    @Option(names = {"--max-steps"},
            description = "Maximum agent reasoning steps per turn (default: " + DEFAULT_STEPS + ").",
            defaultValue = "" + DEFAULT_STEPS)
    int maxSteps;

    @Option(names = {"--once"},
            description = "Run once with the given prompt and exit without entering the REPL.")
    boolean once;

    @Option(names = {"--no-color"},
            description = "Disable ANSI colours.")
    boolean noColor;

    private GollekSdk sdk;
    private ChatSession chatSession;

    @Override
    public Integer call() {
        WayangCliContext ctx = parent.context();
        PrintStream out = ctx.out();

        boolean color = !noColor && isColorSupported();

        // Initialize GollekSdk locally
        try {
            this.sdk = GollekSdkFactory.createLocalSdk();
        } catch (Exception e) {
            out.println((color ? RED : "") + "Error: Failed to initialize local Gollek SDK: " + e.getMessage() + (color ? RESET : ""));
            return 1;
        }

        // Resolve model
        String resolvedModel = modelId;
        if (resolvedModel == null || resolvedModel.isBlank()) {
            try {
                resolvedModel = sdk.resolveDefaultModel().orElse("gemma-4-12B-it");
            } catch (Exception e) {
                resolvedModel = "gemma-4-12B-it";
            }
        }

        // Resolve session
        String resolvedSession = (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();

        // Initialize ChatSession
        this.chatSession = new ChatSessionImpl(sdk, resolvedModel, null, !noMemory);

        // Resolve workspace
        String resolvedWorkspace = (workspacePath != null && !workspacePath.isBlank())
                ? workspacePath : ".";
        Path workspaceDir = Paths.get(resolvedWorkspace).toAbsolutePath().normalize();

        // Print banner
        printBanner(out, color, resolvedSession, workspaceDir, resolvedModel);

        // Single-shot mode (prompt supplied + --once, or prompt supplied and not interactive)
        if (prompt != null && !prompt.isBlank() && once) {
            int code = runOnce(out, color, prompt);
            return code;
        }

        // If prompt given without --once, run it then enter REPL
        if (prompt != null && !prompt.isBlank()) {
            int code = runOnce(out, color, prompt);
            if (code != 0) {
                return code;
            }
        }

        // Interactive REPL loop
        int code = runRepl(ctx, out, color, resolvedWorkspace);

        return code;
    }

    // ─── banner ──────────────────────────────────────────────────────────────

    private void printBanner(PrintStream out, boolean color, String session, Path workspace, String resolvedModel) {
        String c = color ? CYAN + BOLD : "";
        String d = color ? DIM : "";
        String r = color ? RESET : "";
        String g = color ? GREEN : "";
        String y = color ? YELLOW : "";

        out.println();
        out.println(c + "  ██╗    ██╗ █████╗ ██╗   ██╗ █████╗ ███╗   ██╗ ██████╗" + r);
        out.println(c + "  ██║    ██║██╔══██╗╚██╗ ██╔╝██╔══██╗████╗  ██║██╔════╝" + r);
        out.println(c + "  ██║ █╗ ██║███████║ ╚████╔╝ ███████║██╔██╗ ██║██║  ███╗" + r);
        out.println(c + "  ██║███╗██║██╔══██║  ╚██╔╝  ██╔══██║██║╚██╗██║██║   ██║" + r);
        out.println(c + "  ╚███╔███╔╝██║  ██║   ██║   ██║  ██║██║ ╚████║╚██████╔╝" + r);
        out.println(c + "   ╚══╝╚══╝ ╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═══╝ ╚═════╝ " + r);
        out.println();
        out.println(g + "  Coding Agent" + r + d + "  ·  surface: coding-agent  ·  profile: " + profileId + r);
        out.println(d + "  workspace : " + workspace + r);
        out.println(d + "  session   : " + session.substring(0, Math.min(8, session.length())) + "…" + r);
        out.println(d + "  model     : " + resolvedModel + r);
        out.println();
        out.println(y + "  Type your task or question. Use /help for commands, Ctrl-C or /exit to quit." + r);
        out.println();
    }

    // ─── REPL ────────────────────────────────────────────────────────────────

    private int runRepl(WayangCliContext ctx, PrintStream out, boolean color, String workspace) {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(ctx.in(), StandardCharsets.UTF_8));

        while (true) {
            printPrompt(out, color);
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                break;
            }
            if (line == null) {
                // EOF (Ctrl-D)
                break;
            }
            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }

            // Slash commands
            if (line.startsWith("/")) {
                boolean shouldExit = handleSlashCommand(line, ctx, out, color, workspace);
                if (shouldExit) break;
                continue;
            }

            // Regular prompt → agent run
            int code = runOnce(out, color, line);
            if (code != 0) {
                printError(out, color, "Run returned exit code " + code + ". Continue? (Ctrl-C to exit)");
            }
        }

        printGoodbye(out, color);
        return 0;
    }

    /**
     * Handle a slash command. Returns {@code true} if the REPL should exit.
     */
    private boolean handleSlashCommand(String line, WayangCliContext ctx, PrintStream out,
                                       boolean color, String workspace) {
        String cmd = line.toLowerCase();
        
        if (cmd.equals("/exit") || cmd.equals("/quit") || cmd.equals("/q")) {
            return true;
        }

        if (cmd.equals("/reset")) {
            if (chatSession != null) {
                chatSession.reset();
            }
            printInfo(out, color, "Session reset.");
            return false;
        }

        if (cmd.equals("/models") || cmd.equals("/list")) {
            try {
                List<ModelInfo> models = sdk.listModels();
                if (models.isEmpty()) {
                    out.println((color ? YELLOW : "") + "  No models found." + (color ? RESET : ""));
                } else {
                    out.println();
                    out.printf(color ? BOLD + "  %-7s %-14s %-26s %-12s %-10s %-12s %-10s" + RESET + "%n"
                                    : "  %-7s %-14s %-26s %-12s %-10s %-12s %-10s%n",
                            "ID", "GROUP", "NAME", "ARCH", "FORMAT", "SIZE", "MODIFIED");
                    out.println(color ? DIM + "  " + "─".repeat(97) + RESET : "  " + "─".repeat(97));
                    for (ModelInfo model : models) {
                        String id = model.getShortId();
                        if (id == null || id.isBlank() || id.equalsIgnoreCase("n/a")) {
                            id = tech.kayys.gollek.spi.model.ModelUtils.generateShortId(model.getModelId());
                        }
                        String group = "";
                        String displayName = model.getName() != null ? model.getName() : model.getModelId();
                        String modelIdStr = model.getModelId();
                        if (modelIdStr != null && modelIdStr.contains("/")) {
                            int slash = modelIdStr.indexOf('/');
                            group = modelIdStr.substring(0, slash);
                        }
                        String arch = model.getArchitecture() != null ? model.getArchitecture() : "unknown";
                        String modified = model.getUpdatedAt() != null ? model.getUpdatedAt().toString().substring(0, 10) : "N/A";
                        String format = model.getFormat() != null ? model.getFormat() : "N/A";
                        
                        out.printf("  %-7s %-14s %-26s %-12s %-10s %-12s %-10s%n",
                                color ? YELLOW + id + RESET : id,
                                truncate(group, 14),
                                truncate(displayName, 26),
                                truncate(arch, 12),
                                truncate(format, 10),
                                model.getSizeFormatted(),
                                modified);
                    }
                    out.printf(color ? BOLD + "%n  %d model(s) found" + RESET + "%n" : "%n  %d model(s) found%n", models.size());
                }
            } catch (Exception e) {
                e.printStackTrace();
                printError(out, color, "Failed to list models: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/model ")) {
            String newModelId = cmd.substring(7).trim();
            if (newModelId.isEmpty()) {
                out.println((color ? YELLOW : "") + "  Usage: /model <model-id>" + (color ? RESET : ""));
            } else {
                try {
                    this.chatSession = new ChatSessionImpl(sdk, newModelId, null, !noMemory);
                    out.println(color ? GREEN + "  Switched to model: " + RESET + CYAN + newModelId + RESET
                                      : "  Switched to model: " + newModelId);
                } catch (Exception e) {
                    printError(out, color, "Failed to switch model: " + e.getMessage());
                }
            }
            return false;
        }

        if (cmd.equals("/providers")) {
            try {
                List<tech.kayys.gollek.spi.provider.ProviderInfo> providers = sdk.listAvailableProviders();
                if (providers.isEmpty()) {
                    out.println("  No providers found.");
                } else {
                    out.println();
                    out.printf("  %-15s %-15s %-30s%n", "ID", "NAME", "DESCRIPTION");
                    out.println("  " + "─".repeat(60));
                    for (tech.kayys.gollek.spi.provider.ProviderInfo p : providers) {
                        out.printf("  %-15s %-15s %-30s%n", p.id(), p.name(), p.description());
                    }
                }
            } catch (Exception e) {
                printError(out, color, "Failed to list providers: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/provider ")) {
            String newProviderId = cmd.substring(10).trim();
            if (newProviderId.isEmpty()) {
                out.println("  Usage: /provider <provider-id>");
            } else {
                try {
                    sdk.setPreferredProvider(newProviderId);
                    this.chatSession = new ChatSessionImpl(sdk, chatSession.getModelId(), newProviderId, !noMemory);
                    out.println(color ? GREEN + "  Switched to provider: " + RESET + CYAN + newProviderId + RESET
                                      : "  Switched to provider: " + newProviderId);
                } catch (Exception e) {
                    printError(out, color, "Failed to switch provider: " + e.getMessage());
                }
            }
            return false;
        }

        if (cmd.equals("/info")) {
            try {
                tech.kayys.gollek.sdk.model.SystemInfo info = sdk.getSystemInfo();
                out.println();
                out.println(color ? BOLD + "  System Info:" + RESET : "  System Info:");
                out.println("    OS:      " + info.getOsName() + " (" + info.getOsArch() + ")");
                out.println("    Java:    " + info.getJavaVersion());
                out.println("    Memory:  " + (info.getTotalMemory() / (1024 * 1024)) + " MB / " + (info.getMaxMemory() / (1024 * 1024)) + " MB");
            } catch (Exception e) {
                printError(out, color, "Failed to get system info: " + e.getMessage());
            }
            return false;
        }

        if (cmd.equals("/status")) {
            runSubcommand(out, color, "platform status", () -> {
                WayangPlatformCommands.StatusCommand sub = new WayangPlatformCommands.StatusCommand();
                sub.parent = parent;
                sub.call();
            });
            return false;
        }

        if (cmd.equals("/workspace") || cmd.equals("/ws")) {
            runSubcommand(out, color, "workspace", () -> {
                WayangContextCommands.WorkspaceCommand sub = new WayangContextCommands.WorkspaceCommand();
                sub.parent = parent;
                sub.path = workspace;
                sub.maxEntries = 80;
                sub.call();
            });
            return false;
        }

        if (cmd.equals("/harness") || cmd.equals("/checks")) {
            runSubcommand(out, color, "harness checks", () -> {
                WayangContextCommands.HarnessCommand sub = new WayangContextCommands.HarnessCommand();
                sub.parent = parent;
                sub.path = workspace;
                sub.maxChecks = 8;
                sub.call();
            });
            return false;
        }

        if (cmd.equals("/help") || cmd.equals("/?")) {
            printHelp(out, color);
            return false;
        }

        printError(out, color, "Unknown slash command: " + line + "  (try /help)");
        return false;
    }

    // ─── single run ──────────────────────────────────────────────────────────

    private int runOnce(PrintStream out, boolean color, String userPrompt) {
        printThinking(out, color);
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] assistantPrefixPrinted = { false };
        int[] exitCode = { 0 };

        try {
            // Use chatSession.stream(String) so that getHistoryWithPrompt() is called,
            // which adds the user message to the list before the request is built.
            // Building a raw InferenceRequest with .prompt() leaves messages empty,
            // causing "At least one message is required" from downstream providers.
            chatSession.stream(userPrompt)
                    .subscribe().with(
                            chunk -> {
                                String delta = chunk.getDelta();
                                if (delta != null && !delta.isEmpty()) {
                                    if (!assistantPrefixPrinted[0]) {
                                        out.println();
                                        out.print(color ? GREEN + BOLD + "Assistant: " + RESET : "Assistant: ");
                                        assistantPrefixPrinted[0] = true;
                                    }
                                    out.print(delta);
                                    out.flush();
                                }
                            },
                            error -> {
                                out.println();
                                printError(out, color, "Inference error: " + error.getMessage());
                                exitCode[0] = 1;
                                latch.countDown();
                            },
                            () -> {
                                out.println();
                                latch.countDown();
                            }
                    );

            latch.await();
            return exitCode[0];
        } catch (Exception e) {
            printError(out, color, "Failed to execute inference: " + e.getMessage());
            return 1;
        }
    }

    // ─── output helpers ──────────────────────────────────────────────────────

    private void printPrompt(PrintStream out, boolean color) {
        String c = color ? MAGENTA + BOLD : "";
        String r = color ? RESET : "";
        out.print(c + "❯ wayang " + r);
        out.flush();
    }

    private void printThinking(PrintStream out, boolean color) {
        String c = color ? DIM + CYAN : "";
        String r = color ? RESET : "";
        out.print(c + "  ⠿ thinking…" + r);
        out.flush();
    }

    private void printInfo(PrintStream out, boolean color, String msg) {
        String c = color ? CYAN : "";
        String r = color ? RESET : "";
        out.println(c + "  ℹ " + msg + r);
    }

    private void printError(PrintStream out, boolean color, String msg) {
        String c = color ? RED : "";
        String r = color ? RESET : "";
        out.println(c + "  ✗ " + msg + r);
    }

    private void printGoodbye(PrintStream out, boolean color) {
        String c = color ? DIM : "";
        String r = color ? RESET : "";
        out.println();
        out.println(c + "  Goodbye. Run 'wayang code' to start a new session." + r);
        out.println();
    }

    private void printHelp(PrintStream out, boolean color) {
        String h = color ? BOLD + YELLOW : "";
        String d = color ? DIM : "";
        String r = color ? RESET : "";
        out.println();
        out.println(h + "  Slash commands:" + r);
        out.println(d + "    /exit  /quit  /q     Exit the session" + r);
        out.println(d + "    /reset               Start a new session (new session id)" + r);
        out.println(d + "    /models  /list       List available local models in Gollek" + r);
        out.println(d + "    /model <model-id>    Switch to a different model" + r);
        out.println(d + "    /providers           List available LLM providers" + r);
        out.println(d + "    /provider <id>       Switch to a different provider" + r);
        out.println(d + "    /info                Display system info" + r);
        out.println(d + "    /status              Show platform boundary and adapter status" + r);
        out.println(d + "    /workspace  /ws      Show workspace snapshot for the current path" + r);
        out.println(d + "    /harness  /checks    Show planned verification checks" + r);
        out.println(d + "    /help  /?            Show this help" + r);
        out.println();
        out.println(d + "  Options: see 'wayang code --help' for model, session, workspace, etc." + r);
        out.println();
    }

    private String truncate(String str, int maxLen) {
        if (str == null)
            return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 3) + "..." : str;
    }

    // ─── utilities ───────────────────────────────────────────────────────────

    /** Run a picocli sub-command inline and swallow its return code. */
    private void runSubcommand(PrintStream out, boolean color, String name, Runnable task) {
        String c = color ? DIM + CYAN : "";
        String r = color ? RESET : "";
        out.println(c + "  ─── " + name + " ───" + r);
        try {
            task.run();
        } catch (RuntimeException e) {
            printError(out, color, "Failed to run /" + name + ": " + e.getMessage());
        }
        out.println();
    }

    private static boolean isColorSupported() {
        // Disable colour when output is piped or NO_COLOR env var is set
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null) return false;
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) return false;
        return System.console() != null || System.getenv("COLORTERM") != null
                || System.getenv("TERM_PROGRAM") != null;
    }
}
