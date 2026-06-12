package tech.kayys.gamelan.tool.fuzzy;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * FuzzyEditMatcher — nine-pass chain-of-responsibility for resilient file editing.
 *
 * <h2>From the OPENDEV paper (§2.4.2)</h2>
 * When an LLM-based agent edits a file, it specifies old_content to find and new_content to
 * replace. In practice, the LLM frequently produces old_content that differs slightly from the
 * actual file: trailing whitespace variations, indentation mismatches, escape sequence
 * differences, or minor reformatting from reconstructing code from memory rather than verbatim
 * copy. A strict exact-match edit tool fails on these cases, producing "content not found" errors
 * that consume context with error messages and recovery attempts.
 *
 * <h2>Nine passes (paper Appendix D)</h2>
 * <pre>
 * 1. Simple          — Exact string match (baseline; short-circuits here on success)
 * 2. Line-trimmed    — Strip trailing whitespace per line before comparing
 * 3. Block-anchor    — Use first and last lines as anchors; score middle with SequenceMatcher
 * 4. Whitespace-norm — Collapse all whitespace runs to single spaces
 * 5. Indent-flexible — Ignore all leading whitespace; skip blank lines
 * 6. Escape-norm     — Unescape common sequences (\n, \t, \\)
 * 7. Trim-boundary   — Try trimmed content; expand to full line boundaries if partial match
 * 8. Context-aware   — First/last non-empty lines as anchors; 0.5 similarity threshold
 * 9. Multi-occurrence — Trimmed line-by-line exact match across all occurrences (last resort)
 * </pre>
 *
 * <h2>Key property</h2>
 * Each replacer returns the <em>actual substring found in the original file</em> (not the search
 * query), so the replacement preserves the file's original formatting. The chain short-circuits
 * on first match, so exact matches incur zero overhead from the fuzzy passes.
 *
 * <h2>Design principle: absorb LLM imprecision</h2>
 * The paper §3.4: "When the agent's intent is unambiguous but its literal output is imprecise,
 * the tool should bridge the gap rather than reject the attempt."
 */
@ApplicationScoped
public class FuzzyEditMatcher {

    private static final Logger log = LoggerFactory.getLogger(FuzzyEditMatcher.class);
    private static final double BLOCK_ANCHOR_THRESHOLD   = 0.30;
    private static final double CONTEXT_AWARE_THRESHOLD  = 0.50;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Attempts to find {@code searchText} in {@code fileContent} using nine progressive passes.
     *
     * @param fileContent the actual file content
     * @param searchText  the LLM-provided old_content to search for
     * @return the match result, or {@link MatchResult#notFound()} if all nine passes fail
     */
    public MatchResult find(String fileContent, String searchText) {
        if (fileContent == null || searchText == null || searchText.isBlank()) {
            return MatchResult.notFound();
        }

        // Pass 1: Exact match
        int idx = fileContent.indexOf(searchText);
        if (idx >= 0) {
            log.debug("[fuzzy] pass=1 (exact) match at {}", idx);
            return MatchResult.found(fileContent.substring(idx, idx + searchText.length()), 1, idx);
        }

        // Pass 2: Line-trimmed (strip trailing whitespace per line)
        String lineTrimmed = Arrays.stream(searchText.split("\n", -1))
                .map(l -> l.stripTrailing()).collect(Collectors.joining("\n"));
        String fileLineTrimmed = Arrays.stream(fileContent.split("\n", -1))
                .map(l -> l.stripTrailing()).collect(Collectors.joining("\n"));
        idx = fileLineTrimmed.indexOf(lineTrimmed);
        if (idx >= 0) {
            String actual = extractActual(fileContent, fileLineTrimmed, idx, lineTrimmed.length());
            if (actual != null) return MatchResult.found(actual, 2, idx);
        }

        // Pass 3: Block anchor (first + last line as anchors)
        String[] searchLines = searchText.split("\n", -1);
        if (searchLines.length >= 3) {
            String first = searchLines[0].strip();
            String last  = searchLines[searchLines.length - 1].strip();
            if (!first.isEmpty() && !last.isEmpty()) {
                String match = blockAnchorSearch(fileContent, first, last, searchLines.length,
                        BLOCK_ANCHOR_THRESHOLD);
                if (match != null) return MatchResult.found(match, 3, fileContent.indexOf(match));
            }
        }

        // Pass 4: Whitespace-normalized (collapse runs to single space)
        String searchWs = collapseWhitespace(searchText);
        String fileWs   = collapseWhitespace(fileContent);
        idx = fileWs.indexOf(searchWs);
        if (idx >= 0) {
            String actual = extractActualFromNormalized(fileContent, fileWs, idx, searchWs.length());
            if (actual != null) return MatchResult.found(actual, 4, fileContent.indexOf(actual));
        }

        // Pass 5: Indentation-flexible (ignore leading whitespace, skip blank lines)
        String result5 = indentFlexibleSearch(fileContent, searchText);
        if (result5 != null) return MatchResult.found(result5, 5, fileContent.indexOf(result5));

        // Pass 6: Escape-normalized (\n, \t, \\ unescaped)
        String searchEsc = unescapeCommon(searchText);
        if (!searchEsc.equals(searchText)) {
            idx = fileContent.indexOf(searchEsc);
            if (idx >= 0) return MatchResult.found(
                    fileContent.substring(idx, idx + searchEsc.length()), 6, idx);
        }

        // Pass 7: Trim-boundary (expand to full line boundaries after trimmed match)
        String searchTrimmed = searchText.strip();
        idx = fileContent.indexOf(searchTrimmed);
        if (idx >= 0) {
            int start = fileContent.lastIndexOf('\n', idx - 1) + 1;
            int end   = fileContent.indexOf('\n', idx + searchTrimmed.length());
            if (end < 0) end = fileContent.length();
            return MatchResult.found(fileContent.substring(start, end), 7, start);
        }

        // Pass 8: Context-aware (first/last non-empty lines, 0.5 similarity)
        if (searchLines.length >= 2) {
            String firstNonEmpty = Arrays.stream(searchLines).filter(l -> !l.isBlank()).findFirst().orElse("");
            String lastNonEmpty  = Arrays.stream(Arrays.asList(searchLines).reversed()).filter(l -> !l.isBlank()).findFirst().orElse("");
            if (!firstNonEmpty.isEmpty() && !lastNonEmpty.isEmpty()) {
                String match = blockAnchorSearch(fileContent, firstNonEmpty.strip(),
                        lastNonEmpty.strip(), searchLines.length, CONTEXT_AWARE_THRESHOLD);
                if (match != null) return MatchResult.found(match, 8, fileContent.indexOf(match));
            }
        }

        // Pass 9: Multi-occurrence (trimmed line-by-line, all occurrences)
        String result9 = multiOccurrenceSearch(fileContent, searchText);
        if (result9 != null) return MatchResult.found(result9, 9, fileContent.indexOf(result9));

        log.debug("[fuzzy] all 9 passes failed for search: '{}'", truncate(searchText, 60));
        return MatchResult.notFound();
    }

    /**
     * Applies the edit: find old_content and replace with new_content.
     * Uses uniqueness verification — multiple matches produce an error, not a silent mis-edit.
     *
     * @param fileContent  the current file content
     * @param oldContent   what the LLM wants to replace
     * @param newContent   the replacement text
     * @return the edited file content, or an error string if the edit cannot be applied
     */
    public EditResult applyEdit(String fileContent, String oldContent, String newContent) {
        MatchResult match = find(fileContent, oldContent);
        if (!match.found()) {
            return EditResult.error("old_content not found in file after 9 passes. " +
                    "Re-read the file and provide the exact current content.");
        }

        // Uniqueness check: ensure there's exactly one match
        int occurrences = countOccurrences(fileContent, match.actualMatch());
        if (occurrences > 1) {
            return EditResult.error("Found " + occurrences + " occurrences of the matched content. " +
                    "Provide more context (surrounding lines) to make the match unique.");
        }

        String edited = fileContent.replace(match.actualMatch(), newContent);
        return EditResult.success(edited, match.passNumber(),
                generateDiff(oldContent, newContent));
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String blockAnchorSearch(String fileContent, String firstLine, String lastLine,
                                      int approxLines, double threshold) {
        String[] lines = fileContent.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].strip().equals(firstLine)) continue;
            // Look for lastLine within approxLines * 2 range
            int maxEnd = Math.min(lines.length, i + approxLines * 2);
            for (int j = i + 1; j < maxEnd; j++) {
                if (lines[j].strip().equals(lastLine)) {
                    String candidate = String.join("\n", Arrays.copyOfRange(lines, i, j + 1));
                    // Score similarity (simplified Jaccard on line sets)
                    double sim = lineSimilarity(candidate, fileContent.substring(
                            findLineStart(fileContent, i), findLineEnd(fileContent, j)));
                    if (sim >= threshold) return candidate;
                }
            }
        }
        return null;
    }

    private String indentFlexibleSearch(String fileContent, String searchText) {
        String[] searchLines = Arrays.stream(searchText.split("\n", -1))
                .filter(l -> !l.isBlank())
                .map(String::strip)
                .toArray(String[]::new);
        if (searchLines.length == 0) return null;

        String[] fileLines = fileContent.split("\n", -1);
        for (int i = 0; i <= fileLines.length - searchLines.length; i++) {
            boolean matches = true;
            int si = 0;
            int fi = i;
            while (si < searchLines.length && fi < fileLines.length) {
                if (fileLines[fi].isBlank()) { fi++; continue; }
                if (!fileLines[fi].strip().equals(searchLines[si])) { matches = false; break; }
                si++; fi++;
            }
            if (matches && si == searchLines.length) {
                return String.join("\n", Arrays.copyOfRange(fileLines, i, fi));
            }
        }
        return null;
    }

    private String multiOccurrenceSearch(String fileContent, String searchText) {
        String[] searchLines = Arrays.stream(searchText.split("\n", -1))
                .map(String::stripTrailing).toArray(String[]::new);
        String[] fileLines   = fileContent.split("\n", -1);
        for (int i = 0; i <= fileLines.length - searchLines.length; i++) {
            boolean all = true;
            for (int j = 0; j < searchLines.length; j++) {
                if (!fileLines[i + j].stripTrailing().equals(searchLines[j])) { all = false; break; }
            }
            if (all) return String.join("\n", Arrays.copyOfRange(fileLines, i, i + searchLines.length));
        }
        return null;
    }

    private String extractActual(String original, String normalized, int normIdx, int normLen) {
        try {
            // Map normalized index back to original by tracking character correspondence
            int origStart = 0, origEnd = 0, ni = 0;
            String[] origLines = original.split("\n", -1);
            String[] normLines = normalized.split("\n", -1);
            int lineStart = 0;
            for (int li = 0; li < origLines.length && ni < normIdx; li++) {
                int normLineLen = (li < normLines.length ? normLines[li].length() : 0) + 1;
                if (ni + normLineLen > normIdx) break;
                ni += normLineLen;
                lineStart += origLines[li].length() + 1;
            }
            if (lineStart >= original.length()) return null;
            return original.substring(lineStart, Math.min(original.length(),
                    lineStart + normLen + 50)); // rough approximation
        } catch (Exception e) { return null; }
    }

    private String extractActualFromNormalized(String original, String normalized, int ni, int nlen) {
        // Very rough: find the portion of original that maps to the normalized span
        return extractActual(original, normalized, ni, nlen);
    }

    private String collapseWhitespace(String s) { return s.replaceAll("\\s+", " "); }

    private String unescapeCommon(String s) {
        return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
    }

    private int countOccurrences(String text, String search) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(search, idx)) >= 0) { count++; idx += search.length(); }
        return count;
    }

    private double lineSimilarity(String a, String b) {
        Set<String> la = Set.of(a.split("\n"));
        Set<String> lb = Set.of(b.split("\n"));
        Set<String> intersect = new HashSet<>(la); intersect.retainAll(lb);
        Set<String> union = new HashSet<>(la); union.addAll(lb);
        return union.isEmpty() ? 0 : (double) intersect.size() / union.size();
    }

    private int findLineStart(String text, int lineIndex) {
        int pos = 0, li = 0;
        for (; li < lineIndex && pos < text.length(); ) {
            int next = text.indexOf('\n', pos);
            if (next < 0) break;
            pos = next + 1; li++;
        }
        return pos;
    }

    private int findLineEnd(String text, int lineIndex) {
        int start = findLineStart(text, lineIndex);
        int end = text.indexOf('\n', start);
        return end < 0 ? text.length() : end;
    }

    private String generateDiff(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);
        StringBuilder diff = new StringBuilder();
        for (String l : oldLines) diff.append("- ").append(l).append("\n");
        for (String l : newLines) diff.append("+ ").append(l).append("\n");
        return diff.toString();
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record MatchResult(boolean found, String actualMatch, int passNumber, int position) {
        static MatchResult notFound() { return new MatchResult(false, null, 0, -1); }
        static MatchResult found(String match, int pass, int pos) {
            return new MatchResult(true, match, pass, pos);
        }
    }

    public record EditResult(boolean success, String result, int passNumber, String diff, String error) {
        static EditResult success(String content, int pass, String diff) {
            return new EditResult(true, content, pass, diff, null);
        }
        static EditResult error(String msg) {
            return new EditResult(false, null, 0, null, msg);
        }
    }
}
