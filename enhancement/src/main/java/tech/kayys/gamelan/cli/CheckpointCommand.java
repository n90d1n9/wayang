package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.*;
import tech.kayys.gamelan.agent.CheckpointManager;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Checkpoint management — list, resume, and delete saved agent sessions.
 *
 * <pre>
 * gamelan checkpoint list                  # list all saved checkpoints
 * gamelan checkpoint resume abc123         # resume by session ID prefix
 * gamelan checkpoint resume "fix the bug"  # resume by task description
 * gamelan checkpoint delete abc123         # delete a checkpoint
 * </pre>
 */
@Command(
    name = "checkpoint",
    description = "List and resume saved agent checkpoints",
    mixinStandardHelpOptions = true,
    subcommands = {
        CheckpointCommand.ListCmd.class,
        CheckpointCommand.ResumeCmd.class,
        CheckpointCommand.DeleteCmd.class
    }
)
public class CheckpointCommand implements Runnable {
    @Override public void run() { new CommandLine(this).usage(System.out); }

    // ── list ───────────────────────────────────────────────────────────────

    @Command(name = "list", aliases = {"ls"}, description = "List saved checkpoints")
    static class ListCmd implements Runnable {
        @Inject CheckpointManager checkpoints;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            var all = checkpoints.listAll();
            if (all.isEmpty()) {
                printer.warn("No checkpoints found. They are created automatically during agent tasks.");
                return;
            }
            printer.sectionHeader("Checkpoints (" + all.size() + ")");
            var fmt = DateTimeFormatter.ofPattern("MMM d HH:mm")
                    .withZone(ZoneId.systemDefault());
            all.forEach(cp -> printer.listItem(
                    cp.shortId() + " [" + fmt.format(cp.savedAt()) + "]",
                    cp.taskPreview() + "  model=" + cp.model()));
        }
    }

    // ── resume ─────────────────────────────────────────────────────────────

    @Command(name = "resume", description = "Resume a saved checkpoint and continue working")
    static class ResumeCmd implements Runnable {
        @Inject CheckpointManager    checkpoints;
        @Inject OrchestratorSelector selector;
        @Inject GamelanConfig        config;

        @Parameters(index = "0", description = "Session ID prefix or task keyword")
        String idOrKeyword;

        @Option(names = {"--task", "-t"}, description = "Override the task for this resume")
        String taskOverride;

        @Option(names = {"-m", "--model"})
        String model;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(config.color());
            var cpOpt = checkpoints.load(idOrKeyword);
            if (cpOpt.isEmpty()) {
                printer.error("No checkpoint found for: " + idOrKeyword);
                printer.info("Run 'gamelan checkpoint list' to see available checkpoints.");
                System.exit(1);
                return;
            }

            var cp = cpOpt.get();
            printer.sectionHeader("Resuming checkpoint " + cp.shortId());
            printer.info("Original task: " + cp.taskPreview());
            printer.info("Saved turns:   " + cp.messages().size() / 2);

            ConversationSession session = checkpoints.restore(cp);
            String effectiveModel    = model != null ? model : cp.model();
            String effectiveTask     = taskOverride != null ? taskOverride : cp.task();
            String effectiveStrategy = cp.strategy() != null ? cp.strategy() : "auto";

            AgentRequest req = AgentRequest.builder(effectiveTask)
                    .model(effectiveModel)
                    .session(session)
                    .stream(true)
                    .maxSteps(10)
                    .build();

            AgentOrchestrator orch = selector.select(
                    effectiveStrategy.equals("auto") ? null : effectiveStrategy, effectiveTask);

            long t0 = System.currentTimeMillis();
            var result = orch.execute(req);
            printer.agentFooter(result, System.currentTimeMillis() - t0);

            // Save the updated checkpoint
            checkpoints.save(session, effectiveTask, effectiveModel, effectiveStrategy);

            System.exit(result.success() ? 0 : 1);
        }
    }

    // ── delete ─────────────────────────────────────────────────────────────

    @Command(name = "delete", aliases = {"rm"}, description = "Delete a saved checkpoint")
    static class DeleteCmd implements Runnable {
        @Inject CheckpointManager checkpoints;

        @Parameters(index = "0", description = "Session ID to delete")
        String sessionId;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            if (checkpoints.delete(sessionId)) {
                printer.success("Deleted checkpoint: " + sessionId);
            } else {
                printer.warn("Checkpoint not found: " + sessionId);
            }
        }
    }
}
