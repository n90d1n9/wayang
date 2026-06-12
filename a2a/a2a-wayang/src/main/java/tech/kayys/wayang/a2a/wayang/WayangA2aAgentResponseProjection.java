package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared projection rules for Wayang agent responses exposed through A2A.
 */
final class WayangA2aAgentResponseProjection {

    private WayangA2aAgentResponseProjection() {
    }

    static String messageId(AgentResponse response) {
        AgentResponse resolved = requireResponse(response);
        String runId = WayangA2aMaps.optional(resolved.runId());
        if (runId != null) {
            return runId;
        }
        String requestId = WayangA2aMaps.optional(resolved.requestId());
        return requestId == null ? "wayang-response" : requestId + "-response";
    }

    static String text(AgentResponse response) {
        AgentResponse resolved = requireResponse(response);
        String answer = WayangA2aMaps.optional(resolved.answer());
        if (answer != null) {
            return answer;
        }
        String error = WayangA2aMaps.optional(resolved.error());
        return error == null ? "" : error;
    }

    static Map<String, Object> metadata(AgentResponse response) {
        AgentResponse resolved = requireResponse(response);
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (resolved.runId() != null) {
            metadata.put("runId", resolved.runId());
        }
        if (resolved.requestId() != null) {
            metadata.put("requestId", resolved.requestId());
        }
        metadata.put("successful", resolved.successful());
        metadata.put("totalSteps", resolved.totalSteps());
        metadata.put("durationMs", resolved.durationMs());
        if (resolved.strategy() != null) {
            metadata.put("strategy", resolved.strategy());
        }
        if (resolved.error() != null) {
            metadata.put("error", resolved.error());
        }
        return WayangA2aMaps.copyMap(metadata);
    }

    private static AgentResponse requireResponse(AgentResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("response must not be null");
        }
        return response;
    }
}
