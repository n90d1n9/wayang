package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.projection.SessionProjection;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;

/**
 * Compact operator-facing summary for A2UI request session config diagnostics.
 */
public record SessionConfigRequestDiagnosticsSummary(
        String diagnosticsId,
        boolean passed,
        int exitCode,
        String activeInput,
        SessionConfigLoadStatus status,
        boolean contextPresent,
        boolean configPresent,
        boolean sourcePresent,
        boolean sourceDiagnosticsPresent,
        String sourceType,
        boolean sourceValid,
        boolean sourceAllowed,
        int validationErrorCount,
        int policyErrorCount,
        int attemptCount,
        String message,
        Map<String, Object> attributes) {

    public static final String DIAGNOSTICS_ID = "a2ui.session-config.request-diagnostics-summary";
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;

    public SessionConfigRequestDiagnosticsSummary {
        diagnosticsId = DecodeValues.text(diagnosticsId, DIAGNOSTICS_ID);
        exitCode = RecordNumbers.nonNegative(exitCode);
        activeInput = DecodeValues.text(activeInput, SessionConfigRequestDiagnostics.ACTIVE_NONE);
        status = status == null ? SessionConfigLoadStatus.MISSING : status;
        sourceType = DecodeValues.text(sourceType, "none");
        validationErrorCount = RecordNumbers.nonNegative(validationErrorCount);
        policyErrorCount = RecordNumbers.nonNegative(policyErrorCount);
        attemptCount = RecordNumbers.nonNegative(attemptCount);
        message = DecodeValues.text(message);
        attributes = TransportMaps.copy(attributes);
    }

    public static SessionConfigRequestDiagnosticsSummary from(SessionConfigRequestDiagnostics diagnostics) {
        if (diagnostics == null) {
            return empty();
        }
        SessionConfigSourceDiagnostics source = diagnostics.sourceDiagnostics();
        boolean sourceDiagnosticsPresent = source != null;
        return new SessionConfigRequestDiagnosticsSummary(
                DIAGNOSTICS_ID,
                diagnostics.loaded(),
                diagnostics.loaded() ? EXIT_SUCCESS : EXIT_FAILURE,
                diagnostics.activeInput(),
                diagnostics.status(),
                diagnostics.contextPresent(),
                diagnostics.configPresent(),
                diagnostics.sourcePresent(),
                sourceDiagnosticsPresent,
                sourceDiagnosticsPresent ? source.sourceType() : "none",
                sourceDiagnosticsPresent && source.valid(),
                sourceDiagnosticsPresent && source.allowed(),
                sourceDiagnosticsPresent ? source.validationErrors().size() : 0,
                sourceDiagnosticsPresent ? source.policyErrors().size() : 0,
                diagnostics.loadResult().attempts().size(),
                diagnostics.loadResult().message(),
                Map.of(
                        "contextKey", diagnostics.contextKey(),
                        "configKey", diagnostics.configKey(),
                        "sourceKey", diagnostics.sourceKey()));
    }

    public static SessionConfigRequestDiagnosticsSummary empty() {
        return new SessionConfigRequestDiagnosticsSummary(
                DIAGNOSTICS_ID,
                false,
                EXIT_FAILURE,
                SessionConfigRequestDiagnostics.ACTIVE_NONE,
                SessionConfigLoadStatus.MISSING,
                false,
                false,
                false,
                false,
                "none",
                false,
                false,
                0,
                0,
                0,
                "A2UI session config source did not provide JSON.",
                Map.of());
    }

    public static SessionConfigRequestDiagnosticsSummary fromMap(Map<?, ?> values) {
        return SessionConfigRequestDiagnosticsSummaryDecoder.fromMap(values);
    }

    public static SessionConfigRequestDiagnosticsSummary fromJson(String json) {
        return SessionConfigRequestDiagnosticsSummaryDecoder.fromJson(json);
    }

    public static SessionConfigRequestDiagnosticsSummary fromDiagnosticsMap(Map<?, ?> values) {
        return SessionConfigRequestDiagnosticsSummaryDecoder.fromDiagnosticsMap(values);
    }

    public static SessionConfigRequestDiagnosticsSummary fromDiagnosticsJson(String json) {
        return SessionConfigRequestDiagnosticsSummaryDecoder.fromDiagnosticsJson(json);
    }

    public boolean successfulExit() {
        return passed && exitCode == EXIT_SUCCESS;
    }

    public Map<String, Object> toMap() {
        return SessionProjection.requestDiagnosticsSummary(this);
    }

    public String toJson() {
        return TransportJson.json(
                toMap(),
                "Unable to encode A2UI session config request diagnostics summary");
    }
}
