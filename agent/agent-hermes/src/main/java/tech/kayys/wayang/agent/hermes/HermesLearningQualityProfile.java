package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentState;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reusable quality profile for deciding whether a run should become a skill.
 */
public record HermesLearningQualityProfile(
        int stepCount,
        int successfulStepCount,
        int actionStepCount,
        int toolCount,
        int taskTokenCount,
        int answerLength,
        int failureMarkerCount,
        double stepScore,
        double toolScore,
        double taskSpecificityScore,
        double answerCompletenessScore,
        double noisePenalty,
        double reusePotentialScore,
        double qualityScore,
        double threshold) {

    public static final double DEFAULT_THRESHOLD = 0.60;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "do", "for", "from",
            "in", "is", "it", "of", "on", "or", "run", "say", "task", "the", "this",
            "to", "with");

    public HermesLearningQualityProfile {
        stepCount = Math.max(stepCount, 0);
        successfulStepCount = Math.max(successfulStepCount, 0);
        actionStepCount = Math.max(actionStepCount, 0);
        toolCount = Math.max(toolCount, 0);
        taskTokenCount = Math.max(taskTokenCount, 0);
        answerLength = Math.max(answerLength, 0);
        failureMarkerCount = Math.max(failureMarkerCount, 0);
        stepScore = clamp(stepScore);
        toolScore = clamp(toolScore);
        taskSpecificityScore = clamp(taskSpecificityScore);
        answerCompletenessScore = clamp(answerCompletenessScore);
        noisePenalty = clamp(noisePenalty);
        reusePotentialScore = clamp(reusePotentialScore);
        qualityScore = clamp(qualityScore);
        threshold = clamp(threshold <= 0.0 ? DEFAULT_THRESHOLD : threshold);
    }

    public static HermesLearningQualityProfile from(
            HermesLearningSignal signal,
            HermesAgentModeConfig config) {
        HermesLearningSignal resolvedSignal = signal == null
                ? new HermesLearningSignal("", "", "", false, null, null, null, null)
                : signal;
        HermesAgentModeConfig resolvedConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        int stepCount = resolvedSignal.steps().size();
        int successfulSteps = (int) resolvedSignal.steps().stream()
                .filter(AgentState.ReasoningStep::successful)
                .count();
        int actionSteps = (int) resolvedSignal.steps().stream()
                .filter(step -> step.action() != null)
                .count();
        int toolCount = resolvedSignal.toolIds().size();
        int taskTokens = taskTokenCount(resolvedSignal.task());
        int answerLength = resolvedSignal.answer().length();
        int failureMarkers = failureMarkerCount(resolvedSignal);

        double stepScore = ratio(stepCount, resolvedConfig.minStepsToLearn());
        double toolScore = clamp((toolCount + (actionSteps * 0.25)) / 2.0);
        double taskScore = ratio(taskTokens, 4);
        double answerScore = ratio(answerLength, 80);
        double stepFailurePenalty = stepCount == 0 ? 0.0 : (1.0 - ratio(successfulSteps, stepCount)) * 0.35;
        double metadataNoisePenalty = failureMarkers > 0 ? Math.min(0.30, failureMarkers * 0.10) : 0.0;
        double noisePenalty = clamp(stepFailurePenalty + metadataNoisePenalty);
        double reusePotential = clamp((taskScore * 0.65) + (toolScore * 0.35));
        double quality = clamp(
                (stepScore * 0.35)
                        + (toolScore * 0.25)
                        + (taskScore * 0.20)
                        + (answerScore * 0.20)
                        - noisePenalty);

        return new HermesLearningQualityProfile(
                stepCount,
                successfulSteps,
                actionSteps,
                toolCount,
                taskTokens,
                answerLength,
                failureMarkers,
                stepScore,
                toolScore,
                taskScore,
                answerScore,
                noisePenalty,
                reusePotential,
                quality,
                DEFAULT_THRESHOLD);
    }

    public boolean passes() {
        return qualityScore >= threshold;
    }

    public String roundedQualityScore() {
        return rounded(qualityScore);
    }

    public String roundedThreshold() {
        return rounded(threshold);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("stepCount", stepCount);
        metadata.put("successfulStepCount", successfulStepCount);
        metadata.put("actionStepCount", actionStepCount);
        metadata.put("toolCount", toolCount);
        metadata.put("taskTokenCount", taskTokenCount);
        metadata.put("answerLength", answerLength);
        metadata.put("failureMarkerCount", failureMarkerCount);
        metadata.put("stepScore", rounded(stepScore));
        metadata.put("toolScore", rounded(toolScore));
        metadata.put("taskSpecificityScore", rounded(taskSpecificityScore));
        metadata.put("answerCompletenessScore", rounded(answerCompletenessScore));
        metadata.put("noisePenalty", rounded(noisePenalty));
        metadata.put("reusePotentialScore", rounded(reusePotentialScore));
        metadata.put("qualityScore", roundedQualityScore());
        metadata.put("threshold", roundedThreshold());
        metadata.put("passes", passes());
        return Map.copyOf(metadata);
    }

    private static int taskTokenCount(String task) {
        if (task == null || task.isBlank()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(task.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(value -> value.length() > 1)
                .filter(value -> !STOPWORDS.contains(value))
                .distinct()
                .count();
    }

    private static int failureMarkerCount(HermesLearningSignal signal) {
        int markers = 0;
        for (String key : signal.metadata().keySet()) {
            String normalized = key.toLowerCase(Locale.ROOT);
            if (normalized.contains("error")
                    || normalized.contains("exception")
                    || normalized.contains("failure")
                    || normalized.contains("failed")) {
                markers++;
            }
        }
        for (AgentState.ReasoningStep step : signal.steps()) {
            if (!step.successful()) {
                markers++;
            }
        }
        return markers;
    }

    private static double ratio(double value, double target) {
        if (target <= 0.0) {
            return 0.0;
        }
        return clamp(value / target);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String rounded(double value) {
        return String.format(Locale.ROOT, "%.2f", clamp(value));
    }
}
