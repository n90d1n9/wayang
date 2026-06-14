package tech.kayys.wayang.gollek.sdk.remote;

import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunForgetResult;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStates;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RemoteRunLifecycleMapper {

    private final URI endpoint;

    RemoteRunLifecycleMapper(URI endpoint) {
        this.endpoint = endpoint;
    }

    RemoteRunSubmission submission(RemoteResponse response) {
        String runId = RemoteJson.stringField(response.body(), "runId");
        String strategy = RemoteJson.stringField(response.body(), "strategy");
        AgentRunState state = state(RemoteJson.stringField(response.body(), "state"), AgentRunState.RUNNING);
        return new RemoteRunSubmission(
                runId.isBlank() ? "remote-" + UUID.randomUUID() : runId,
                state,
                strategy.isBlank() ? "wayang-remote-api" : strategy);
    }

    String statusPath(String runId) {
        return runPath(runId, "status");
    }

    String eventsPath(String runId) {
        return eventsPath(runId, AgentRunEventsQuery.all());
    }

    String eventsPath(String runId, AgentRunEventsQuery query) {
        AgentRunEventsQuery normalized = query == null ? AgentRunEventsQuery.all() : query;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("limit", String.valueOf(normalized.limit()));
        if (normalized.state() != null) {
            params.put("state", AgentRunStates.wireName(normalized.state()));
        }
        if (!normalized.type().isBlank()) {
            params.put("type", normalized.type());
        }
        if (normalized.afterSequence() > 0) {
            params.put("afterSequence", String.valueOf(normalized.afterSequence()));
        }
        return withQuery(runPath(runId, "events"), params);
    }

    String cancelPath(String runId) {
        return runPath(runId, "cancel");
    }

    String forgetPath(String runId) {
        return runPath(runId);
    }

    String historyPath(AgentRunHistoryQuery query) {
        AgentRunHistoryQuery normalized = query == null ? AgentRunHistoryQuery.all() : query;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("limit", String.valueOf(normalized.limit()));
        if (normalized.offset() > 0) {
            params.put("offset", String.valueOf(normalized.offset()));
        }
        if (normalized.state() != null) {
            params.put("state", AgentRunStates.wireName(normalized.state()));
        }
        if (!normalized.tenantId().isBlank()) {
            params.put("tenantId", normalized.tenantId());
        }
        if (!normalized.sessionId().isBlank()) {
            params.put("sessionId", normalized.sessionId());
        }
        if (!normalized.surfaceId().isBlank()) {
            params.put("surfaceId", normalized.surfaceId());
        }
        if (!normalized.profileId().isBlank()) {
            params.put("profileId", normalized.profileId());
        }
        return withQuery("/runs", params);
    }

    AgentRunStatus status(RemoteResponse response, String fallbackRunId, String fallbackMessage) {
        return status(
                response.body(),
                fallbackRunId,
                fallbackMessage,
                response.statusCode());
    }

    AgentRunHistory history(RemoteResponse response, AgentRunHistoryQuery query) {
        AgentRunHistoryQuery normalized = query == null ? AgentRunHistoryQuery.all() : query;
        List<AgentRunStatus> runs = RemoteJson.objectArrayField(response.body(), "runs").stream()
                .map(body -> status(
                        body,
                        "",
                        "Remote run history item.",
                        response.statusCode()))
                .toList();
        int totalRuns = RemoteJson.intField(response.body(), "totalRuns", runs.size());
        String message = RemoteJson.stringField(response.body(), "message");
        if (message.isBlank()) {
            message = "Remote run history endpoint returned HTTP " + response.statusCode() + ".";
        }
        return new AgentRunHistory(
                normalized,
                runs,
                totalRuns,
                message);
    }

    AgentRunEvents events(RemoteResponse response, String fallbackRunId) {
        return events(response, fallbackRunId, AgentRunEventsQuery.all());
    }

    AgentRunEvents events(RemoteResponse response, String fallbackRunId, AgentRunEventsQuery query) {
        AgentRunEventsQuery normalized = query == null ? AgentRunEventsQuery.all() : query;
        List<AgentRunEvent> events = RemoteJson.objectArrayField(response.body(), "events").stream()
                .map(body -> event(body, fallbackRunId, response))
                .toList();
        int totalEvents = RemoteJson.intField(response.body(), "totalEvents", events.size());
        String runId = RemoteJson.stringField(response.body(), "runId");
        String message = RemoteJson.stringField(response.body(), "message");
        if (message.isBlank()) {
            message = events.isEmpty()
                    ? "No remote run events are recorded for this run id."
                    : "Remote run events endpoint returned HTTP " + response.statusCode() + ".";
        }
        return new AgentRunEvents(
                runId.isBlank() ? fallbackRunId : runId,
                normalized,
                events,
                totalEvents,
                message);
    }

    AgentRunCancelResult cancel(RemoteResponse response, String fallbackRunId, String reason) {
        String body = response.body();
        String runId = RemoteJson.stringField(body, "runId");
        String strategy = RemoteJson.stringField(body, "strategy");
        boolean cancelled = RemoteJson.booleanField(
                body,
                "cancelled",
                RemoteJson.booleanField(body, "canceled", true));
        String message = RemoteJson.stringField(body, "message");
        AgentRunHandle handle = new AgentRunHandle(
                runId.isBlank() ? fallbackRunId : runId,
                state(RemoteJson.stringField(body, "state"), cancelled ? AgentRunState.CANCELLED : AgentRunState.UNKNOWN),
                strategy.isBlank() ? "wayang-remote-api" : strategy);
        Map<String, Object> metadata = transportMetadata(response);
        String normalizedReason = reason == null ? "" : reason.trim();
        if (!normalizedReason.isEmpty()) {
            metadata.put("reason", normalizedReason);
        }
        return new AgentRunCancelResult(
                handle.runId(),
                cancelled,
                handle,
                message.isBlank()
                        ? "Remote run cancellation endpoint returned HTTP " + response.statusCode() + "."
                        : message,
                metadata);
    }

    AgentRunForgetResult forget(RemoteResponse response, String fallbackRunId) {
        String body = response.body();
        String runId = RemoteJson.stringField(body, "runId");
        boolean forgotten = RemoteJson.booleanField(
                body,
                "forgotten",
                RemoteJson.booleanField(body, "deleted", true));
        String message = RemoteJson.stringField(body, "message");
        Map<String, Object> metadata = transportMetadata(response);
        String state = RemoteJson.stringField(body, "state");
        if (!state.isBlank()) {
            metadata.put("state", state(state, AgentRunState.UNKNOWN).name());
        }
        String strategy = RemoteJson.stringField(body, "strategy");
        if (!strategy.isBlank()) {
            metadata.put("strategy", strategy);
        }
        return new AgentRunForgetResult(
                runId.isBlank() ? fallbackRunId : runId,
                forgotten,
                message.isBlank()
                        ? "Remote run forget endpoint returned HTTP " + response.statusCode() + "."
                        : message,
                metadata);
    }

    private AgentRunEvent event(String body, String fallbackRunId, RemoteResponse response) {
        String runId = RemoteJson.stringField(body, "runId");
        String type = RemoteJson.stringField(body, "type");
        String message = RemoteJson.stringField(body, "message");
        Map<String, Object> metadata = transportMetadata(body, response.statusCode());
        return new AgentRunEvent(
                runId.isBlank() ? fallbackRunId : runId,
                RemoteJson.intField(body, "sequence", 1),
                type,
                state(RemoteJson.stringField(body, "state"), AgentRunState.UNKNOWN),
                message,
                metadata);
    }

    private AgentRunStatus status(String body, String fallbackRunId, String fallbackMessage, int statusCode) {
        AgentRunStatus status = statusFromBody(body, fallbackRunId, fallbackMessage);
        Map<String, Object> metadata = transportMetadata(body, statusCode);
        metadata.putAll(status.metadata());
        return new AgentRunStatus(status.handle(), status.known(), status.message(), metadata);
    }

    private AgentRunStatus statusFromBody(String body, String fallbackRunId, String fallbackMessage) {
        String runId = RemoteJson.stringField(body, "runId");
        String strategy = RemoteJson.stringField(body, "strategy");
        String message = RemoteJson.stringField(body, "message");
        return new AgentRunStatus(
                new AgentRunHandle(
                        runId.isBlank() ? fallbackRunId : runId,
                        state(RemoteJson.stringField(body, "state"), AgentRunState.UNKNOWN),
                        strategy.isBlank() ? "wayang-remote-api" : strategy),
                RemoteJson.booleanField(body, "known", true),
                message.isBlank() ? fallbackMessage : message,
                Map.of());
    }

    private AgentRunState state(String value, AgentRunState fallback) {
        return AgentRunStates.parseOrDefault(value, fallback);
    }

    private Map<String, Object> transportMetadata(RemoteResponse response) {
        return transportMetadata(response.body(), response.statusCode());
    }

    private Map<String, Object> transportMetadata(String body, int statusCode) {
        Map<String, Object> metadata = new LinkedHashMap<>(RemoteJson.objectField(body, "metadata"));
        metadata.put("endpoint", endpoint.toString());
        metadata.put("httpStatus", statusCode);
        metadata.put("responsePreview", preview(body));
        return metadata;
    }

    private String runPath(String runId) {
        return "/runs/" + encode(normalizeRunId(runId));
    }

    private String runPath(String runId, String childPath) {
        return runPath(runId) + "/" + encode(childPath);
    }

    private String withQuery(String path, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return path;
        }
        StringBuilder output = new StringBuilder(path);
        String separator = "?";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (entry.getKey() == null || value == null || value.isBlank()) {
                continue;
            }
            output.append(separator)
                    .append(encode(entry.getKey()))
                    .append("=")
                    .append(encode(value));
            separator = "&";
        }
        return output.toString();
    }

    private String normalizeRunId(String runId) {
        return runId == null ? "" : runId.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String preview(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    record RemoteRunSubmission(String runId, AgentRunState state, String strategy) {

        AgentRunHandle handle() {
            return new AgentRunHandle(runId, state, strategy);
        }

        boolean successful() {
            return state != AgentRunState.FAILED;
        }
    }
}
