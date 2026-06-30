package tech.kayys.wayang.tui.render;

/** Centralized color palette so REPL and panel modes look consistent. */
public final class Theme {
    private Theme() {}

    public static final String USER = "#7aa2f7";       // soft blue
    public static final String ASSISTANT = "#c0caf5";  // near-white lavender
    public static final String ACCENT = "#bb9af7";     // purple accent (branding)
    public static final String TOOL = "#e0af68";       // amber
    public static final String TOOL_OK = "#9ece6a";    // green
    public static final String ERROR = "#f7768e";      // red/pink
    public static final String WARN = "#e0af68";       // amber/yellow for warnings
    public static final String DIM = "#565f89";        // muted gray-blue
    public static final String CODE_BG = "#1f2335";    // dark panel for code blocks
    public static final String CODE_FG = "#9ece6a";    // green code text
    public static final String BORDER = "#3b4261";     // subtle border lines
    public static final String HEADER = "#7dcfff";     // cyan for markdown headers
    public static final String THINKING = "#2ac3de";   // teal for CoT/thinking blocks
}
