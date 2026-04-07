package tech.kayys.wayang.guardrails;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.guardrails.plugin.api.*;
import tech.kayys.wayang.guardrails.plugin.GuardrailPluginRegistry;

import java.util.*;

@ApplicationScoped
public class GuardrailsService {

    private static final Logger LOG = Logger.getLogger(GuardrailsService.class);

    @Inject
    GuardrailPluginRegistry pluginRegistry;

    /**
     * Pre-execution guardrails check for a specific text and configuration.
     * This is useful for direct node execution or embedding in other services.
     */
    public Uni<GuardrailResult> preCheck(String text, Map<String, Object> config) {
        LOG.debugf("Running guardrails pre-check on text");

        // In a full implementation, we would filter detectors based on config
        return pluginRegistry.runDetectorsForPhase(text, CheckPhase.PRE_EXECUTION)
                .map(results -> {
                    DetectionResults detectionResults = new DetectionResults(results);

                    if (!detectionResults.isSafe()) {
                        return GuardrailResult.failure(
                                "Guardrail checks failed",
                                results.stream().filter(r -> !r.safe()).map(DetectionResult::category).toList(),
                                results.stream().flatMap(r -> r.findings().stream()).toList());
                    }

                    return GuardrailResult.success()
                            .withFindings(results.stream().flatMap(r -> r.findings().stream()).toList());
                });
    }

    /**
     * Post-execution guardrails check.
     */
    public Uni<GuardrailResult> postCheck(String text, Map<String, Object> config) {
        LOG.debugf("Running guardrails post-check on text");

        return pluginRegistry.runDetectorsForPhase(text, CheckPhase.POST_EXECUTION)
                .map(results -> {
                    DetectionResults detectionResults = new DetectionResults(results);

                    if (!detectionResults.isSafe()) {
                        return GuardrailResult.failure(
                                "Guardrail checks failed",
                                results.stream().filter(r -> !r.safe()).map(DetectionResult::category).toList(),
                                results.stream().flatMap(r -> r.findings().stream()).toList());
                    }

                    return GuardrailResult.success()
                            .withFindings(results.stream().flatMap(r -> r.findings().stream()).toList());
                });
    }
}