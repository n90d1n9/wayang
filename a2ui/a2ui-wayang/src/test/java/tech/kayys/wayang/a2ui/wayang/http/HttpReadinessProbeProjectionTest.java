package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpBindingReportProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpReadinessProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractReadinessProbeResult;

class HttpReadinessProbeProjectionTest {

    @Test
    void projectsOrderedReadinessEnvelopeAndRecordDelegates() {
        WayangA2uiHttpReadinessProbeResult readiness = contractReadinessProbeResult();

        Map<String, Object> values = HttpReadinessProbeProjection.readiness(readiness);

        assertThat(readiness.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                WayangA2uiTransportFields.PASSED,
                WayangA2uiTransportFields.EXIT_CODE,
                "bindingReportPassed",
                "actionBindingPassed",
                "smokeRequired",
                "smokePassed",
                WayangA2uiTransportFields.ISSUE_COUNT,
                "issues",
                "bindingReportProbe",
                "actionBindingProbe",
                "smokeProbe");
        assertThat(values)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 0)
                .containsEntry("bindingReportPassed", true)
                .containsEntry("actionBindingPassed", true)
                .containsEntry("smokeRequired", true)
                .containsEntry("smokePassed", true)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 0);
    }

    @Test
    void keepsNativeIssuesSeparateFromSharedReadinessIssues() {
        WayangA2uiHttpBindingReportProbeResult bindingProbe =
                WayangA2uiHttpBindingReportProbeResult.fromMap(Map.of(
                        "statusCode",
                        500,
                        "routeOperation",
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT,
                        WayangA2uiTransportFields.ISSUE_COUNT,
                        1,
                        "issues",
                        List.of(Map.of("message", "binding failed"))));
        WayangA2uiHttpReadinessProbeResult readiness =
                new WayangA2uiHttpReadinessProbeResult(bindingProbe, null, false);

        Map<String, Object> nativeIssue = HttpReadinessProbeProjection.issues(readiness).getFirst();
        Map<String, Object> standardIssue = TransportMaps
                .copyMapList(HttpReadinessProbeProjection.standardReadiness(readiness)
                        .toMap()
                        .get("issues"))
                .getFirst();

        assertThat(readiness.issues()).containsExactly(nativeIssue);
        assertThat(nativeIssue)
                .containsEntry("code", "binding_report_probe_failed")
                .containsEntry("statusCode", 500)
                .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_BINDING_REPORT)
                .doesNotContainKey("source");
        assertThat(standardIssue)
                .containsEntry("code", "binding_report_probe_failed")
                .containsEntry("source", "http")
                .containsEntry("statusCode", 500)
                .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_BINDING_REPORT);
    }

    @Test
    void projectsSharedReadinessReportAndRecordDelegates() {
        WayangA2uiHttpReadinessProbeResult readiness = contractReadinessProbeResult();

        Map<String, Object> values = HttpReadinessProbeProjection.standardReadiness(readiness).toMap();

        assertThat(readiness.standardReadiness().toMap()).isEqualTo(values);
        assertThat(values)
                .containsEntry("readinessId", WayangA2uiHttpReadinessProbeResult.READINESS_ID)
                .containsEntry("ready", true)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 0)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 0)
                .containsEntry("issues", List.of());
        assertThat(TransportMaps.copyMapList(values.get("probes")))
                .hasSize(3)
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "bindingReport")
                        .containsEntry("required", true)
                        .containsEntry("passed", true))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "actionBinding")
                        .containsEntry("required", true)
                        .containsEntry("passed", true))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "smoke")
                        .containsEntry("required", true)
                        .containsEntry("passed", true));
    }
}
