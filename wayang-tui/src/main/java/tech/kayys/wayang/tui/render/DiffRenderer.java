package tech.kayys.wayang.tui.render;

import tech.kayys.wayang.tui.term.Ansi;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a unified diff string (as produced by {@code diff -u} or {@code git diff})
 * as a list of ANSI-coloured lines suitable for appending to the REPL transcript.
 *
 * <p>Color scheme:
 * <ul>
 *   <li>{@code +} added lines  — green ({@link Theme#TOOL_OK})</li>
 *   <li>{@code -} removed lines — red/pink ({@link Theme#ERROR})</li>
 *   <li>{@code @@} hunk headers — cyan/header ({@link Theme#HEADER})</li>
 *   <li>{@code ---/+++} file headers — bold</li>
 *   <li>context lines — dim ({@link Theme#DIM})</li>
 * </ul>
 */
public final class DiffRenderer {

    private DiffRenderer() {}

    /**
     * Render a unified diff string as a list of styled terminal lines.
     * Returns an empty list if the diff is null or blank.
     */
    public static List<String> render(String diff) {
        if (diff == null || diff.isBlank()) return List.of();

        List<String> out = new ArrayList<>();
        out.add(""); // breathing space before diff block
        out.add(Ansi.DIM + "┌─── diff ───────────────────────────────────" + Ansi.RESET);

        for (String raw : diff.split("\n", -1)) {
            out.add(styleLine(raw));
        }

        out.add(Ansi.DIM + "└────────────────────────────────────────────" + Ansi.RESET);
        out.add(""); // breathing space after
        return out;
    }

    /**
     * Generate a simple unified diff between two strings (old → new).
     * Returns a unified diff string (without file headers) that can be passed to {@link #render}.
     */
    public static String diff(String oldContent, String newContent) {
        if (oldContent == null) oldContent = "";
        if (newContent == null) newContent = "";
        if (oldContent.equals(newContent)) return "";

        String[] oldLines = oldContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);

        // Use a simple Myers-like LCS-based diff approach
        // For long files, generate a minimal context diff (±3 context lines)
        int[][] lcs = computeLcs(oldLines, newLines);
        return buildUnifiedDiff(oldLines, newLines, lcs, 3);
    }

    // -----------------------------------------------------------------------

    private static String styleLine(String line) {
        if (line.startsWith("+++") || line.startsWith("---")) {
            return Ansi.BOLD + line + Ansi.RESET;
        }
        if (line.startsWith("@@")) {
            return Ansi.fg(Theme.HEADER) + line + Ansi.RESET;
        }
        if (line.startsWith("+")) {
            return Ansi.fg(Theme.TOOL_OK) + line + Ansi.RESET;
        }
        if (line.startsWith("-")) {
            return Ansi.fg(Theme.ERROR) + line + Ansi.RESET;
        }
        // context line
        return Ansi.DIM + line + Ansi.RESET;
    }

    // -----------------------------------------------------------------------
    // Simple diff computation
    // -----------------------------------------------------------------------

    /** Compute LCS edit matrix using dynamic programming. */
    private static int[][] computeLcs(String[] a, String[] b) {
        int m = a.length, n = b.length;
        // Cap to avoid O(n^2) blowup on huge files
        if (m > 500 || n > 500) return null;
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i - 1].equals(b[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp;
    }

    /**
     * Build a unified diff string with {@code context} surrounding lines around each change.
     * Falls back to a simple "remove all, add all" diff for very large files.
     */
    private static String buildUnifiedDiff(String[] a, String[] b, int[][] lcs, int context) {
        if (lcs == null) {
            // Large file fallback: emit a single huge hunk
            StringBuilder sb = new StringBuilder();
            sb.append("@@ -1,").append(a.length).append(" +1,").append(b.length).append(" @@\n");
            for (String l : a) sb.append("-").append(l).append("\n");
            for (String l : b) sb.append("+").append(l).append("\n");
            return sb.toString();
        }

        // Extract edit script via backtracking
        List<int[]> edits = new ArrayList<>(); // [type, oldIdx, newIdx] type: 0=context,1=del,2=add
        int i = a.length, j = b.length;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && a[i - 1].equals(b[j - 1])) {
                edits.add(0, new int[]{0, i - 1, j - 1});
                i--; j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                edits.add(0, new int[]{2, -1, j - 1});
                j--;
            } else {
                edits.add(0, new int[]{1, i - 1, -1});
                i--;
            }
        }

        // Group into hunks
        StringBuilder out = new StringBuilder();
        int e = 0;
        while (e < edits.size()) {
            // Find next change
            while (e < edits.size() && edits.get(e)[0] == 0) e++;
            if (e >= edits.size()) break;

            // Range: [startContext .. endContext]
            int hunkStart = Math.max(0, e - context);
            int hunkEnd   = e;
            while (hunkEnd < edits.size() && edits.get(hunkEnd)[0] != 0) hunkEnd++;
            hunkEnd = Math.min(edits.size(), hunkEnd + context);

            // Compute hunk header numbers
            int oldStart = -1, newStart = -1, oldCount = 0, newCount = 0;
            for (int k = hunkStart; k < hunkEnd; k++) {
                int[] ed = edits.get(k);
                if (ed[0] != 2 && oldStart == -1) oldStart = ed[1] + 1;
                if (ed[0] != 1 && newStart == -1) newStart = ed[2] + 1;
                if (ed[0] != 2) oldCount++;
                if (ed[0] != 1) newCount++;
            }
            if (oldStart == -1) oldStart = 1;
            if (newStart == -1) newStart = 1;

            out.append("@@ -").append(oldStart).append(",").append(oldCount)
               .append(" +").append(newStart).append(",").append(newCount).append(" @@\n");

            for (int k = hunkStart; k < hunkEnd; k++) {
                int[] ed = edits.get(k);
                switch (ed[0]) {
                    case 0 -> out.append(" ").append(a[ed[1]]).append("\n");
                    case 1 -> out.append("-").append(a[ed[1]]).append("\n");
                    case 2 -> out.append("+").append(b[ed[2]]).append("\n");
                }
            }
            e = hunkEnd;
        }
        return out.toString();
    }
}
