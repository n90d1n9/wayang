package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies a unified diff patch to a file.
 *
 * <p>The patch is expected in standard unified diff format as produced by
 * {@code diff -u old new} or {@code git diff}. This implementation applies
 * hunks directly without shelling out, so it works cross-platform.
 *
 * <p>Limitations:
 * <ul>
 *   <li>Binary files are not supported.</li>
 *   <li>Context lines must match exactly (same whitespace).</li>
 *   <li>Only single-file patches are accepted (one "---/+++" pair).</li>
 * </ul>
 */
public final class PatchTool implements Tool {

    @Override public String id() { return "patch"; }
    @Override public String name() { return "patch"; }

    @Override public String description() {
        return "Apply a unified diff patch to a file. " +
               "The patch string should be in standard 'diff -u' or 'git diff' format. " +
               "Only single-file patches are supported. " +
               "Use when you have a full diff to apply rather than a single old/new replacement.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "path",    Schema.string("Path to the file to patch."),
                "patch",   Schema.string("The unified diff patch string."),
                "reverse", Schema.bool("Apply the patch in reverse (undo). Default: false.")
        ), "path", "patch");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String pathStr  = (String) params.get("path");
        String patch    = (String) params.get("patch");
        boolean reverse = params.containsKey("reverse") && ((Boolean) params.get("reverse")).booleanValue();

        Path path = context.workingDirectory().resolve(pathStr);
        if (!Files.exists(path)) return ToolResult.error("File not found: " + pathStr);

        String original = Files.readString(path, StandardCharsets.UTF_8);
        List<String> lines = new ArrayList<>(List.of(original.split("\n", -1)));

        try {
            List<String> patched = applyPatch(lines, patch, reverse);
            Files.writeString(path, String.join("\n", patched), StandardCharsets.UTF_8);
            return ToolResult.success("Patched " + pathStr + " successfully.");
        } catch (PatchException e) {
            return ToolResult.error("Patch failed: " + e.getMessage());
        }
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------
    // Minimal unified diff parser & applier
    // -------------------------------------------------------------------

    private static List<String> applyPatch(List<String> original, String patch, boolean reverse)
            throws PatchException {

        List<String> patchLines = List.of(patch.split("\n", -1));
        List<Hunk> hunks = parseHunks(patchLines);

        // Apply hunks in reverse order so that line offsets stay consistent
        List<String> result = new ArrayList<>(original);
        int offsetAccum = 0;

        if (reverse) {
            hunks = hunks.stream().map(Hunk::reversed).toList();
        }

        for (Hunk h : hunks) {
            int startLine = h.origStart - 1 + offsetAccum; // 0-based
            int linesMatched = 0;

            // Verify context + removed lines match
            int ri = startLine;
            for (PatchLine pl : h.lines) {
                if (pl.type == '+') continue;
                if (ri >= result.size()) throw new PatchException(
                        "Patch line " + (ri + 1) + " is beyond end of file.");
                if (!result.get(ri).equals(pl.text)) throw new PatchException(
                        "Context mismatch at line " + (ri + 1) +
                        ": expected [" + pl.text + "] but found [" + result.get(ri) + "]");
                ri++;
                linesMatched++;
            }

            // Apply: remove old lines, insert new lines
            List<String> replacement = new ArrayList<>();
            for (PatchLine pl : h.lines) {
                if (pl.type == ' ' || pl.type == '+') replacement.add(pl.text);
            }
            int removeCount = (int) h.lines.stream().filter(pl -> pl.type != '+').count();
            for (int i = 0; i < removeCount; i++) result.remove(startLine);
            result.addAll(startLine, replacement);

            offsetAccum += replacement.size() - removeCount;
        }
        return result;
    }

    private static List<Hunk> parseHunks(List<String> lines) throws PatchException {
        List<Hunk> hunks = new ArrayList<>();
        int i = 0;

        // Skip file header lines (---, +++)
        while (i < lines.size() && (lines.get(i).startsWith("---") || lines.get(i).startsWith("+++"))) {
            i++;
        }

        while (i < lines.size()) {
            String line = lines.get(i);
            if (!line.startsWith("@@")) { i++; continue; }

            // @@ -a,b +c,d @@
            Hunk hunk = parseHunkHeader(line);
            i++;
            while (i < lines.size() && !lines.get(i).startsWith("@@")
                    && !lines.get(i).startsWith("---") && !lines.get(i).startsWith("+++")) {
                String l = lines.get(i);
                if (l.isEmpty()) { hunk.lines.add(new PatchLine(' ', "")); i++; continue; }
                char type = l.charAt(0);
                if (type == '+' || type == '-' || type == ' ') {
                    hunk.lines.add(new PatchLine(type, l.substring(1)));
                }
                i++;
            }
            hunks.add(hunk);
        }
        return hunks;
    }

    private static Hunk parseHunkHeader(String header) throws PatchException {
        // Format: @@ -origStart[,origCount] +newStart[,newCount] @@
        int dash = header.indexOf('-');
        if (dash < 0) throw new PatchException("Invalid hunk header: " + header);
        int comma = header.indexOf(',', dash);
        int space = header.indexOf(' ', dash);
        int end   = comma >= 0 && comma < space ? comma : space;
        int origStart;
        try {
            origStart = Integer.parseInt(header.substring(dash + 1, end));
        } catch (NumberFormatException e) {
            throw new PatchException("Cannot parse hunk header: " + header);
        }
        return new Hunk(origStart);
    }

    private static class Hunk {
        final int origStart;
        final List<PatchLine> lines = new ArrayList<>();
        Hunk(int origStart) { this.origStart = origStart; }

        Hunk reversed() {
            Hunk r = new Hunk(origStart);
            for (PatchLine pl : lines) {
                r.lines.add(new PatchLine(
                        pl.type == '+' ? '-' : pl.type == '-' ? '+' : ' ',
                        pl.text));
            }
            return r;
        }
    }

    private record PatchLine(char type, String text) {}

    private static class PatchException extends Exception {
        PatchException(String msg) { super(msg); }
    }
}
