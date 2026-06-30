package tech.kayys.wayang.tui.term;

/** A single decoded key press, covering printable chars and common control/escape sequences. */
public record Key(Kind kind, int codePoint) {

    public enum Kind {
        CHAR, ENTER, BACKSPACE, TAB, ESCAPE,
        ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
        HOME, END, DELETE, PAGE_UP, PAGE_DOWN,
        CTRL_C, CTRL_D, CTRL_L, CTRL_R, CTRL_A, CTRL_E, CTRL_K, CTRL_U, CTRL_W, NEWLINE,
        SHIFT_ENTER, ALT_ENTER, UNKNOWN
    }

    public static Key of(Kind kind) { return new Key(kind, 0); }
    public static Key ofChar(int cp) { return new Key(Kind.CHAR, cp); }

    public boolean isChar() { return kind == Kind.CHAR; }

    public String asString() {
        return kind == Kind.CHAR ? new String(Character.toChars(codePoint)) : "";
    }
}
