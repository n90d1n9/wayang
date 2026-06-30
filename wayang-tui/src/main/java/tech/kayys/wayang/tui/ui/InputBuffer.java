package tech.kayys.wayang.tui.ui;
import tech.kayys.wayang.sdk.agent.WayangAgent;
import tech.kayys.wayang.sdk.agent.WayangAgentListener;

import tech.kayys.wayang.sdk.agent.WayangAgentListener;

import tech.kayys.wayang.tools.spi.Tool;

import tech.kayys.wayang.tools.spi.ToolResult;

import tech.kayys.wayang.sdk.json.Json;

import tech.kayys.wayang.sdk.json.JsonValue;

import tech.kayys.wayang.sdk.agent.PermissionDecision;

import tech.kayys.wayang.tui.term.Key;

/**
 * Holds the text currently being composed in the input box, with a
 * cursor position and basic line-editing operations (backspace, arrow
 * navigation, word-delete, line-kill). Supports embedded newlines for
 * multi-line prompts (inserted via Shift+Enter or pasted text).
 */
public final class InputBuffer {

    private final StringBuilder text = new StringBuilder();
    private int cursor = 0; // index into `text`, in codepoints-as-chars terms (chars, not UTF-16 surrogate-safe for simplicity)

    public String text() { return text.toString(); }
    public int cursor() { return cursor; }
    public boolean isEmpty() { return text.length() == 0; }

    public void clear() {
        text.setLength(0);
        cursor = 0;
    }

    public void insert(String s) {
        text.insert(cursor, s);
        cursor += s.length();
    }

    public void insertChar(int codePoint) {
        insert(new String(Character.toChars(codePoint)));
    }

    public void newline() {
        insert("\n");
    }

    public void backspace() {
        if (cursor > 0) {
            text.deleteCharAt(cursor - 1);
            cursor--;
        }
    }

    public void deleteForward() {
        if (cursor < text.length()) {
            text.deleteCharAt(cursor);
        }
    }

    public void moveLeft() { if (cursor > 0) cursor--; }
    public void moveRight() { if (cursor < text.length()) cursor++; }

    public void moveUp() {
        int lineStart = currentLineStart();
        if (lineStart == 0) { cursor = 0; return; }
        int col = cursor - lineStart;
        int prevLineEnd = lineStart - 1;
        int prevLineStart = text.lastIndexOf("\n", prevLineEnd - 1) + 1;
        int prevLineLen = prevLineEnd - prevLineStart;
        cursor = prevLineStart + Math.min(col, prevLineLen);
    }

    public void moveDown() {
        int lineEnd = currentLineEnd();
        if (lineEnd >= text.length()) { cursor = text.length(); return; }
        int lineStart = currentLineStart();
        int col = cursor - lineStart;
        int nextLineStart = lineEnd + 1;
        int nextLineEnd = text.indexOf("\n", nextLineStart);
        if (nextLineEnd == -1) nextLineEnd = text.length();
        int nextLineLen = nextLineEnd - nextLineStart;
        cursor = nextLineStart + Math.min(col, nextLineLen);
    }

    public void moveHome() { cursor = currentLineStart(); }
    public void moveEnd() { cursor = currentLineEnd(); }

    public void killToLineStart() {
        int start = currentLineStart();
        text.delete(start, cursor);
        cursor = start;
    }

    public void killToLineEnd() {
        int end = currentLineEnd();
        text.delete(cursor, end);
    }

    public void deleteWordBackward() {
        int i = cursor;
        while (i > 0 && Character.isWhitespace(text.charAt(i - 1))) i--;
        while (i > 0 && !Character.isWhitespace(text.charAt(i - 1))) i--;
        text.delete(i, cursor);
        cursor = i;
    }

    private int currentLineStart() {
        int idx = text.lastIndexOf("\n", cursor - 1);
        return idx + 1;
    }

    private int currentLineEnd() {
        int idx = text.indexOf("\n", cursor);
        return idx == -1 ? text.length() : idx;
    }

    /** Number of visual lines (1 + count of newlines). */
    public int lineCount() {
        int count = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') count++;
        return count;
    }

    public String[] lines() {
        return text.toString().split("\n", -1);
    }

    /**
     * Applies a key event to this buffer. Returns true if the key was
     * consumed as an editing operation; false if the caller should
     * handle it itself (e.g. ENTER to submit, CTRL_C to cancel).
     */
    public boolean apply(Key key) {
        switch (key.kind()) {
            case CHAR -> { insertChar(key.codePoint()); return true; }
            case BACKSPACE -> { backspace(); return true; }
            case DELETE -> { deleteForward(); return true; }
            case ARROW_LEFT -> { moveLeft(); return true; }
            case ARROW_RIGHT -> { moveRight(); return true; }
            case ARROW_UP -> { moveUp(); return true; }
            case ARROW_DOWN -> { moveDown(); return true; }
            case HOME, CTRL_A -> { moveHome(); return true; }
            case END, CTRL_E -> { moveEnd(); return true; }
            case CTRL_K -> { killToLineEnd(); return true; }
            case CTRL_U -> { killToLineStart(); return true; }
            case CTRL_W -> { deleteWordBackward(); return true; }
            case TAB -> { insert("    "); return true; }
            case SHIFT_ENTER, ALT_ENTER, NEWLINE -> { newline(); return true; }
            default -> { return false; }
        }
    }
}
