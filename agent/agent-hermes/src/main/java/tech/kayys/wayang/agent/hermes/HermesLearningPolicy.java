package tech.kayys.wayang.agent.hermes;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Decides whether a successful run is worth converting into a reusable skill.
 */
public final class HermesLearningPolicy {

    private final HermesAgentModeConfig config;

    public HermesLearningPolicy(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
    }

    public HermesLearningAssessment assess(HermesLearningSignal signal) {
        Objects.requireNonNull(signal, "signal");
        HermesLearningQualityProfile quality = HermesLearningQualityProfile.from(signal, config);
        if (!config.skillLearningEnabled()) {
            return HermesLearningAssessment.skipped("skill learning disabled", quality);
        }
        if (!signal.successful()) {
            return HermesLearningAssessment.skipped("run was not successful", quality);
        }
        if (metadataBoolean(signal, HermesAgentMode.PARAM_SKIP_LEARN_KEY).orElse(false)) {
            return HermesLearningAssessment.skipped("learning explicitly skipped", quality);
        }
        if (metadataBoolean(signal, HermesAgentMode.PARAM_LEARN_KEY).orElse(false)) {
            return HermesLearningAssessment.eligible("learning explicitly requested", true, quality);
        }
        int stepCount = signal.steps().size();
        if (stepCount < config.minStepsToLearn()) {
            return HermesLearningAssessment.skipped(
                    "run had " + stepCount + " step(s), below learning threshold " + config.minStepsToLearn(),
                    quality);
        }
        if (!quality.passes()) {
            return HermesLearningAssessment.skipped(
                    "learning quality score "
                            + quality.roundedQualityScore()
                            + " below threshold "
                            + quality.roundedThreshold(),
                    quality);
        }
        return HermesLearningAssessment.eligible("run met learning quality threshold", false, quality);
    }

    private static Optional<Boolean> metadataBoolean(HermesLearningSignal signal, String key) {
        Object value = signal.metadata().get(key);
        if (value instanceof Boolean bool) {
            return Optional.of(bool);
        }
        if (value == null) {
            return Optional.empty();
        }
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return switch (normalized) {
            case "true", "yes", "y", "1", "on", "enabled" -> Optional.of(true);
            case "false", "no", "n", "0", "off", "disabled" -> Optional.of(false);
            default -> Optional.empty();
        };
    }
}
