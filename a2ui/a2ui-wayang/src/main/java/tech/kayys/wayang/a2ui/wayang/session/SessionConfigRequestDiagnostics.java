package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.projection.SessionProjection;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import java.util.Map;

/**
 * Request-level diagnostic envelope for A2UI session config resolution.
 */
public record SessionConfigRequestDiagnostics(
        String diagnosticsId,
        String contextKey,
        String configKey,
        String sourceKey,
        boolean contextPresent,
        boolean configPresent,
        boolean sourcePresent,
        String activeInput,
        SessionConfigLoadResult loadResult,
        SessionConfigSourceDiagnostics sourceDiagnostics) {

    public static final String DIAGNOSTICS_ID = "a2ui.session-config.request-diagnostics";
    public static final String ACTIVE_DIRECT_CONFIG = "direct-config";
    public static final String ACTIVE_SOURCE = "source";
    public static final String ACTIVE_NONE = "none";

    public SessionConfigRequestDiagnostics {
        diagnosticsId = DecodeValues.text(diagnosticsId, DIAGNOSTICS_ID);
        contextKey = DecodeValues.text(contextKey, "a2ui");
        configKey = DecodeValues.text(configKey, "sessionConfig");
        sourceKey = DecodeValues.text(sourceKey, "sessionConfigSource");
        activeInput = DecodeValues.text(activeInput, ACTIVE_NONE);
        loadResult = loadResult == null
                ? SessionConfigLoadResult.missing(contextKey + "." + configKey)
                : loadResult;
    }

    public static SessionConfigRequestDiagnostics fromMap(Map<?, ?> values) {
        return SessionConfigRequestDiagnosticsDecoder.fromMap(values);
    }

    public static SessionConfigRequestDiagnostics fromJson(String json) {
        return SessionConfigRequestDiagnosticsDecoder.fromJson(json);
    }

    public SessionConfigRequestDiagnosticsSummary summary() {
        return SessionConfigRequestDiagnosticsSummary.from(this);
    }

    public boolean sourceActive() {
        return ACTIVE_SOURCE.equals(activeInput);
    }

    public boolean directConfigActive() {
        return ACTIVE_DIRECT_CONFIG.equals(activeInput);
    }

    public boolean sourceDiagnosticsPresent() {
        return sourceDiagnostics != null;
    }

    public SessionConfigLoadStatus status() {
        return loadResult.status();
    }

    public boolean loaded() {
        return loadResult.loaded();
    }

    public boolean missing() {
        return loadResult.missing();
    }

    public boolean failed() {
        return loadResult.failed();
    }

    public Map<String, Object> toMap() {
        return SessionProjection.requestDiagnostics(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI session config request diagnostics");
    }
}
