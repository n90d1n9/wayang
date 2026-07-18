package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps agent execution results and failures to API-facing responses.
 */
final class AgentRunResponseMapper {

    Response ok(AgentResponse response) {
        return Response.ok(toApiResponse(response)).build();
    }

    Response badRequest(Throwable error) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse(error))
                .build();
    }

    Response serverError(Throwable error) {
        return Response.serverError()
                .entity(errorResponse(error))
                .build();
    }

    String streamData(AgentResponse response) {
        return "data: " + safeAnswer(response) + "\n\n";
    }

    String streamError(Throwable error) {
        return "event: error\ndata: " + safeMessage(error) + "\n\n";
    }

    Map<String, Object> toApiResponse(AgentResponse response) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runId", response.runId());
        result.put("requestId", response.requestId());
        result.put("answer", response.answer());
        result.put("totalSteps", response.totalSteps());
        result.put("successful", response.successful());
        result.put("strategy", response.strategy());
        result.put("durationMs", response.durationMs());
        result.put("error", response.error());
        return result;
    }

    Map<String, Object> errorResponse(Throwable error) {
        return Map.of("error", safeMessage(error));
    }

    private String safeAnswer(AgentResponse response) {
        if (response == null) {
            return "";
        }
        return response.answer() == null ? "" : response.answer();
    }

    private String safeMessage(Throwable error) {
        if (error == null) {
            return "Unknown error";
        }
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
