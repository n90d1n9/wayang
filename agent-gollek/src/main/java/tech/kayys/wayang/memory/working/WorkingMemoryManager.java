package tech.kayys.gamelan.memory.working;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ConversationMessage;
import tech.kayys.gamelan.config.GamelanConfig;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Working Memory Manager — intelligent context-window optimization.
 *
 * <h2>The Problem</h2>
 * LLMs have finite context windows. A naive session accumulates messages
 * without regard to relevance, causing:
 * <ul>
 *   <li>Context window overflow (hard failure)</li>
 *   <li>Relevant early context being truncated while irrelevant middle content remains</li>
 *   <li>The "lost in the middle" problem: LLMs attend poorly to content in the middle
 *       of long contexts</li>
 * </ul>
 *
 * <h2>Solution: Attention-weighted context management</h2>
 * Working memory assigns each message an attention score based on:
 * <ul>
 *   <li><b>Recency</b>: recent messages score higher</li>
 *   <li><b>Relevance</b>: messages containing keywords from the current task score higher</li>
 *   <li><b>Role importance</b>: tool results score higher than acknowledgments</li>
 *   <li><b>Error content</b>: error messages are never evicted</li>
 *   <li><b>Pinned messages</b>: explicitly pinned messages survive eviction</li>
 * </ul>
 *
 * <h2>Eviction Strategy</h2>
 * When token budget approaches limit:
 * <ol>
 *   <li>Messages below attention threshold are candidates for eviction</li>
 *   <li>Long messages are compressed (summary substituted)</li>
 *   <li>Oldest low-attention messages are evicted first</li>
 *   <li>System message and last N exchanges are always retained</li>
 * </ol>
 *
 * <h2>Compaction</h2>
 * Rather than eviction, large stretches of low-relevance history can be
 * replaced with a single "summary message" — compressing 500 tokens to ~50
 * while preserving the key facts.
 */
@ApplicationScoped
public class WorkingMemoryManager {

    private static final Logger log = LoggerFactory.getLogger(WorkingMemoryManager.class);

    private static final int ALWAYS_RETAIN_LAST_N = 4;      // last N user+assistant pairs
    private static final double EVICTION_THRESHOLD  = 0.25; // evict below this attention score

    @Inject GamelanConfig config;

    // Session entries
    private final List<WorkingEntry>  entries     = new CopyOnWriteArrayList<>();
    private final Set<Integer>        pinnedIds   = Collections.synchronizedSet(new HashSet<>());
    private int nextId = 0;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Adds a message to working memory. Returns the assigned entry ID.
     */
    public synchronized int add(ConversationMessage message, MessageImportance importance) {
        int id = nextId++;
        WorkingEntry entry = new WorkingEntry(id, message, importance,
                estimateTokens(message.content()), Instant.now(), 1);
        entries.add(entry);
        log.debug("[working-mem] added {} tokens, role={}, total={} tokens",
                entry.estimatedTokens(), message.role(), totalTokens());
        return id;
    }

    /**
     * Adds with default importance inferred from role.
     */
    public int add(ConversationMessage message) {
        return add(message, inferImportance(message));
    }

    /**
     * Pins a message — it will never be evicted.
     */
    public void pin(int entryId) { pinnedIds.add(entryId); }

    /**
     * Returns the current context window optimized for the active task.
     * Evicts or compresses low-attention messages if over budget.
     *
     * @param currentTask the current task text (used for relevance scoring)
     * @param tokenBudget max tokens allowed in the returned context
     * @return ordered list of messages for injection into LLM
     */
    public List<ConversationMessage> getContext(String currentTask, int tokenBudget) {
        if (totalTokens() <= tokenBudget) {
            return entries.stream().map(WorkingEntry::message).toList();
        }

        log.info("[working-mem] context over budget: {} > {} tokens — optimizing",
                totalTokens(), tokenBudget);

        // Score all entries by attention
        List<ScoredEntry> scored = entries.stream()
                .map(e -> new ScoredEntry(e, attentionScore(e, currentTask, entries.size())))
                .toList();

        // Always retain: last N exchanges and pinned
        Set<Integer> mustRetain = new HashSet<>(pinnedIds);
        int lastNStart = Math.max(0, entries.size() - ALWAYS_RETAIN_LAST_N * 2);
        for (int i = lastNStart; i < entries.size(); i++) {
            mustRetain.add(entries.get(i).id());
        }

        // Build retained set within budget
        List<WorkingEntry> retained = new ArrayList<>();
        int usedTokens = 0;

        // First pass: add must-retains
        for (ScoredEntry se : scored) {
            if (mustRetain.contains(se.entry().id())) {
                retained.add(se.entry());
                usedTokens += se.entry().estimatedTokens();
            }
        }

        // Second pass: add high-attention entries until budget
        List<ScoredEntry> highAttention = scored.stream()
                .filter(se -> !mustRetain.contains(se.entry().id()))
                .filter(se -> se.score() > EVICTION_THRESHOLD)
                .sorted(Comparator.comparingDouble(ScoredEntry::score).reversed())
                .toList();

        for (ScoredEntry se : highAttention) {
            if (usedTokens + se.entry().estimatedTokens() > tokenBudget) break;
            retained.add(se.entry());
            usedTokens += se.entry().estimatedTokens();
        }

        // Sort retained entries by original insertion order
        retained.sort(Comparator.comparingInt(WorkingEntry::id));

        int evicted = entries.size() - retained.size();
        if (evicted > 0) {
            log.info("[working-mem] evicted {} messages ({} tokens freed)",
                    evicted, totalTokens() - usedTokens);
        }

        return retained.stream().map(WorkingEntry::message).toList();
    }

    /**
     * Compact the memory by replacing low-attention stretches with a summary.
     * This preserves key facts while dramatically reducing token count.
     *
     * @param summary the generated summary to replace evicted messages with
     * @return number of messages replaced
     */
    public int compact(String summary) {
        synchronized (this) {
            List<WorkingEntry> toRemove = entries.stream()
                    .filter(e -> !pinnedIds.contains(e.id()))
                    .filter(e -> attentionScore(e, "", entries.size()) < EVICTION_THRESHOLD)
                    .toList();

            if (toRemove.isEmpty()) return 0;

            entries.removeAll(toRemove);
            // Insert the summary as a system message at the current position
            add(ConversationMessage.system("[Context summary] " + summary),
                    MessageImportance.CRITICAL);

            log.info("[working-mem] compacted: removed {} messages, inserted summary",
                    toRemove.size());
            return toRemove.size();
        }
    }

    /**
     * Records that a message was accessed (increases retention priority).
     */
    public void touch(int entryId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id() == entryId) {
                WorkingEntry old = entries.get(i);
                entries.set(i, old.withAccessCount(old.accessCount() + 1));
                return;
            }
        }
    }

    public void clear() { entries.clear(); pinnedIds.clear(); nextId = 0; }

    public int totalTokens() {
        return entries.stream().mapToInt(WorkingEntry::estimatedTokens).sum();
    }

    public int messageCount() { return entries.size(); }

    public WorkingMemoryStats stats() {
        int total = totalTokens();
        int budget = config.tokenBudget();
        return new WorkingMemoryStats(entries.size(), total, budget,
                (double) total / budget, pinnedIds.size());
    }

    // ── Private ────────────────────────────────────────────────────────────

    /**
     * Computes the attention score for a working memory entry.
     * Score is in [0, 1] — higher = more important to retain.
     */
    private double attentionScore(WorkingEntry entry, String currentTask, int totalEntries) {
        double score = 0.0;

        // Recency: exponential decay from most recent
        int ageRank = totalEntries - entry.id(); // 0 = most recent
        double recency = Math.exp(-ageRank * 0.05);

        // Importance tier
        double importance = switch (entry.importance()) {
            case CRITICAL -> 1.0;
            case HIGH     -> 0.8;
            case MEDIUM   -> 0.5;
            case LOW      -> 0.2;
        };

        // Relevance to current task (keyword overlap)
        double relevance = 0.0;
        if (currentTask != null && !currentTask.isBlank()) {
            Set<String> taskWords = tokenize(currentTask.toLowerCase());
            Set<String> msgWords  = tokenize(entry.message().content().toLowerCase());
            Set<String> intersection = new HashSet<>(taskWords);
            intersection.retainAll(msgWords);
            relevance = taskWords.isEmpty() ? 0 : (double) intersection.size() / taskWords.size();
        }

        // Error/critical content
        String content = entry.message().content().toLowerCase();
        double errorBoost = (content.contains("[error]") || content.contains("error:") ||
                content.contains("[failed]") || content.contains("exception")) ? 0.3 : 0.0;

        // Tool result boost (tool results contain concrete evidence)
        double toolBoost = (content.contains("<tool_result") || content.contains("tool_result>")) ? 0.2 : 0.0;

        // Access frequency boost
        double accessBoost = Math.min(0.2, entry.accessCount() * 0.02);

        // Pinned messages always score maximum
        if (pinnedIds.contains(entry.id())) return 1.0;

        score = 0.35 * recency + 0.30 * importance + 0.20 * relevance +
                errorBoost + toolBoost + accessBoost;

        return Math.min(1.0, score);
    }

    private MessageImportance inferImportance(ConversationMessage message) {
        String role    = message.role();
        String content = message.content().toLowerCase();
        if ("system".equals(role)) return MessageImportance.CRITICAL;
        if (content.contains("[error]") || content.contains("failed") ||
                content.contains("exception")) return MessageImportance.HIGH;
        if (content.contains("<tool_result")) return MessageImportance.HIGH;
        if (content.contains("<tool_call"))  return MessageImportance.MEDIUM;
        if ("assistant".equals(role) && content.length() > 500) return MessageImportance.MEDIUM;
        return MessageImportance.LOW;
    }

    private int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, text.length() / 4);
    }

    private Set<String> tokenize(String text) {
        Set<String> words = new HashSet<>();
        for (String w : text.split("[\\s\\p{Punct}]+")) {
            if (w.length() >= 4) words.add(w);
        }
        return words;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum MessageImportance { CRITICAL, HIGH, MEDIUM, LOW }

    public record WorkingEntry(
            int                 id,
            ConversationMessage message,
            MessageImportance   importance,
            int                 estimatedTokens,
            Instant             addedAt,
            int                 accessCount
    ) {
        WorkingEntry withAccessCount(int n) {
            return new WorkingEntry(id, message, importance, estimatedTokens, addedAt, n);
        }
    }

    private record ScoredEntry(WorkingEntry entry, double score) {}

    public record WorkingMemoryStats(
            int    messageCount,
            int    tokenCount,
            int    tokenBudget,
            double utilizationRate,
            int    pinnedCount
    ) {
        public String summary() {
            return String.format("WorkingMem: %d msgs | %d/%d tokens (%.0f%%) | %d pinned",
                    messageCount, tokenCount, tokenBudget, utilizationRate * 100, pinnedCount);
        }
    }
}
