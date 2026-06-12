package tech.kayys.gamelan.cli;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.*;

/**
 * Gamelan CLI entry point.
 *
 * <h2>Command tree</h2>
 * <pre>
 * gamelan [task]              # interactive REPL or one-shot
 * gamelan chat [task]         # explicit REPL
 * gamelan run TASK            # non-interactive, exits 0/1
 * gamelan watch DIR           # file-watch mode
 * gamelan workflow ...        # multi-step workflow presets
 * gamelan skill ...           # skill management
 * gamelan models ...          # model management
 * gamelan memory ...          # persistent memory management
 * gamelan checkpoint ...      # save/resume agent sessions
 * gamelan config ...          # configuration
 * </pre>
 */
@QuarkusMain
@Command(
    name = "gamelan",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = {
        "@|bold,cyan Gamelan CLI|@ — Agentic AI for local software development",
        "Powered by the Gollek inference engine + agentskills.io skills system",
        ""
    },
    subcommands = {
        ChatCommand.class,
        RunCommand.class,
        WatchCommand.class,
        WorkflowCommand.class,
        SkillCommand.class,
        ModelCommand.class,
        MemoryCommand.class,
        CheckpointCommand.class,
        EvalCommand.class,
        PlanCommand.class,
        ConfigCommand.class
    }
)
public class GamelanApplication implements Runnable, QuarkusApplication {

    @Inject IFactory    factory;
    @Inject ChatCommand chatCommand;

    @Option(names = {"-m", "--model"},   description = "Model override for the REPL")
    String modelOverride;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    boolean verbose;

    @Option(names = {"--no-color"},      description = "Disable ANSI colour")
    boolean noColor;

    /** Default: no subcommand → start the interactive REPL. */
    @Override
    public void run() {
        chatCommand.startRepl(modelOverride, verbose, noColor);
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory)
                .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                    System.err.println("[ERROR] " + ex.getMessage());
                    if (verbose) ex.printStackTrace(System.err);
                    return 1;
                })
                .execute(args);
    }
}
