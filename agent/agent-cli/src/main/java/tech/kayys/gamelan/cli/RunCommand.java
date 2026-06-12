package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import tech.kayys.gamelan.agent.AgentLoop;
import tech.kayys.gamelan.agent.AgentResponse;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.util.AnsiPrinter;

/**
 * Non-interactive one-shot task execution command.
 *
 * <p>Accepts a task description, runs the agent loop once, and exits.
 * Suitable for scripting and CI pipelines.
 *
 * <pre>
 * Usage:
 *   gamelan run "add unit tests to UserService.java"
 *   gamelan run --model qwen2 "explain the architecture of this repo"
 *   gamelan run --no-stream "fix the bug in payment.go" > result.md
 * </pre>
 */
@Command(
    name = "run",
    description = "Run a one-shot task non-interactively",
    mixinStandardHelpOptions = true
)
public class RunCommand implements Runnable {

    @Inject
    AgentLoop agentLoop;

    @Inject
    GamelanConfig config;

    @Parameters(index = "0", description = "Task description", paramLabel = "TASK")
    String task;

    @Option(names = {"-m", "--model"}, description = "Model override")
    String model;

    @Option(names = {"--no-stream"}, description = "Disable streaming")
    boolean noStream;

    @Option(names = {"--json"}, description = "Output result as JSON")
    boolean jsonOutput;

    @Override
    public void run() {
        String effectiveModel = model != null ? model : config.defaultModel();
        AnsiPrinter printer = new AnsiPrinter(!jsonOutput);

        try {
            AgentResponse response = agentLoop.process(
                    task,
                    new ConversationSession(null),
                    effectiveModel,
                    !noStream && !jsonOutput);

            if (jsonOutput) {
                System.out.println(response.toJson());
            } else {
                printer.agentResponse(response, !noStream);
            }

            System.exit(response.hasError() ? 1 : 0);
        } catch (Exception e) {
            printer.error(e.getMessage());
            System.exit(1);
        }
    }
}
