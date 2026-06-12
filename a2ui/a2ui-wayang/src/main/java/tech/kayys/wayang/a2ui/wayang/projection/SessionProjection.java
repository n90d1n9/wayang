package tech.kayys.wayang.a2ui.wayang.projection;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionPolicy;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadAttempt;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadResult;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnostics;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnosticsSummary;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceDiagnostics;
import tech.kayys.wayang.a2ui.wayang.support.ProjectionCollections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered transport projections for A2UI session configuration.
 */
public final class SessionProjection {

    private SessionProjection() {
    }

    public static Map<String, Object> config(WayangA2uiSessionConfig config) {
        Objects.requireNonNull(config, "config");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiSessionConfig.KEY_ENABLED, config.enabled());
        values.put(WayangA2uiSessionConfig.KEY_POLICY, actionPolicy(config.actionPolicy()));
        return ProjectionMaps.freeze(values);
    }

    public static Map<String, Object> actionPolicy(WayangA2uiActionPolicy policy) {
        Objects.requireNonNull(policy, "policy");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiSessionConfig.KEY_ALLOWED_ACTIONS,
                ProjectionCollections.sortedStrings(policy.allowedActions()));
        values.put(WayangA2uiSessionConfig.KEY_ALLOWED_RUN_IDS,
                ProjectionCollections.sortedStrings(policy.allowedRunIds()));
        values.put(WayangA2uiSessionConfig.KEY_REQUIRED_CONTEXT,
                ProjectionMaps.copy(policy.requiredContext()));
        return ProjectionMaps.freeze(values);
    }

    public static Map<String, Object> loadResult(SessionConfigLoadResult result) {
        Objects.requireNonNull(result, "result");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceDescription", result.sourceDescription());
        values.put("status", result.status().name());
        values.put("loaded", result.loaded());
        values.put("config", config(result.config()));
        values.put("message", result.message());
        if (!result.attempts().isEmpty()) {
            values.put("attempts", result.attempts().stream()
                    .map(SessionProjection::loadAttempt)
                    .toList());
        }
        return ProjectionMaps.freeze(values);
    }

    public static Map<String, Object> loadAttempt(SessionConfigLoadAttempt attempt) {
        Objects.requireNonNull(attempt, "attempt");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceDescription", attempt.sourceDescription());
        values.put("status", attempt.status().name());
        values.put("loaded", attempt.loaded());
        values.put("message", attempt.message());
        return ProjectionMaps.freeze(values);
    }

    public static Map<String, Object> sourceDiagnostics(SessionConfigSourceDiagnostics diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", diagnostics.diagnosticsId());
        values.put("sourceType", diagnostics.sourceType());
        values.put("valid", diagnostics.valid());
        values.put("allowed", diagnostics.allowed());
        values.put("status", diagnostics.status().name());
        values.put("loaded", diagnostics.loaded());
        values.put("missing", diagnostics.missing());
        values.put("failed", diagnostics.failed());
        values.put("validationErrors", diagnostics.validationErrors());
        values.put("policyErrors", diagnostics.policyErrors());
        values.put("sourceSpec", diagnostics.sourceSpec());
        values.put("sourcePolicy", diagnostics.sourcePolicy());
        values.put("capability", diagnostics.capability());
        values.put("sourceCapabilities", diagnostics.sourceCapabilities());
        values.put("loadResult", loadResult(diagnostics.loadResult()));
        return ProjectionMaps.freeze(values);
    }

    public static Map<String, Object> requestDiagnostics(SessionConfigRequestDiagnostics diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", diagnostics.diagnosticsId());
        values.put("summary", diagnostics.summary().toMap());
        values.put("contextKey", diagnostics.contextKey());
        values.put("configKey", diagnostics.configKey());
        values.put("sourceKey", diagnostics.sourceKey());
        values.put("contextPresent", diagnostics.contextPresent());
        values.put("configPresent", diagnostics.configPresent());
        values.put("sourcePresent", diagnostics.sourcePresent());
        values.put("activeInput", diagnostics.activeInput());
        values.put("status", diagnostics.status().name());
        values.put("loaded", diagnostics.loaded());
        values.put("missing", diagnostics.missing());
        values.put("failed", diagnostics.failed());
        values.put("sourceDiagnosticsPresent", diagnostics.sourceDiagnosticsPresent());
        values.put("loadResult", loadResult(diagnostics.loadResult()));
        if (diagnostics.sourceDiagnosticsPresent()) {
            values.put("sourceDiagnostics", sourceDiagnostics(diagnostics.sourceDiagnostics()));
        }
        return ProjectionMaps.freeze(values);
    }

    public static Map<String, Object> requestDiagnosticsSummary(SessionConfigRequestDiagnosticsSummary summary) {
        Objects.requireNonNull(summary, "summary");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", summary.diagnosticsId());
        values.put("passed", summary.passed());
        values.put("exitCode", summary.exitCode());
        values.put("successfulExit", summary.successfulExit());
        values.put("activeInput", summary.activeInput());
        values.put("status", summary.status().name());
        values.put("contextPresent", summary.contextPresent());
        values.put("configPresent", summary.configPresent());
        values.put("sourcePresent", summary.sourcePresent());
        values.put("sourceDiagnosticsPresent", summary.sourceDiagnosticsPresent());
        values.put("sourceType", summary.sourceType());
        values.put("sourceValid", summary.sourceValid());
        values.put("sourceAllowed", summary.sourceAllowed());
        values.put("validationErrorCount", summary.validationErrorCount());
        values.put("policyErrorCount", summary.policyErrorCount());
        values.put("attemptCount", summary.attemptCount());
        values.put("message", summary.message());
        values.put("attributes", summary.attributes());
        return ProjectionMaps.freeze(values);
    }
}
