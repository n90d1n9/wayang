package tech.kayys.wayang.tui.term;

/**
 * Raw ANSI/VT100 escape sequences and small styling helpers.
 * No external lib needed -- every modern terminal (including the
 * ones Claude Code / Copilot CLI / openclaw target) understands these.
 */
public final class Ansi {

    private Ansi() {}

    public static final String ESC = "\u001b";
    public static final String CSI = ESC + "[";

    // Screen / cursor
    public static final String CLEAR_SCREEN = CSI + "2J";
    public static final String CLEAR_SCROLLBACK = CSI + "3J";
    public static final String CURSOR_HOME = CSI + "H";
    public static final String HIDE_CURSOR = CSI + "?25l";
    public static final String SHOW_CURSOR = CSI + "?25h";
    public static final String SAVE_CURSOR = ESC + "7";
    public static final String RESTORE_CURSOR = ESC + "8";
    public static final String ENABLE_ALT_BUFFER = CSI + "?1049h";
    public static final String DISABLE_ALT_BUFFER = CSI + "?1049l";

    public static final String CLEAR_LINE = CSI + "2K";
    public static final String CLEAR_TO_LINE_END = CSI + "0K";
    public static final String CLEAR_TO_SCREEN_END = CSI + "0J";

    public static String cursorTo(int row, int col) { return CSI + row + ";" + col + "H"; }
    public static String cursorUp(int n) { return n <= 0 ? "" : CSI + n + "A"; }
    public static String cursorDown(int n) { return n <= 0 ? "" : CSI + n + "B"; }
    public static String cursorForward(int n) { return n <= 0 ? "" : CSI + n + "C"; }
    public static String cursorBack(int n) { return n <= 0 ? "" : CSI + n + "D"; }
    public static String cursorToCol(int col) { return CSI + col + "G"; }

    /** Set the scrollable region to [top, bottom] (1-indexed, inclusive). */
    public static String setScrollRegion(int top, int bottom) { return CSI + top + ";" + bottom + "r"; }
    public static String resetScrollRegion() { return CSI + "r"; }
    public static String scrollUp(int n) { return CSI + n + "S"; }
    public static String scrollDown(int n) { return CSI + n + "T"; }

    // Styles
    public static final String RESET = CSI + "0m";
    public static final String BOLD = CSI + "1m";
    public static final String DIM = CSI + "2m";
    public static final String ITALIC = CSI + "3m";
    public static final String UNDERLINE = CSI + "4m";
    public static final String STRIKE = CSI + "9m";

    /** 24-bit truecolor foreground from a "#rrggbb" hex string. */
    public static String fg(String hex) {
        int[] rgb = hexToRgb(hex);
        return CSI + "38;2;" + rgb[0] + ";" + rgb[1] + ";" + rgb[2] + "m";
    }

    /** 24-bit truecolor background from a "#rrggbb" hex string. */
    public static String bg(String hex) {
        int[] rgb = hexToRgb(hex);
        return CSI + "48;2;" + rgb[0] + ";" + rgb[1] + ";" + rgb[2] + "m";
    }

    private static int[] hexToRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new int[]{r, g, b};
    }

    /** Wrap text in a color + reset, convenience for one-off styling. */
    public static String paint(String hex, String text) {
        return fg(hex) + text + RESET;
    }

    public static String bold(String text) { return BOLD + text + RESET; }
    public static String dim(String text) { return DIM + text + RESET; }
    public static String italic(String text) { return ITALIC + text + RESET; }

    /** Strip ANSI escape sequences -- needed to measure real display width. */
    public static String strip(String s) {
        return s.replaceAll("\u001b\\[[0-9;?]*[a-zA-Z]", "");
    }

    public static int visibleLength(String s) {
        return strip(s).codePointCount(0, strip(s).length());
    }
}
