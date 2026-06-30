package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import tech.kayys.wayang.gollek.cli.GollekSdkAdapter;
import tech.kayys.wayang.gollek.cli.ShellChatSession;
import tech.kayys.wayang.gollek.sdk.WayangCodeAgentContext;
import tech.kayys.wayang.gollek.sdk.WayangCodeAgentExtensionDiscovery;
import tech.kayys.wayang.gollek.sdk.WayangCodeAgentExtensions;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.sdk.gollek.ProjectStore;
import tech.kayys.wayang.sdk.gollek.model.Project;
import tech.kayys.wayang.sdk.gollek.tools.Scanner;
import tech.kayys.wayang.sdk.gollek.tools.Grep;
import tech.kayys.wayang.sdk.gollek.tools.PlannerIface;
import tech.kayys.wayang.sdk.gollek.tools.TaskStoreIface;
import tech.kayys.wayang.sdk.gollek.tools.ToolsFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
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

    @Option(names = {"--project", "-p"},
            description = "Project id or name to group sessions (defaults to workspace-based project).")
    String projectId;

    @Option(names = {"--workspace", "-w"},
            arity = "0..1", fallbackValue = ".",
            description = "Workspace root path (default: current directory).")
    String workspacePath;

    @Option(names = {"--model", "-m"},
            description = "Model id or backend alias.")
    String modelId;

    @Option(names = {"--provider"}, description = "Preferred Gollek provider id (e.g., 'cerebras').")
    String providerId;

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

    @Option(names = {"--format"},
            description = "Output format (text, json-stream). Default is text.",
            defaultValue = "text")
    String format;

    private GollekSdkAdapter sdkAdapter;
    private Object chatSession;

    // Project/session persistence manager (SDK ProjectStore)
    private tech.kayys.wayang.sdk.gollek.ProjectStore projectStore;
    private String resolvedProjectKey;
    // Current active session id (resolved/auto-generated)
    private String currentSessionId;

    // ToolsFactory loaded via ServiceLoader for pluggable tool implementations
    private ToolsFactory toolsFactory;
    private WayangCodeAgentContext codeAgentContext;
    private WayangCodeAgentExtensionDiscovery codeExtensionDiscovery =
            WayangCodeAgentExtensionDiscovery.empty();

    @Override
    public Integer call() {
        WayangCliContext ctx = parent.context();
        PrintStream out = ctx.out();

        // Debug: show parsed options at entry (only when wayang.cli.debug=true)
        if (Boolean.getBoolean("wayang.cli.debug")) {
            try { System.err.println("[DEBUG] WayangCodeCommand.call() prompt='" + prompt + "' model='" + modelId + "' provider='" + providerId + "' once='" + once + "'"); } catch (Throwable ignore) {}
            try { if (parent != null && parent.context() != null) parent.context().out().println("  Debug: WayangCodeCommand.call() prompt='" + prompt + "' model='" + modelId + "' provider='" + providerId + "' once='" + once + "'"); } catch (Throwable ignore) {}
        }

        boolean color = !noColor && isColorSupported();

        // Initialize GollekSdk locally — with config.json authoritative unless --ignore-config
        try {
            String effectiveProvider = providerId;
            try {
                WayangGollekSdk wayangSdk = parent.sdk();
                if (!parent.isIgnoreConfig()) {
                    try {
                        java.util.Optional<String> p = wayangSdk.getPreferredProvider();
                        if (p.isPresent()) {
                            // config.json preferred provider overrides CLI flag
                            effectiveProvider = p.get();
                        }
                    } catch (Throwable ignore) {
                        // ignore if parent SDK doesn't expose preferred provider
                    }
                } else {
                    // ignore-config: only use parent provider if CLI flag absent
                    if (effectiveProvider == null || effectiveProvider.isBlank()) {
                        try {
                            java.util.Optional<String> p = wayangSdk.getPreferredProvider();
                            if (p.isPresent()) {
                                effectiveProvider = p.get();
                            }
                        } catch (Throwable ignore) {}
                    }
                }
            } catch (Throwable ignore) {
                // parent.sdk() may not be accessible; fall back to CLI flag only
            }

            // Use lightweight adapter that does not require full Gollek SDK at runtime.
            this.sdkAdapter = GollekSdkAdapter.create(effectiveProvider);
            this.providerId = effectiveProvider;
        } catch (Exception e) {
            out.println((color ? RED : "") + "Error: Failed to initialize local Gollek SDK: " + e.getMessage() + (color ? RESET : ""));
            return 1;
        }

        // Resolve model: --model CLI flag always wins; config/env only used as fallback
        // when no explicit --model is provided.
        String resolvedModel = modelId;
        try {
            if (resolvedModel == null || resolvedModel.isBlank()) {
                // No --model provided: use config, then env
                if (!parent.isIgnoreConfig()) {
                    java.util.Optional<String> cfg = sdkAdapter.resolveDefaultModel();
                    if (cfg.isPresent()) {
                        resolvedModel = cfg.get();
                    }
                }
                if (resolvedModel == null || resolvedModel.isBlank()) {
                    String env = System.getenv("WAYANG_MODEL");
                    if (env != null && !env.isBlank()) resolvedModel = env.trim();
                }
            }
            // else: --model was provided, use it as-is
        } catch (Exception e) {
            // continue to fallback below
        }

        // Resolve session
        String resolvedSession = (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();
        // persist resolved id on the command object for runOnce to access
        this.sessionId = resolvedSession;


        tech.kayys.wayang.tui.ui.ModelManager modelManager = new tech.kayys.wayang.tui.ui.ModelManager() {
            @Override
            public java.util.List<ModelRow> listModels() {
                java.util.List<ModelRow> rows = new java.util.ArrayList<>();
                for (GollekSdkAdapter.ModelRow r : sdkAdapter.listModelsStructured()) {
                    rows.add(new ModelRow(r.shortId(), r.name(), r.format(), r.sizeStr()));
                }
                return rows;
            }

            @Override
            public int pullModel(java.io.PrintStream out, String modelSpec) {
                return tech.kayys.wayang.gollek.sdk.WayangGollekFacade.pullModel(out, modelSpec);
            }
        };

        // Resolve workspace
        String resolvedWorkspace = (workspacePath != null && !workspacePath.isBlank())
                ? workspacePath : ".";
        Path workspaceDir = resolveWorkspacePath(resolvedWorkspace);

        // Initialize project/session manager for grouping
        this.toolsFactory = null;
        try {
            this.resolvedProjectKey = tech.kayys.wayang.gollek.sdk.session.WayangSessionStore.computeProjectKey(projectId, workspaceDir);
            this.projectStore = new tech.kayys.wayang.sdk.gollek.ProjectStore(null);
            // Ensure project exists (create if missing)
            boolean exists = this.projectStore.listProjects().stream().anyMatch(p -> p.id().equals(resolvedProjectKey));
            if (!exists) {
                this.projectStore.createProject(resolvedProjectKey, workspaceDir.getFileName() != null ? workspaceDir.getFileName().toString() : resolvedProjectKey, workspaceDir.toAbsolutePath().toString());
            }
        } catch (Exception e) {
            printInfo(out, color, "Warning: failed to initialize project persistence: " + e.getMessage());
            this.projectStore = null;
            this.resolvedProjectKey = null;
        }

        refreshCodeAgentExtensions(workspaceDir, resolvedModel, resolvedSession, providerId);

        this.toolsFactory = WayangCodeToolFactories.discover().orElseGet(WayangCodeToolFactories::fallback);
        if (this.toolsFactory instanceof WayangCodeFallbackToolsFactory) {
            printInfo(out, color, "Using fallback ToolsFactory (no implementations found via ServiceLoader).");
        }

        // Initialize ChatSession with the durable Wayang coding-agent system prompt.
        try {
            // Best-effort: apply preferred provider into lightweight adapter
            try { sdkAdapter.setPreferredProvider(providerId); } catch (Throwable ignore) {}
        } catch (Throwable ignored) {}
        this.chatSession = createChatSession(resolvedModel, providerId, workspaceDir);
        // If a session id was provided and persistence is available, attempt to resume
        if (projectStore != null && sessionId != null && !sessionId.isBlank()) {
            try {
                java.util.List<?> transcript = projectStore.loadTranscript(resolvedProjectKey, resolvedSession);
                if (transcript != null && !transcript.isEmpty()) {
                    for (Object m : transcript) {
                        chatSessionAddMessage(chatSession, m);
                    }
                    printInfo(out, color, "Resumed session " + resolvedSession + " (" + transcript.size() + " messages)");
                }
            } catch (Exception e) {
                printInfo(out, color, "Warning: failed to resume session: " + e.getMessage());
            }
        }

        // Print banner
        if (!"json-stream".equalsIgnoreCase(format)) {
            printBanner(out, color, resolvedSession, workspaceDir, resolvedModel);
        }

        if ("json-stream".equalsIgnoreCase(format)) {
            return runJsonStreamLoop(ctx, out, resolvedModel, workspaceDir);
        }

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

        // Initialize agentic-tui components
        try {
            tech.kayys.wayang.tui.config.Config tuiConfig = new tech.kayys.wayang.tui.config.Config();
            tech.kayys.wayang.tui.config.Config.Profile profile = new tech.kayys.wayang.tui.config.Config.Profile();
            profile.name = profileId;
            profile.model = resolvedModel;
            Object promptObj = tech.kayys.wayang.gollek.cli.WayangCodePromptComposer.systemPrompt(new tech.kayys.wayang.gollek.cli.WayangCodePromptContext(
                    profileId,
                    workspaceDir,
                    resolvedModel,
                    !noMemory,
                    harness,
                    maxSteps),
                    codeExtensionDiscovery == null ? java.util.List.of() : codeExtensionDiscovery.promptAdditions());
            profile.systemPrompt = promptObj == null ? "You are a helpful coding assistant." : promptObj.toString();
            tuiConfig.profiles.add(profile);
            tuiConfig.activeProfile = profile.name;

            tech.kayys.wayang.sdk.provider.Provider tuiProvider = new tech.kayys.wayang.gollek.cli.code.WayangProvider(resolvedModel);
            
            tech.kayys.wayang.sdk.agent.WayangAgent tuiAgent = new tech.kayys.wayang.sdk.agent.WayangAgentBuilder()
                    .provider(tuiProvider)
                    .registerOsTools()
                    .addAllTools(tech.kayys.wayang.gollek.cli.code.WayangCodeSkillAdapter.discoverSkills(workspaceDir))
                    .addAllTools(tech.kayys.wayang.gollek.cli.code.WayangCodeMcpAdapter.discoverMcpTools(workspaceDir))
                    .systemPrompt(profile.systemPrompt)
                    .temperature(profile.temperature)
                    .maxTokens(profile.maxTokens)
                    .autoApproveTools(profile.autoApproveTools)
                    .workspace(workspaceDir)
                    .build();

            // Interactive REPL loop
            tech.kayys.wayang.tui.ui.ReplUi ui = new tech.kayys.wayang.tui.ui.ReplUi(tuiConfig, tuiAgent, modelManager);
            ui.run();

        } catch (Exception e) {
            printError(out, color, "Error starting TUI: " + e.getMessage());
            return 1;
        }

        return 0;
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
        out.println(d + "  session   : " + session.substring(0, Math.min(8, session.length())) + "\u2026" + r);
        String modelDisplay = (resolvedModel == null || resolvedModel.isBlank())
                ? y + "No model selected" + r + d + "  (use /models to select, or pass --model <id>)" : resolvedModel;
        out.println(d + "  model     : " + modelDisplay + r);
        if (codeExtensionDiscovery != null && codeExtensionDiscovery.discoveredCount() > 0) {
            out.println(d + "  extensions: " + codeExtensionDiscovery.activeCount()
                    + "/" + codeExtensionDiscovery.discoveredCount() + " active" + r);
        }
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
        String command = line == null ? "" : line.trim();
        String cmd = command.toLowerCase();
        
        if (cmd.equals("/exit") || cmd.equals("/quit") || cmd.equals("/q")) {
            return true;
        }

        if (cmd.equals("/reset")) {
            if (chatSession != null) {
                chatSessionReset(chatSession);
                applyCodingAgentPrompt(chatSession, chatSessionGetModelId(chatSession), workspace);
            }
            printInfo(out, color, "Session reset.");
            return false;
        }

        if (cmd.equals("/models") || cmd.equals("/list")) {
            try {
                List<String> models = sdkAdapter.listModelsStrings();
                if (models == null || models.isEmpty()) {
                    out.println((color ? YELLOW : "") + "  No models found." + (color ? RESET : ""));
                } else {
                    out.println();
                    for (String mi : models) {
                        out.println("  " + mi);
                    }
                    out.println();
                    out.println("  " + models.size() + " model(s) found");
                }
            } catch (Exception e) {
                printError(out, color, "Failed to list models: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/model ")) {
            String newModelId = command.substring(7).trim();
            if (newModelId.isEmpty()) {
                out.println((color ? YELLOW : "") + "  Usage: /model <model-id>" + (color ? RESET : ""));
            } else {
                try {
                    String provider = chatSession == null ? null : chatSessionGetProviderId(chatSession);
                    refreshCodeAgentExtensions(resolveWorkspacePath(workspace), newModelId, currentSessionId, provider);
                    this.chatSession = createChatSession(newModelId, provider, resolveWorkspacePath(workspace));
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
                java.util.List<String> providers = sdkAdapter.listAvailableProviders();
                if (providers.isEmpty()) {
                    out.println("  No providers found.");
                } else {
                    out.println();
                    out.printf("  %-15s %-15s %-30s%n", "ID", "NAME", "DESCRIPTION");
                    out.println("  " + "─".repeat(60));
                    for (String pid : providers) {
                        out.printf("  %-15s %-15s %-30s%n", pid, "-", "");
                    }
                }
            } catch (Exception e) {
                printError(out, color, "Failed to list providers: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/provider ")) {
            String newProviderId = command.substring(10).trim();
            if (newProviderId.isEmpty()) {
                out.println("  Usage: /provider <provider-id>");
            } else {
                try {
                    // Validate provider exists via Gollek SDK
                    java.util.List<String> available = sdkAdapter.listAvailableProviders();
                    boolean found = false;
                    if (available != null) {
                        for (var p : available) { if (p.equals(newProviderId)) { found = true; break; } }
                    }
                    if (!found) {
                        String known = available == null ? "<unknown>" : available.toString();
                        printError(out, color, "Provider '" + newProviderId + "' not found. Known providers: " + known);
                    } else {
                        sdkAdapter.setPreferredProvider(newProviderId);
                        String currentModel = chatSession == null ? modelId : chatSessionGetModelId(chatSession);
                        refreshCodeAgentExtensions(resolveWorkspacePath(workspace), currentModel, currentSessionId, newProviderId);
                        this.chatSession = createChatSession(currentModel, newProviderId, resolveWorkspacePath(workspace));
                        out.println(color ? GREEN + "  Switched to provider: " + RESET + CYAN + newProviderId + RESET
                                          : "  Switched to provider: " + newProviderId);
                    }
                } catch (Exception e) {
                    printError(out, color, "Failed to switch provider: " + e.getMessage());
                }
            }
            return false;
        }

        if (cmd.equals("/info")) {
            try {
                Object info = sdkAdapter.getSystemInfo();
                out.println();
                out.println(color ? BOLD + "  System Info:" + RESET : "  System Info:");
                try {
                    if (info instanceof java.util.Map) {
                        java.util.Map<?,?> m = (java.util.Map<?,?>) info;
                        Object osv = m.get("os");
                        Object javav = m.get("java");
                        Object totalMem = m.get("totalMemory");
                        Object maxMem = m.get("maxMemory");
                        out.println("    OS:      " + (osv == null ? "<unknown>" : String.valueOf(osv)));
                        out.println("    Java:    " + (javav == null ? "<unknown>" : String.valueOf(javav)));
                        long tMB = (totalMem instanceof Number) ? ((Number) totalMem).longValue() / (1024 * 1024) : -1L;
                        long mMB = (maxMem instanceof Number) ? ((Number) maxMem).longValue() / (1024 * 1024) : -1L;
                        out.println("    Memory:  " + (tMB >= 0 ? tMB + " MB" : "?") + " / " + (mMB >= 0 ? mMB + " MB" : "?"));
                    } else {
                        out.println("    Info: " + String.valueOf(info));
                    }
                } catch (Throwable ignored) {}

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

        if (cmd.equals("/extensions") || cmd.equals("/ext")) {
            WayangCodeExtensionTextFormat.render(out, color, codeExtensionDiscovery);
            return false;
        }

        if (cmd.equals("/projects") || cmd.equals("/project list")) {
            try {
                ProjectStore store = new ProjectStore(null);
                var projects = store.listProjects();
                String current = null;
                try { current = store.currentProject(); } catch (Exception ignored) {}
                out.println();
                out.println(color ? BOLD + "  Projects:" + RESET : "  Projects:");
                for (var p : projects) {
                    String marker = (current != null && current.equals(p.id())) ? "  (current)" : "";
                    out.println("    " + p.id() + " - " + p.name() + " (" + p.directory() + ")" + marker);
                }
                out.println();
                out.println("    Use '/project <id>' to switch or 'wayang project switch <id>' from shell.");
            } catch (Exception e) {
                printError(out, color, "Failed to list projects: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/project ")) {
            String arg = command.substring(9).trim();
            if (arg.isEmpty()) {
                printInfo(out, color, "Usage: /project <project-id>");
                return false;
            }
            try {
                this.resolvedProjectKey = arg;
                try {
                    ProjectStore store = new ProjectStore(null);
                    store.switchProject(resolvedProjectKey);
                    printInfo(out, color, "Switched project to: " + resolvedProjectKey);
                } catch (Exception ex) {
                    // Save pointer fallback
                    try {
                        java.nio.file.Path cfg = java.nio.file.Path.of(System.getProperty("user.home"), ".wayang", "current_project.txt");
                        java.nio.file.Files.createDirectories(cfg.getParent());
                        java.nio.file.Files.writeString(cfg, resolvedProjectKey);
                        printInfo(out, color, "Switched project to: " + resolvedProjectKey);
                    } catch (Exception e2) {
                        printError(out, color, "Failed to switch project: " + e2.getMessage());
                    }
                }
            } catch (Exception e) {
                printError(out, color, "Failed to switch project: " + e.getMessage());
            }
            return false;
        }

        if (cmd.equals("/sessions") || cmd.equals("/sessions list") ) {
            if (projectStore == null || !isGollekSpiAvailable()) {
                printInfo(out, color, "Session persistence unavailable.");
                return false;
            }
            try {
                List<String> sessions = projectStore.listSessions(resolvedProjectKey);
                out.println();
                out.println(color ? BOLD + "  Sessions (project: " + resolvedProjectKey + "):" + RESET : "  Sessions:");
                for (String s : sessions) {
                    out.println("    " + s);
                }
                out.println();
            } catch (Exception e) {
                printError(out, color, "Failed to list sessions: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/sessions resume ")) {
            String sid = command.substring("/sessions resume ".length()).trim();
            if (sid.isEmpty()) { printInfo(out, color, "Usage: /sessions resume <session-id>"); return false; }
            if (projectStore == null) { printInfo(out, color, "Session persistence unavailable."); return false; }
            try {
                var transcript = projectStore.loadTranscript(resolvedProjectKey, sid);
                if (transcript == null || transcript.isEmpty()) { printInfo(out, color, "No transcript found for " + sid); return false; }
                for (var m : transcript) chatSessionAddMessage(chatSession, m);
                this.currentSessionId = sid;
                printInfo(out, color, "Resumed session " + sid + " (" + transcript.size() + " messages)");
            } catch (Exception e) {
                printError(out, color, "Failed to resume session: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/sessions fork ") || cmd.startsWith("/sessions clone ")) {
            String rest = command.substring(command.indexOf(' ') + 1).trim();
            // rest should be "sessions fork <id> [new-name]" so extract after the subcommand
            String payload = command.startsWith("/sessions fork ") ? command.substring("/sessions fork ".length()).trim() : command.substring("/sessions clone ".length()).trim();
            if (payload.isEmpty()) { printInfo(out, color, "Usage: /sessions fork <session-id> [new-name]"); return false; }
            if (projectStore == null) { printInfo(out, color, "Session persistence unavailable."); return false; }
            try {
                // Tokenize payload to support optional --checkpoint flag after session-id and new name
                String[] tokens = payload.split("\\s+");
                if (tokens.length == 0) { printInfo(out, color, "Usage: /sessions fork <session-id> [new-name] [--checkpoint <index>]"); return false; }
                String sid = tokens[0];
                String newName = null;
                Integer checkpointIndex = null;
                // Reconstruct remaining args and parse options
                for (int i = 1; i < tokens.length; i++) {
                    String t = tokens[i];
                    if (t.startsWith("--checkpoint=")) {
                        try { checkpointIndex = Integer.parseInt(t.substring("--checkpoint=".length())); } catch (NumberFormatException ignored) {}
                    } else if (t.equals("--checkpoint") && i + 1 < tokens.length) {
                        try { checkpointIndex = Integer.parseInt(tokens[++i]); } catch (NumberFormatException ignored) {}
                    } else {
                        // treat as part of newName (allow multi-word name)
                        if (newName == null) newName = t;
                        else newName = newName + " " + t;
                    }
                }
                var newSession = projectStore.cloneSession(resolvedProjectKey, sid, newName, checkpointIndex);
                if (newSession != null) {
                    String ck = checkpointIndex == null ? "full" : ("checkpoint=" + checkpointIndex);
                    printInfo(out, color, "Forked session " + sid + " -> " + newSession.id() + " (name='" + newSession.name() + "', branch=" + ck + ")");
                } else {
                    printInfo(out, color, "Failed to fork session " + sid);
                }
            } catch (Exception e) {
                printError(out, color, "Failed to fork session: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/sessions delete ")) {
            String sid = command.substring("/sessions delete ".length()).trim();
            if (sid.isEmpty()) { printInfo(out, color, "Usage: /sessions delete <session-id>"); return false; }
            if (projectStore == null) { printInfo(out, color, "Session persistence unavailable."); return false; }
            try {
                boolean deleted = projectStore.deleteSession(resolvedProjectKey, sid);
                if (deleted) printInfo(out, color, "Deleted session " + sid);
                else printInfo(out, color, "No session found: " + sid);
            } catch (Exception e) {
                printError(out, color, "Failed to delete session: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/find ") || cmd.startsWith("/files ")) {
            String pattern = command.substring(command.indexOf(' ')).trim();
            if (pattern.isEmpty()) { printInfo(out, color, "Usage: /find <glob> (e.g. '**/*.java')"); return false; }
            try {
                Path ws = resolveWorkspacePath(workspacePath);
                Scanner scanner = toolsFactory.createScanner(ws);
                var files = scanner.findFiles(pattern);
                out.println();
                out.println(color ? BOLD + "  Files:" + RESET : "  Files:");
                for (var f : files) out.println("    " + ws.relativize(f).toString());
                out.println();
            } catch (Exception e) {
                printError(out, color, "Find failed: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/grep ")) {
            String regex = command.substring("/grep ".length()).trim();
            if (regex.isEmpty()) { printInfo(out, color, "Usage: /grep <regex>"); return false; }
            try {
                Path ws = resolveWorkspacePath(workspacePath);
                Scanner scanner = toolsFactory.createScanner(ws);
                var files = scanner.findFiles("**/*");
                Grep greper = toolsFactory.createGrep(ws);
                var matches = greper.grep(regex, files);
                out.println();
                out.println(color ? BOLD + "  Matches:" + RESET : "  Matches:");
                int shown = 0;
                for (var m : matches) {
                    out.println("    " + ws.relativize(m.file) + ":" + m.lineNumber + "  " + truncate(m.line, 240));
                    if (++shown >= 200) break;
                }
                out.println();
            } catch (Exception e) {
                printError(out, color, "Grep failed: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/plan ")) {
            String goal = command.substring("/plan ".length()).trim();
            if (goal.isEmpty()) { printInfo(out, color, "Usage: /plan <goal-description>"); return false; }
            try {
                PlannerIface p = toolsFactory.createPlanner();
                var steps = p.makePlan(goal);
                out.println();
                out.println(color ? BOLD + "  Plan:" + RESET : "  Plan:");
                for (var s : steps) {
                    out.println("    " + s.index + ". " + s.text);
                }
                out.println();
                // persist as tasks under project if available
                try {
                    Path projectDir = Paths.get(System.getProperty("user.home"), ".wayang", "projects", resolvedProjectKey == null ? "default" : resolvedProjectKey);
                    TaskStoreIface ts = toolsFactory.createTaskStore(projectDir);
                    for (var s : steps) ts.addTask(s.index + ". " + s.text);
                    printInfo(out, color, "Plan saved as tasks under project: " + (resolvedProjectKey == null ? "default" : resolvedProjectKey));
                } catch (Throwable ignored) {}
            } catch (Exception e) {
                printError(out, color, "Plan failed: " + e.getMessage());
            }
            return false;
        }

        if (cmd.startsWith("/tasks ")) {
            String rest = command.substring("/tasks ".length()).trim();
            if (rest.equals("list")) {
                try {
                    Path projectDir = Paths.get(System.getProperty("user.home"), ".wayang", "projects", resolvedProjectKey == null ? "default" : resolvedProjectKey);
                    TaskStoreIface ts = toolsFactory.createTaskStore(projectDir);
                    var tasks = ts.listTasks();
                    out.println();
                    out.println(color ? BOLD + "  Tasks:" + RESET : "  Tasks:");
                    for (var t : tasks) out.println("    " + t.id() + " [" + t.status() + "] " + t.description());
                    out.println();
                } catch (Exception e) { printError(out, color, "Tasks list failed: " + e.getMessage()); }
                return false;
            } else if (rest.startsWith("add ")) {
                String desc = rest.substring("add ".length()).trim();
                try {
                    Path projectDir = Paths.get(System.getProperty("user.home"), ".wayang", "projects", resolvedProjectKey == null ? "default" : resolvedProjectKey);
                    TaskStoreIface ts = toolsFactory.createTaskStore(projectDir);
                    var t = ts.addTask(desc);
                    printInfo(out, color, "Added task: " + t.id());
                } catch (Exception e) { printError(out, color, "Add task failed: " + e.getMessage()); }
                return false;
            } else if (rest.startsWith("done ")) {
                String id = rest.substring("done ".length()).trim();
                try {
                    Path projectDir = Paths.get(System.getProperty("user.home"), ".wayang", "projects", resolvedProjectKey == null ? "default" : resolvedProjectKey);
                    TaskStoreIface ts = toolsFactory.createTaskStore(projectDir);
                    var tasks = ts.listTasks();
                    for (var t : tasks) if (t.id().equals(id)) { t.setStatus("done"); ts.updateTask(t); printInfo(out, color, "Marked done: " + id); return false; }
                    printInfo(out, color, "Task not found: " + id);
                } catch (Exception e) { printError(out, color, "Mark done failed: " + e.getMessage()); }
                return false;
            }
        }

        if (cmd.startsWith("/help") || cmd.startsWith("/?")) {
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
        final boolean[] fallbackTried = { false }; // ensure we only attempt subprocess fallback once per request

        try {
            // Defensive: if the active chat session uses safetensor as provider,
            // attempt to prefer gguf and recreate the session so local GGUF models work.
            try {
                String activeProvider = null;
                try {
                    activeProvider = chatSession == null ? null : chatSessionGetProviderId(chatSession);
                } catch (Throwable ignored) {}
                if (activeProvider != null && activeProvider.equalsIgnoreCase("safetensor")) {
                    try {
                        try { sdkAdapter.setPreferredProvider("gguf"); } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {}
                    // Recreate chat session with gguf preference
                    try {
                        String currentModel = chatSession == null ? modelId : chatSessionGetModelId(chatSession);
                        this.chatSession = createChatSession(currentModel, "gguf", resolveWorkspacePath(workspacePath));
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            // Use chatSession.stream(String) so that getHistoryWithPrompt() is called,
            // which adds the user message to the list before the request is built.
            // Building a raw InferenceRequest with .prompt() leaves messages empty,
            // causing "At least one message is required" from downstream providers.
            chatSessionStream(chatSession, userPrompt,
                    chunk -> {
                        String delta = chunk.getDelta();
                        if (delta != null && !delta.isEmpty()) {
                            if (!assistantPrefixPrinted[0]) {
                                // Erase the "⠿ thinking…" spinner line
                                out.print("\r" + " ".repeat(20) + "\r");
                                out.flush();
                                out.print(color ? GREEN + BOLD + "Assistant: " + RESET : "");
                                assistantPrefixPrinted[0] = true;
                            }
                            out.print(delta);
                            out.flush();
                        }
                    },
                    error -> {
                        out.println();
                        String msg = error == null ? "" : error.getMessage();
                        // If SDK failed because provider missing for the model, attempt subprocess fallback
                        if (!fallbackTried[0]) {
                            fallbackTried[0] = true;
                            System.err.println("[wayang] Attempting subprocess fallback due to inference error: " + msg);
                            try {
                                String currentModel = chatSession == null ? modelId : chatSessionGetModelId(chatSession);
                                tech.kayys.wayang.gollek.sdk.WayangInferenceService fallback = tech.kayys.wayang.gollek.sdk.WayangInferenceServiceFactory.create(null, "You are a helpful coding assistant.", currentModel == null ? "unknown-model" : currentModel);
                                java.util.concurrent.CountDownLatch fbLatch = new java.util.concurrent.CountDownLatch(1);
                                final boolean[] fbPrefixPrinted = { assistantPrefixPrinted[0] };
                                fallback.inferenceStreaming(currentModel == null ? "unknown-model" : currentModel, "You are a helpful coding assistant.", java.util.List.of(tech.kayys.gollek.spi.Message.user(userPrompt)), java.util.List.of(), tech.kayys.gollek.sdk.core.ChatParams.of(0.7, 4096))
                                        .subscribe().with(chunk2 -> {
                                            if (chunk2 != null && chunk2.delta() != null) {
                                                if (!fbPrefixPrinted[0]) {
                                                    // Erase the "⠿ thinking…" spinner line
                                                    out.print("\r" + " ".repeat(20) + "\r");
                                                    out.flush();
                                                    out.print(color ? GREEN + BOLD + "Assistant: " + RESET : "");
                                                    fbPrefixPrinted[0] = true;
                                                    assistantPrefixPrinted[0] = true;
                                                }
                                                out.print(chunk2.delta());
                                                out.flush();
                                            }
                                        }, err2 -> {
                                            out.println();
                                            printError(out, color, "Fallback inference error: " + (err2 == null ? "" : err2.getMessage()));
                                            exitCode[0] = 1;
                                            fbLatch.countDown();
                                        }, () -> {
                                            out.println();
                                            fbLatch.countDown();
                                        });
                                fbLatch.await();
                                latch.countDown();
                                return;
                            } catch (Exception ex) {
                                printError(out, color, "Fallback failed: " + ex.getMessage());
                                exitCode[0] = 1;
                                latch.countDown();
                                return;
                            }
                        }

                        printError(out, color, "Inference error: " + msg);
                        exitCode[0] = 1;
                        latch.countDown();
                    },
                    () -> {
                        out.println();
                        latch.countDown();
                    }
            );

            latch.await();

            // Persist transcript for session resume if session manager is available
            try {
                if (projectStore != null && currentSessionId != null) {
                    java.util.List<?> history = chatSessionGetHistory(chatSession);
                    projectStore.saveTranscript(resolvedProjectKey, currentSessionId, (java.util.List) history);
                }
            } catch (Exception e) {
                printWarn(out, color, "Warning: failed to persist session transcript: " + e.getMessage());
            }

            return exitCode[0];
        } catch (Exception e) {
            printError(out, color, "Failed to execute inference: " + e.getMessage());
            return 1;
        }
    }

    private int runJsonStreamLoop(WayangCliContext ctx, PrintStream out, String resolvedModel, Path workspaceDir) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(ctx.in(), StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                final String userPrompt = line;
                CountDownLatch latch = new CountDownLatch(1);
                
                chatSessionStream(chatSession, userPrompt,
                        chunk -> {
                            String delta = chunk.getDelta();
                            if (delta != null && !delta.isEmpty()) {
                                String escaped = delta.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
                                String json = String.format("{\"id\":\"chatcmpl-%s\",\"object\":\"chat.completion.chunk\",\"model\":\"%s\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"%s\"}}]}",
                                        currentSessionId != null ? currentSessionId : "cli",
                                        resolvedModel != null ? resolvedModel : "unknown",
                                        escaped);
                                out.print("data: " + json + "\n\n");
                                out.flush();
                            }
                        },
                        error -> {
                            String msg = error != null ? error.getMessage() : "Unknown error";
                            String escaped = msg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                            out.print("data: {\"error\":\"" + escaped + "\"}\n\n");
                            out.flush();
                            latch.countDown();
                        },
                        () -> {
                            out.print("data: [DONE]\n\n");
                            out.flush();
                            latch.countDown();
                        }
                );
                latch.await();
            }
        } catch (Exception e) {
            return 1;
        }
        return 0;
    }

    // ─── output helpers ──────────────────────────────────────────────────────

    private void printPrompt(PrintStream out, boolean color) {
        String c = color ? MAGENTA + BOLD : "";
        String r = color ? RESET : "";
        String projectPart = (resolvedProjectKey != null && !resolvedProjectKey.isBlank()) ? " [" + truncate(resolvedProjectKey, 16) + "]" : "";
        String sessionPart = (currentSessionId != null && !currentSessionId.isBlank()) ? " (s:" + truncate(currentSessionId, 8) + ")" : "";
        out.print(c + "❯ wayang" + projectPart + sessionPart + " " + r);
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

    private void printWarn(PrintStream out, boolean color, String msg) {
        String c = color ? YELLOW : "";
        String r = color ? RESET : "";
        out.println(c + "  ⚠ " + msg + r);
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
        out.println(d + "    /extensions  /ext    Show coding-agent extension diagnostics" + r);
        out.println(d + "    /projects            List known projects (shows current project with '(current)')");
        out.println(d + "    /project <id>        Switch current project (stores pointer in ~/.wayang/current_project.txt)");
        out.println(d + "    /sessions            List sessions for current project (use with /project to scope)");
        out.println(d + "    /sessions resume <id>  Resume a session by id (loads transcript into the current chat)");
        out.println(d + "    /sessions fork <id> [new-name]  Fork/clone an existing session into a new session") ;
        out.println(d + "    /sessions delete <id>  Delete a stored session transcript") ;
        out.println(d + "    /find <glob>          Find files by glob (e.g. '**/*.java')");
        out.println(d + "    /grep <regex>         Search workspace files for regex (shows up to 200 matches)");
        out.println(d + "    /plan <goal>          Generate a numbered implementation plan and save as tasks");
        out.println(d + "    /tasks list|add|done  Manage tasks for current project");
        out.println(d + "    /help  /?            Show this help" + r);
        out.println();
        out.println(d + "  Examples:");
        out.println(d + "    /projects");
        out.println(d + "    /project <project-id>");
        out.println(d + "    /sessions resume <session-id>");
        out.println(d + "    /sessions fork <session-id> my-branch");
        out.println(d + "    /find '**/*.java'");
        out.println(d + "    /grep 'TODO\\b'");
        out.println(d + "    /plan 'Make wayang code act as a coding agent to support project/session management.'");
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

    private Object createChatSession(String resolvedModel, String providerId, Path workspaceDir) {
        // Prefer SDK-backed ChatSession when Gollek SDK is available on the classpath.
        try {
            try {
                Class<?> factory = Class.forName("tech.kayys.gollek.factory.GollekSdkFactory");
                java.lang.reflect.Method create = factory.getMethod("createLocalSdk");
                Object sdk = create.invoke(null);
                if (sdk != null) {
                    try {
                        Class<?> impl = Class.forName("tech.kayys.gollek.sdk.session.ChatSessionImpl");
                        java.lang.reflect.Constructor<?> ctor = null;
                        for (java.lang.reflect.Constructor<?> c : impl.getConstructors()) {
                            Class<?>[] pts = c.getParameterTypes();
                            if (pts.length >= 3 && pts[1] == String.class) {
                                ctor = c; break;
                            }
                        }
                        if (ctor != null) {
                            Object chat = ctor.newInstance(sdk, resolvedModel, providerId, !noMemory);
                            applyCodingAgentPrompt(chat, resolvedModel, workspaceDir);
                            return chat;
                        }
                    } catch (Throwable t) {
                        // fall through to other session types
                    }
                }
            } catch (ClassNotFoundException cnf) {
                // SDK factory not present
            }
        } catch (Throwable ignored) {}

        // Prefer local gguf via adapter if caller didn't request a provider (best-effort)
        try {
            if ((providerId == null || providerId.isBlank()) && sdkAdapter != null) {
                try {
                    boolean hasGguf = sdkAdapter.listModelsStrings().stream().anyMatch(m -> m.toLowerCase().contains("gguf"));
                    if (hasGguf) providerId = "gguf";
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Prefer a shell-backed session delegating to the local `gollek` CLI when possible.
        try {
            try {
                ShellChatSession shell = new ShellChatSession(resolvedModel, providerId, !noMemory);
                applyCodingAgentPrompt(shell, resolvedModel, workspaceDir);
                return shell;
            } catch (Throwable t) {
                // fall through to Noop
            }
        } catch (Throwable ignored) {}

        // Fallback: NoopChatSession so CLI runs without Gollek on classpath or without gollek CLI.
        Object session = new NoopChatSession(resolvedModel, providerId, !noMemory);
        applyCodingAgentPrompt(session, resolvedModel, workspaceDir);
        return session;
    }

    private void applyCodingAgentPrompt(Object session, String resolvedModel, String workspace) {
        applyCodingAgentPrompt(session, resolvedModel, resolveWorkspacePath(workspace));
    }

    private void applyCodingAgentPrompt(Object session, String resolvedModel, Path workspaceDir) {
        if (session == null) return;
        Object promptObj = WayangCodePromptComposer.systemPrompt(new WayangCodePromptContext(
                profileId,
                workspaceDir,
                resolvedModel,
                !noMemory,
                harness,
                maxSteps),
                codeExtensionDiscovery == null ? List.of() : codeExtensionDiscovery.promptAdditions());
        String promptStr = promptObj == null ? null : promptObj.toString();
        // Try typed setters for known session implementations
        try {
            if (session instanceof NoopChatSession) {
                ((NoopChatSession) session).setSystemPrompt(promptObj);
                return;
            }
            if (session instanceof ShellChatSession) {
                ((ShellChatSession) session).setSystemPrompt(promptStr);
                return;
            }
            // Try ChatSessionImpl's setSystemPrompt(String)
            try {
                session.getClass().getMethod("setSystemPrompt", String.class).invoke(session, promptStr);
                return;
            } catch (NoSuchMethodException ns) {
                // Try Object-typed setter
                try {
                    session.getClass().getMethod("setSystemPrompt", Object.class).invoke(session, promptObj);
                    return;
                } catch (NoSuchMethodException ns2) {
                    // ignore
                }
            }
        } catch (Throwable ignored) {}
    }

    private Path resolveWorkspacePath(String workspace) {
        String resolved = (workspace == null || workspace.isBlank()) ? "." : workspace;
        return Paths.get(resolved).toAbsolutePath().normalize();
    }

    private void refreshCodeAgentExtensions(
            Path workspaceDir,
            String resolvedModel,
            String resolvedSession,
            String resolvedProvider) {
        this.codeAgentContext = WayangCodeAgentContext.builder()
                .surfaceId(CODING_SURFACE)
                .profileId(profileId)
                .workspacePath(workspaceDir)
                .projectId(resolvedProjectKey)
                .sessionId(resolvedSession)
                .modelId(resolvedModel)
                .providerId(resolvedProvider)
                .memoryEnabled(!noMemory)
                .harnessEnabled(harness)
                .maxSteps(maxSteps)
                .metadata("ui", "cli")
                .metadata("command", "wayang code")
                .build();
        this.codeExtensionDiscovery = WayangCodeAgentExtensions.discover(codeAgentContext);
    }

    private static boolean modelExists(String modelId) throws Exception {
        return tech.kayys.wayang.gollek.sdk.WayangGollekFacade.modelExists(modelId);
    }

    private static int runProcessAndPipe(PrintStream out, String... cmd) throws IOException, InterruptedException {
        // Preserve backward-compatible behaviour for arbitrary commands by shelling out.
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
        }
        return p.waitFor();
    }

    // Utility shim layer to isolate direct gollek SPI usage so CLI can run without Gollek on classpath.
    private static boolean isGollekSpiAvailable() {
        try {
            Class.forName("tech.kayys.gollek.spi.Message");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isGollekCliAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("gollek", "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            int rc = p.exitValue();
            return rc == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String chatSessionGetProviderId(Object session) {
        if (session == null) return null;
        try {
            if (session instanceof NoopChatSession) return ((NoopChatSession) session).getProviderId();
            return (String) session.getClass().getMethod("getProviderId").invoke(session);
        } catch (Throwable t) { return null; }
    }

    private static String chatSessionGetModelId(Object session) {
        if (session == null) return null;
        try {
            if (session instanceof NoopChatSession) return ((NoopChatSession) session).getModelId();
            return (String) session.getClass().getMethod("getModelId").invoke(session);
        } catch (Throwable t) { return null; }
    }

    private static void chatSessionReset(Object session) {
        if (session == null) return;
        try {
            if (session instanceof NoopChatSession) { ((NoopChatSession) session).reset(); return; }
            session.getClass().getMethod("reset").invoke(session);
        } catch (Throwable ignored) {}
    }

    private static void chatSessionAddMessage(Object session, Object msg) {
        if (session == null) return;
        try {
            java.nio.file.Files.write(
                java.nio.file.Path.of(System.getProperty("user.home"), ".wayang", "sessions", "debug.log"),
                ("[chatSessionAddMessage] msg=" + (msg == null ? "null" : msg.getClass().getSimpleName()) + "\n").getBytes(),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) { }
        
        try {
            if (session instanceof NoopChatSession) { ((NoopChatSession) session).addMessage(msg); return; }
            session.getClass().getMethod("addMessage", Object.class).invoke(session, msg);
        } catch (Throwable t) {
            try {
                java.nio.file.Files.write(
                    java.nio.file.Path.of(System.getProperty("user.home"), ".wayang", "sessions", "debug.log"),
                    ("[chatSessionAddMessage] ERROR: " + t.getClass().getSimpleName() + ": " + t.getMessage() + "\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) { }
        }
    }

    private static java.util.List<?> chatSessionGetHistory(Object session) {
        if (session == null) return List.of();
        try {
            if (session instanceof NoopChatSession) return ((NoopChatSession) session).getHistory();
            return (java.util.List<?>) session.getClass().getMethod("getHistory").invoke(session);
        } catch (Throwable t) { return List.of(); }
    }

    private static void chatSessionStream(Object session, String prompt, Consumer<NoopChatSession.Chunk> onItem, Consumer<Throwable> onFailure, Runnable onComplete) {
        if (session == null) return;
       try {
           if (session instanceof NoopChatSession) {
               ((NoopChatSession) session).stream(prompt).subscribe().with(onItem, onFailure, onComplete);
               return;
           }
           if (session instanceof ShellChatSession) {
               // ShellChatSession produces its own Chunk type; adapt to NoopChatSession.Chunk for callers.
               ShellChatSession sh = (ShellChatSession) session;
               sh.stream(prompt).subscribe().with(chunk -> {
                   if (onItem != null) onItem.accept(new NoopChatSession.Chunk(chunk.getDelta()));
               }, onFailure, onComplete);
               return;
           }
           // Attempt reflective invocation compatible with Gollek ChatSession.stream(String).subscribe().with(...)
           Object stream = session.getClass().getMethod("stream", String.class).invoke(session, prompt);
           if (stream == null) return;
           Object subscriber = stream.getClass().getMethod("subscribe").invoke(stream);
           if (subscriber == null) return;
           
           java.util.function.Consumer<Object> adapter = obj -> {
               if (obj == null) return;
               try {
                   String delta = null;
                   if (obj instanceof NoopChatSession.Chunk) {
                       delta = ((NoopChatSession.Chunk) obj).getDelta();
                   } else {
                       try {
                           delta = (String) obj.getClass().getMethod("delta").invoke(obj);
                       } catch (Throwable ignored) {
                           try {
                               delta = (String) obj.getClass().getMethod("getText").invoke(obj);
                           } catch (Throwable ignore) {}
                       }
                   }
                   if (onItem != null) onItem.accept(new NoopChatSession.Chunk(delta));
               } catch (Throwable ignore) {}
           };
           
           // Try to call 'with' with three java.util.function types
           try {
               subscriber.getClass().getMethod("with", java.util.function.Consumer.class, java.util.function.Consumer.class, Runnable.class)
                       .invoke(subscriber, adapter, onFailure, onComplete);
               return;
           } catch (NoSuchMethodException nsme) {
               // Fallback: try versions that accept different arg types used by some reactive libs
               try {
                   subscriber.getClass().getMethod("with", java.util.function.Consumer.class, java.util.function.Consumer.class, java.lang.Runnable.class)
                           .invoke(subscriber, adapter, onFailure, onComplete);
               } catch (Throwable ignored) {}
           }
       } catch (Throwable t) {
           System.err.println("[chatSessionStream] Caught exception: " + (t == null ? "null" : t.getMessage()));
           t.printStackTrace(System.err);
           // If the SDK-backed chat session failed due to missing provider for the model,
           // attempt a subprocess-based fallback by creating a ShellChatSession and streaming from it.
           try {
               String msg = t == null || t.getMessage() == null ? "" : t.getMessage();
               if (msg.contains("No provider found for model")) {
                   System.err.println("[chatSessionStream] Detected 'No provider found', attempting subprocess fallback");
                   try {
                       String mid = chatSessionGetModelId(session);
                       String pid = chatSessionGetProviderId(session);
                       // Create a WayangInferenceService with null SDK to force subprocess fallback
                       try {
                           tech.kayys.wayang.gollek.sdk.WayangInferenceService fallbackService = tech.kayys.wayang.gollek.sdk.WayangInferenceServiceFactory.create(null, "You are a helpful coding assistant.", mid == null ? "unknown-model" : mid);
                           System.err.println("[chatSessionStream] Fallback service created, calling inferenceStreaming");
                           fallbackService.inferenceStreaming(mid == null ? "unknown-model" : mid, "You are a helpful coding assistant.", java.util.List.of(tech.kayys.gollek.spi.Message.user(prompt)), java.util.List.of(), tech.kayys.gollek.sdk.core.ChatParams.of(0.7, 4096))
                               .subscribe().with(chunk -> {
                                   if (onItem != null) onItem.accept(new NoopChatSession.Chunk(chunk.delta()));
                                }, onFailure, onComplete);
                            return;
                        } catch (Throwable ignore) { }
                        
                    } catch (Throwable ignore) { }
                }
            } catch (Throwable ignore) { }
            if (onFailure != null) onFailure.accept(t);
        }
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
