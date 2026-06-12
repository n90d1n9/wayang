package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;

/**
 * Decodes stored or remote A2UI session config request diagnostic summaries.
 */
public final class SessionConfigRequestDiagnosticsSummaryDecoder {

    public static SessionConfigRequestDiagnosticsSummary fromMap(Map<?, ?> values) {
        Map<String, Object> summary = TransportMaps.copy(values);
        if (summary.isEmpty()) {
            return SessionConfigRequestDiagnosticsSummary.empty();
        }
        return new SessionConfigRequestDiagnosticsSummary(
                DecodeValues.text(
                        summary.get("diagnosticsId"),
                        SessionConfigRequestDiagnosticsSummary.DIAGNOSTICS_ID),
                DecodeValues.bool(summary.get("passed"), false),
                DecodeValues.clampedNonNegativeInt(
                        summary.get("exitCode"),
                        SessionConfigRequestDiagnosticsSummary.EXIT_FAILURE),
                DecodeValues.text(summary.get("activeInput"), SessionConfigRequestDiagnostics.ACTIVE_NONE),
                SessionConfigLoadResultDecoder.status(summary.get("status"), SessionConfigLoadStatus.MISSING),
                DecodeValues.bool(summary.get("contextPresent"), false),
                DecodeValues.bool(summary.get("configPresent"), false),
                DecodeValues.bool(summary.get("sourcePresent"), false),
                DecodeValues.bool(summary.get("sourceDiagnosticsPresent"), false),
                DecodeValues.text(summary.get("sourceType"), "none"),
                DecodeValues.bool(summary.get("sourceValid"), false),
                DecodeValues.bool(summary.get("sourceAllowed"), false),
                DecodeValues.clampedNonNegativeInt(summary.get("validationErrorCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("policyErrorCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("attemptCount"), 0),
                DecodeValues.text(summary.get("message")),
                TransportMaps.copyMap(summary.get("attributes")));
    }

    public static SessionConfigRequestDiagnosticsSummary fromDiagnosticsMap(Map<?, ?> values) {
        Map<String, Object> diagnostics = TransportMaps.copy(values);
        Map<String, Object> summary = TransportMaps.copyMap(diagnostics.get("summary"));
        if (!summary.isEmpty()) {
            return fromMap(summary);
        }
        return SessionConfigRequestDiagnosticsDecoder.fromMap(diagnostics).summary();
    }

    public static SessionConfigRequestDiagnosticsSummary fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI session config request diagnostics summary JSON must not be blank",
                "Unable to decode A2UI session config request diagnostics summary JSON"));
    }

    public static SessionConfigRequestDiagnosticsSummary fromDiagnosticsJson(String json) {
        return fromDiagnosticsMap(TransportJson.map(
                json,
                "A2UI session config request diagnostics JSON must not be blank",
                "Unable to decode A2UI session config request diagnostics JSON"));
    }

    private SessionConfigRequestDiagnosticsSummaryDecoder() {
    }
}
