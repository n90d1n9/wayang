package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Decodes stored or remote A2UI session config load diagnostics.
 */
public final class SessionConfigLoadResultDecoder {

    public static SessionConfigLoadResult fromMap(Map<?, ?> values) {
        Map<String, Object> result = TransportMaps.copy(values);
        SessionConfigLoadStatus status = status(result.get("status"), SessionConfigLoadStatus.MISSING);
        List<SessionConfigLoadAttempt> attempts = DecodeCollections.maps(result.get("attempts")).stream()
                .map(SessionConfigLoadResultDecoder::attemptFromMap)
                .toList();
        return new SessionConfigLoadResult(
                DecodeValues.text(result.get("sourceDescription"), "session-config"),
                status,
                WayangA2uiSessionConfig.fromMap(TransportMaps.copyMap(result.get("config"))),
                DecodeValues.text(result.get("message"), defaultMessage(status)),
                attempts);
    }

    public static SessionConfigLoadResult fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI session config load result JSON must not be blank",
                "Unable to decode A2UI session config load result JSON"));
    }

    public static SessionConfigLoadAttempt attemptFromMap(Map<?, ?> values) {
        Map<String, Object> attempt = TransportMaps.copy(values);
        SessionConfigLoadStatus status = status(attempt.get("status"), SessionConfigLoadStatus.MISSING);
        return new SessionConfigLoadAttempt(
                DecodeValues.text(attempt.get("sourceDescription"), "session-config"),
                status,
                DecodeValues.text(attempt.get("message"), defaultMessage(status)));
    }

    static SessionConfigLoadStatus status(Object value, SessionConfigLoadStatus fallback) {
        String text = DecodeValues.text(value);
        if (text.isBlank()) {
            return fallback == null ? SessionConfigLoadStatus.MISSING : fallback;
        }
        String normalized = text.replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return SessionConfigLoadStatus.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback == null ? SessionConfigLoadStatus.MISSING : fallback;
        }
    }

    static String defaultMessage(SessionConfigLoadStatus status) {
        return switch (status == null ? SessionConfigLoadStatus.MISSING : status) {
            case LOADED -> "A2UI session config loaded.";
            case FAILED -> "Unable to load A2UI session config.";
            case MISSING -> "A2UI session config source did not provide JSON.";
        };
    }

    private SessionConfigLoadResultDecoder() {
    }
}
