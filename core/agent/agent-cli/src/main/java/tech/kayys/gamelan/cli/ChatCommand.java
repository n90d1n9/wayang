package tech.kayys.gamelan.cli;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import tech.kayys.gamelan.agent.AgentLoop;
import tech.kayys.gamelan.agent.AgentResponse;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interactive REPL and one-shot task execution command.
 */
@ApplicationScoped
@Command(
    name = "chat",
    description = "Start an interactive AI session (default when no subcommand is given)",
    mixinStandardHelpOptions = true
)
public class ChatCommand implements Runnable {

    @Inject AgentLoop agentLoop;
    @Inject GamelanConfig config;

    @Parameters(index = "0", arity = "0..1", description = "One-shot task (optional)")
    String oneShotTask;

    @Option(names = {"-m", "--model"}, description = "Model override", paramLabel = "MODEL")
    String modelOverride;

    @Option(names = {"--no-stream"}, description = "Disable streaming output")
    boolean noStream;

    @Option(names = {"-s", "--session"}, description = "Resume session ID", paramLabel = "SESSION_ID")
    String sessionId;

    @Override
    public void run() {
        if (oneShotTask != null && !oneShotTask.isBlank()) {
            runOneShot(oneShotTask, modelOverride, false, false);
        } else {
            startRepl(modelOverride, false, false);
        }
    }

    public void startRepl(String modelOverride, boolean verbose, boolean noColor) {
        AnsiPrinter printer = new AnsiPrinter(!noColor);
        ConversationSession session = new ConversationSession(sessionId);
        String model = resolveModel(modelOverride);

        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(new DefaultHistory())
                    .variable(LineReader.HISTORY_FILE,
                            Path.of(System.getProperty("user.home"), ".gamelan", "history"))
                    .variable(LineReader.BELL_STYLE, "none")
                    .build();

            printer.banner();
            printer.info("Model: " + model);
            printer.info("Skills: " + config.skillsDir() + " | Type /help for commands");
            printer.println();

            while (true) {
                String line;
                try {
                    line = reader.readLine(printer.prompt(session));
                } catch (UserInterruptException e) {
                    printer.warn("Interrupted. Press Ctrl+D to exit.");
                    agentLoop.cancelCurrentTask();
                    continue;
                } catch (EndOfFileException e) {
                    printer.info("Goodbye! 🎶");
                    break;
                }

                if (line == null || line.isBlank()) continue;

                if (line.startsWith("/")) {
                    handleMetaCommand(line.trim(), session, printer, model);
                    continue;
                }

                try {
                    AgentResponse response = agentLoop.process(line, session, model, !noStream);
                    printer.agentResponse(response, !noStream);
                    session.addTurn(line, response);
                } catch (Exception e) {
                    printer.error("Agent error: " + e.getMessage());
                    if (verbose) e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Terminal error: " + e.getMessage());
        }
    }

    public void runOneShot(String task, String modelOverride, boolean verbose, boolean noColor) {
        AnsiPrinter printer = new AnsiPrinter(!noColor);
        ConversationSession session = new ConversationSession(null);
        String model = resolveModel(modelOverride);
        printer.info("Running: " + task);
        try {
            AgentResponse response = agentLoop.process(task, session, model, true);
            printer.agentResponse(response, true);
            System.exit(response.hasError() ? 1 : 0);
        } catch (Exception e) {
            printer.error(e.getMessage());
            System.exit(1);
        }
    }

    private void handleMetaCommand(String line, ConversationSession session,
                                    AnsiPrinter printer, String model) {
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/help" -> printHelp(printer);
            case "/skills" -> agentLoop.getSkillRegistry().listAll()
                    .forEach(s -> printer.listItem(s.name(), s.description()));
            case "/models" -> {
                try {
                    agentLoop.getSdk().listModels()
                            .forEach(m -> printer.listItem(m.id(),
                                    m.format() + (m.contextWindow() > 0 ? " ctx:" + m.contextWindow() : "")));
                } catch (Exception e) {
                    printer.error("Cannot list models: " + e.getMessage());
                }
            }
            case "/clear" -> { session.clear(); printer.info("Session cleared."); }
            case "/session" -> printer.info("Session: " + session.id() + " | Turns: " + session.turnCount());
            case "/model" -> printer.info("Current model: " + model);
            case "/exit", "/quit", "/q" -> { printer.info("Goodbye! 🎶"); System.exit(0); }
            default -> printer.warn("Unknown command: " + cmd + ". Type /help.");
        }
    }

    private void printHelp(AnsiPrinter printer) {
        printer.println("""
            REPL Commands
              /help          Show this help
              /skills        List available skills
              /models        List local models
              /clear         Clear conversation history
              /session       Show session info
              /model         Show active model
              /exit          Exit Gamelan

            Keyboard shortcuts
              Ctrl+C         Cancel current task
              Ctrl+D         Exit
            """);
    }

    private String resolveModel(String override) {
        if (override != null && !override.isBlank()) return override;
        return config.defaultModel();
    }
}
