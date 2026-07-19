package tech.kayys.wayang.guardrails.plugin.api;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.WayangPlugin;

import java.util.Map;

/**
 * Interface for guardrail detector plugins.
 */
public interface GuardrailDetectorPlugin extends WayangPlugin {

    /**
     * Get the phases when this detector should be applied.
     */
    CheckPhase[] applicablePhases();

    /**
     * Detect issues in the provided text.
     */
    default Uni<DetectionResult> detect(String text) {
        return detect(text, Map.of());
    }

    /**
     * Detect issues in the provided text with context metadata.
     */
    Uni<DetectionResult> detect(String text, Map<String, Object> metadata);

    /**
     * Get the category of this detector.
     */
    String getCategory();

    /**
     * Get the severity level of this detector.
     */
    DetectionSeverity getSeverity();
}
