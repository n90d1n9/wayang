package tech.kayys.wayang.tui.ui;
import tech.kayys.wayang.sdk.agent.WayangAgent;
import tech.kayys.wayang.sdk.agent.WayangAgentListener;

import tech.kayys.wayang.sdk.agent.WayangAgentListener;

import tech.kayys.wayang.tools.spi.Tool;

import tech.kayys.wayang.tools.spi.ToolResult;

import tech.kayys.wayang.sdk.json.Json;

import tech.kayys.wayang.sdk.json.JsonValue;

import tech.kayys.wayang.sdk.agent.PermissionDecision;

import tech.kayys.wayang.tui.term.Ansi;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Writes directly to the terminal's raw output stream. Because the
 * terminal is put into `stty raw` mode, OPOST (output processing) is
 * disabled -- the driver will NOT translate "\n" into "\r\n" for us,
 * so every line break here must be explicit "\r\n" or the cursor will
 * "stair-step" right instead of returning to column 1.
 */
public final class TermOut {

    private final OutputStream raw;

    public TermOut(OutputStream raw) {
        this.raw = raw;
    }

    public void write(String s) {
        try {
            raw.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Output stream broken (e.g. terminal closed) -- nothing useful to do but stop.
        }
    }

    public void flush() {
        try { raw.flush(); } catch (IOException ignored) {}
    }

    /** Writes text followed by an explicit CRLF (required since OPOST is off in raw mode). */
    public void line(String s) {
        write(s);
        write("\r\n");
    }

    public void crlf() { write("\r\n"); }

    public void moveTo(int row, int col) { write(Ansi.cursorTo(row, col)); }

    public void clearLine() { write(Ansi.CLEAR_LINE); }

    public void setScrollRegion(int top, int bottom) { write(Ansi.setScrollRegion(top, bottom)); }

    public void resetScrollRegion() { write(Ansi.resetScrollRegion()); }

    public void hideCursor() { write(Ansi.HIDE_CURSOR); }

    public void showCursor() { write(Ansi.SHOW_CURSOR); }
}
