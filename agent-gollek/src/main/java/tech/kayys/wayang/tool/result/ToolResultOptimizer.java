package tech.kayys.gamelan.tool.result;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.context.compaction.AdaptiveContextCompaction;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ToolResultOptimizer — per-tool-type summarization and large output offloading.
 *
 * <h2>From the OPENDEV paper (§2.3.2)</h2>
 * Raw tool outputs consume far more tokens than their informational value warrants. A single
 * read_file may return 2,000–3,000 tokens of source code, a directory listing may enumerate
 * hundreds of entries, and a test-runner invocation may produce thousands of lines of TAP output.
 * Left unchecked, verbose results dominate the context window within a few iterations, crowding
 * out the user query and system instructions.
 *
 * <h2>Per-tool-type summarization strategies</h2>
 * <pre>
 * FILE_READ      → "✓ Read file (142 lines, 4,831 chars)"
 * SEARCH_RESULT  → "✓ Search completed (23 matches found)"
 * DIR_LISTING    → "✓ Listed directory (47 items)"
 * COMMAND_SHORT  → verbatim (≤100 chars)
 * COMMAND_LONG   → "✓ Command executed (312 lines of output)"
 * ERROR          → truncated to 200 chars with classified prefix
 * </pre>
 *
 * <h2>Large output offloading (8,000 chars threshold)</h2>
 * Outputs exceeding 8,000 characters (~2,000 tokens) are written to a scratch file.
 * The context carries only a 500-char preview + file reference + agent-aware recovery hint.
 *
 * <h2>Agent-aware truncation hints</h2>
 * When offloaded, the recovery hint adapts to the agent's capability:
 * - With subagent access → "Delegate to a Code Explorer subagent"
 * - Without subagent access → "Use search tool with offset/limit parameters"
 *
 * This prevents the common failure mode where an agent attempts a recovery strategy
 * unavailable in its tool set (paper §2.3.2).
 */
@ApplicationScoped
public class ToolResultOptimizer {

    private static final Logger log = LoggerFactory.getLogger(ToolResultOptimizer.class);

    // Paper constants (§2.3.2)
    private static final int OFFLOAD_THRESHOLD      = 8_000;  // chars → ~2K tokens
    private static final int PREVIEW_LENGTH         = 500;    // chars kept before offload note
    private static final int SHORT_COMMAND_LIMIT    = 100;    // verbatim if shorter
    private static final int ERROR_TRUNCATION       = 200;    // error message cap

    @Inject AgentTelemetry            telemetry;
    @Inject AdaptiveContextCompaction acc;

    // Session-scoped scratch directory (created lazily)
    private volatile Path scratchDir;
    private final Map<String, Integer> offloadCounts = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Optimizes a tool result before it enters the conversation history.
     * May offload to disk for large outputs.
     *
     * @param toolName         the tool that produced this result
     * @param rawOutput        the full raw output
     * @param hasSubagentTools whether the current agent can spawn subagents
     * @return an OptimizedResult containing the context-ready summary and optional file path
     */
    public OptimizedResult optimize(String toolName, String rawOutput, boolean hasSubagentTools) {
        if (rawOutput == null) rawOutput = "";

        ToolCategory category = classify(toolName);
        boolean isError = isErrorOutput(rawOutput);

        // Large output offloading (before summarization to catch even short-category long outputs)
        if (rawOutput.length() > OFFLOAD_THRESHOLD && !isError) {
            return offloadToDisk(toolName, rawOutput, category, hasSubagentTools);
        }

        String summary = summarize(toolName, rawOutput, category, isError);
        int savedTokens = estimateTokens(rawOutput) - estimateTokens(summary);

        if (savedTokens > 0) {
            telemetry.count("tool_result.optimized." + category.name().toLowerCase());
            log.debug("[tool-result] {} → {} ({} tokens saved)", toolName,
                    category, savedTokens);
        }

        return new OptimizedResult(summary, null, category, estimateTokens(rawOutput),
                estimateTokens(summary), false);
    }

    /**
     * Optimizes a batch of tool results in one call (more efficient for parallel tools).
     */
    public List<OptimizedResult> optimizeBatch(List<ToolOutput> outputs, boolean hasSubagentTools) {
        List<OptimizedResult> results = new ArrayList<>(outputs.size());
        for (ToolOutput output : outputs) {
            results.add(optimize(output.toolName(), output.rawOutput(), hasSubagentTools));
        }
        // Record all file ops in ArtifactIndex
        for (int i = 0; i < outputs.size(); i++) {
            recordArtifactOp(outputs.get(i).toolName(), outputs.get(i).rawOutput());
        }
        return results;
    }

    /** Returns total tokens saved this session through optimization. */
    public int totalTokensSaved() { return offloadCounts.values().stream().mapToInt(Integer::intValue).sum(); }

    /** Sets the scratch directory for large output offloading. */
    public void setScratchDir(Path dir) { this.scratchDir = dir; }

    // ── Per-tool summarization ─────────────────────────────────────────────

    private String summarize(String toolName, String raw, ToolCategory category, boolean isError) {
        if (isError) {
            String truncated = raw.length() > ERROR_TRUNCATION
                    ? raw.substring(0, ERROR_TRUNCATION) + "…" : raw;
            return "✗ Error: " + truncated;
        }
        return switch (category) {
            case FILE_READ   -> summarizeFileRead(toolName, raw);
            case SEARCH      -> summarizeSearch(raw);
            case DIR_LISTING -> summarizeDir(raw);
            case COMMAND     -> summarizeCommand(raw);
            case SUBAGENT    -> summarizeSubagent(raw);
            case SYMBOL      -> summarizeSymbol(raw);
            default          -> raw.length() > 300 ? raw.substring(0, 300) + "…" : raw;
        };
    }

    private String summarizeFileRead(String toolName, String raw) {
        int lines = raw.split("\n", -1).length;
        int chars = raw.length();
        return String.format("✓ Read file (%d lines, %,d chars)", lines, chars);
    }

    private String summarizeSearch(String raw) {
        if (raw.isBlank() || raw.contains("0 matches") || raw.contains("no matches")) {
            return "✓ Search completed (0 matches found)";
        }
        long matchCount = raw.lines().filter(l -> l.matches(".*:\\d+:.*")).count();
        if (matchCount == 0) matchCount = raw.lines().filter(l -> !l.isBlank()).count();
        return String.format("✓ Search completed (%d matches found)", matchCount);
    }

    private String summarizeDir(String raw) {
        long itemCount = raw.lines().filter(l -> !l.isBlank()).count();
        return String.format("✓ Listed directory (%d items)", itemCount);
    }

    private String summarizeCommand(String raw) {
        if (raw.length() <= SHORT_COMMAND_LIMIT) return "✓ " + raw.strip();
        long lineCount = raw.lines().count();
        // For test output, extract pass/fail summary
        if (raw.contains("BUILD") || raw.contains("Tests run") || raw.contains("PASSED")) {
            String lastLines = raw.lines().reduce("", (a, b) -> b); // last line
            return "✓ Command executed (" + lineCount + " lines) — " + lastLines.strip();
        }
        return String.format("✓ Command executed (%d lines of output)", lineCount);
    }

    private String summarizeSubagent(String raw) {
        return raw.length() > 400 ? raw.substring(0, 400) + "\n… [subagent output truncated]" : raw;
    }

    private String summarizeSymbol(String raw) {
        long symbols = raw.lines().filter(l -> l.contains(":") || l.contains("→")).count();
        return symbols > 3 ? String.format("✓ Found %d symbol(s)\n%s",
                symbols, raw.lines().limit(3).reduce("", (a, b) -> a + "\n" + b).strip())
                : raw;
    }

    // ── Large output offloading ────────────────────────────────────────────

    private OptimizedResult offloadToDisk(String toolName, String rawOutput,
                                           ToolCategory category, boolean hasSubagentTools) {
        Path dir = ensureScratchDir();
        String filename = toolName + "-" + Instant.now().toEpochMilli() + ".txt";
        Path file = dir.resolve(filename);

        try {
            Files.writeString(file, rawOutput);
            int lines = rawOutput.split("\n", -1).length;
            String preview = rawOutput.substring(0, Math.min(PREVIEW_LENGTH, rawOutput.length()));
            String recoveryHint = hasSubagentTools
                    ? "Delegate to a Code Explorer subagent to process the full output via search and read tools."
                    : "Use the search tool with offset/limit parameters to process the output incrementally.";

            String summary = String.format(
                    "[Output offloaded: %,d lines, %,d chars → `%s`]\n" +
                    "Preview:\n%s\n…\n\n%s",
                    lines, rawOutput.length(), file.toAbsolutePath(),
                    preview, recoveryHint);

            // Record the offload for token accounting
            int saved = estimateTokens(rawOutput) - estimateTokens(summary);
            offloadCounts.merge(toolName, saved, Integer::sum);
            telemetry.count("tool_result.offloaded");
            log.info("[tool-result] offloaded {} output to {} ({} tokens saved)",
                    toolName, filename, saved);

            // Register file op in artifact index
            acc.recordFileOp(file.toString(), AdaptiveContextCompaction.ArtifactOp.CREATE);

            return new OptimizedResult(summary, file, category,
                    estimateTokens(rawOutput), estimateTokens(summary), true);
        } catch (IOException e) {
            log.warn("[tool-result] offload failed: {}", e.getMessage());
            // Graceful degradation: return truncated output
            String truncated = rawOutput.substring(0, PREVIEW_LENGTH) +
                    "\n… [truncated — offload failed]";
            return new OptimizedResult(truncated, null, category,
                    estimateTokens(rawOutput), estimateTokens(truncated), false);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private ToolCategory classify(String toolName) {
        if (toolName == null) return ToolCategory.GENERIC;
        String lower = toolName.toLowerCase();
        if (lower.contains("read_file") || lower.contains("read")) return ToolCategory.FILE_READ;
        if (lower.contains("search") || lower.contains("grep")) return ToolCategory.SEARCH;
        if (lower.contains("list") || lower.contains("dir")) return ToolCategory.DIR_LISTING;
        if (lower.contains("run") || lower.contains("exec") || lower.contains("command")) return ToolCategory.COMMAND;
        if (lower.contains("subagent") || lower.contains("spawn")) return ToolCategory.SUBAGENT;
        if (lower.contains("symbol") || lower.contains("lsp")) return ToolCategory.SYMBOL;
        return ToolCategory.GENERIC;
    }

    private boolean isErrorOutput(String raw) {
        if (raw == null) return false;
        String lower = raw.toLowerCase();
        return lower.startsWith("error:") || lower.startsWith("exception:") ||
               lower.startsWith("✗") || lower.contains("stack trace") ||
               (lower.contains("error") && raw.length() < 500);
    }

    private void recordArtifactOp(String toolName, String output) {
        if (toolName == null) return;
        String lower = toolName.toLowerCase();
        // Extract file paths from output and register in artifact index
        if (lower.contains("write") || lower.contains("create")) {
            extractFilePaths(output).forEach(p ->
                    acc.recordFileOp(p, AdaptiveContextCompaction.ArtifactOp.CREATE));
        } else if (lower.contains("edit") || lower.contains("modify")) {
            extractFilePaths(output).forEach(p ->
                    acc.recordFileOp(p, AdaptiveContextCompaction.ArtifactOp.MODIFY));
        } else if (lower.contains("read")) {
            extractFilePaths(output).forEach(p ->
                    acc.recordFileOp(p, AdaptiveContextCompaction.ArtifactOp.READ));
        }
    }

    private List<String> extractFilePaths(String text) {
        List<String> paths = new ArrayList<>();
        if (text == null) return paths;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?:src|main|test|lib|resources)/[\\w/.-]+\\.(?:java|xml|yml|yaml|json|kt|py|js|ts)");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) paths.add(m.group());
        return paths;
    }

    private Path ensureScratchDir() {
        if (scratchDir != null) return scratchDir;
        Path dir = Path.of(System.getProperty("user.home"), ".gamelan", "scratch",
                String.valueOf(System.currentTimeMillis() / 1000));
        try { Files.createDirectories(dir); } catch (IOException e) { /* ignored */ }
        scratchDir = dir;
        return dir;
    }

    private int estimateTokens(String s) { return s == null ? 0 : s.length() / 4; }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum ToolCategory { FILE_READ, SEARCH, DIR_LISTING, COMMAND, SUBAGENT, SYMBOL, GENERIC }

    public record OptimizedResult(
            String   contextContent,
            Path     offloadedPath,
            ToolCategory category,
            int      originalTokens,
            int      optimizedTokens,
            boolean  wasOffloaded
    ) {
        public int tokensSaved() { return Math.max(0, originalTokens - optimizedTokens); }
        public boolean wasOptimized() { return tokensSaved() > 0; }
        public String summary() {
            return wasOffloaded
                    ? String.format("[offloaded %d→%d tokens]", originalTokens, optimizedTokens)
                    : String.format("[optimized %d→%d tokens]", originalTokens, optimizedTokens);
        }
    }

    public record ToolOutput(String toolName, String rawOutput) {}
}
