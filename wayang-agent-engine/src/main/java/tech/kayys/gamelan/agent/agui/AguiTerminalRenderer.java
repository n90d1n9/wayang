package tech.kayys.gamelan.agent.agui;

import tech.kayys.gamelan.util.AnsiPrinter;

import java.util.function.Consumer;

/**
 * Renders AG-UI events to the terminal.
 *
 * <p>Provides a {@link Consumer<AguiEvent>} that translates the AG-UI
 * event stream into readable terminal output. Used by the CLI when running
 * in AG-UI mode.
 *
 * <p>This keeps the AG-UI event protocol fully decoupled from terminal
 * rendering — the same events can be sent over SSE to a browser.
 */
public final class AguiTerminalRenderer implements Consumer<AguiEvent> {

    private final AnsiPrinter printer;
    private int               currentStep = -1;
    private final StringBuilder tokenBuffer = new StringBuilder();

    public AguiTerminalRenderer(boolean color) {
        this.printer = new AnsiPrinter(color);
    }

    @Override
    public void accept(AguiEvent event) {
        if (event == null) return;
        switch (event.type()) {
            case "RUN_STARTED"           -> printer.info("⟳ Agent started [" + shortId(event.runId()) + "]");
            case "STEP_STARTED"          -> { currentStep = event.step(); }
            case "TEXT_MESSAGE_START"    -> { tokenBuffer.setLength(0); }
            case "TEXT_MESSAGE_CONTENT"  -> {
                if (event.delta() != null) {
                    System.out.print(event.delta());
                    System.out.flush();
                    tokenBuffer.append(event.delta());
                }
            }
            case "TEXT_MESSAGE_END"      -> System.out.println();
            case "TOOL_CALL_START"       -> printer.info("  ⚙ " + event.toolName() + " …");
            case "TOOL_CALL_END"         -> {
                Object result = event.result();
                boolean ok = result instanceof java.util.Map<?,?> m
                        && Boolean.TRUE.equals(m.get("success"));
                printer.info("  " + (ok ? "✓" : "✗") + " " + event.toolName());
            }
            case "STEP_FINISHED"         -> { /* step boundary, no output needed */ }
            case "STATE_SNAPSHOT"        -> { /* consumed by UIs, not terminal */ }
            case "RUN_FINISHED"          -> {
                if (event.error() != null) {
                    printer.error("Run failed: " + event.error());
                }
            }
            case "ERROR"                 -> printer.error("Error: " + event.error());
            default                      -> { /* unknown event type, ignore */ }
        }
    }

    private String shortId(String id) {
        if (id == null) return "?";
        return id.length() > 8 ? id.substring(0, 8) : id;
    }
}
