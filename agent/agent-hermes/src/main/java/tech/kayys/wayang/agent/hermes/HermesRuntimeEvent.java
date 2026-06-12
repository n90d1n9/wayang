package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Durable-shaped lifecycle event emitted by Hermes mode.
 */
public record HermesRuntimeEvent(
        String eventId,
        String type,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String outcome,
        Instant occurredAt,
        Map<String, Object> metadata) {

    public static final String TYPE_REQUEST_PLANNED = "request.planned";
    public static final String TYPE_DIRECTIVES_DISPATCHED = "directives.dispatched";
    public static final String TYPE_RESPONSE_COMPLETED = "response.completed";
    public static final String TYPE_RESPONSE_FAILED = "response.failed";
    public static final String TYPE_LEARNING_AUDIT_RETENTION_ATTENTION = "learning-audit.retention.attention";
    public static final String TYPE_SKILL_LEARNING_COMPLETED = "skill.learning.completed";
    public static final String TYPE_SKILL_LEARNING_FAILED = "skill.learning.failed";

    public HermesRuntimeEvent {
        type = HermesDirectiveSupport.clean(type, "runtime.event");
        HermesDirectiveSupport.Identity identity = new HermesDirectiveSupport.Identity(
                requestId,
                tenantId,
                sessionId,
                userId);
        requestId = identity.requestId();
        tenantId = identity.tenantId();
        sessionId = identity.sessionId();
        userId = identity.userId();
        outcome = HermesDirectiveSupport.clean(outcome, "unknown");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        eventId = HermesDirectiveSupport.clean(eventId, "");
        if (eventId.isBlank()) {
            eventId = HermesDirectiveSupport.prefixedId(
                    "hermes-event",
                    type + "-" + requestId + "-" + occurredAt.toEpochMilli(),
                    "runtime");
        }
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesRuntimeEvent requestPlanned(AgentRequest request, HermesRequestPlan plan) {
        Map<String, Object> metadata = baseRequestMetadata(request);
        metadata.put("requestPlan", plan == null ? Map.of() : plan.parameterMetadata());
        return fromRequest(TYPE_REQUEST_PLANNED, request, "planned", metadata);
    }

    public static HermesRuntimeEvent directivesDispatched(
            AgentRequest request,
            HermesDirectiveDispatchReport report) {
        Map<String, Object> metadata = baseRequestMetadata(request);
        metadata.put("successful", report == null || report.successful());
        metadata.put("attentionCount", report == null ? 0 : report.attentionCount());
        metadata.put("remediationPlan", report == null
                ? HermesRemediationPlan.none().toMetadata()
                : report.remediationPlan().toMetadata());
        metadata.put("dispatchReport", report == null ? Map.of() : report.toMetadata());
        return fromRequest(
                TYPE_DIRECTIVES_DISPATCHED,
                request,
                report == null ? "unknown" : report.outcome(),
                metadata);
    }

    public static HermesRuntimeEvent responseCompleted(AgentRequest request, AgentResponse response) {
        Map<String, Object> metadata = baseRequestMetadata(request);
        metadata.put("runId", response == null ? "" : HermesDirectiveSupport.clean(response.runId(), ""));
        metadata.put("strategy", response == null ? "" : HermesDirectiveSupport.clean(response.strategy(), ""));
        metadata.put("totalSteps", response == null ? 0 : response.totalSteps());
        metadata.put("durationMs", response == null ? 0L : response.durationMs());
        metadata.put("successful", response == null || response.successful());
        String error = response == null ? "" : HermesDirectiveSupport.clean(response.error(), "");
        if (!error.isBlank()) {
            metadata.put("error", error);
        }
        return fromRequest(
                TYPE_RESPONSE_COMPLETED,
                request,
                response == null || response.successful() ? "successful" : "failed",
                metadata);
    }

    public static HermesRuntimeEvent responseFailed(AgentRequest request, Throwable error, long durationMs) {
        Map<String, Object> metadata = baseRequestMetadata(request);
        metadata.put("durationMs", Math.max(durationMs, 0L));
        metadata.put("errorType", error == null ? "" : error.getClass().getName());
        metadata.put("error", error == null ? "" : HermesDirectiveSupport.clean(error.getMessage(), ""));
        return fromRequest(TYPE_RESPONSE_FAILED, request, "failed", metadata);
    }

    public static HermesRuntimeEvent skillLearningCompleted(
            AgentRequest request,
            AgentResponse response,
            HermesLearningResult result) {
        HermesLearningRuntimeEventProjection projection =
                HermesLearningRuntimeEventProjection.completed(request, response, result);
        return fromRequest(
                TYPE_SKILL_LEARNING_COMPLETED,
                request,
                projection.outcome(),
                projection.metadata());
    }

    public static HermesRuntimeEvent skillLearningFailed(
            AgentRequest request,
            AgentResponse response,
            Throwable error) {
        HermesLearningRuntimeEventProjection projection =
                HermesLearningRuntimeEventProjection.failed(request, response, error);
        return fromRequest(TYPE_SKILL_LEARNING_FAILED, request, projection.outcome(), projection.metadata());
    }

    public static HermesRuntimeEvent fromMetadata(Map<String, ?> values) {
        Map<String, ?> metadata = values == null ? Map.of() : values;
        return new HermesRuntimeEvent(
                text(metadata.get("eventId")),
                text(metadata.get("type")),
                text(metadata.get("requestId")),
                text(metadata.get("tenantId")),
                text(metadata.get("sessionId")),
                text(metadata.get("userId")),
                text(metadata.get("outcome")),
                instant(metadata.get("occurredAt")),
                objectMap(metadata.get("metadata")));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("eventId", eventId);
        values.put("type", type);
        values.put("requestId", requestId);
        values.put("tenantId", tenantId);
        values.put("sessionId", sessionId);
        values.put("userId", userId);
        values.put("outcome", outcome);
        values.put("occurredAt", occurredAt.toString());
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static HermesRuntimeEvent fromRequest(
            String type,
            AgentRequest request,
            String outcome,
            Map<String, Object> metadata) {
        HermesDirectiveSupport.Identity identity = HermesDirectiveSupport.identity(request);
        return new HermesRuntimeEvent(
                "",
                type,
                identity.requestId(),
                identity.tenantId(),
                identity.sessionId(),
                identity.userId(),
                outcome,
                Instant.now(),
                metadata);
    }

    private static Map<String, Object> baseRequestMetadata(AgentRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mode", HermesAgentMode.MODE_ID);
        metadata.put("modelId", request == null ? "" : HermesDirectiveSupport.clean(request.modelId(), ""));
        metadata.put("stream", request != null && request.stream());
        metadata.put("verbose", request != null && request.verbose());
        return metadata;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        String text = text(value);
        if (text.isBlank()) {
            return Instant.now();
        }
        return Instant.parse(text);
    }

    private static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> values.put(String.valueOf(key), mapValue));
        return values;
    }
}
