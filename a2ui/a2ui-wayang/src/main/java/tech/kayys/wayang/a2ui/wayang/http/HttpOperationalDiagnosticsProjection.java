package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpOperationalDiagnostics;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpOperationalDiagnosticsSummary;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP operational diagnostics.
 */
public final class HttpOperationalDiagnosticsProjection {

    public static Map<String, Object> diagnostics(WayangA2uiHttpOperationalDiagnostics diagnostics) {
        WayangA2uiHttpOperationalDiagnostics resolved = Objects.requireNonNull(diagnostics, "diagnostics");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", WayangA2uiHttpOperationalDiagnostics.DIAGNOSTICS_ID);
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("issues", resolved.issues());
        values.put("summary", resolved.summary().toMap());
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("actionBindingPassed", resolved.actionBindingPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        values.put("bindingReportProbe", resolved.bindingReportProbe().toMap());
        values.put("actionBindingProbe", resolved.actionBindingProbe().toMap());
        values.put("smokeProbe", resolved.smokeProbe().toMap());
        values.put("readinessProbe", resolved.readinessProbe().toMap());
        values.put("standardReadiness", resolved.standardReadiness().toMap());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> summary(WayangA2uiHttpOperationalDiagnosticsSummary summary) {
        WayangA2uiHttpOperationalDiagnosticsSummary resolved = Objects.requireNonNull(summary, "summary");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", resolved.diagnosticsId());
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put("successfulExit", resolved.successfulExit());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("issueCodes", resolved.issueCodes());
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("actionBindingPassed", resolved.actionBindingPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        values.put("routeOperationCount", resolved.routeOperationCount());
        values.put("routeHandlerOperationCount", resolved.routeHandlerOperationCount());
        values.put("missingRouteHandlerCount", resolved.missingRouteHandlerCount());
        values.put("orphanRouteHandlerCount", resolved.orphanRouteHandlerCount());
        values.put("policyActionCount", resolved.policyActionCount());
        values.put("actionHandlerCount", resolved.actionHandlerCount());
        values.put("missingActionHandlerCount", resolved.missingActionHandlerCount());
        values.put("orphanActionHandlerCount", resolved.orphanActionHandlerCount());
        values.put("smokeScenarioCount", resolved.smokeScenarioCount());
        values.put("smokeIssueCount", resolved.smokeIssueCount());
        values.put("smokeRouteCount", resolved.smokeRouteCount());
        values.put("attributes", resolved.attributes());
        return TransportMaps.freeze(values);
    }

    private HttpOperationalDiagnosticsProjection() {
    }
}
