package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Learning-owned projection for Hermes skill-learning runtime event metadata.
 */
public record HermesLearningRuntimeEventProjection(
        String outcome,
        Map<String, Object> metadata) {

    public HermesLearningRuntimeEventProjection {
        outcome = HermesDirectiveSupport.clean(outcome, "unknown");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesLearningRuntimeEventProjection completed(
            AgentRequest request,
            AgentResponse response,
            HermesLearningResult result) {
        HermesLearningResult resolved = result == null
                ? HermesLearningResult.skipped("learning result missing")
                : result;
        Map<String, Object> metadata = baseLearningMetadata(request, response);
        metadata.put("decision", decision(resolved));
        metadata.put("skillId", HermesDirectiveSupport.clean(resolved.skillId(), ""));
        metadata.put("reason", HermesDirectiveSupport.clean(resolved.reason(), ""));
        metadata.put("persisted", resolved.decision() == HermesLearningDecision.CREATED
                || resolved.decision() == HermesLearningDecision.UPDATED);

        HermesLearningResultMetadata learningMetadata = resolved.metadataView();
        if (!learningMetadata.emptyMetadata()) {
            metadata.put(HermesLearningMetadataKeys.RESULT, learningMetadata.toMetadata());
        }
        if (!learningMetadata.lifecycle().isEmpty()) {
            metadata.put(HermesLearningMetadataKeys.LIFECYCLE, learningMetadata.lifecycle());
        }
        resolved.skillDefinition().ifPresent(skill -> {
            metadata.put("skillName", HermesDirectiveSupport.clean(skill.name(), ""));
            metadata.put("skillCategory", HermesDirectiveSupport.clean(skill.category(), ""));
            metadata.put("revision", HermesDirectiveSupport.clean(
                    String.valueOf(skill.metadata().getOrDefault("hermes.revision", "")),
                    ""));
        });
        return new HermesLearningRuntimeEventProjection(
                response != null && !response.successful() ? "failed" : decision(resolved),
                metadata);
    }

    public static HermesLearningRuntimeEventProjection failed(
            AgentRequest request,
            AgentResponse response,
            Throwable error) {
        Map<String, Object> metadata = baseLearningMetadata(request, response);
        metadata.put("errorType", error == null ? "" : error.getClass().getName());
        metadata.put("error", error == null ? "" : HermesDirectiveSupport.clean(error.getMessage(), ""));
        return new HermesLearningRuntimeEventProjection("failed", metadata);
    }

    private static Map<String, Object> baseLearningMetadata(
            AgentRequest request,
            AgentResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mode", HermesAgentMode.MODE_ID);
        metadata.put("modelId", request == null ? "" : HermesDirectiveSupport.clean(request.modelId(), ""));
        metadata.put("stream", request != null && request.stream());
        metadata.put("verbose", request != null && request.verbose());
        metadata.put("runId", response == null ? "" : HermesDirectiveSupport.clean(response.runId(), ""));
        metadata.put("responseSuccessful", response == null || response.successful());
        metadata.put("responseStrategy", response == null ? "" : HermesDirectiveSupport.clean(response.strategy(), ""));
        metadata.put("totalSteps", response == null ? 0 : response.totalSteps());
        return metadata;
    }

    private static String decision(HermesLearningResult result) {
        return result.decision().name().toLowerCase(java.util.Locale.ROOT);
    }
}
