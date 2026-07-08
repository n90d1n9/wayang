package tech.kayys.wayang.tui.ui;
import tech.kayys.wayang.sdk.agent.WayangAgent;
import tech.kayys.wayang.sdk.agent.WayangAgentListener;

import tech.kayys.wayang.sdk.agent.WayangAgentListener;

import tech.kayys.wayang.tools.spi.ToolResult;

import tech.kayys.wayang.sdk.agent.PermissionDecision;

import tech.kayys.wayang.sdk.agent.*;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tui.config.Config;
import tech.kayys.wayang.sdk.json.Json;
import tech.kayys.wayang.sdk.json.JsonValue;
import tech.kayys.wayang.tui.render.MarkdownRenderer;
import tech.kayys.wayang.tui.render.TextWrap;
import tech.kayys.wayang.tui.render.Theme;
import tech.kayys.wayang.tui.term.*;
import tech.kayys.wayang.tools.spi.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.List;

/**
 * Full-screen "panel" UI: a lazygit/htop-style layout with a header,
 * a sidebar showing session info, a scrolling chat pane, and a fixed
 * input bar. Unlike the REPL UI (which uses native terminal scrollback
 * and incremental appends), this mode owns the whole screen via the
 * alternate buffer and redraws a complete frame on every update --
 * simpler to reason about for a multi-pane layout, at the cost of not
 * using the terminal's own scrollback.
 */
public final class PanelUi {

    private final Config config;
    private final WayangAgent agent;
    
    private final TerminalMode termMode = new TerminalMode();
    private TermOut out;
    private KeyDecoder keys;
    private final InputBuffer input = new InputBuffer();
    private final Object lock = new Object();

    private volatile boolean running = true;
    private volatile boolean streaming = false;
    private volatile Consumer<PermissionDecision> pendingPermissionResponder;
    private volatile String pendingToolName;
    private volatile Thread agentThread;

    private volatile int polledCols = 100, polledRows = 30;
    private int cols = 100, rows = 30;

    private final List<Entry> entries = new ArrayList<>();
    private volatile Entry liveAssistantEntry;
    private int scrollOffset = 0; // 0 = following the latest content
    private int toolCallCount = 0, toolErrorCount = 0;

    private String exitAction = null;

    private enum Kind { USER, ASSISTANT, TOOL_CALL, TOOL_RESULT, SYSTEM, ERROR, PERMISSION }

    private static final class Entry {
        Kind kind;
        StringBuilder text = new StringBuilder();
        boolean isError;
        Entry(Kind k, String initial) { kind = k; text.append(initial); }
    }

    public PanelUi(Config config, WayangAgent agent) {
        this.config = config;
        this.agent = agent;
            }

    public String run() throws IOException {
        PrintStream utf8Out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        System.setOut(utf8Out);
        out = new TermOut(new FileOutputStream(FileDescriptor.out));
        keys = new KeyDecoder(System.in);

        boolean raw = termMode.enterRaw();
        if (!raw) {
            // Panel mode fundamentally needs a real terminal; fall back to the REPL's line mode instead.
            System.out.println("Panel mode requires an interactive terminal; falling back to line mode.");
            return new ReplUi(config, agent, null, null).run();
        }

        try {
            runInteractive();
        } finally {
            out.write(Ansi.DISABLE_ALT_BUFFER);
            out.showCursor();
            out.flush();
            termMode.restore();
        }
        return exitAction;
    }

    private void runInteractive() throws IOException {
        Thread poller = new Thread(this::pollSizeLoop, "size-poller-panel");
        poller.setDaemon(true);
        poller.start();

        int[] s = TerminalMode.size();
        polledCols = s[0]; polledRows = s[1];
        cols = s[0]; rows = s[1];

        out.write(Ansi.ENABLE_ALT_BUFFER);
        out.write(Ansi.HIDE_CURSOR);

        entries.add(systemEntry("Panel mode -- " + (agent.tools().isEmpty() ? "chat" : "agent") + " session started. /help for commands."));
        render();

        while (running) {
            Key key;
            try { key = keys.readKey(); } catch (IOException e) { break; }

            if (cols != polledCols || rows != polledRows) {
                cols = polledCols; rows = polledRows;
            }

            if (pendingPermissionResponder != null) {
                handlePermissionKey(key);
                render();
                continue;
            }

            if (streaming) {
                if (key.kind() == Key.Kind.CTRL_C) {
                    Thread t = agentThread;
                    if (t != null) t.interrupt();
                } else if (key.kind() != Key.Kind.ENTER) {
                    input.apply(key);
                    render();
                }
                continue;
            }

            handleKeyIdle(key);
            render();
        }
    }

    private void pollSizeLoop() {
        while (running) {
            int[] s = TerminalMode.size();
            polledCols = s[0]; polledRows = s[1];
            try { Thread.sleep(400); } catch (InterruptedException e) { return; }
        }
    }

    private void handleKeyIdle(Key key) {
        switch (key.kind()) {
            case CTRL_C -> { if (input.isEmpty()) running = false; else input.clear(); }
            case CTRL_D -> { if (input.isEmpty()) running = false; }
            case CTRL_L -> { /* full redraw happens every frame anyway */ }
            case PAGE_UP -> scrollOffset += Math.max(1, visibleChatHeight() - 2);
            case PAGE_DOWN -> scrollOffset = Math.max(0, scrollOffset - Math.max(1, visibleChatHeight() - 2));
            case ENTER -> submit();
            default -> input.apply(key);
        }
    }

    private void handlePermissionKey(Key key) {
        PermissionDecision decision = null;
        if (key.isChar()) {
            char c = Character.toLowerCase((char) key.codePoint());
            if (c == 'y') decision = PermissionDecision.APPROVE_ONCE;
            else if (c == 'a') decision = PermissionDecision.APPROVE_ALWAYS_THIS_TOOL;
            else if (c == 'n') decision = PermissionDecision.DENY;
        } else if (key.kind() == Key.Kind.ESCAPE || key.kind() == Key.Kind.CTRL_C) {
            decision = PermissionDecision.DENY;
        }
        if (decision == null) return;

        String label = switch (decision) {
            case APPROVE_ONCE -> "approved (once)";
            case APPROVE_ALWAYS_THIS_TOOL -> "approved (always for " + pendingToolName + ")";
            case DENY -> "denied";
        };
        entries.add(systemEntry("  " + label));

        Consumer<PermissionDecision> responder = pendingPermissionResponder;
        pendingPermissionResponder = null;
        pendingToolName = null;
        responder.accept(decision);
    }

    private void submit() {
        String text = input.text().strip();
        input.clear();
        if (text.isEmpty()) return;

        if (text.startsWith("/")) {
            handleSlash(text);
            return;
        }

        entries.add(new Entry(Kind.USER, text));
        scrollOffset = 0;
        streaming = true;

        WayangAgentListener listener = new PanelListener();
        agentThread = new Thread(() -> {
            try {
                agent.send(text, listener);
            } finally {
                streaming = false;
                render();
            }
        }, "agent-turn-panel");
        agentThread.setDaemon(true);
        agentThread.start();
    }

    private void handleSlash(String cmd) {
        String[] parts = cmd.split("\\s+", 2);
        switch (parts[0]) {
            case "/quit", "/exit" -> running = false;
            case "/help" -> entries.add(systemEntry(String.join("\n",
                    "Commands: /help /clear /agent chat|agent /repl /quit",
                    "Keys: Enter send · Shift+Enter newline · PgUp/PgDn scroll · Ctrl+C clear/exit")));
            case "/clear" -> { agent.clearHistory(); entries.clear(); toolCallCount = 0; toolErrorCount = 0; }
            case "/agent" -> {
                String mode = parts.length > 1 ? parts[1].strip().toLowerCase() : "";
                if (mode.equals("chat")) agent.setTools(List.of());
                else if (mode.equals("agent")) agent.setTools(new ArrayList<>(agent.tools()));
                entries.add(systemEntry("mode: " + (agent.tools().isEmpty() ? "chat" : "agent")));
            }
            case "/repl" -> { exitAction = "repl"; running = false; }
            default -> entries.add(errorEntry("Unknown command: " + parts[0]));
        }
    }

    private Entry systemEntry(String text) { return new Entry(Kind.SYSTEM, text); }
    private Entry errorEntry(String text) { return new Entry(Kind.ERROR, text); }

    // ---------- rendering ----------

    private int visibleChatHeight() {
        int inputLines = Math.max(1, input.lineCount());
        // Fixed rows: header(1) + topBorder(1) + bottomBorder(1) + hint(1) = 4, plus inputLines.
        return Math.max(3, rows - 4 - inputLines);
    }

    private void render() {
        synchronized (lock) {
            int headerH = 1;
            int inputLines = Math.max(1, input.lineCount());
            // Fixed rows: header(1) + topBorder(1) + bottomBorder(1) + hint(1) = 4, plus inputLines.
            int bodyH = Math.max(3, rows - 4 - inputLines);
            int sidebarW = Math.max(18, Math.min(28, cols / 4));
            int chatW = Math.max(20, cols - sidebarW - 3);

            StringBuilder fb = new StringBuilder(cols * rows / 2 + 256);

            // Header
            fb.append(Ansi.cursorTo(1, 1)).append(Ansi.CLEAR_LINE);
            String modeLabel = agent.tools().isEmpty() ? "chat" : "agent";
            String header = Ansi.BOLD + Ansi.fg(Theme.ACCENT) + " agentic-tui " + Ansi.RESET +
                    Ansi.dim("· " + modeLabel + " mode ");
            fb.append(padVisible(header, cols));

            // Border under header
            int topBorderRow = 2;
            fb.append(Ansi.cursorTo(topBorderRow, 1)).append(Ansi.CLEAR_LINE);
            fb.append(Ansi.fg(Theme.BORDER));
            fb.append("├").append("─".repeat(Math.max(0, sidebarW))).append("┬").append("─".repeat(Math.max(0, chatW))).append("┤");
            fb.append(Ansi.RESET);

            // Body: sidebar | chat
            List<String> sidebarLines = sidebarLines(sidebarW, bodyH);
            List<String> chatLines = chatLines(chatW, bodyH);

            for (int i = 0; i < bodyH; i++) {
                int row = topBorderRow + 1 + i;
                fb.append(Ansi.cursorTo(row, 1)).append(Ansi.CLEAR_LINE);
                fb.append(Ansi.fg(Theme.BORDER)).append("│").append(Ansi.RESET);
                String sLine = i < sidebarLines.size() ? sidebarLines.get(i) : "";
                fb.append(padVisible(sLine, sidebarW));
                fb.append(Ansi.fg(Theme.BORDER)).append("│").append(Ansi.RESET);
                String cLine = i < chatLines.size() ? chatLines.get(i) : "";
                fb.append(padVisible(cLine, chatW));
                fb.append(Ansi.fg(Theme.BORDER)).append("│").append(Ansi.RESET);
            }

            int bottomBorderRow = topBorderRow + 1 + bodyH;
            fb.append(Ansi.cursorTo(bottomBorderRow, 1)).append(Ansi.CLEAR_LINE);
            fb.append(Ansi.fg(Theme.BORDER));
            fb.append("├").append("─".repeat(Math.max(0, sidebarW))).append("┴").append("─".repeat(Math.max(0, chatW))).append("┤");
            fb.append(Ansi.RESET);

            // Input box
            String[] inputTextLines = input.lines();
            int cursorPos = input.cursor();
            String fullText = input.text();
            int cursorLineIdx = 0;
            for (int i = 0; i < cursorPos && i < fullText.length(); i++) if (fullText.charAt(i) == '\n') cursorLineIdx++;
            int lineStartIdx = fullText.lastIndexOf('\n', Math.max(0, cursorPos - 1)) + 1;
            int cursorCol = cursorPos - lineStartIdx;

            int promptW = 2;
            int availW = Math.max(4, cols - promptW - 2);
            int cursorVisRow = bottomBorderRow + 1, cursorVisCol = promptW + 1;

            for (int i = 0; i < inputLines; i++) {
                int row = bottomBorderRow + 1 + i;
                fb.append(Ansi.cursorTo(row, 1)).append(Ansi.CLEAR_LINE);
                String marker = i == 0 ? Ansi.fg(Theme.ACCENT) + (streaming ? "…" : "›") + " " + Ansi.RESET : "  ";
                String lineText = i < inputTextLines.length ? inputTextLines[i] : "";
                String display;
                if (i == cursorLineIdx) {
                    int start = Math.max(0, cursorCol - availW + 1);
                    int end = Math.min(lineText.length(), start + availW);
                    display = lineText.substring(Math.min(start, lineText.length()), end);
                    cursorVisRow = row;
                    cursorVisCol = promptW + (cursorCol - start) + 1;
                } else {
                    display = lineText.length() > availW ? lineText.substring(0, Math.max(0, availW - 1)) + "…" : lineText;
                }
                fb.append(marker).append(streaming ? Ansi.dim(display) : display);
            }

            int hintRow = bottomBorderRow + 1 + inputLines;
            fb.append(Ansi.cursorTo(hintRow, 1)).append(Ansi.CLEAR_LINE);
            String hint = streaming
                    ? Ansi.fg(Theme.DIM) + "Working…  Ctrl+C interrupt" + Ansi.RESET
                    : Ansi.fg(Theme.DIM) + "Enter send · Shift+Enter newline · PgUp/PgDn scroll · /help" + Ansi.RESET;
            fb.append(TextWrap.truncate(hint, cols));

            fb.append(Ansi.cursorTo(cursorVisRow, cursorVisCol));

            out.write(fb.toString());
            out.flush();
        }
    }

    private List<String> sidebarLines(int width, int height) {
        List<String> lines = new ArrayList<>();
        lines.add(Ansi.BOLD + "Session" + Ansi.RESET);
        lines.add(Ansi.dim("provider ") + provName());
        lines.add(Ansi.dim("model    ") + modelName());
        lines.add(Ansi.dim("mode     ") + (agent.tools().isEmpty() ? "chat" : "agent"));
        lines.add("");
        lines.add(Ansi.BOLD + "Tools" + Ansi.RESET);
        lines.add(Ansi.dim("calls    ") + toolCallCount);
        lines.add(Ansi.dim("errors   ") + (toolErrorCount > 0 ? Ansi.fg(Theme.ERROR) + toolErrorCount + Ansi.RESET : "0"));
        lines.add("");
        lines.add(Ansi.BOLD + "Available" + Ansi.RESET);
        for (Tool t : agent.tools()) {
            boolean active = agent.tools().contains(t);
            String mark = active ? Ansi.fg(Theme.TOOL_OK) + "● " : Ansi.dim("○ ");
            lines.add(mark + Ansi.RESET + (active ? t.name() : Ansi.dim(t.name())));
        }
        return lines;
    }

    private String provName() {
        Config.ProviderConfig pc = config.provider(config.activeProfile().provider);
        return pc != null ? pc.name : "?";
    }

    private String modelName() {
        String m = config.activeProfile() != null ? config.activeProfile().model : null;
        return (m == null || m.isBlank()) ? Ansi.fg(Theme.WARN) + "No model selected" + Ansi.RESET : m;
    }

    private List<String> chatLines(int width, int height) {
        List<String> all = new ArrayList<>();
        for (Entry e : entries) {
            all.addAll(renderEntry(e, width));
        }
        if (all.isEmpty()) all.add(Ansi.dim("  (no messages yet -- type below to start)"));

        int total = all.size();
        int start = Math.max(0, total - height - scrollOffset);
        int end = Math.max(start, total - scrollOffset);
        end = Math.min(end, total);
        start = Math.min(start, end);
        return all.subList(start, end);
    }

    private List<String> renderEntry(Entry e, int width) {
        List<String> out = new ArrayList<>();
        switch (e.kind) {
            case USER -> {
                out.add("");
                out.addAll(TextWrap.wrap(Ansi.BOLD + Ansi.fg(Theme.USER) + "› " + Ansi.RESET +
                        Ansi.fg(Theme.USER) + e.text + Ansi.RESET, width));
            }
            case ASSISTANT -> {
                out.add("");
                out.addAll(MarkdownRenderer.render(e.text.toString(), width));
            }
            case TOOL_CALL -> out.addAll(TextWrap.wrap(Ansi.fg(Theme.TOOL) + e.text + Ansi.RESET, width));
            case TOOL_RESULT -> {
                String color = e.isError ? Theme.ERROR : Theme.TOOL_OK;
                out.addAll(TextWrap.wrap(Ansi.fg(color) + e.text + Ansi.RESET, width));
            }
            case SYSTEM -> out.addAll(TextWrap.wrap(Ansi.dim(e.text.toString()), width));
            case ERROR -> out.addAll(TextWrap.wrap(Ansi.fg(Theme.ERROR) + e.text + Ansi.RESET, width));
            case PERMISSION -> out.addAll(TextWrap.wrap(Ansi.fg(Theme.TOOL) + e.text + Ansi.RESET, width));
        }
        return out;
    }

    private String padVisible(String s, int width) {
        int visLen = Ansi.visibleLength(s);
        if (visLen > width) {
            String clipped = TextWrap.truncate(s, width);
            int clippedLen = Ansi.visibleLength(clipped);
            return clipped + " ".repeat(Math.max(0, width - clippedLen));
        }
        return s + " ".repeat(width - visLen);
    }

    private String summarizeArgs(JsonValue v) {
        String s = Json.write(v);
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }

    private String firstLine(String s) {
        if (s == null) return "";
        int idx = s.indexOf('\n');
        String first = idx == -1 ? s : s.substring(0, idx);
        return first.length() > 140 ? first.substring(0, 140) + "..." : first;
    }

    private final class PanelListener implements WayangAgentListener {
        @Override public void onTextDelta(String t) {
            if (liveAssistantEntry == null) {
                liveAssistantEntry = new Entry(Kind.ASSISTANT, "");
                entries.add(liveAssistantEntry);
            }
            liveAssistantEntry.text.append(t);
            render();
        }

        @Override public void onToolCallStart(String id, String name) {
            entries.add(new Entry(Kind.TOOL_CALL, "→ " + name));
            liveAssistantEntry = null;
            render();
        }

        @Override public void onToolCallReady(String id, String name, JsonValue toolInput) {
            entries.add(new Entry(Kind.TOOL_CALL, "  " + summarizeArgs(toolInput)));
            render();
        }

        @Override public void onToolPermissionNeeded(String id, String name, JsonValue toolInput, Consumer<PermissionDecision> responder) {
            pendingPermissionResponder = responder;
            pendingToolName = name;
            entries.add(new Entry(Kind.PERMISSION, "  Allow '" + name + "' to run?  [y]es [a]lways [n]o"));
            render();
        }

        @Override public void onToolResult(String id, String name, ToolResult result) {
            toolCallCount++;
            if (!result.success()) toolErrorCount++;
            String marker = !result.success() ? "✗ " : "✓ ";
            Entry e = new Entry(Kind.TOOL_RESULT, marker + name + ": " + firstLine(result.output().orElse(result.error() != null ? result.error() : "")));
            e.isError = !result.success();
            entries.add(e);
            render();
        }

        @Override public void onUsage(int inTok, int outTok) { /* shown via sidebar later if desired */ }

        @Override public void onDone(String reason) {
            liveAssistantEntry = null;
            render();
        }

        @Override public void onError(String message) {
            liveAssistantEntry = null;
            entries.add(errorEntry("Error: " + message));
            render();
        }
    }
}
