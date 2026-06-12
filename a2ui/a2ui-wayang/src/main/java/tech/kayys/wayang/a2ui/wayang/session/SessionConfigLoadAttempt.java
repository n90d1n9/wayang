package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.projection.SessionProjection;

import java.util.Map;
import java.util.Objects;

/**
 * Lightweight diagnostic row for one attempted A2UI session config source load.
 */
public record SessionConfigLoadAttempt(
        String sourceDescription,
        SessionConfigLoadStatus status,
        String message) {

    public SessionConfigLoadAttempt {
        sourceDescription = sourceDescription == null || sourceDescription.isBlank()
                ? "session-config"
                : sourceDescription.trim();
        status = status == null ? SessionConfigLoadStatus.MISSING : status;
        message = message == null ? "" : message.trim();
    }

    public static SessionConfigLoadAttempt from(SessionConfigLoadResult result) {
        SessionConfigLoadResult resolved = Objects.requireNonNull(result, "result");
        return new SessionConfigLoadAttempt(
                resolved.sourceDescription(),
                resolved.status(),
                resolved.message());
    }

    public static SessionConfigLoadAttempt fromMap(Map<?, ?> values) {
        return SessionConfigLoadResultDecoder.attemptFromMap(values);
    }

    public static SessionConfigLoadAttempt loaded(String sourceDescription) {
        return new SessionConfigLoadAttempt(
                sourceDescription,
                SessionConfigLoadStatus.LOADED,
                "A2UI session config loaded.");
    }

    public static SessionConfigLoadAttempt missing(String sourceDescription) {
        return new SessionConfigLoadAttempt(
                sourceDescription,
                SessionConfigLoadStatus.MISSING,
                "A2UI session config source did not provide JSON.");
    }

    public static SessionConfigLoadAttempt failed(String sourceDescription, String message) {
        return new SessionConfigLoadAttempt(
                sourceDescription,
                SessionConfigLoadStatus.FAILED,
                message == null || message.isBlank()
                        ? "Unable to load A2UI session config."
                        : message);
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

    public Map<String, Object> toMap() {
        return SessionProjection.loadAttempt(this);
    }
}
