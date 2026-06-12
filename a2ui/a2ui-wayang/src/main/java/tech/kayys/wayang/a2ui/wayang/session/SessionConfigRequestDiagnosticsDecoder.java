package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;

/**
 * Decodes stored or remote A2UI session config request diagnostics.
 */
public final class SessionConfigRequestDiagnosticsDecoder {

    public static SessionConfigRequestDiagnostics fromMap(Map<?, ?> values) {
        Map<String, Object> diagnostics = TransportMaps.copy(values);
        return new SessionConfigRequestDiagnostics(
                DecodeValues.text(
                        diagnostics.get("diagnosticsId"),
                        SessionConfigRequestDiagnostics.DIAGNOSTICS_ID),
                DecodeValues.text(diagnostics.get("contextKey"), "a2ui"),
                DecodeValues.text(diagnostics.get("configKey"), "sessionConfig"),
                DecodeValues.text(diagnostics.get("sourceKey"), "sessionConfigSource"),
                DecodeValues.bool(diagnostics.get("contextPresent"), false),
                DecodeValues.bool(diagnostics.get("configPresent"), false),
                DecodeValues.bool(diagnostics.get("sourcePresent"), false),
                DecodeValues.text(diagnostics.get("activeInput"), SessionConfigRequestDiagnostics.ACTIVE_NONE),
                loadResult(diagnostics),
                sourceDiagnostics(diagnostics));
    }

    public static SessionConfigRequestDiagnostics fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI session config request diagnostics JSON must not be blank",
                "Unable to decode A2UI session config request diagnostics JSON"));
    }

    private static SessionConfigLoadResult loadResult(Map<String, Object> diagnostics) {
        Map<String, Object> loadResult = TransportMaps.copyMap(diagnostics.get("loadResult"));
        return loadResult.isEmpty()
                ? SessionConfigLoadResult.missing("a2ui.sessionConfig")
                : SessionConfigLoadResult.fromMap(loadResult);
    }

    private static SessionConfigSourceDiagnostics sourceDiagnostics(Map<String, Object> diagnostics) {
        Map<String, Object> sourceDiagnostics = TransportMaps.copyMap(diagnostics.get("sourceDiagnostics"));
        return sourceDiagnostics.isEmpty()
                ? null
                : SessionConfigSourceDiagnostics.fromMap(sourceDiagnostics);
    }

    private SessionConfigRequestDiagnosticsDecoder() {
    }
}
