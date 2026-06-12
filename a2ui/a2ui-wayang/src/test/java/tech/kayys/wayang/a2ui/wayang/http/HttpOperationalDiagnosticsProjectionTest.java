package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpOperationalDiagnostics;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpOperationalDiagnosticsSummary;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpReadinessProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractReadinessProbeResult;

class HttpOperationalDiagnosticsProjectionTest {

    @Test
    void projectsOrderedDiagnosticsEnvelopeAndRecordDelegates() {
        WayangA2uiHttpOperationalDiagnostics diagnostics =
                new WayangA2uiHttpOperationalDiagnostics(contractReadinessProbeResult());

        Map<String, Object> values = HttpOperationalDiagnosticsProjection.diagnostics(diagnostics);

        assertThat(diagnostics.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "diagnosticsId",
                WayangA2uiTransportFields.PASSED,
                WayangA2uiTransportFields.EXIT_CODE,
                WayangA2uiTransportFields.ISSUE_COUNT,
                "issues",
                "summary",
                "bindingReportPassed",
                "actionBindingPassed",
                "smokeRequired",
                "smokePassed",
                "bindingReportProbe",
                "actionBindingProbe",
                "smokeProbe",
                "readinessProbe",
                "standardReadiness");
        assertThat(values)
                .containsEntry("diagnosticsId", WayangA2uiHttpOperationalDiagnostics.DIAGNOSTICS_ID)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 0)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 0);
    }

    @Test
    void projectsOrderedSummaryEnvelopeAndRecordDelegates() {
        WayangA2uiHttpOperationalDiagnosticsSummary summary =
                new WayangA2uiHttpOperationalDiagnostics(contractReadinessProbeResult()).summary();

        Map<String, Object> values = HttpOperationalDiagnosticsProjection.summary(summary);

        assertThat(summary.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "diagnosticsId",
                WayangA2uiTransportFields.PASSED,
                WayangA2uiTransportFields.EXIT_CODE,
                "successfulExit",
                WayangA2uiTransportFields.ISSUE_COUNT,
                "issueCodes",
                "bindingReportPassed",
                "actionBindingPassed",
                "smokeRequired",
                "smokePassed",
                "routeOperationCount",
                "routeHandlerOperationCount",
                "missingRouteHandlerCount",
                "orphanRouteHandlerCount",
                "policyActionCount",
                "actionHandlerCount",
                "missingActionHandlerCount",
                "orphanActionHandlerCount",
                "smokeScenarioCount",
                "smokeIssueCount",
                "smokeRouteCount",
                "attributes");
        assertThat(values)
                .containsEntry("diagnosticsId", WayangA2uiHttpOperationalDiagnostics.DIAGNOSTICS_ID)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry("successfulExit", true)
                .containsEntry("routeOperationCount", 6);
        assertThat((Map<String, Object>) values.get("attributes"))
                .containsEntry("readinessId", WayangA2uiHttpReadinessProbeResult.READINESS_ID);
    }
}
