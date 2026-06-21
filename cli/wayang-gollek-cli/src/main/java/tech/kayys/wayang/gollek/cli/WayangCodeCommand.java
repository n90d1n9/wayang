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
import tech.kayys.gollek.sdk.model.ModelResolution;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.sdk.gollek.ProjectStore;
import tech.kayys.wayang.sdk.gollek.model.Project;
import tech.kayys.wayang.sdk.gollek.tools.CodeScanner;
import tech.kayys.wayang.sdk.gollek.tools.CodeGrep;
import tech.kayys.wayang.sdk.gollek.tools.Planner;
import tech.kayys.wayang.sdk.gollek.tools.TaskStore;

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

    private GollekSdk sdk;
    private ChatSession chatSession;

    // Project/session persistence manager (SDK ProjectStore)
    private tech.kayys.wayang.sdk.gollek.ProjectStore projectStore;
    private String resolvedProjectKey;
    // Current active session id (resolved/auto-generated)
    private String currentSessionId;

    @Override
    public Integer call() {
        WayangCliContext ctx = parent.context();
        PrintStream out = ctx.out();

        // Debug: show parsed options at entry
        try { System.err.println("[DEBUG] WayangCodeCommand.call() prompt='" + prompt + "' model='" + modelId + "' provider='" + providerId + "' once='" + once + "'"); } catch (Throwable ignore) {}
        try { if (parent != null && parent.context() != null) parent.context().out().println("  Debug: WayangCodeCommand.call() prompt='" + prompt + "' model='" + modelId + "' provider='" + providerId + "' once='" + once + "'"); } catch (Throwable ignore) {}

        boolean color = !noColor && isColorSupported();

        // Initialize GollekSdk locally — prefer CLI flag, then Wayang preferred provider, then default
        try {
            String effectiveProvider = providerId;
            try {
                WayangGollekSdk wayangSdk = parent.sdk();
                if (effectiveProvider == null || effectiveProvider.isBlank()) {
                    try {
                        java.util.Optional<String> p = wayangSdk.getPreferredProvider();
                        if (p.isPresent()) {
                            effectiveProvider = p.get();
                        }
                    } catch (Throwable ignore) {
                        // ignore if parent SDK doesn't expose preferred provider
                    }
                }
            } catch (Throwable ignore) {
                // parent.sdk() may not be accessible; fall back to CLI flag only
            }

            if (effectiveProvider != null && "gguf".equalsIgnoreCase(effectiveProvider)) {
                // Create GGUF-optimized local SDK
                this.sdk = GollekSdkFactory.createForGguf();
            } else if (effectiveProvider != null && !effectiveProvider.isBlank()) {
                // Create local sdk with preferred provider
                try {
                    tech.kayys.gollek.sdk.config.SdkConfig cfg = tech.kayys.gollek.sdk.config.SdkConfig.builder()
                            .preferredProvider(effectiveProvider)
                            .build();
                    this.sdk = GollekSdkFactory.createLocalSdk(cfg);
                } catch (Throwable t) {
                    // fallback
                    this.sdk = GollekSdkFactory.createLocalSdk();
                }
            } else {
                this.sdk = GollekSdkFactory.createLocalSdk();
            }
        } catch (Exception e) {
            out.println((color ? RED : "") + "Error: Failed to initialize local Gollek SDK: " + e.getMessage() + (color ? RESET : ""));
            return 1;
        }

        // Resolve model
        String resolvedModel = modelId;
        if (resolvedModel == null || resolvedModel.isBlank()) {
            try {
                resolvedModel = sdk.resolveDefaultModel()
                        .orElse("gemma-4-E2B-it");
            } catch (Exception e) {
                resolvedModel = "gemma-4-E2B-it";
            }
        }

        // Resolve session
        String resolvedSession = (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();
        // persist resolved id on the command object for runOnce to access
        this.currentSessionId = resolvedSession;

        // Resolve workspace
        String resolvedWorkspace = (workspacePath != null && !workspacePath.isBlank())
                ? workspacePath : ".";
        Path workspaceDir = resolveWorkspacePath(resolvedWorkspace);

        // Initialize project/session manager for grouping
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

        // Initialize ChatSession with the durable Wayang coding-agent system prompt.
        if (providerId != null && !providerId.isBlank()) {
            try {
                // Best-effort: set preferred provider directly on the Gollek SDK first.
                try {
                    this.sdk.setPreferredProvider(providerId);
                } catch (Throwable ignore) {
                    // fall through to probing / auto-load if direct set failed
                }

                List<tech.kayys.gollek.spi.provider.ProviderInfo> available = this.sdk.listAvailableProviders();
                boolean found = false;
                if (available != null) {
                    for (tech.kayys.gollek.spi.provider.ProviderInfo p : available) {
                        if (p.id().equals(providerId)) { found = true; break; }
                    }
                }
                if (!found) {
                    String known = "<unknown>";
                    if (available != null) {
                        known = available.stream().map(tech.kayys.gollek.spi.provider.ProviderInfo::id).toList().toString();
                    }
                    printInfo(out, color, "Preferred provider '" + providerId + "' is not available. Known providers: " + known);

                    // Attempt to auto-load provider JAR from local Maven repository (same fallback as root CLI)
                    try {
                        java.lang.reflect.Method loadMethod = null;
                        try { loadMethod = this.sdk.getClass().getMethod("loadProviderJar", String.class); } catch (NoSuchMethodException ignore) {}

                        java.nio.file.Path candidateDir = java.nio.file.Paths.get(System.getProperty("user.home"), ".m2", "repository", "tech", "kayys", "gollek", "gollek-plugin-" + providerId);
                        if (java.nio.file.Files.exists(candidateDir)) {
                            java.util.Optional<java.nio.file.Path> jarOpt = java.nio.file.Files.walk(candidateDir)
                                    .filter(x -> x.getFileName().toString().endsWith(".jar"))
                                    .findFirst();
                            if (jarOpt.isPresent()) {
                                java.nio.file.Path jarPath = jarOpt.get();
                                printInfo(out, color, "Found provider JAR candidate: " + jarPath.toString());
                                if (loadMethod != null) {
                                    try {
                                        loadMethod.invoke(this.sdk, jarPath.toString());
                                        printInfo(out, color, "Loaded provider JAR via SDK: " + jarPath.toString());
                                        // re-check availability
                                        available = this.sdk.listAvailableProviders();
                                        if (available != null) {
                                            for (tech.kayys.gollek.spi.provider.ProviderInfo p : available) {
                                                if (p.id().equals(providerId)) { found = true; break; }
                                            }
                                            if (found) this.sdk.setPreferredProvider(providerId);
                                        }
                                    } catch (Throwable t) {
                                        printInfo(out, color, "Warning: failed to invoke SDK.loadProviderJar: " + t.getMessage());
                                    }
                                } else {
                                    // Reflectively register minimal capability descriptor into provider registry
                                    try {
                                        java.lang.reflect.Method regMethod = null;
                                        try { regMethod = this.sdk.getClass().getMethod("providerCapabilityRegistry"); } catch (NoSuchMethodException ignore) {}
                                        if (regMethod != null) {
                                            Object registry = regMethod.invoke(this.sdk);
                                            if (registry != null) {
                                                Class<?> descClass = Class.forName("tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityDescriptor");
                                                Class<?> stateClass = Class.forName("tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityState");
                                                java.lang.reflect.Constructor<?> ctor = descClass.getConstructor(
                                                        String.class, String.class, String.class, String.class,
                                                        String.class, String.class, String.class, stateClass,
                                                        java.util.List.class, java.util.List.class, java.util.List.class, java.util.Map.class);
                                                String moduleId = jarPath.getFileName().toString().replaceAll("\\.jar$", "");
                                                String providerIdGuess = providerId;
                                                String capabilityId = providerIdGuess + ".inference";
                                                Object stateVal = java.lang.Enum.valueOf((Class<Enum>) stateClass, "AVAILABLE");
                                                Object descriptor = ctor.newInstance(
                                                        capabilityId,
                                                        providerIdGuess,
                                                        "gollek",
                                                        moduleId,
                                                        "inference",
                                                        Character.toUpperCase(providerIdGuess.charAt(0)) + providerIdGuess.substring(1) + " Provider",
                                                        "Dynamically registered provider from JAR: " + jarPath.toString(),
                                                        stateVal,
                                                        java.util.List.of("coding-agent", "assistant-agent"),
                                                        java.util.List.of(),
                                                        java.util.List.of("gollek", "provider", "inference"),
                                                        java.util.Map.of("jar", jarPath.toString())
                                                );
                                                java.lang.reflect.Method registerMethod = registry.getClass().getMethod("register", descClass);
                                                registerMethod.invoke(registry, descriptor);
                                                printInfo(out, color, "Registered provider capability for: " + providerIdGuess);
                                                // attempt to set preferred provider
                                                try {
                                                    java.lang.reflect.Method setMethod = this.sdk.getClass().getMethod("setPreferredProvider", String.class);
                                                    setMethod.invoke(this.sdk, providerIdGuess);
                                                    found = true;
                                                } catch (NoSuchMethodException ignore) {}
                                            }
                                        }
                                    } catch (Throwable t) {
                                        printInfo(out, color, "Warning: failed to register provider capability reflectively on local SDK: " + t.getMessage());
                                        // Fallback: try to register on parent CLI's resolved SDK (which may have provider registry exposed)
                                        try {
                                            java.lang.reflect.Method parentSdkMethod = parent.getClass().getDeclaredMethod("sdk");
                                            parentSdkMethod.setAccessible(true);
                                            Object parentResolvedSdk = parentSdkMethod.invoke(parent);
                                            if (parentResolvedSdk != null) {
                                                try {
                                                    java.lang.reflect.Method regMethod2 = parentResolvedSdk.getClass().getMethod("providerCapabilityRegistry");
                                                    Object registry2 = regMethod2.invoke(parentResolvedSdk);
                                                    if (registry2 != null) {
                                                        Class<?> descClass2 = Class.forName("tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityDescriptor");
                                                        Class<?> stateClass2 = Class.forName("tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityState");
                                                        java.lang.reflect.Constructor<?> ctor2 = descClass2.getConstructor(
                                                                String.class, String.class, String.class, String.class,
                                                                String.class, String.class, String.class, stateClass2,
                                                                java.util.List.class, java.util.List.class, java.util.List.class, java.util.Map.class);
                                                        String moduleId2 = jarPath.getFileName().toString().replaceAll("\\.jar$", "");
                                                        String providerIdGuess2 = providerId;
                                                        String capabilityId2 = providerIdGuess2 + ".inference";
                                                        Object stateVal2 = java.lang.Enum.valueOf((Class<Enum>) stateClass2, "AVAILABLE");
                                                        Object descriptor2 = ctor2.newInstance(
                                                                capabilityId2,
                                                                providerIdGuess2,
                                                                "gollek",
                                                                moduleId2,
                                                                "inference",
                                                                Character.toUpperCase(providerIdGuess2.charAt(0)) + providerIdGuess2.substring(1) + " Provider",
                                                                "Dynamically registered provider from JAR: " + jarPath.toString(),
                                                                stateVal2,
                                                                java.util.List.of("coding-agent", "assistant-agent"),
                                                                java.util.List.of(),
                                                                java.util.List.of("gollek", "provider", "inference"),
                                                                java.util.Map.of("jar", jarPath.toString())
                                                        );
                                                        java.lang.reflect.Method registerMethod2 = registry2.getClass().getMethod("register", descClass2);
                                                        registerMethod2.invoke(registry2, descriptor2);
                                                        printInfo(out, color, "Registered provider capability for: " + providerIdGuess2 + " (via parent SDK)");
                                                        try {
                                                            java.lang.reflect.Method setMethod2 = parentResolvedSdk.getClass().getMethod("setPreferredProvider", String.class);
                                                            setMethod2.invoke(parentResolvedSdk, providerIdGuess2);
                                                            found = true;
                                                        } catch (NoSuchMethodException ignore) {}
                                                    }
                                                } catch (Throwable t2) {
                                                    printInfo(out, color, "Warning: failed to register provider capability via parent SDK: " + t2.getMessage());
                                                }
                                            }
                                        } catch (Throwable ignore) {
                                            // ignore
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable t) {
                        printInfo(out, color, "Warning: provider auto-load attempt failed: " + t.getMessage());
                    }

                    if (!found) {
                        providerId = null;
                    }
                }
            } catch (Exception e) {
                printInfo(out, color, "Warning: failed to apply preferred provider: " + e.getMessage());
                providerId = null;
            }
        }
        this.chatSession = createChatSession(resolvedModel, providerId, workspaceDir);

        // If a session id was provided and persistence is available, attempt to resume
        if (projectStore != null && sessionId != null && !sessionId.isBlank()) {
            try {
                java.util.List<tech.kayys.gollek.spi.Message> transcript = projectStore.loadTranscript(resolvedProjectKey, resolvedSession);
                if (transcript != null && !transcript.isEmpty()) {
                    for (tech.kayys.gollek.spi.Message m : transcript) {
                        chatSession.addMessage(m);
                    }
                    printInfo(out, color, "Resumed session " + resolvedSession + " (" + transcript.size() + " messages)");
                }
            } catch (Exception e) {
                printInfo(out, color, "Warning: failed to resume session: " + e.getMessage());
            }
        }

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
        String command = line == null ? "" : line.trim();
        String cmd = command.toLowerCase();
        
        if (cmd.equals("/exit") || cmd.equals("/quit") || cmd.equals("/q")) {
            return true;
        }

        if (cmd.equals("/reset")) {
            if (chatSession != null) {
                chatSession.reset();
                applyCodingAgentPrompt(chatSession, chatSession.getModelId(), workspace);
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
            String newModelId = command.substring(7).trim();
            if (newModelId.isEmpty()) {
                out.println((color ? YELLOW : "") + "  Usage: /model <model-id>" + (color ? RESET : ""));
            } else {
                try {
                    String provider = chatSession == null ? null : chatSession.getProviderId();
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
            String newProviderId = command.substring(10).trim();
            if (newProviderId.isEmpty()) {
                out.println("  Usage: /provider <provider-id>");
            } else {
                try {
                    // Validate provider exists via Gollek SDK
                    List<tech.kayys.gollek.spi.provider.ProviderInfo> available = sdk.listAvailableProviders();
                    boolean found = false;
                    if (available != null) {
                        for (var p : available) { if (p.id().equals(newProviderId)) { found = true; break; } }
                    }
                    if (!found) {
                        String known = available == null ? "<unknown>" : available.stream().map(tech.kayys.gollek.spi.provider.ProviderInfo::id).toList().toString();
                        printError(out, color, "Provider '" + newProviderId + "' not found. Known providers: " + known);
                    } else {
                        sdk.setPreferredProvider(newProviderId);
                        String currentModel = chatSession == null ? modelId : chatSession.getModelId();
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
            if (projectStore == null) {
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
                for (var m : transcript) chatSession.addMessage(m);
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
                String[] parts = payload.split("\\s+", 2);
                String sid = parts[0];
                String newName = parts.length > 1 ? parts[1] : null;
                var newSession = projectStore.cloneSession(resolvedProjectKey, sid, newName);
                if (newSession != null) {
                    printInfo(out, color, "Forked session " + sid + " -> " + newSession.id() + " (name='" + newSession.name() + "')");
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
                CodeScanner scanner = new CodeScanner(ws);
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
                CodeScanner scanner = new CodeScanner(ws);
                var files = scanner.findFiles("**/*");
                CodeGrep greper = new CodeGrep(ws);
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
                Planner p = new Planner();
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
                    TaskStore ts = new TaskStore(projectDir);
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
                    TaskStore ts = new TaskStore(projectDir);
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
                    TaskStore ts = new TaskStore(projectDir);
                    var t = ts.addTask(desc);
                    printInfo(out, color, "Added task: " + t.id());
                } catch (Exception e) { printError(out, color, "Add task failed: " + e.getMessage()); }
                return false;
            } else if (rest.startsWith("done ")) {
                String id = rest.substring("done ".length()).trim();
                try {
                    Path projectDir = Paths.get(System.getProperty("user.home"), ".wayang", "projects", resolvedProjectKey == null ? "default" : resolvedProjectKey);
                    TaskStore ts = new TaskStore(projectDir);
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

        try {
            // Defensive: if the active chat session uses safetensor as provider,
            // attempt to prefer gguf and recreate the session so local GGUF models work.
            try {
                String activeProvider = null;
                try {
                    activeProvider = chatSession == null ? null : chatSession.getProviderId();
                } catch (Throwable ignored) {}
                if (activeProvider != null && activeProvider.equalsIgnoreCase("safetensor")) {
                    try {
                        sdk.setPreferredProvider("gguf");
                    } catch (Throwable ignored) {}
                    // Recreate chat session with gguf preference
                    try {
                        String currentModel = chatSession == null ? modelId : chatSession.getModelId();
                        this.chatSession = createChatSession(currentModel, "gguf", resolveWorkspacePath(workspacePath));
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

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

            // Persist transcript for session resume if session manager is available
            try {
                if (projectStore != null && currentSessionId != null) {
                    java.util.List<tech.kayys.gollek.spi.Message> history = chatSession.getHistory();
                    projectStore.saveTranscript(resolvedProjectKey, currentSessionId, history);
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

    private ChatSession createChatSession(String resolvedModel, String providerId, Path workspaceDir) {
        // If caller didn't request a provider, prefer local gguf when available.
        if (providerId == null || providerId.isBlank()) {
            try {
                var providers = sdk.listAvailableProviders();
                boolean hasGguf = providers.stream().anyMatch(p -> "gguf".equalsIgnoreCase(p.id()));
                if (hasGguf) {
                    try {
                        sdk.setPreferredProvider("gguf");
                        providerId = "gguf";
                    } catch (Throwable ignored) {
                        // ignore if unavailable
                        providerId = "gguf"; // still prefer when possible
                    }
                }
            } catch (Throwable ignored) {
                // ignore provider listing errors
            }
        }

        // Ensure model is prepared and a provider is selected before creating the session
        try {
            // Debug: show the resolvedModel/provider before asking SDK to prepare
            try { System.err.println("[DEBUG] createChatSession() called with model='" + resolvedModel + "' provider='" + providerId + "'"); } catch (Throwable ignore) {}
            try { if (parent != null && parent.context() != null) parent.context().out().println("  Debug: createChatSession() model='" + resolvedModel + "' provider='" + providerId + "'"); } catch (Throwable ignore) {}
            ModelResolution resolution = sdk.prepareModel(resolvedModel, false, (progress) -> {
                // no-op progress callback for CLI
            });
            if (resolution != null) {
                if (resolution.getModelId() != null && !resolution.getModelId().isBlank()) {
                    resolvedModel = resolution.getModelId();
                }
                if (resolution.getProviderId() != null && !resolution.getProviderId().isBlank()) {
                    // prefer resolution provider unless caller forced one
                    providerId = providerId != null && !providerId.isBlank() ? providerId : resolution.getProviderId();
                }
            }
        } catch (Exception e) {
            // Best-effort: log and continue with the requested model/provider
            try {
                // Print to parent context if available
                if (parent != null && parent.context() != null) {
                    parent.context().out().println("  Warning: failed to prepare model: " + e.getMessage());
                }
            } catch (Exception ignored) {}
        }

        ChatSession session = new ChatSessionImpl(sdk, resolvedModel, providerId, !noMemory);
        applyCodingAgentPrompt(session, resolvedModel, workspaceDir);
        return session;
    }

    private void applyCodingAgentPrompt(ChatSession session, String resolvedModel, String workspace) {
        applyCodingAgentPrompt(session, resolvedModel, resolveWorkspacePath(workspace));
    }

    private void applyCodingAgentPrompt(ChatSession session, String resolvedModel, Path workspaceDir) {
        if (session == null) {
            return;
        }
        session.setSystemPrompt(WayangCodePromptComposer.systemPrompt(new WayangCodePromptContext(
                profileId,
                workspaceDir,
                resolvedModel,
                !noMemory,
                harness,
                maxSteps)));
    }

    private Path resolveWorkspacePath(String workspace) {
        String resolved = (workspace == null || workspace.isBlank()) ? "." : workspace;
        return Paths.get(resolved).toAbsolutePath().normalize();
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
