package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;

/**
 * Decodes stored or remote A2UI session config source diagnostic payloads.
 */
public final class SessionConfigSourceDiagnosticsDecoder {

    public static SessionConfigSourceDiagnostics fromMap(Map<?, ?> values) {
        Map<String, Object> diagnostics = TransportMaps.copy(values);
        return new SessionConfigSourceDiagnostics(
                DecodeValues.text(
                        diagnostics.get("diagnosticsId"),
                        SessionConfigSourceDiagnostics.DIAGNOSTICS_ID),
                DecodeValues.text(diagnostics.get("sourceType"), "unknown"),
                TransportMaps.copyMap(diagnostics.get("sourceSpec")),
                loadResult(diagnostics),
                TransportMaps.copyMap(diagnostics.get("sourcePolicy")),
                DecodeCollections.texts(diagnostics.get("validationErrors")),
                DecodeCollections.texts(diagnostics.get("policyErrors")),
                TransportMaps.copyMap(diagnostics.get("capability")),
                TransportMaps.copyMap(diagnostics.get("sourceCapabilities")));
    }

    public static SessionConfigSourceDiagnostics fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI session config source diagnostics JSON must not be blank",
                "Unable to decode A2UI session config source diagnostics JSON"));
    }

    private static SessionConfigLoadResult loadResult(Map<String, Object> diagnostics) {
        Map<String, Object> result = TransportMaps.copyMap(diagnostics.get("loadResult"));
        if (!result.isEmpty()) {
            return SessionConfigLoadResult.fromMap(result);
        }
        SessionConfigLoadStatus status = SessionConfigLoadResultDecoder.status(
                diagnostics.get("status"),
                SessionConfigLoadStatus.MISSING);
        return new SessionConfigLoadResult(
                "session-config-source",
                status,
                WayangA2uiSessionConfig.defaultConfig(),
                SessionConfigLoadResultDecoder.defaultMessage(status));
    }

    private SessionConfigSourceDiagnosticsDecoder() {
    }
}
