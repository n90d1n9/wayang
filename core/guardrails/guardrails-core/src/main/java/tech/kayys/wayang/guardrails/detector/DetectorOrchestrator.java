package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.guardrails.plugin.api.*;
import tech.kayys.wayang.guardrails.plugin.GuardrailPluginRegistry;
import tech.kayys.wayang.guardrails.ExecutionResult;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DetectorOrchestrator {

    @Inject
    GuardrailPluginRegistry pluginRegistry;

    public Uni<DetectionResults> detectInputIssues(NodeContext context) {
        String inputText = extractText(context.inputs());

        return pluginRegistry.runDetectorsForPhase(inputText, CheckPhase.PRE_EXECUTION)
                .map(DetectionResults::new);
    }

    public Uni<DetectionResults> detectOutputIssues(ExecutionResult result) {
        String outputText = extractText(result.outputs());

        return pluginRegistry.runDetectorsForPhase(outputText, CheckPhase.POST_EXECUTION)
                .map(DetectionResults::new);
    }

    private String extractText(Map<String, Object> data) {
        return data.values().stream()
                .filter(v -> v instanceof String)
                .map(Object::toString)
                .collect(Collectors.joining(" "));
    }
}