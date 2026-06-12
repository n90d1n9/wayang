package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.*;
import tech.kayys.gamelan.agent.evolution.SkillEvolutionEngine;
import tech.kayys.gamelan.communication.AgentMessageBus;
import tech.kayys.gamelan.control.AgentControlPlane;
import tech.kayys.gamelan.economics.TokenEconomy;
import tech.kayys.gamelan.evaluation.BenchmarkHarness;
import tech.kayys.gamelan.execution.DeterministicExecutor;
import tech.kayys.gamelan.memory.hierarchy.*;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;
import tech.kayys.gamelan.safety.ConstraintSolver;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.util.List;

/**
 * CLI commands for all new architectural layers.
 *
 * <pre>
 * gamelan memory4 list                # 4-layer memory status
 * gamelan memory4 episodes            # recent episodes
 * gamelan memory4 knowledge [query]   # query semantic memory
 * gamelan memory4 procedures          # learned procedures
 *
 * gamelan plan "task description"     # generate HTN plan (no execution)
 *
 * gamelan run --enhanced              # run with all layers enabled
 * gamelan run --checkpoint <runId>    # resume from checkpoint
 * gamelan run --replay <runId>        # replay a previous run
 *
 * gamelan safety status               # safety summary + anomalies
 * gamelan safety rules                # list active rules
 * gamelan safety gate --tools run_command,git  # add approval gate
 *
 * gamelan economy status              # token budget report
 * gamelan economy model fast/std/exp  # override model tier
 *
 * gamelan eval run <skill>            # run benchmark suite
 * gamelan eval shadow <skill>         # shadow mode evaluation
 * gamelan eval regression             # check for regressions
 *
 * gamelan evolve <skill> [--dry-run]  # evolve a skill
 * gamelan evolve all [--dry-run]      # evolve all eligible skills
 *
 * gamelan hitl pause                  # pause agent
 * gamelan hitl resume                 # resume agent
 * gamelan hitl approve <requestId>    # approve pending gate
 * gamelan hitl abort                  # abort current run
 *
 * gamelan bus status                  # message bus statistics
 * gamelan bus blackboard              # show blackboard contents
 * </pre>
 */
@Command(
    name = "enhanced",
    description = "Commands for the enhanced architectural layers",
    mixinStandardHelpOptions = true,
    subcommands = {
        EnhancedCommands.Memory4Command.class,
        EnhancedCommands.PlanCommand.class,
        EnhancedCommands.SafetyCommand.class,
        EnhancedCommands.EconomyCommand.class,
        EnhancedCommands.EvalCommand.class,
        EnhancedCommands.EvolveCommand.class,
        EnhancedCommands.HitlCommand.class,
        EnhancedCommands.BusCommand.class,
        EnhancedCommands.CheckpointCommand.class
    }
)
public class EnhancedCommands implements Runnable {

    @Override public void run() { new CommandLine(this).usage(System.out); }

    // ── memory4 ───────────────────────────────────────────────────────────

    @Command(name = "memory4", description = "4-layer memory hierarchy commands",
             subcommands = { Memory4Command.Episodes.class, Memory4Command.Knowledge.class,
                             Memory4Command.Procedures.class, Memory4Command.Status.class })
    static class Memory4Command implements Runnable {
        @Override public void run() { new CommandLine(this).usage(System.out); }

        @Command(name = "status", description = "Show memory hierarchy status")
        static class Status implements Runnable {
            @Inject EpisodicMemory  episodic;
            @Inject SemanticMemory  semantic;
            @Inject ProceduralMemory procedural;

            @Override public void run() {
                AnsiPrinter p = new AnsiPrinter(true);
                p.sectionHeader("Memory Hierarchy Status");
                EpisodicMemory.EpisodeStats stats = episodic.stats();
                p.println("  Layer 1 (Working):   session-scoped (see /session)");
                p.println("  Layer 2 (Episodic):  " + stats.summary());
                p.println("  Layer 3 (Semantic):  " + semantic.allNodes().size() + " knowledge nodes");
                p.println("  Layer 4 (Procedural):" + procedural.all().size() + " learned procedures");
            }
        }

        @Command(name = "episodes", description = "Show recent episodes")
        static class Episodes implements Runnable {
            @Inject EpisodicMemory episodic;
            @Option(names = {"-n"}, defaultValue = "10") int limit;
            @Option(names = {"--failures"}) boolean failuresOnly;

            @Override public void run() {
                AnsiPrinter p = new AnsiPrinter(true);
                List<EpisodicMemory.Episode> eps = failuresOnly
                        ? episodic.recentFailures(limit)
                        : episodic.all().stream().limit(limit).toList();
                p.sectionHeader("Episodes (" + eps.size() + ")");
                eps.forEach(e -> p.listItem(
                        (e.success() ? "✓" : "✗") + " #" + e.id(),
                        e.task().length() > 80 ? e.task().substring(0, 80) + "…" : e.task()));
            }
        }

        @Command(name = "knowledge", description = "Query semantic memory")
        static class Knowledge implements Runnable {
            @Inject SemanticMemory semantic;
            @Parameters(index = "0", arity = "0..1", defaultValue = "") String query;
            @Option(names = {"-n"}, defaultValue = "10") int limit;

            @Override public void run() {
                AnsiPrinter p = new AnsiPrinter(true);
                List<SemanticMemory.KnowledgeNode> nodes = query.isBlank()
                        ? semantic.allNodes().values().stream().limit(limit).toList()
                        : semantic.query(query, limit);
                p.sectionHeader("Knowledge Nodes (" + nodes.size() + ")");
                nodes.forEach(n -> p.listItem(
                        String.format("[%s|%.2f] %s", n.type(), n.confidence(), n.concept()),
                        n.fact()));
            }
        }

        @Command(name = "procedures", description = "Show learned procedures")
        static class Procedures implements Runnable {
            @Inject ProceduralMemory procedural;

            @Override public void run() {
                AnsiPrinter p = new AnsiPrinter(true);
                List<ProceduralMemory.Procedure> procs = procedural.all();
                p.sectionHeader("Learned Procedures (" + procs.size() + ")");
                procs.forEach(proc -> p.listItem(
                        proc.name() + " (" + String.format("%.0f%%", proc.successRate() * 100) + ")",
                        proc.description()));
            }
        }
    }

    // ── plan ──────────────────────────────────────────────────────────────

    @Command(name = "plan", description = "Generate an HTN plan without executing")
    static class PlanCommand implements Runnable {
        @Inject HierarchicalTaskPlanner planner;
        @Parameters(index = "0") String task;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            p.sectionHeader("Generating plan for: " + task);
            HierarchicalTaskPlanner.Plan plan = planner.plan(task,
                    HierarchicalTaskPlanner.PlanningContext.defaults());
            p.println("Plan: " + plan.name() + " [" + plan.mode() + "]");
            p.println("Estimated tokens: " + plan.estimatedTokens());
            p.println("\nTasks:");
            plan.tasks().forEach(t -> p.listItem(
                    "[" + t.type() + "|" + t.risk() + "]", t.task()));
        }
    }

    // ── safety ────────────────────────────────────────────────────────────

    @Command(name = "safety", description = "Safety layer management",
             subcommands = { SafetyCommand.Status.class, SafetyCommand.Gate.class })
    static class SafetyCommand implements Runnable {
        @Override public void run() { new CommandLine(this).usage(System.out); }

        @Command(name = "status") static class Status implements Runnable {
            @Inject ConstraintSolver safety;
            @Override public void run() {
                AnsiPrinter p = new AnsiPrinter(true);
                ConstraintSolver.SafetySummary s = safety.summary();
                p.sectionHeader("Safety Status");
                p.println("  Rules active: " + s.rulesActive());
                p.println("  Total calls:  " + s.totalCalls());
                p.println("  Blocked:      " + s.blocked());
                p.println("  Warnings:     " + s.warnings());
                List<ConstraintSolver.AnomalyEvent> anomalies = safety.anomalies();
                if (!anomalies.isEmpty()) {
                    p.sectionHeader("Anomalies");
                    anomalies.forEach(a -> p.listItem(
                            "[" + a.severity() + "] " + a.type(), a.description()));
                }
            }
        }

        @Command(name = "gate") static class Gate implements Runnable {
            @Inject AgentControlPlane control;
            @Option(names = {"--tools"}, split = ",") List<String> tools;
            @Option(names = {"--pattern"}) String pattern;
            @Option(names = {"--cost"}) int cost;

            @Override public void run() {
                AnsiPrinter p = new AnsiPrinter(true);
                if (tools != null && !tools.isEmpty()) {
                    control.requireApprovalForTools(tools.toArray(new String[0]));
                    p.success("Gate added for tools: " + tools);
                }
                if (pattern != null) {
                    control.requireApprovalForPattern(pattern);
                    p.success("Gate added for pattern: " + pattern);
                }
                if (cost > 0) {
                    control.requireApprovalForCost(cost);
                    p.success("Gate added for cost threshold: " + cost + " tokens");
                }
            }
        }
    }

    // ── economy ───────────────────────────────────────────────────────────

    @Command(name = "economy", description = "Token economy management")
    static class EconomyCommand implements Runnable {
        @Inject TokenEconomy economy;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            TokenEconomy.EconomyReport r = economy.report();
            p.sectionHeader("Token Economy");
            p.println(r.summary());
            p.println("\nModel ladder:");
            r.modelLadder().forEach((tier, model) ->
                    p.listItem("  " + tier, model));
            if (!r.skillCosts().isEmpty()) {
                p.println("\nTop spenders:");
                r.skillCosts().entrySet().stream()
                        .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(5)
                        .forEach(e -> p.listItem("  " + e.getKey(), e.getValue() + " tokens"));
            }
        }
    }

    // ── eval ──────────────────────────────────────────────────────────────

    @Command(name = "eval", description = "Benchmark and evaluation",
             subcommands = { EvalCommand.Run.class, EvalCommand.Regression.class })
    static class EvalCommand implements Runnable {
        @Override public void run() { new CommandLine(this).usage(System.out); }

        @Command(name = "run") static class Run implements Runnable {
            @Inject BenchmarkHarness bench;
            @Parameters(index = "0") String skillName;
            @Override public void run() {
                AnsiPrinter p = new AnsiPrinter(true);
                p.info("Running benchmark for: " + skillName);
                BenchmarkHarness.BenchmarkReport r = bench.evaluate(skillName);
                p.sectionHeader("Benchmark Report");
                p.println(r.summary());
            }
        }

        @Command(name = "regression") static class Regression implements Runnable {
            @Inject BenchmarkHarness bench;
            @Parameters(index = "0") String skillName;
            @Override public void run() {
                AnsiPrinter p = new AnsiPrinter(true);
                List<BenchmarkHarness.RegressionAlert> alerts = bench.detectRegression(skillName);
                if (alerts.isEmpty()) { p.success("No regressions detected for: " + skillName); return; }
                p.sectionHeader("Regressions Detected");
                alerts.forEach(a -> p.listItem("⚠ " + a.metric(), a.message()));
            }
        }
    }

    // ── evolve ────────────────────────────────────────────────────────────

    @Command(name = "evolve", description = "Skill evolution (EvoSkill)")
    static class EvolveCommand implements Runnable {
        @Inject SkillEvolutionEngine evolution;
        @Parameters(index = "0", arity = "0..1") String skillName;
        @Option(names = {"--dry-run"}) boolean dryRun;
        @Option(names = {"--all"}) boolean all;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            if (all) {
                p.info("Running evolution cycle for all eligible skills...");
                List<SkillEvolutionEngine.EvolutionOutcome> outcomes = evolution.evolveAll(dryRun);
                p.sectionHeader("Evolution Results");
                outcomes.forEach(o -> p.listItem(o.skillName(), o.summary()));
            } else if (skillName != null) {
                // find skill and evolve
                p.info("Evolving skill: " + skillName + (dryRun ? " [DRY RUN]" : ""));
                p.warn("Use --all to evolve all eligible skills or specify skill via registry");
            } else {
                p.warn("Specify a skill name or --all");
            }
        }
    }

    // ── hitl ──────────────────────────────────────────────────────────────

    @Command(name = "hitl", description = "Human-in-the-loop control",
             subcommands = { HitlCommand.Pause.class, HitlCommand.Resume.class,
                             HitlCommand.Abort.class })
    static class HitlCommand implements Runnable {
        @Override public void run() { new CommandLine(this).usage(System.out); }

        @Command(name = "pause")  static class Pause  implements Runnable {
            @Inject AgentControlPlane control;
            @Override public void run() { control.pause(); System.out.println("⏸ Pause requested"); }
        }
        @Command(name = "resume") static class Resume implements Runnable {
            @Inject AgentControlPlane control;
            @Override public void run() { control.resume(); System.out.println("▶ Resumed"); }
        }
        @Command(name = "abort")  static class Abort  implements Runnable {
            @Inject AgentControlPlane control;
            @Override public void run() { control.abort(); System.out.println("⛔ Aborted"); }
        }
    }

    // ── bus ───────────────────────────────────────────────────────────────

    @Command(name = "bus", description = "Agent message bus status")
    static class BusCommand implements Runnable {
        @Inject AgentMessageBus bus;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            AgentMessageBus.Blackboard bb = bus.blackboard();
            p.sectionHeader("Blackboard (" + bb.size() + " entries)");
            bb.all().stream().limit(20).forEach(e ->
                    p.listItem("[" + e.agentId() + "|" + String.format("%.2f", e.confidence()) + "] " + e.key(),
                            e.value().toString()));
            List<AgentMessageBus.AgentMessage> dl = bus.deadLetters();
            if (!dl.isEmpty()) {
                p.warn(dl.size() + " undelivered messages in dead letter queue");
            }
        }
    }

    // ── checkpoint ────────────────────────────────────────────────────────

    @Command(name = "checkpoints", description = "Manage execution checkpoints")
    static class CheckpointCommand implements Runnable {
        @Inject DeterministicExecutor executor;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            List<DeterministicExecutor.RunSummary> runs = executor.listRuns();
            p.sectionHeader("Checkpointed Runs (" + runs.size() + ")");
            runs.stream().limit(20).forEach(r ->
                    p.listItem(r.runId() + " (" + r.steps() + " steps)",
                            r.task().length() > 70 ? r.task().substring(0, 70) + "…" : r.task()));
        }
    }

    // ── Layer VII: DAG execution ──────────────────────────────────────────

    @Command(name = "dag", description = "Execute a task as a DAG of parallel steps")
    static class DagCommand implements Runnable {
        @Inject DagExecutionEngine dagEngine;

        @Option(names = "--task",   required = true,  description = "Top-level task to decompose")
        String task;

        @Option(names = "--steps",  defaultValue = "3", description = "Number of parallel steps")
        int steps;

        @Option(names = "--policy", defaultValue = "SKIP_ON_FAILURE",
                description = "Failure policy: ABORT_ON_FAILURE | SKIP_ON_FAILURE | CONTINUE_ON_FAILURE")
        String policy;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            p.sectionHeader("DAG Execution");
            p.info("Task: " + task);
            p.info("Policy: " + policy);

            // Build a simple linear DAG for now; production would use GoT to decompose
            List<DagExecutionEngine.DagNode> nodes = new java.util.ArrayList<>();
            nodes.add(DagExecutionEngine.DagNode.node("analyze", "Analyze: " + task).build());
            nodes.add(DagExecutionEngine.DagNode.node("execute", "Execute: " + task)
                    .dependsOn("analyze").build());
            nodes.add(DagExecutionEngine.DagNode.node("verify",  "Verify: " + task)
                    .dependsOn("execute").build());

            DagExecutionEngine.FailurePolicy fp;
            try { fp = DagExecutionEngine.FailurePolicy.valueOf(policy); }
            catch (IllegalArgumentException e) { fp = DagExecutionEngine.FailurePolicy.SKIP_ON_FAILURE; }

            DagExecutionEngine.ExecutionDag dag = dagEngine.build(nodes);
            p.info("Critical path length: " + dag.criticalPathLength());

            DagExecutionEngine.DagResult result = dagEngine.execute(dag, fp,
                    nr -> p.listItem("[" + nr.nodeId() + "]",
                            nr.success() ? "✓ " + nr.elapsed().toMillis() + "ms"
                                        : "✗ " + nr.error()));

            p.sectionHeader("DAG Result");
            p.info(result.summary());
            result.nodeResults().forEach((id, nr) -> {
                String status = nr.success() ? "✓" : "✗";
                p.listItem(status + " " + id + " (" + nr.elapsed().toMillis() + "ms)",
                        nr.output().length() > 120 ? nr.output().substring(0, 120) + "…" : nr.output());
            });
        }
    }

    @Command(name = "actor-health", description = "Show actor system health for tool routing")
    static class ActorHealthCommand implements Runnable {
        @Inject tech.kayys.gamelan.execution.actor.ToolActorRouter router;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            tech.kayys.gamelan.execution.actor.ToolActorRouter.SystemHealth h = router.health();
            p.sectionHeader("Actor System Health");
            p.info(h.summary());
            h.actorMetrics().forEach((name, metrics) ->
                    p.listItem(name, metrics.toString()));
        }
    }

    @Command(name = "replay", description = "Manage execution replay recordings")
    static class ReplayCommand implements Runnable {
        @Inject tech.kayys.gamelan.execution.replay.ReplayEngine replayEngine;

        @Option(names = "--list",   description = "List all recordings")   boolean list;
        @Option(names = "--verify", description = "Verify integrity of a recording") String verifyId;
        @Option(names = "--replay", description = "Replay a recording")    String replayId;
        @Option(names = "--from",   defaultValue = "0")                    int fromStep;
        @Option(names = "--delete", description = "Delete a recording")    String deleteId;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            try {
                if (list || (verifyId == null && replayId == null && deleteId == null)) {
                    p.sectionHeader("Replay Recordings");
                    replayEngine.listRecordings().forEach(m ->
                            p.listItem(m.runId() + " (" + m.stepCount() + " steps)",
                                    m.task().length() > 60 ? m.task().substring(0, 60) + "…" : m.task()));

                } else if (verifyId != null) {
                    var result = replayEngine.verify(verifyId);
                    p.sectionHeader("Integrity Check");
                    p.info(result.summary());
                    result.violations().forEach(v -> p.warn("VIOLATION: " + v));

                } else if (replayId != null) {
                    p.info("Replaying '" + replayId + "' from step " + fromStep + "…");
                    var result = replayEngine.replay(replayId, fromStep);
                    p.sectionHeader("Replay Result");
                    p.info(result.summary());
                    p.info("Matches original: " + result.matchesOriginal());

                } else if (deleteId != null) {
                    replayEngine.delete(deleteId);
                    p.info("Deleted recording: " + deleteId);
                }
            } catch (Exception e) {
                p.error("Replay error: " + e.getMessage());
            }
        }
    }

    // ── Layer VIII: Memory ────────────────────────────────────────────────

    @Command(name = "memory-consolidate",
             description = "Consolidate episodic memory into semantic knowledge")
    static class MemoryConsolidateCommand implements Runnable {
        @Inject tech.kayys.gamelan.memory.consolidation.MemoryConsolidationPipeline pipeline;

        @Option(names = "--force", description = "Force consolidation even below threshold")
        boolean force;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            p.sectionHeader("Memory Consolidation");
            p.info("Running " + (force ? "forced" : "threshold-triggered") + " consolidation…");

            var result = pipeline.consolidate(force);
            p.info(result.summary());

            if (result.ran()) {
                var run = result.run();
                p.listItem("New AKUs", String.valueOf(run.newAKUs()));
                p.listItem("New procedures", String.valueOf(run.newProcedures()));
                p.listItem("Contradictions resolved", String.valueOf(run.contradictionsResolved()));
                p.listItem("Episodes pruned", String.valueOf(run.episodesPruned()));
                p.listItem("Facts reinforced", String.valueOf(run.factsReinforced()));
                p.listItem("Duration", run.elapsed().toMillis() + "ms");
            }

            p.sectionHeader("Memory Health");
            var health = pipeline.health();
            p.info(health.summary());
        }
    }

    @Command(name = "memory-stats", description = "Show working memory and hierarchy statistics")
    static class MemoryStatsCommand implements Runnable {
        @Inject tech.kayys.gamelan.memory.working.WorkingMemoryManager workingMemory;
        @Inject tech.kayys.gamelan.memory.consolidation.MemoryConsolidationPipeline pipeline;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);

            p.sectionHeader("Working Memory");
            var stats = workingMemory.stats();
            p.info(stats.summary());
            p.listItem("Messages", String.valueOf(stats.messageCount()));
            p.listItem("Tokens used", stats.tokenCount() + " / " + stats.tokenBudget());
            p.listItem("Utilization", String.format("%.1f%%", stats.utilizationRate() * 100));
            p.listItem("Pinned", String.valueOf(stats.pinnedCount()));

            p.sectionHeader("Memory Hierarchy");
            var health = pipeline.health();
            p.info(health.summary());
            p.listItem("Episodes", String.valueOf(health.episodeCount()));
            p.listItem("Stale episodes", String.valueOf(health.staleEpisodes()));
            p.listItem("Semantic AKUs", String.valueOf(health.semanticNodes()));
            p.listItem("Procedures", String.valueOf(health.procedures()));
            p.listItem("Consolidations run", String.valueOf(health.consolidationCount()));
        }
    }

    // ── Layer IX: Planning ────────────────────────────────────────────────

    @Command(name = "got-plan", description = "Plan a task using Graph-of-Thought + MCTS")
    static class GotPlanCommand implements Runnable {
        @Inject GraphOfThoughtPlanner gotPlanner;

        @Option(names = "--task",        required = true, description = "Task to plan")
        String task;

        @Option(names = "--simulations", defaultValue = "20",
                description = "Number of MCTS simulations (quality vs speed)")
        int simulations;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            p.sectionHeader("Graph-of-Thought Planning");
            p.info("Task: " + task);
            p.info("MCTS simulations: " + simulations);

            GraphOfThoughtPlanner.ThoughtGraph graph = gotPlanner.plan(task, simulations);

            p.info(graph.summary());
            p.sectionHeader("Best Reasoning Path (" + graph.bestPath().size() + " steps)");
            int step = 1;
            for (var node : graph.bestPath()) {
                p.listItem("Step " + step++ + " (score=" + String.format("%.2f", node.avgScore()) + ")",
                        node.thought().length() > 120 ? node.thought().substring(0, 120) + "…"
                                                      : node.thought());
            }

            p.sectionHeader("Full Reasoning");
            String reasoning = graph.bestReasoning();
            p.info(reasoning.length() > 1000 ? reasoning.substring(0, 1000) + "\n…" : reasoning);
        }
    }

    @Command(name = "plan-versions", description = "Manage and compare plan versions")
    static class PlanVersionsCommand implements Runnable {
        @Inject PlanVersionStore versionStore;

        @Option(names = "--task",    description = "Show history for a task")   String task;
        @Option(names = "--compare", description = "Two version IDs to compare", arity = "2") String[] compare;
        @Option(names = "--tag",     description = "Tag a version ID")           String tagId;
        @Option(names = "--with",    description = "Tag label to apply")         String tagLabel;
        @Option(names = "--find-tag",description = "Find all versions with tag") String findTag;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);

            if (task != null) {
                p.sectionHeader("Plan History: " + task);
                versionStore.history(task).forEach(v -> {
                    String metrics = v.hasMetrics()
                            ? String.format("success=%.0f%% tokens=%d",
                                    v.metrics().successRate() * 100, v.metrics().actualTokens())
                            : "no metrics";
                    p.listItem(v.id().substring(0, 8) + " [" + String.join(",", v.tags()) + "]",
                            metrics + " — " + v.createdAt());
                });

            } else if (compare != null && compare.length == 2) {
                p.sectionHeader("Plan Diff");
                var diff = versionStore.compare(compare[0], compare[1]);
                p.info(diff.summary());
                if (!diff.tasksAdded().isEmpty())   p.listItem("Tasks added",   diff.tasksAdded().toString());
                if (!diff.tasksRemoved().isEmpty()) p.listItem("Tasks removed", diff.tasksRemoved().toString());
                if (diff.modeChanged())             p.listItem("Mode changed",  diff.modeA() + " → " + diff.modeB());
                p.listItem("Token delta", (diff.tokenDelta() >= 0 ? "+" : "") + diff.tokenDelta());

            } else if (tagId != null && tagLabel != null) {
                versionStore.tag(tagId, tagLabel);
                p.info("Tagged version " + tagId.substring(0, 8) + " with '" + tagLabel + "'");

            } else if (findTag != null) {
                p.sectionHeader("Versions tagged: " + findTag);
                versionStore.findByTag(findTag).forEach(v ->
                        p.listItem(v.id().substring(0, 8), v.task().substring(0, Math.min(60, v.task().length()))));

            } else {
                p.info("Total plan versions stored: " + versionStore.totalVersions());
                p.info("Use --task, --compare, --tag/--with, or --find-tag");
            }
        }
    }

    @Command(name = "cost-plan", description = "Estimate and optimize plan token costs")
    static class CostPlanCommand implements Runnable {
        @Inject CostAwarePlanner costPlanner;
        @Inject HierarchicalTaskPlanner htn;

        @Option(names = "--task",   required = true, description = "Task to cost-estimate")
        String task;

        @Option(names = "--budget", description = "Token budget override (default=configured)")
        Integer budget;

        @Option(names = "--optimize", description = "Optimize plan for budget")
        boolean optimize;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            p.sectionHeader("Cost-Aware Planning");

            // Get historical cost profile
            var profile = costPlanner.historicalProfile(task);
            p.sectionHeader("Historical Profile");
            p.info(profile.summary());

            // Build a plan via HTN
            var plan = htn.plan(task, null, null);
            p.sectionHeader("Raw Plan Estimate");
            var estimate = costPlanner.estimate(plan);
            p.info(estimate.summary());
            p.listItem("Execution tokens", String.valueOf(estimate.executionTokens()));
            p.listItem("System prompt tokens", String.valueOf(estimate.systemPromptTokens()));
            p.listItem("Synthesis tokens", String.valueOf(estimate.synthesisTokens()));

            if (optimize) {
                p.sectionHeader("Optimized Plan");
                var optimized = costPlanner.optimize(plan, task, budget);
                p.info(optimized.explanation());
                p.listItem("Budget tier", optimized.budgetTier().toString());
                p.listItem("Action taken", optimized.action().toString());
                p.listItem("Optimized tokens", String.valueOf(optimized.estimate().totalTokens()));
                p.listItem("Steps after pruning",
                        String.valueOf(optimized.plan().tasks().size()));
            }
        }
    }
}
