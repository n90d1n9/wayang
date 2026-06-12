package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;
import tech.kayys.wayang.a2ui.wayang.projection.SessionProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import java.util.List;
import java.util.Map;

/**
 * Diagnostic result for loading A2UI session configuration from a source.
 */
public record SessionConfigLoadResult(
        String sourceDescription,
        SessionConfigLoadStatus status,
        WayangA2uiSessionConfig config,
        String message,
        List<SessionConfigLoadAttempt> attempts) {

    public SessionConfigLoadResult(
            String sourceDescription,
            SessionConfigLoadStatus status,
            WayangA2uiSessionConfig config,
            String message) {
        this(sourceDescription, status, config, message, List.of());
    }

    public SessionConfigLoadResult {
        sourceDescription = sourceDescription == null || sourceDescription.isBlank()
                ? "session-config"
                : sourceDescription.trim();
        status = status == null ? SessionConfigLoadStatus.MISSING : status;
        config = config == null ? WayangA2uiSessionConfig.defaultConfig() : config;
        message = message == null ? "" : message.trim();
        attempts = attempts == null ? List.of() : List.copyOf(attempts);
    }

    public static SessionConfigLoadResult loaded(String sourceDescription, WayangA2uiSessionConfig config) {
        return new SessionConfigLoadResult(
                sourceDescription,
                SessionConfigLoadStatus.LOADED,
                config,
                "A2UI session config loaded.");
    }

    public static SessionConfigLoadResult missing(String sourceDescription) {
        return new SessionConfigLoadResult(
                sourceDescription,
                SessionConfigLoadStatus.MISSING,
                WayangA2uiSessionConfig.defaultConfig(),
                "A2UI session config source did not provide JSON.");
    }

    public static SessionConfigLoadResult failed(String sourceDescription, RuntimeException failure) {
        String details = failure == null || failure.getMessage() == null ? "" : " " + failure.getMessage();
        return new SessionConfigLoadResult(
                sourceDescription,
                SessionConfigLoadStatus.FAILED,
                WayangA2uiSessionConfig.defaultConfig(),
                "Unable to load A2UI session config." + details);
    }

    public static SessionConfigLoadResult fromMap(Map<?, ?> values) {
        return SessionConfigLoadResultDecoder.fromMap(values);
    }

    public static SessionConfigLoadResult fromJson(String json) {
        return SessionConfigLoadResultDecoder.fromJson(json);
    }

    public boolean loaded() {
        return status == SessionConfigLoadStatus.LOADED;
    }

    public boolean missing() {
        return status == SessionConfigLoadStatus.MISSING;
    }

    public boolean failed() {
        return status == SessionConfigLoadStatus.FAILED;
    }

    public boolean traced() {
        return !attempts.isEmpty();
    }

    public SessionConfigLoadResult withAttempts(List<SessionConfigLoadAttempt> attempts) {
        return new SessionConfigLoadResult(sourceDescription, status, config, message, attempts);
    }

    public Map<String, Object> toMap() {
        return SessionProjection.loadResult(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI session config load result");
    }
}
