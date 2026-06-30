package tech.kayys.wayang.tui.render;

import tech.kayys.wayang.tui.term.Ansi;
import java.util.*;
import java.util.regex.*;

/**
 * Word-wraps text that may contain ANSI escape sequences, counting only
 * visible characters toward the line width. Escape sequences are kept
 * attached to the word that follows them and never split across lines.
 */
public final class TextWrap {

    private static final Pattern ANSI_OR_CHAR = Pattern.compile("\u001b\\[[0-9;?]*[a-zA-Z]|.", Pattern.DOTALL);

    /**
     * Truncates a single line (which may contain ANSI codes) to at most `width`
     * visible columns, appending an ellipsis if anything was cut. Unlike
     * {@link #wrap}, this never moves overflow to a second line -- the
     * intended use is single-line labels (sidebars, table cells) where
     * dropped content should be signaled, not silently discarded.
     */
    public static String truncate(String text, int width) {
        if (width < 1) return "";
        int visLen = Ansi.visibleLength(text);
        if (visLen <= width) return text;

        int budget = Math.max(0, width - 1); // reserve 1 column for the ellipsis
        StringBuilder out = new StringBuilder();
        Matcher m = ANSI_OR_CHAR.matcher(text);
        int visible = 0;
        while (m.find() && visible < budget) {
            String piece = m.group();
            if (piece.startsWith("\u001b")) {
                out.append(piece);
            } else {
                out.append(piece);
                visible++;
            }
        }
        out.append(Ansi.RESET).append("…");
        return out.toString();
    }

    private TextWrap() {}

    public static List<String> wrap(String text, int width) {
        List<String> result = new ArrayList<>();
        if (width < 4) width = 4;
        for (String paragraph : text.split("\n", -1)) {
            result.addAll(wrapLine(paragraph, width));
        }
        return result;
    }

    private static List<String> wrapLine(String line, int width) {
        List<String> lines = new ArrayList<>();
        if (line.isEmpty()) { lines.add(""); return lines; }

        // Tokenize into "words" (runs of non-space, with attached ANSI codes) and spaces.
        List<String> tokens = tokenize(line);

        StringBuilder current = new StringBuilder();
        int currentVisible = 0;

        for (String token : tokens) {
            int tokenVisible = Ansi.visibleLength(token);
            boolean isSpace = token.equals(" ");

            if (currentVisible + tokenVisible > width && currentVisible > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
                currentVisible = 0;
                if (isSpace) continue; // don't start a new line with a space
            }

            if (tokenVisible > width) {
                // Single token longer than the whole width (e.g. a long URL) -- hard break it.
                for (char c : token.toCharArray()) {
                    if (currentVisible >= width) {
                        lines.add(current.toString());
                        current = new StringBuilder();
                        currentVisible = 0;
                    }
                    current.append(c);
                    currentVisible++;
                }
                continue;
            }

            current.append(token);
            currentVisible += tokenVisible;
        }
        lines.add(current.toString());
        return lines;
    }

    /** Splits into words (with any leading ANSI codes attached) and single-space tokens. */
    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        Matcher m = ANSI_OR_CHAR.matcher(line);
        StringBuilder word = new StringBuilder();
        while (m.find()) {
            String piece = m.group();
            if (piece.startsWith("\u001b")) {
                word.append(piece); // ANSI code: zero visible width, attach to current word
                continue;
            }
            if (piece.equals(" ")) {
                if (word.length() > 0) { tokens.add(word.toString()); word.setLength(0); }
                tokens.add(" ");
            } else {
                word.append(piece);
            }
        }
        if (word.length() > 0) tokens.add(word.toString());
        return tokens;
    }
}
