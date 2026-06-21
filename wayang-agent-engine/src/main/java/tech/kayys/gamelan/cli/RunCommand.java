package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.*;
import tech.kayys.gamelan.agent.agui.AguiAgentRunner;
import tech.kayys.gamelan.agent.agui.AguiTerminalRenderer;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.hitl.HumanInTheLoop;
import tech.kayys.gamelan.planning.PlanRepository;
import tech.kayys.gamelan.planning.TaskPlanner;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Non-interactive one-shot task runner — integrates all four sections.
 *
 * <h2>Section IX — Planning</h2>
 * {@code --plan} activates the explicit planning step before execution.
 * The plan is displayed for review, then the agent executes each step.
 *
 * <h2>Section XI — HITL</h2>
 * {@code --auto-approve} bypasses approval gates (for CI).
 * Without it, write/run operations pause for user confirmation.
 *
 * <pre>
 * gamelan run "fix the NPE in UserService"                 # auto strategy
 * gamelan run --plan "refactor the auth module"             # plan first
 * gamelan run --strategy multi "full audit of src/"
 * gamelan run --auto-approve --no-stream "mvn test"        # CI mode
 * gamelan run --agui "task"                                 # AG-UI stream
 * gamelan run --strategy help                               # list strategies
 * </pre>
 */
@Command(
    name = "run",
    description = "Execute a one-shot task non-interactively",
    mixinStandardHelpOptions = true
)
public class RunCommand implements Runnable {

    @Inject OrchestratorSelector selector;
    @Inject AguiAgentRunner      aguiRunner;
    @Inject GamelanConfig        config;
    @Inject TaskPlanner          planner;
    @Inject PlanRepository       planRepo;
    @Inject HumanInTheLoop       hitl;

    @Parameters(index = "0", arity = "0..1", description = "Task description")
    String task;

    @Option(names = {"--file", "-f"},   description = "Read task from file")
    Path taskFile;

    @Option(names = {"-m", "--model"},  description = "Model override")
    String model;

    @Option(names = {"--strategy", "-s"},
            description = "Orchestration strategy: direct|react|reflexion|multi|pipeline|auto",
            defaultValue = "auto")
    String strategy;

    @Option(names = {"--plan"},         description = "Generate and show a plan before executing")
    boolean showPlan;

    @Option(names = {"--plan-only"},    description = "Generate plan but do not execute")
    boolean planOnly;

    @Option(names = {"--auto-approve"}, description = "Skip HITL approval gates (CI mode)")
    boolean autoApprove;

    @Option(names = {"--no-stream"},    description = "Disable streaming output")
    boolean noStream;

    @Option(names = {"--json"},         description = "Output as JSON")
    boolean jsonOutput;

    @Option(names = {"--agui"},         description = "Emit AG-UI SSE events")
    boolean aguiMode;

    @Option(names = {"--max-steps"}, defaultValue = "10")
    int maxSteps;

    @Option(names = {"--no-color"})
    boolean noColor;

    @Override
    public void run() {
        if ("help".equalsIgnoreCase(strategy)) { printStrategyHelp(); return; }

        String resolvedTask = resolveTask();
        boolean useColor    = !noColor && !jsonOutput && config.color();
        AnsiPrinter printer = new AnsiPrinter(useColor);

        // Section XI: configure HITL
        hitl.setAutoApprove(autoApprove);
        if (autoApprove) printer.warn("HITL: auto-approve mode active — gates bypassed");

        // Section IX: optional planning step
        if (showPlan || planOnly || planner.shouldPlan(resolvedTask)) {
            String m = model != null ? model : config.defaultModel();
            printer.info("Generating execution plan...");
            TaskPlanner.Plan plan = planner.plan(resolvedTask, m);
            planRepo.save(plan);

            printer.sectionHeader("Execution Plan");
            printer.println(plan.summary());

            if (planOnly) return;

            // Pause for confirmation if plan has human gates
            if (plan.hasHumanGates() && !autoApprove) {
                printer.warn("This plan has steps requiring human approval during execution.");
            }
        }

        // AG-UI mode
        if (aguiMode) {
            AguiTerminalRenderer renderer = new AguiTerminalRenderer(useColor);
            AgentRequest req = buildRequest(resolvedTask);
            aguiRunner.run(req, strategy.equals("auto") ? null : strategy, renderer);
            return;
        }

        // Normal execution
        AgentOrchestrator orch = selector.select(
                strategy.equals("auto") ? null : strategy, resolvedTask);

        if (!jsonOutput) printer.info("Strategy: " + orch.displayName());

        AgentRequest req = buildRequest(resolvedTask);
        long t0 = System.currentTimeMillis();
        OrchestratorResult result = orch.execute(req);

        if (jsonOutput) {
            System.out.println("{\"strategy\":\"" + result.strategy()
                    + "\",\"steps\":" + result.steps()
                    + ",\"success\":" + result.success()
                    + ",\"answer\":" + jsonString(result.answer()) + "}");
        } else {
            if (!req.stream()) System.out.println(result.answer());
            printer.agentFooter(result, System.currentTimeMillis() - t0);
        }

        System.exit(result.success() ? 0 : 1);
    }

    private void printStrategyHelp() {
        AnsiPrinter p = new AnsiPrinter(config.color());
        p.sectionHeader("Available Strategies");
        for (AgentOrchestrator o : selector.all())
            p.listItem(o.strategyId(), o.description());
        p.println("\nExtra options:");
        p.listItem("--plan",         "Generate a plan before executing");
        p.listItem("--plan-only",    "Show plan but do not execute");
        p.listItem("--auto-approve", "Skip HITL approval gates (CI mode)");
        p.listItem("--agui",         "Emit AG-UI protocol events");
    }

    private AgentRequest buildRequest(String task) {
        return AgentRequest.builder(task)
                .model(model)
                .session(new ConversationSession(null, config.sessionPersist(), config.tokenBudget()))
                .stream(!noStream && !jsonOutput && !aguiMode)
                .maxSteps(maxSteps)
                .build();
    }

    private String resolveTask() {
        if (task != null && !task.isBlank()) return task;
        if (taskFile != null) {
            try { return Files.readString(taskFile, StandardCharsets.UTF_8).strip(); }
            catch (IOException e) {
                throw new ParameterException(new CommandLine(this), "Cannot read task file: " + e.getMessage());
            }
        }
        if (System.console() == null) {
            try {
                String piped = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).strip();
                if (!piped.isBlank()) return piped;
            } catch (IOException ignored) {}
        }
        throw new ParameterException(new CommandLine(this), "Provide a task via argument, --file, or stdin.");
    }

    private String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n") + "\"";
    }
}
