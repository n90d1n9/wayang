package tech.kayys.gamelan.evolution.trace;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;

import java.time.Instant;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Execution Trace Analyzer — extracts structured evolution signals from agent traces.
 *
 * <h2>What is an Execution Trace</h2>
 * Every agent run produces a trace: LLM outputs, tool calls, tool results, errors.
 * These traces contain valuable signals for evolution, but they are unstructured text.
 * The analyzer extracts:
 * <ul>
 *   <li><b>Tool call patterns</b>: which tools are called, in what order, how often</li>
 *   <li><b>Repeated failures</b>: same tool called with same params but different outcomes</li>
 *   <li><b>Inefficiency patterns</b>: redundant reads, unnecessary re-tries, context thrashing</li>
 *   <li><b>Success patterns</b>: sequences that consistently lead to task completion</li>
 *   <li><b>Failure root causes</b>: categorized causes (tool error, hallucination, context loss)</li>
 *   <li><b>Skill activation accuracy</b>: was the right skill selected for the task?</li>
 * </ul>
 *
 * <h2>Signal Types Used by AVO</h2>
 * <pre>
 * INEFFICIENCY → VariationStrategy.TOOL_SEQUENCE
 * FAILURE_PATTERN → VariationStrategy.CONSTRAINT_TIGHTENING
 * AMBIGUITY → VariationStrategy.PRECISION + EXAMPLE_INJECTION
 * COMPLEXITY → VariationStrategy.DECOMPOSITION
 * SUCCESS_PATTERN → extract into ProceduralMemory
 * </pre>
 *
 * <h2>Output Consumed By</h2>
 * <ul>
 *   <li>{@link tech.kayys.gamelan.evolution.avo.AgenticVariationOperator} — to guide variation strategy selection</li>
 *   <li>{@link tech.kayys.gamelan.agent.evolution.SkillEvolutionEngine} — to identify which skills need evolution</li>
 *   <li>{@link tech.kayys.gamelan.meta.MetaCognitiveEngine} — for cross-skill pattern analysis</li>
 * </ul>
 */
@ApplicationScoped
public class ExecutionTraceAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ExecutionTraceAnalyzer.class);

    // Patterns for signal extraction
    private static final Pattern TOOL_CALL_PATTERN =
            Pattern.compile("<tool_call[^>]*>\\s*<n>(\\w+)</n>", Pattern.DOTALL);
    private static final Pattern TOOL_ERROR_PATTERN =
            Pattern.compile("<tool_result[^>]*status=\"error\"[^>]*>([^<]+)", Pattern.DOTALL);
    private static final Pattern LLM_ERROR_PATTERN =
            Pattern.compile("\\[LLM_ERROR\\](.+)", Pattern.DOTALL);
    private static final Pattern HALLUCINATION_MARKERS =
            Pattern.compile("(?i)(as I mentioned|I recall|I remember|as stated earlier|" +
                    "based on my (previous|earlier)|I believe I|it was established that)");

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Analyzes a list of episodes to extract evolution signals.
     *
     * @param episodes  execution episodes to analyze
     * @param skillName filter to only episodes involving this skill (null = all)
     * @return structured analysis result
     */
    public TraceAnalysis analyze(List<EpisodicMemory.Episode> episodes, String skillName) {
        List<EpisodicMemory.Episode> filtered = skillName == null ? episodes
                : episodes.stream()
                        .filter(e -> e.toolsUsed().stream()
                                .anyMatch(t -> t.contains(skillName)))
                        .toList();

        if (filtered.isEmpty()) return TraceAnalysis.empty(skillName);

        List<InefficiencySignal>     inefficiencies = detectInefficiencies(filtered);
        List<FailurePattern>         failures       = detectFailurePatterns(filtered);
        List<AmbiguitySignal>        ambiguities    = detectAmbiguities(filtered);
        List<ComplexitySignal>       complexities   = detectComplexities(filtered);
        List<SuccessPattern>         successes      = extractSuccessPatterns(filtered);
        Map<String, ToolCallStats>   toolStats      = computeToolStats(filtered);
        List<RootCause>              rootCauses     = categorizeRootCauses(filtered);

        log.debug("[trace-analyzer] skill={}: {} inefficiencies, {} failures, {} successes",
                skillName, inefficiencies.size(), failures.size(), successes.size());

        return new TraceAnalysis(skillName, filtered.size(),
                inefficiencies, failures, ambiguities, complexities,
                successes, toolStats, rootCauses, Instant.now());
    }

    /**
     * Returns the recommended AVO variation strategy for a given analysis.
     */
    public List<tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy>
            recommendStrategies(TraceAnalysis analysis) {
        List<tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy> strategies = new ArrayList<>();

        if (!analysis.inefficiencies().isEmpty())
            strategies.add(tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.TOOL_SEQUENCE);
        if (!analysis.failures().isEmpty())
            strategies.add(tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.CONSTRAINT_TIGHTENING);
        if (!analysis.ambiguities().isEmpty()) {
            strategies.add(tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.PRECISION);
            strategies.add(tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.EXAMPLE_INJECTION);
        }
        if (!analysis.complexities().isEmpty())
            strategies.add(tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.DECOMPOSITION);
        if (strategies.isEmpty())
            strategies.add(tech.kayys.gamelan.evolution.avo.AgenticVariationOperator.VariationStrategy.PRECISION);

        return strategies.stream().distinct().toList();
    }

    // ── Detection methods ──────────────────────────────────────────────────

    private List<InefficiencySignal> detectInefficiencies(List<EpisodicMemory.Episode> episodes) {
        List<InefficiencySignal> signals = new ArrayList<>();

        for (EpisodicMemory.Episode ep : episodes) {
            List<String> tools = ep.toolsUsed();

            // Repeated tool: same tool called 3+ times consecutively
            for (int i = 0; i < tools.size() - 2; i++) {
                if (tools.get(i).equals(tools.get(i+1)) && tools.get(i).equals(tools.get(i+2))) {
                    signals.add(new InefficiencySignal(
                            InefficiencyType.REPEATED_TOOL_CALL,
                            tools.get(i),
                            "Tool '" + tools.get(i) + "' called 3+ times consecutively in episode " + ep.id(),
                            0.8));
                }
            }

            // Read after write: write_file immediately followed by read_file (redundant)
            for (int i = 0; i < tools.size() - 1; i++) {
                if (tools.get(i).equals("write_file") && tools.get(i+1).equals("read_file")) {
                    signals.add(new InefficiencySignal(
                            InefficiencyType.READ_AFTER_WRITE,
                            "read_file+write_file",
                            "Redundant read after write in episode " + ep.id(),
                            0.6));
                }
            }

            // Excessive tool calls: more than 8 tools for a simple task
            if (tools.size() > 8 && ep.task().length() < 200) {
                signals.add(new InefficiencySignal(
                        InefficiencyType.EXCESSIVE_TOOL_CALLS,
                        String.valueOf(tools.size()),
                        "Used " + tools.size() + " tools for a seemingly simple task",
                        0.7));
            }
        }
        return signals.stream().distinct().toList();
    }

    private List<FailurePattern> detectFailurePatterns(List<EpisodicMemory.Episode> episodes) {
        List<EpisodicMemory.Episode> failures = episodes.stream()
                .filter(e -> !e.success()).toList();

        // Group failures by tool sequence
        Map<String, Long> sequenceCounts = failures.stream()
                .collect(Collectors.groupingBy(
                        e -> String.join("→", e.toolsUsed()),
                        Collectors.counting()));

        return sequenceCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 2) // recurring pattern
                .map(e -> new FailurePattern(
                        e.getKey(), e.getValue().intValue(),
                        "Tool sequence '" + e.getKey() + "' failed " + e.getValue() + " times",
                        inferFailureCause(failures, e.getKey())))
                .toList();
    }

    private List<AmbiguitySignal> detectAmbiguities(List<EpisodicMemory.Episode> episodes) {
        List<AmbiguitySignal> signals = new ArrayList<>();
        HALLUCINATION_MARKERS.matcher("").reset(); // reset the pattern matcher state

        for (EpisodicMemory.Episode ep : episodes) {
            Matcher m = HALLUCINATION_MARKERS.matcher(ep.result());
            if (m.find()) {
                signals.add(new AmbiguitySignal(
                        AmbiguityType.POTENTIAL_HALLUCINATION,
                        "'" + m.group() + "' suggests context loss or confabulation",
                        ep.id(), 0.7));
            }
            // Check for very long tasks with generic results (possible misunderstanding)
            if (ep.task().length() > 500 && ep.result().length() < 100 && !ep.success()) {
                signals.add(new AmbiguitySignal(
                        AmbiguityType.TASK_MISUNDERSTOOD,
                        "Long task with short result suggests task was not understood",
                        ep.id(), 0.6));
            }
        }
        return signals;
    }

    private List<ComplexitySignal> detectComplexities(List<EpisodicMemory.Episode> episodes) {
        List<ComplexitySignal> signals = new ArrayList<>();
        for (EpisodicMemory.Episode ep : episodes) {
            // Many tools + long duration → complex task that might benefit from decomposition
            if (ep.toolsUsed().size() > 6 && ep.durationMs() > 30_000) {
                signals.add(new ComplexitySignal(
                        ep.toolsUsed().size(), ep.durationMs(),
                        "High tool count (" + ep.toolsUsed().size() + ") + long duration suggests" +
                        " the task needs decomposition into sub-steps"));
            }
        }
        return signals;
    }

    private List<SuccessPattern> extractSuccessPatterns(List<EpisodicMemory.Episode> episodes) {
        List<EpisodicMemory.Episode> successes = episodes.stream()
                .filter(EpisodicMemory.Episode::success).toList();

        // Find tool sequences that appear in 3+ successes
        Map<String, Long> sequenceCounts = successes.stream()
                .collect(Collectors.groupingBy(
                        e -> String.join("→", e.toolsUsed()),
                        Collectors.counting()));

        return sequenceCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 3)
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .map(e -> new SuccessPattern(
                        e.getKey(), e.getValue().intValue(),
                        computeAvgDuration(successes, e.getKey()),
                        (double) e.getValue() / successes.size()))
                .toList();
    }

    private Map<String, ToolCallStats> computeToolStats(List<EpisodicMemory.Episode> episodes) {
        Map<String, List<Long>> toolDurations = new LinkedHashMap<>();
        Map<String, Integer>    toolCounts    = new LinkedHashMap<>();
        Map<String, Integer>    toolErrors    = new LinkedHashMap<>();

        for (EpisodicMemory.Episode ep : episodes) {
            ep.toolsUsed().forEach(t -> {
                toolCounts.merge(t, 1, Integer::sum);
                if (!ep.success()) toolErrors.merge(t, 1, Integer::sum);
            });
        }

        Map<String, ToolCallStats> stats = new LinkedHashMap<>();
        toolCounts.forEach((tool, count) -> {
            int errors = toolErrors.getOrDefault(tool, 0);
            stats.put(tool, new ToolCallStats(tool, count, errors,
                    (double) errors / count, 0));
        });
        return stats;
    }

    private List<RootCause> categorizeRootCauses(List<EpisodicMemory.Episode> episodes) {
        List<RootCause> causes = new ArrayList<>();
        Map<RootCauseType, Long> counts = new EnumMap<>(RootCauseType.class);

        for (EpisodicMemory.Episode ep : episodes) {
            if (ep.success()) continue;
            String result = ep.result().toLowerCase();

            if (result.contains("not found") || result.contains("file not found"))
                counts.merge(RootCauseType.RESOURCE_NOT_FOUND, 1L, Long::sum);
            else if (result.contains("permission") || result.contains("forbidden"))
                counts.merge(RootCauseType.PERMISSION_DENIED, 1L, Long::sum);
            else if (result.contains("timeout") || result.contains("timed out"))
                counts.merge(RootCauseType.TIMEOUT, 1L, Long::sum);
            else if (result.contains("llm_error") || result.contains("connection"))
                counts.merge(RootCauseType.LLM_ERROR, 1L, Long::sum);
            else if (result.contains("unknown tool") || result.contains("not registered"))
                counts.merge(RootCauseType.TOOL_NOT_FOUND, 1L, Long::sum);
            else
                counts.merge(RootCauseType.UNKNOWN, 1L, Long::sum);
        }

        counts.forEach((type, count) ->
                causes.add(new RootCause(type, count.intValue(),
                        String.format("%.0f%% of failures", 100.0 * count / episodes.size()))));
        return causes;
    }

    private String inferFailureCause(List<EpisodicMemory.Episode> failures, String sequence) {
        return failures.stream()
                .filter(e -> String.join("→", e.toolsUsed()).equals(sequence))
                .findFirst()
                .map(e -> truncate(e.result(), 200))
                .orElse("Unknown");
    }

    private long computeAvgDuration(List<EpisodicMemory.Episode> episodes, String sequence) {
        return (long) episodes.stream()
                .filter(e -> String.join("→", e.toolsUsed()).equals(sequence))
                .mapToLong(EpisodicMemory.Episode::durationMs)
                .average()
                .orElse(0);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum InefficiencyType { REPEATED_TOOL_CALL, READ_AFTER_WRITE, EXCESSIVE_TOOL_CALLS, CIRCULAR_LOOP }
    public enum AmbiguityType    { POTENTIAL_HALLUCINATION, TASK_MISUNDERSTOOD, CONTEXT_LOSS }
    public enum RootCauseType    { RESOURCE_NOT_FOUND, PERMISSION_DENIED, TIMEOUT, LLM_ERROR, TOOL_NOT_FOUND, UNKNOWN }

    public record InefficiencySignal(InefficiencyType type, String subject, String description, double confidence) {}
    public record FailurePattern(String toolSequence, int occurrences, String description, String likelyCause) {}
    public record AmbiguitySignal(AmbiguityType type, String description, long episodeId, double confidence) {}
    public record ComplexitySignal(int toolCount, long durationMs, String description) {}
    public record SuccessPattern(String toolSequence, int occurrences, long avgDurationMs, double dominance) {}
    public record ToolCallStats(String toolName, int totalCalls, int errorCalls, double errorRate, long avgDurationMs) {}
    public record RootCause(RootCauseType type, int count, String proportion) {}

    public record TraceAnalysis(
            String                    skillName,
            int                       episodeCount,
            List<InefficiencySignal>  inefficiencies,
            List<FailurePattern>      failures,
            List<AmbiguitySignal>     ambiguities,
            List<ComplexitySignal>    complexities,
            List<SuccessPattern>      successes,
            Map<String, ToolCallStats> toolStats,
            List<RootCause>           rootCauses,
            Instant                   analyzedAt
    ) {
        static TraceAnalysis empty(String name) {
            return new TraceAnalysis(name, 0, List.of(), List.of(), List.of(),
                    List.of(), List.of(), Map.of(), List.of(), Instant.now());
        }

        public boolean hasSignals() {
            return !inefficiencies.isEmpty() || !failures.isEmpty() ||
                   !ambiguities.isEmpty()    || !complexities.isEmpty();
        }

        public String summary() {
            return String.format("Trace[%s/%d eps]: %d inefficiencies, %d failures, " +
                    "%d ambiguities, %d successes, %d root causes",
                    skillName, episodeCount, inefficiencies.size(), failures.size(),
                    ambiguities.size(), successes.size(), rootCauses.size());
        }
    }
}
