package tech.kayys.gamelan.agent.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.memory.hierarchy.SemanticMemory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * FeedbackLearningEngine — captures, stores and applies user feedback to improve agent behavior.
 *
 * <h2>Why explicit feedback matters</h2>
 * Episodic memory captures what happened; feedback captures whether the user
 * was satisfied. These are different signals:
 * <ul>
 *   <li>A technically-correct answer may be unsatisfying (wrong level of detail)</li>
 *   <li>A partially-wrong answer may still be rated positively (addressed the intent)</li>
 *   <li>Style preferences (verbose vs concise, raw code vs explanation) only emerge from feedback</li>
 * </ul>
 *
 * <h2>Feedback types</h2>
 * <pre>
 * THUMBS_UP / THUMBS_DOWN    — explicit binary rating (CLI or UI)
 * EDIT_CORRECTION            — user rewrites the agent's output (strong negative signal)
 * FOLLOW_UP_QUESTION         — user needed more information (weak negative signal)
 * COPY_PASTE                 — user immediately used the output (strong positive signal)
 * REGENERATION_REQUEST       — user asked for a different version (negative signal)
 * STYLE_PREFERENCE           — user explicitly states a formatting preference
 * TOOL_PREFERENCE            — user requests specific tools be used/avoided
 * </pre>
 *
 * <h2>Learning application</h2>
 * Accumulated feedback is distilled into:
 * <ul>
 *   <li>Style rules injected into system prompts ("always include unit tests with code examples")</li>
 *   <li>Tool preferences ("prefer apply_patch over write_file for edits")</li>
 *   <li>Verbosity calibration (positive feedback on concise responses → reduce length)</li>
 *   <li>Domain-specific preferences ("for Java, always add @Override annotations")</li>
 * </ul>
 *
 * <h2>Privacy</h2>
 * Feedback is stored locally at {@code ~/.gamelan/feedback/}.
 * No feedback is sent to external services without explicit opt-in.
 */
@ApplicationScoped
public class FeedbackLearningEngine {

    private static final Logger log = LoggerFactory.getLogger(FeedbackLearningEngine.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final int  MAX_FEEDBACK_ENTRIES = 10_000;
    private static final int  MIN_FEEDBACK_FOR_LEARNING = 5;
    private static final Path FEEDBACK_DIR =
            Path.of(System.getProperty("user.home"), ".gamelan", "feedback");

    @Inject EpisodicMemory episodic;
    @Inject SemanticMemory semantic;
    @Inject AgentTelemetry telemetry;

    private final List<FeedbackEntry>            entries         = new CopyOnWriteArrayList<>();
    private final Map<String, StylePreference>   stylePrefs      = new ConcurrentHashMap<>();
    private final Map<String, ToolPreference>    toolPrefs       = new ConcurrentHashMap<>();
    private final AtomicDouble                   satisfactionEMA = new AtomicDouble(0.5);

    @PostConstruct
    void init() {
        loadFromDisk();
        log.info("[feedback] initialized: {} entries, satisfaction={:.2f}",
                entries.size(), satisfactionEMA.get());
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Records explicit positive feedback (thumbs up).
     *
     * @param taskId      the task being rated
     * @param task        the task description
     * @param agentAnswer the answer being rated
     * @param note        optional user note
     */
    public FeedbackEntry thumbsUp(String taskId, String task, String agentAnswer, String note) {
        return record(taskId, task, agentAnswer, FeedbackType.THUMBS_UP, note, 1.0);
    }

    /**
     * Records explicit negative feedback (thumbs down).
     */
    public FeedbackEntry thumbsDown(String taskId, String task, String agentAnswer, String note) {
        return record(taskId, task, agentAnswer, FeedbackType.THUMBS_DOWN, note, 0.0);
    }

    /**
     * Records that the user edited/corrected the agent's output.
     * This is a strong negative signal about the answer quality.
     *
     * @param original  what the agent produced
     * @param corrected what the user changed it to
     */
    public FeedbackEntry editCorrection(String taskId, String task,
                                         String original, String corrected) {
        FeedbackEntry entry = record(taskId, task, original,
                FeedbackType.EDIT_CORRECTION, "User corrected output", 0.1);
        // Extract the correction as a learning signal
        learnFromCorrection(task, original, corrected);
        return entry;
    }

    /**
     * Records a style preference expressed by the user.
     *
     * @param key   the preference key (e.g. "verbosity", "code_style", "language")
     * @param value the preferred value (e.g. "concise", "Google Java Style", "Java")
     */
    public void recordStylePreference(String key, String value, String example) {
        StylePreference pref = new StylePreference(key, value, example, Instant.now(), 1);
        stylePrefs.merge(key, pref, (old, newer) ->
                new StylePreference(key, value, example, Instant.now(), old.reinforcements() + 1));
        persistStylePrefs();
        log.info("[feedback] style preference: {} = {}", key, value);
    }

    /**
     * Records a tool preference.
     *
     * @param toolName   the tool being rated
     * @param preferred  true = user wants more of this tool, false = less
     * @param reason     why
     */
    public void recordToolPreference(String toolName, boolean preferred, String reason) {
        toolPrefs.put(toolName, new ToolPreference(toolName, preferred, reason, Instant.now()));
        log.info("[feedback] tool preference: {} = {} ({})", toolName, preferred ? "preferred" : "avoid", reason);
    }

    /**
     * Generates a personalized system prompt addition based on accumulated feedback.
     * Inject this into every system prompt for personalized agent behavior.
     */
    public String generatePersonalizationBlock() {
        if (entries.isEmpty() && stylePrefs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("## User Preferences (learned from feedback)\n");

        // Style preferences
        if (!stylePrefs.isEmpty()) {
            sb.append("### Style\n");
            stylePrefs.values().stream()
                    .sorted(Comparator.comparingInt(StylePreference::reinforcements).reversed())
                    .limit(5)
                    .forEach(p -> sb.append("- ").append(p.key()).append(": ")
                            .append(p.value()).append("\n"));
        }

        // Tool preferences
        if (!toolPrefs.isEmpty()) {
            sb.append("### Tool Preferences\n");
            toolPrefs.values().forEach(p ->
                    sb.append("- ").append(p.preferred() ? "Prefer" : "Avoid")
                            .append(" `").append(p.toolName()).append("`")
                            .append(p.reason() != null ? ": " + p.reason() : "").append("\n"));
        }

        // Verbosity signal from positive/negative feedback distribution
        double sat = satisfactionEMA.get();
        if (sat > 0.7) {
            sb.append("### Response Style\n- User is generally satisfied. Maintain current approach.\n");
        } else if (sat < 0.4) {
            sb.append("### Response Style\n- User has given negative feedback recently. Be more concise and direct.\n");
        }

        // Learned corrections
        List<LearnedCorrection> corrections = getTopCorrections(3);
        if (!corrections.isEmpty()) {
            sb.append("### Common Corrections\n");
            corrections.forEach(c -> sb.append("- Instead of: ")
                    .append(truncate(c.original(), 60))
                    .append(" → Use: ").append(truncate(c.corrected(), 60)).append("\n"));
        }

        return sb.toString();
    }

    /**
     * Returns the overall satisfaction rate (0.0–1.0).
     * Computed as an EMA of thumbs up vs thumbs down.
     */
    public double satisfactionRate() { return satisfactionEMA.get(); }

    /**
     * Returns feedback statistics.
     */
    public FeedbackStats stats() {
        Map<FeedbackType, Long> byType = entries.stream()
                .collect(Collectors.groupingBy(FeedbackEntry::type, Collectors.counting()));

        long positive = byType.getOrDefault(FeedbackType.THUMBS_UP, 0L) +
                        byType.getOrDefault(FeedbackType.COPY_PASTE, 0L);
        long negative = byType.getOrDefault(FeedbackType.THUMBS_DOWN, 0L) +
                        byType.getOrDefault(FeedbackType.EDIT_CORRECTION, 0L) +
                        byType.getOrDefault(FeedbackType.REGENERATION_REQUEST, 0L);

        return new FeedbackStats(entries.size(), positive, negative,
                satisfactionEMA.get(), stylePrefs.size(), toolPrefs.size(),
                byType);
    }

    /**
     * Returns all feedback entries, newest first.
     */
    public List<FeedbackEntry> all() {
        return entries.stream()
                .sorted(Comparator.comparing(FeedbackEntry::timestamp).reversed())
                .toList();
    }

    /**
     * Clears all feedback (useful on project switch).
     */
    public void clear() {
        entries.clear();
        stylePrefs.clear();
        toolPrefs.clear();
        satisfactionEMA.set(0.5);
        log.info("[feedback] cleared all feedback data");
    }

    // ── Private ────────────────────────────────────────────────────────────

    private FeedbackEntry record(String taskId, String task, String answer,
                                  FeedbackType type, String note, double satisfaction) {
        FeedbackEntry entry = new FeedbackEntry(
                UUID.randomUUID().toString(), taskId, task, answer, type,
                note, satisfaction, Instant.now());

        entries.add(entry);
        if (entries.size() > MAX_FEEDBACK_ENTRIES) entries.remove(0);

        // Update satisfaction EMA
        satisfactionEMA.updateAndGet(cur -> 0.2 * satisfaction + 0.8 * cur);

        // Update telemetry
        telemetry.count("feedback." + type.name().toLowerCase());
        telemetry.gauge("feedback.satisfaction.ema", satisfactionEMA.get());

        // Persist async
        Thread.ofVirtual().start(() -> persistEntry(entry));

        log.debug("[feedback] recorded {} for task '{}'", type, truncate(task, 50));
        return entry;
    }

    private void learnFromCorrection(String task, String original, String corrected) {
        // Store the correction pattern in semantic memory
        if (original.isBlank() || corrected.isBlank()) return;
        String concept = "correction:" + task.hashCode();
        String fact = "When asked to '" + truncate(task, 80) + "', user corrected '" +
                truncate(original, 60) + "' to '" + truncate(corrected, 60) + "'";
        semantic.upsert(concept, fact, SemanticMemory.NodeType.PREFERENCE, 0, 0.9);
    }

    private List<LearnedCorrection> getTopCorrections(int limit) {
        return entries.stream()
                .filter(e -> e.type() == FeedbackType.EDIT_CORRECTION)
                .limit(limit)
                .map(e -> new LearnedCorrection(e.answer(), e.note()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        Path file = FEEDBACK_DIR.resolve("entries.json");
        if (!Files.exists(file)) return;
        try {
            List<FeedbackEntry> loaded = MAPPER.readValue(file.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, FeedbackEntry.class));
            entries.addAll(loaded);
            // Reconstruct satisfaction EMA from loaded data
            loaded.stream()
                    .sorted(Comparator.comparing(FeedbackEntry::timestamp))
                    .forEach(e -> satisfactionEMA.updateAndGet(
                            cur -> 0.2 * e.satisfactionScore() + 0.8 * cur));
        } catch (IOException e) {
            log.warn("[feedback] could not load entries: {}", e.getMessage());
        }

        Path styleFile = FEEDBACK_DIR.resolve("style-prefs.json");
        if (Files.exists(styleFile)) {
            try {
                Map<String, StylePreference> loaded = MAPPER.readValue(styleFile.toFile(),
                        MAPPER.getTypeFactory().constructMapType(
                                Map.class, String.class, StylePreference.class));
                stylePrefs.putAll(loaded);
            } catch (IOException e) {
                log.warn("[feedback] could not load style prefs: {}", e.getMessage());
            }
        }
    }

    private void persistEntry(FeedbackEntry entry) {
        try {
            Files.createDirectories(FEEDBACK_DIR);
            String line = MAPPER.writeValueAsString(entry) + "\n";
            Files.writeString(FEEDBACK_DIR.resolve("entries.jsonl"), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("[feedback] persist failed: {}", e.getMessage());
        }
    }

    private void persistStylePrefs() {
        Thread.ofVirtual().start(() -> {
            try {
                Files.createDirectories(FEEDBACK_DIR);
                MAPPER.writerWithDefaultPrettyPrinter()
                      .writeValue(FEEDBACK_DIR.resolve("style-prefs.json").toFile(), stylePrefs);
            } catch (IOException e) {
                log.warn("[feedback] style prefs persist failed: {}", e.getMessage());
            }
        });
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum FeedbackType {
        THUMBS_UP, THUMBS_DOWN,
        EDIT_CORRECTION, FOLLOW_UP_QUESTION,
        COPY_PASTE, REGENERATION_REQUEST,
        STYLE_PREFERENCE, TOOL_PREFERENCE
    }

    public record FeedbackEntry(
            String       id,
            String       taskId,
            String       task,
            String       answer,
            FeedbackType type,
            String       note,
            double       satisfactionScore,
            Instant      timestamp
    ) {}

    public record StylePreference(
            String  key,
            String  value,
            String  example,
            Instant learnedAt,
            int     reinforcements
    ) {}

    public record ToolPreference(
            String  toolName,
            boolean preferred,
            String  reason,
            Instant learnedAt
    ) {}

    public record LearnedCorrection(String original, String corrected) {}

    public record FeedbackStats(
            int                      totalEntries,
            long                     positiveCount,
            long                     negativeCount,
            double                   satisfactionRate,
            int                      stylePreferences,
            int                      toolPreferences,
            Map<FeedbackType, Long>  byType
    ) {
        public String summary() {
            return String.format(
                    "Feedback: %d entries | %.0f%% satisfaction | %d style prefs | %d tool prefs",
                    totalEntries, satisfactionRate * 100, stylePreferences, toolPreferences);
        }
    }

    // Thread-safe AtomicDouble helper
    private static final class AtomicDouble {
        private final AtomicLong bits;
        AtomicDouble(double init) { bits = new AtomicLong(Double.doubleToLongBits(init)); }
        double get() { return Double.longBitsToDouble(bits.get()); }
        double set(double v) { bits.set(Double.doubleToLongBits(v)); return v; }
        double updateAndGet(java.util.function.DoubleUnaryOperator f) {
            long prev, next;
            do {
                prev = bits.get();
                next = Double.doubleToLongBits(f.applyAsDouble(Double.longBitsToDouble(prev)));
            } while (!bits.compareAndSet(prev, next));
            return Double.longBitsToDouble(next);
        }
    }
}
