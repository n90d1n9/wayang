package tech.kayys.gamelan.cli;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine.*;
import tech.kayys.gamelan.agent.*;
import tech.kayys.gamelan.agent.agui.AguiAgentRunner;
import tech.kayys.gamelan.agent.agui.AguiTerminalRenderer;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.context.ProjectContext;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gamelan.util.AnsiPrinter;
import tech.kayys.gamelan.agent.CheckpointManager;
import tech.kayys.gamelan.agent.ConversationCompactor;
import tech.kayys.gamelan.agent.TokenBudgetAdvisor;
import tech.kayys.gollek.sdk.core.GollekSdk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Interactive REPL — the primary user interface for Gamelan.
 *
 * <h2>Architecture change</h2>
 * Now routes through {@link OrchestratorSelector} instead of calling
 * {@link AgentLoop} directly. This means REPL sessions can benefit from
 * all three tiers: the selector auto-picks the right one per message,
 * or the user can fix the strategy for the session with {@code /strategy}.
 *
 * <h2>REPL commands</h2>
 * <pre>
 * /help               — show all commands
 * /strategy [name]    — show or set orchestration strategy
 * /model [name]       — show or switch model
 * /models             — list available local models
 * /skills             — list loaded skills
 * /session            — show session stats
 * /stats              — token + tool usage summary
 * /metrics            — per-tool call metrics (latency, errors)
 * /memory [query]     — show remembered facts
 * /context            — show project context
 * /clear              — clear history + reset counters
 * /compact            — compress conversation history
 * /agui               — toggle AG-UI event rendering
 * /exit               — quit
 * </pre>
 */
@ApplicationScoped
@Command(
    name = "chat",
    description = "Start an interactive AI session",
    mixinStandardHelpOptions = true
)
public class ChatCommand implements Runnable {

    @Inject OrchestratorSelector  selector;
    @Inject AguiAgentRunner       aguiRunner;
    @Inject GamelanConfig         config;
    @Inject TokenTracker          tokenTracker;
    @Inject ToolMetrics           toolMetrics;
    @Inject AgentMemory           memory;
    @Inject ProjectContext        projectContext;
    @Inject SkillRegistry         skillRegistry;
    @Inject GollekSdk             sdk;
    @Inject ConversationCompactor compactor;
    @Inject CheckpointManager      checkpoints;
    @Inject TokenBudgetAdvisor     budgetAdvisor;

    @Parameters(index = "0", arity = "0..1") String oneShotTask;
    @Option(names = {"-m", "--model"})        String modelOverride;
    @Option(names = {"--strategy", "-s"}, defaultValue = "auto") String strategy;
    @Option(names = {"--no-stream"})          boolean noStream;
    @Option(names = {"--session"})            String sessionId;
    @Option(names = {"--no-color"})           boolean noColor;
    @Option(names = {"-v", "--verbose"})      boolean verbose;
    @Option(names = {"--agui"})               boolean aguiMode;

    private String activeModel;
    private String activeStrategy;
    private boolean activeAguiMode;

    @Override
    public void run() {
        activeModel    = resolve(modelOverride);
        activeStrategy = strategy;
        activeAguiMode = aguiMode;
        if (oneShotTask != null && !oneShotTask.isBlank()) {
            runOneShotDirect(oneShotTask);
        } else {
            startRepl();
        }
    }

    /** Called from GamelanApplication for the default REPL. */
    public void startRepl(String modelOverride, boolean verbose, boolean noColor) {
        this.modelOverride = modelOverride;
        this.verbose       = verbose;
        this.noColor       = noColor;
        this.activeModel   = resolve(modelOverride);
        this.activeStrategy = "auto";
        startRepl();
    }

    // ── REPL ───────────────────────────────────────────────────────────────

    private void startRepl() {
        boolean    useColor = !noColor && config.color();
        AnsiPrinter printer = new AnsiPrinter(useColor);
        ConversationSession session = new ConversationSession(
                sessionId, config.sessionPersist(), config.tokenBudget());

        try (Terminal terminal = TerminalBuilder.builder().system(true).dumb(true).build()) {
            // Pipe detection: stdin is not a TTY → one-shot and exit
            if (System.console() == null) {
                try {
                    String piped = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).strip();
                    if (!piped.isBlank()) { runSession(piped, session, printer); return; }
                } catch (IOException ignored) {}
            }

            Path histFile = Path.of(System.getProperty("user.home"), ".gamelan", "history");
            Files.createDirectories(histFile.getParent());

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(new DefaultHistory())
                    .parser(new DefaultParser())
                    .variable(LineReader.HISTORY_FILE, histFile)
                    .variable(LineReader.HISTORY_SIZE, 2000)
                    .variable(LineReader.BELL_STYLE, "none")
                    .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                    .build();

            printer.banner();
            printer.info("Model: " + activeModel + "  |  Strategy: " + activeStrategy
                    + "  |  Project: " + projectContext.info().projectType()
                    + "/" + projectContext.info().primaryLanguage());
            printer.info("Skills: " + skillCount() + "  |  Memories: " + memory.relevant().size()
                    + "  |  /help for commands");
            printer.println();

            while (true) {
                String line;
                try {
                    line = reader.readLine(printer.prompt(session));
                } catch (UserInterruptException e) {
                    printer.warn("Cancelled  (Ctrl+D to exit)");
                    continue;
                } catch (EndOfFileException e) {
                    printer.println();
                    printer.info("Goodbye! 🎶  " + tokenTracker.oneLiner());
                    break;
                }

                if (line == null || line.isBlank()) continue;
                String trimmed = line.strip();

                if (trimmed.startsWith("/")) {
                    handleMeta(trimmed, session, printer);
                    continue;
                }

                runSession(trimmed, session, printer);
            }
        } catch (IOException e) {
            System.err.println("Terminal error: " + e.getMessage());
        }
    }

    // ── Task execution (routes through orchestrator tier) ──────────────────

    private void runSession(String task, ConversationSession session, AnsiPrinter printer) {
        long t0 = System.currentTimeMillis();
        try {
            if (activeAguiMode) {
                AguiTerminalRenderer renderer = new AguiTerminalRenderer(config.color());
                AgentRequest req = buildRequest(task, session);
                String answer = aguiRunner.run(req,
                        activeStrategy.equals("auto") ? null : activeStrategy, renderer);
                session.addTurn(task, AgentResponse.builder().text(answer).build());
                return;
            }

            AgentRequest req  = buildRequest(task, session);
            AgentOrchestrator orch = selector.select(
                    activeStrategy.equals("auto") ? null : activeStrategy, task);
            OrchestratorResult result = orch.execute(req);

            if (!req.stream()) {
                System.out.println(result.answer());
            }

            printer.agentFooter(result, System.currentTimeMillis() - t0);
            session.addTurn(task, AgentResponse.builder()
                    .text(result.answer())
                    .error(!result.success())
                    .build());
            // Auto-save checkpoint after each turn
            checkpoints.save(session, task, activeModel, activeStrategy);
            // Auto-compact if context is filling up
            if (compactor.compactIfNeeded(session, activeModel)) {
                printer.info("Auto-compacted context to fit token budget.");
            }
        } catch (Exception e) {
            printer.error("Error: " + e.getMessage());
            if (verbose) e.printStackTrace(System.err);
        }
    }

    private void runOneShotDirect(String task) {
        AnsiPrinter printer = new AnsiPrinter(!noColor && config.color());
        ConversationSession session = new ConversationSession(
                sessionId, config.sessionPersist(), config.tokenBudget());
        runSession(task, session, printer);
        System.exit(0);
    }

    private AgentRequest buildRequest(String task, ConversationSession session) {
        return AgentRequest.builder(task)
                .model(activeModel)
                .session(session)
                .stream(!noStream)
                .maxSteps(10)
                .build();
    }

    // ── Meta-commands ──────────────────────────────────────────────────────

    private void handleMeta(String line, ConversationSession session, AnsiPrinter printer) {
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "/help"     -> printHelp(printer);
            case "/strategy" -> {
                if (!arg.isBlank()) {
                    activeStrategy = arg;
                    printer.success("Strategy set to: " + activeStrategy);
                } else {
                    printer.info("Strategy: " + activeStrategy);
                    printer.info("Options: auto | direct | react | reflexion | multi");
                }
            }
            case "/model"    -> {
                if (!arg.isBlank()) { activeModel = arg; printer.success("Model: " + activeModel); }
                else printer.info("Model: " + activeModel);
            }
            case "/models"   -> listModels(printer);
            case "/skills"   -> listSkills(printer);
            case "/session"  -> printer.info("ID: " + session.id()
                    + "  Turns: " + session.turnCount()
                    + "  Tokens: ~" + session.tokenCount());
            case "/stats"    -> printer.println(tokenTracker.fullSummary());
            case "/budget"   -> {
                printer.info(budgetAdvisor.indicator(session));
                printer.info("Used: ~" + budgetAdvisor.usedTokens(session)
                    + " / " + budgetAdvisor.budget() + " tokens");
                String adv = budgetAdvisor.advisory(session);
                if (!adv.isBlank()) printer.warn(adv);
            }
            case "/metrics"  -> printer.println(toolMetrics.summary());
            case "/memory"   -> showMemory(printer, arg);
            case "/context"  -> printer.println(projectContext.contextBlock());
            case "/agui"     -> {
                activeAguiMode = !activeAguiMode;
                printer.info("AG-UI mode: " + (activeAguiMode ? "ON" : "OFF"));
            }
            case "/clear"    -> {
                session.clear(); tokenTracker.reset(); toolMetrics.reset();
                printer.info("Cleared.");
            }
            case "/compact"  -> doCompact(session, printer);
            case "/exit", "/quit", "/q" -> {
                printer.info("Goodbye! 🎶  " + tokenTracker.oneLiner());
                System.exit(0);
            }
            default -> printer.warn("Unknown: " + cmd + "  (/help for commands)");
        }
    }

    private void doCompact(ConversationSession session, AnsiPrinter printer) {
        if (session.turnCount() == 0) { printer.warn("Nothing to compact."); return; }
        printer.info("Compacting " + session.turnCount() + " turns with AI summary...");
        String summary = compactor.compact(session, activeModel);
        printer.success("Compacted. Summary: " + summary.length() + " chars, ~"
                + session.tokenCount() + " tokens remaining.");
    }

    private void showMemory(AnsiPrinter printer, String filter) {
        List<AgentMemory.MemoryEntry> entries = filter.isBlank()
                ? memory.relevant()
                : memory.relevant().stream()
                        .filter(e -> e.key().contains(filter) || e.value().contains(filter))
                        .toList();
        if (entries.isEmpty()) { printer.warn("No memories for this project yet."); return; }
        printer.sectionHeader("Memories (" + entries.size() + ")");
        entries.forEach(e -> printer.listItem("[" + e.type().name().charAt(0) + "] " + e.key(), e.value()));
    }

    private void listSkills(AnsiPrinter printer) {
        var skills = skillRegistry.listAll();
        if (skills.isEmpty()) { printer.warn("No skills loaded"); return; }
        printer.sectionHeader("Skills (" + skills.size() + ")");
        skills.forEach(s -> printer.listItem(s.name(), s.description()));
    }

    private void listModels(AnsiPrinter printer) {
        try {
            var models = sdk.listModels();
            if (models.isEmpty()) { printer.warn("No models. Run: gamelan models pull <n>"); return; }
            printer.sectionHeader("Models (" + models.size() + ")");
            models.forEach(m -> printer.listItem(m.getModelId(),
                    "[" + m.getFormat() + "] " + m.getSizeFormatted()
                    + (activeModel.equals(m.getModelId()) ? "  ← active" : "")));
        } catch (Exception e) { printer.error("Cannot list models: " + e.getMessage()); }
    }

    private void printHelp(AnsiPrinter printer) {
        printer.println("""

            @|bold,cyan REPL Commands|@
              /strategy [name]  Show or set strategy: auto|direct|react|reflexion|multi
              /model [name]     Show or switch model
              /models           List local models
              /skills           List loaded skills
              /session          Session stats
              /stats            Token usage summary
              /metrics          Per-tool latency and error metrics
              /memory [query]   Show remembered project facts
              /context          Show detected project context
              /agui             Toggle AG-UI event rendering
              /clear            Clear history + reset counters
              /compact          Compress conversation history
              /help             This help
              /exit             Quit (also Ctrl+D)

            @|bold,cyan Keyboard|@
              Ctrl+C   Cancel current task (stay in REPL)
              Ctrl+D   Exit
              ↑ / ↓    Browse history
            """);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String resolve(String override) {
        return (override != null && !override.isBlank()) ? override : config.defaultModel();
    }

    private int skillCount() {
        try { return skillRegistry.listAll().size(); } catch (Exception e) { return 0; }
    }
}
