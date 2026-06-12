package tech.kayys.gamelan.context.compaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ConversationMessage;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * AdaptiveContextCompaction (ACC) — five-stage graduated context pressure management.
 *
 * <h2>From the OPENDEV paper (§2.3.6)</h2>
 * Standard systems rely on a binary emergency compaction threshold (typically triggered at
 * 95–99% capacity) that performs a lossy summarization of the conversation history. This approach
 * results in late activation, severe information loss, and compounding errors upon subsequent
 * compactions. ACC instead monitors token usage incrementally and applies five progressively
 * aggressive reduction strategies as pressure rises.
 *
 * <h2>Five stages</h2>
 * <pre>
 * Stage 1 – Warning    (70%): Log pressure. No data reduction. Begin tracking trends.
 * Stage 2 – Masking    (80%): Replace older tool results in-place with reference pointers.
 *                             Reduces tokens from thousands → ~15 per observation.
 *                             Most-recent outputs preserved at full fidelity.
 * Stage 2.5–Fast Prune (85%): Lightweight backward pass. Replace older results with [pruned].
 *                             Deletion-class, cheaper than LLM-based compaction.
 *                             Often reclaims enough to avoid stages 3+.
 * Stage 3 – Aggressive (90%): Preservation window shrinks to only the most recent outputs.
 *                             All others masked.
 * Stage 4 – Full       (99%): Full conversation serialized to scratch file (non-lossy).
 *                             LLM-based summarizer compresses middle, preserves recent verbatim.
 * </pre>
 *
 * <h2>ArtifactIndex</h2>
 * Every file touched (read/created/modified/deleted) during a session is tracked in an
 * ArtifactIndex that is serialized into the compaction summary. After full compaction the agent
 * still knows every file it worked with, even though the raw tool outputs are gone.
 *
 * <h2>Token calibration</h2>
 * Context pressure is computed from the API-reported {@code prompt_tokens} count, not local
 * character estimation. Providers inject invisible content (safety preambles, tool schema
 * serializations) that makes local counting systematically underestimate actual usage.
 */
@ApplicationScoped
public class AdaptiveContextCompaction {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveContextCompaction.class);

    // Stage thresholds (paper: §2.3.6)
    public static final double THRESHOLD_WARNING    = 0.70;
    public static final double THRESHOLD_MASKING    = 0.80;
    public static final double THRESHOLD_FAST_PRUNE = 0.85;
    public static final double THRESHOLD_AGGRESSIVE = 0.90;
    public static final double THRESHOLD_FULL       = 0.99;

    // How many most-recent tool results to preserve at full fidelity per stage
    private static final int PRESERVE_RECENT_MASKING    = 6;
    private static final int PRESERVE_RECENT_AGGRESSIVE = 3;

    // Maximum characters before large output is offloaded to scratch file
    private static final int OFFLOAD_THRESHOLD_CHARS = 8_000; // ~2K tokens

    @Inject GamelanConfig            config;
    @Inject AgentTelemetry           telemetry;
    @Inject SingleAgentOrchestrator  orchestrator;

    private final ArtifactIndex      artifactIndex   = new ArtifactIndex();
    private final AtomicInteger      compactionCount = new AtomicInteger(0);
    private volatile long            lastApiTokens   = 0;  // calibrated from API response

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Updates the calibration anchor from the most recent API call's reported token count.
     * Call this after every LLM invocation.
     *
     * @param reportedPromptTokens the {@code prompt_tokens} field from the API response
     */
    public void calibrate(long reportedPromptTokens) {
        if (reportedPromptTokens > 0) lastApiTokens = reportedPromptTokens;
    }

    /**
     * Records a file operation in the ArtifactIndex.
     *
     * @param path      the file path
     * @param operation READ, CREATE, MODIFY, or DELETE
     */
    public void recordFileOp(String path, ArtifactOp operation) {
        artifactIndex.record(path, operation);
    }

    /**
     * Applies the appropriate compaction stage to the given message list based on
     * current context pressure.
     *
     * @param messages     the current conversation history (mutated in-place for cheap stages)
     * @param contextLimit the model's maximum token budget
     * @return a {@link CompactionResult} describing what was done
     */
    public CompactionResult compact(List<ConversationMessage> messages, int contextLimit) {
        long usedTokens   = computeUsedTokens(messages);
        double pressure   = (double) usedTokens / Math.max(1, contextLimit);
        Stage stage       = classifyStage(pressure);

        log.debug("[acc] pressure={:.0f}% stage={} tokens={}/{}",
                pressure * 100, stage, usedTokens, contextLimit);
        telemetry.gauge("context.pressure", pressure);
        telemetry.count("context.compaction." + stage.name().toLowerCase());

        Instant start = Instant.now();
        int beforeTokens = (int) usedTokens;

        List<ConversationMessage> result = switch (stage) {
            case NONE    -> messages;
            case WARNING -> { logWarning(pressure); yield messages; }
            case MASKING -> applyObservationMasking(messages, PRESERVE_RECENT_MASKING);
            case FAST_PRUNE -> applyFastPrune(messages, PRESERVE_RECENT_MASKING);
            case AGGRESSIVE -> applyObservationMasking(messages, PRESERVE_RECENT_AGGRESSIVE);
            case FULL    -> applyFullCompaction(messages, contextLimit);
        };

        int afterTokens  = (int) computeUsedTokens(result);
        long elapsedMs   = Duration.between(start, Instant.now()).toMillis();
        int reclaimed    = beforeTokens - afterTokens;

        if (stage != Stage.NONE && stage != Stage.WARNING) {
            log.info("[acc] stage={} reclaimed={}t in {}ms (pressure was {:.0f}%)",
                    stage, reclaimed, elapsedMs, pressure * 100);
        }

        return new CompactionResult(stage, pressure, beforeTokens, afterTokens,
                reclaimed, elapsedMs, artifactIndex.summary());
    }

    /**
     * Determines whether any compaction action will be needed at the given token count.
     */
    public Stage predictStage(long usedTokens, int contextLimit) {
        return classifyStage((double) usedTokens / Math.max(1, contextLimit));
    }

    public ArtifactIndex artifactIndex() { return artifactIndex; }

    // ── Stage logic ────────────────────────────────────────────────────────

    /** Stage 2: Replace tool result messages with compact reference pointers. */
    private List<ConversationMessage> applyObservationMasking(List<ConversationMessage> messages,
                                                               int preserveRecent) {
        List<ConversationMessage> result = new ArrayList<>(messages);
        int toolResultCount = 0;

        // Walk backward to count tool results
        for (int i = result.size() - 1; i >= 0; i--) {
            ConversationMessage msg = result.get(i);
            if (isToolResult(msg)) toolResultCount++;
        }

        int toMask = Math.max(0, toolResultCount - preserveRecent);
        int masked = 0;

        for (int i = 0; i < result.size() && masked < toMask; i++) {
            ConversationMessage msg = result.get(i);
            if (isToolResult(msg) && estimateTokens(msg.content()) > 100) {
                result.set(i, new ConversationMessage(msg.role(),
                        "[tool output masked — " + estimateTokens(msg.content()) +
                        " tokens; re-read if needed]"));
                masked++;
            }
        }
        return result;
    }

    /** Stage 2.5: Fast backward pruning — cheaper than LLM compaction. */
    private List<ConversationMessage> applyFastPrune(List<ConversationMessage> messages,
                                                      int preserveRecent) {
        List<ConversationMessage> result = new ArrayList<>(messages);
        int toolResultCount = 0;
        for (ConversationMessage m : result) { if (isToolResult(m)) toolResultCount++; }

        int toPrune = Math.max(0, toolResultCount - preserveRecent);
        int pruned  = 0;

        for (int i = 0; i < result.size() && pruned < toPrune; i++) {
            ConversationMessage msg = result.get(i);
            if (isToolResult(msg) && estimateTokens(msg.content()) > 200) {
                result.set(i, new ConversationMessage(msg.role(), "[pruned]"));
                pruned++;
            }
        }
        return result;
    }

    /** Stage 4: Full LLM-based compaction. Archives entire history to scratch file. */
    private List<ConversationMessage> applyFullCompaction(List<ConversationMessage> messages,
                                                           int contextLimit) {
        compactionCount.incrementAndGet();
        log.warn("[acc] FULL compaction #{}", compactionCount.get());

        // Archive full history to disk (non-lossy: agent can recover any detail by reading the archive)
        String archivePath = archiveToDisk(messages);

        // Split: preserve last 20% of messages verbatim
        int preserveTail  = Math.max(5, messages.size() / 5);
        List<ConversationMessage> tail   = messages.subList(
                Math.max(0, messages.size() - preserveTail), messages.size());
        List<ConversationMessage> middle = messages.subList(
                0, Math.max(0, messages.size() - preserveTail));

        // LLM summarizes the middle portion
        String summary = summarizeWithLlm(middle);

        // Build compacted history: summary + artifact index + archive reference + tail
        List<ConversationMessage> compacted = new ArrayList<>();
        compacted.add(ConversationMessage.system(buildCompactionSummary(summary, archivePath)));
        compacted.addAll(tail);

        telemetry.count("context.full_compaction.total");
        return compacted;
    }

    private String buildCompactionSummary(String llmSummary, String archivePath) {
        return "[CONTEXT COMPACTED]\n\n" +
                "## Summary of Earlier Work\n" + llmSummary +
                "\n\n## Files Touched This Session\n" + artifactIndex.summary() +
                "\n\n## Archive\nFull conversation history archived at `" + archivePath +
                "`. Use read_file to recover any detail if needed.";
    }

    private String summarizeWithLlm(List<ConversationMessage> messages) {
        if (messages.isEmpty()) return "(no prior context)";
        String history = messages.stream()
                .map(m -> "[" + m.role() + "]: " + truncate(m.content(), 300))
                .collect(Collectors.joining("\n"));
        String prompt = "Summarize this conversation history in under 400 words. " +
                "Preserve all file paths, function names, error messages, and decisions. " +
                "Omit greetings, filler, and redundant tool outputs.\n\n" + history;
        try {
            OrchestratorResult r = orchestrator.execute(
                    tech.kayys.gamelan.agent.orchestration.AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, 4000))
                            .stream(false).maxSteps(1).build());
            return r.success() ? r.answer() : "(summary unavailable)";
        } catch (Exception e) {
            return "(summary unavailable: " + e.getMessage() + ")";
        }
    }

    private String archiveToDisk(List<ConversationMessage> messages) {
        Path dir = Path.of(System.getProperty("user.home"), ".gamelan", "compaction");
        try {
            Files.createDirectories(dir);
            String name = "archive-" + Instant.now().toString().replace(":", "-").substring(0, 19) + ".json";
            Path file = dir.resolve(name);
            String json = messages.stream()
                    .map(m -> "{\"role\":\"" + m.role() + "\",\"content\":" +
                              escapeJson(m.content()) + "}")
                    .collect(Collectors.joining(",\n", "[", "]"));
            Files.writeString(file, json);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            log.warn("[acc] archive failed: {}", e.getMessage());
            return "(archive failed)";
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Stage classifyStage(double pressure) {
        if (pressure >= THRESHOLD_FULL)       return Stage.FULL;
        if (pressure >= THRESHOLD_AGGRESSIVE) return Stage.AGGRESSIVE;
        if (pressure >= THRESHOLD_FAST_PRUNE) return Stage.FAST_PRUNE;
        if (pressure >= THRESHOLD_MASKING)    return Stage.MASKING;
        if (pressure >= THRESHOLD_WARNING)    return Stage.WARNING;
        return Stage.NONE;
    }

    private long computeUsedTokens(List<ConversationMessage> messages) {
        if (lastApiTokens > 0) return lastApiTokens; // use calibrated value if available
        return messages.stream().mapToLong(m -> estimateTokens(m.content())).sum();
    }

    private int estimateTokens(String s) { return s == null ? 0 : s.length() / 4; }

    private boolean isToolResult(ConversationMessage m) {
        return "user".equals(m.role()) && m.content() != null &&
               (m.content().startsWith("<tool_result") || m.content().contains("[output"));
    }

    private void logWarning(double pressure) {
        log.info("[acc] WARNING: context at {:.0f}% — monitoring utilization", pressure * 100);
        telemetry.count("context.pressure.warning");
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    private String escapeJson(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum Stage { NONE, WARNING, MASKING, FAST_PRUNE, AGGRESSIVE, FULL }

    public enum ArtifactOp { READ, CREATE, MODIFY, DELETE }

    public record CompactionResult(
            Stage   stage,
            double  pressure,
            int     tokensBefore,
            int     tokensAfter,
            int     tokensReclaimed,
            long    elapsedMs,
            String  artifactSummary
    ) {
        public boolean wasCompacted() { return stage != Stage.NONE && stage != Stage.WARNING; }
        public double  reductionRate() {
            return tokensBefore == 0 ? 0 : (double) tokensReclaimed / tokensBefore;
        }
        public String summary() {
            return wasCompacted()
                    ? String.format("ACC[%s]: %d→%d tokens (%.0f%% reduction) in %dms",
                            stage, tokensBefore, tokensAfter, reductionRate() * 100, elapsedMs)
                    : "ACC[" + stage + "]: pressure=" + String.format("%.0f%%", pressure * 100);
        }
    }

    /** Tracks every file operation during a session for the compaction summary. */
    public static final class ArtifactIndex {
        private final Map<String, ArtifactOp> files = new LinkedHashMap<>();

        public void record(String path, ArtifactOp op) {
            if (path == null || path.isBlank()) return;
            // Upgrade rule: if we already saw a CREATE, don't downgrade to READ
            files.merge(path, op, (existing, incoming) ->
                    incoming == ArtifactOp.DELETE ? ArtifactOp.DELETE :
                    existing == ArtifactOp.CREATE ? ArtifactOp.CREATE : incoming);
        }

        public Map<String, ArtifactOp> all() { return Collections.unmodifiableMap(files); }

        public String summary() {
            if (files.isEmpty()) return "(no files touched)";
            return files.entrySet().stream()
                    .map(e -> "- `" + e.getKey() + "` [" + e.getValue() + "]")
                    .collect(Collectors.joining("\n"));
        }

        public List<String> modified() {
            return files.entrySet().stream()
                    .filter(e -> e.getValue() == ArtifactOp.MODIFY || e.getValue() == ArtifactOp.CREATE)
                    .map(Map.Entry::getKey).toList();
        }
    }
}
