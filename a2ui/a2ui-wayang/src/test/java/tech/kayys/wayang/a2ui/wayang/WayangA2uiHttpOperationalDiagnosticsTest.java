package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractReadinessProbeResult;

class WayangA2uiHttpOperationalDiagnosticsTest {

    @Test
    void aggregatesReadinessAndProbeAccessorsThroughAdapter() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpOperationalDiagnostics diagnostics = adapter.operationalDiagnostics();
        WayangA2uiHttpOperationalDiagnosticsSummary summary = diagnostics.summary();
        Map<String, Object> values = diagnostics.toMap();

        assertThat(diagnostics.passed()).isTrue();
        assertThat(diagnostics.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_SUCCESS);
        assertThat(diagnostics.issueCount()).isZero();
        assertThat(diagnostics.issues()).isEmpty();
        assertThat(diagnostics.bindingReportPassed()).isTrue();
        assertThat(diagnostics.actionBindingPassed()).isTrue();
        assertThat(diagnostics.smokeRequired()).isTrue();
        assertThat(diagnostics.smokePassed()).isTrue();
        assertThat(diagnostics.bindingReportProbe().routeOperationCount()).isEqualTo(6);
        assertThat(diagnostics.actionBindingProbe().missingHandlerCount()).isZero();
        assertThat(diagnostics.smokeProbe().summary().routeCount()).isEqualTo(6);
        assertThat(diagnostics.standardReadiness().toMap())
                .containsEntry("readinessId", WayangA2uiHttpReadinessProbeResult.READINESS_ID)
                .containsEntry("ready", true);
        assertThat(summary.toMap())
                .containsEntry("diagnosticsId", WayangA2uiHttpOperationalDiagnostics.DIAGNOSTICS_ID)
                .containsEntry("successfulExit", true)
                .containsEntry("routeOperationCount", 6)
                .containsEntry("routeHandlerOperationCount", 6)
                .containsEntry("policyActionCount", 1)
                .containsEntry("actionHandlerCount", 5)
                .containsEntry("smokeScenarioCount", 3)
                .containsEntry("smokeRouteCount", 6);
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
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void decodesOperationalDiagnosticsSummariesFromMapsAndJson() {
        WayangA2uiHttpOperationalDiagnosticsSummary summary =
                WayangA2uiHttpOperationalDiagnosticsSummary.fromMap(Map.ofEntries(
                        Map.entry("diagnosticsId", "ops-summary"),
                        Map.entry(WayangA2uiTransportFields.PASSED, "false"),
                        Map.entry(WayangA2uiTransportFields.EXIT_CODE, "1"),
                        Map.entry(WayangA2uiTransportFields.ISSUE_COUNT, "1"),
                        Map.entry("issueCodes", List.of("action_binding_probe_failed", " ")),
                        Map.entry("bindingReportPassed", "true"),
                        Map.entry("actionBindingPassed", "false"),
                        Map.entry("smokeRequired", "true"),
                        Map.entry("smokePassed", "true"),
                        Map.entry("routeOperationCount", "6"),
                        Map.entry("routeHandlerOperationCount", "6"),
                        Map.entry("missingRouteHandlerCount", "0"),
                        Map.entry("orphanRouteHandlerCount", "0"),
                        Map.entry("policyActionCount", "1"),
                        Map.entry("actionHandlerCount", "0"),
                        Map.entry("missingActionHandlerCount", "1"),
                        Map.entry("orphanActionHandlerCount", "0"),
                        Map.entry("smokeScenarioCount", "1"),
                        Map.entry("smokeIssueCount", "0"),
                        Map.entry("smokeRouteCount", "6"),
                        Map.entry("attributes", Map.of("readinessId", "a2ui.http.readiness"))));

        assertThat(summary.successfulExit()).isFalse();
        assertThat(summary.issueCodes()).containsExactly("action_binding_probe_failed");
        assertThat(summary.attributes()).containsEntry("readinessId", "a2ui.http.readiness");
        assertThat(WayangA2uiHttpOperationalDiagnosticsSummary.fromJson(summary.toJson()))
                .isEqualTo(summary);
        assertThatThrownBy(() -> WayangA2uiHttpOperationalDiagnosticsSummary.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP operational diagnostics summary JSON must not be blank");
    }

    @Test
    void decodesStoredDiagnosticsFromNestedOrFlatReadinessMapsAndJson() {
        WayangA2uiHttpReadinessProbeResult readiness = contractReadinessProbeResult();
        WayangA2uiHttpOperationalDiagnostics diagnostics =
                new WayangA2uiHttpOperationalDiagnostics(readiness);

        WayangA2uiHttpOperationalDiagnostics nestedDecoded =
                WayangA2uiHttpOperationalDiagnostics.fromMap(Map.of("readinessProbe", readiness.toMap()));
        Map<String, Object> flatReadinessMap = new LinkedHashMap<>(readiness.toMap());
        flatReadinessMap.put("diagnosticsId", WayangA2uiHttpOperationalDiagnostics.DIAGNOSTICS_ID);

        assertThat(nestedDecoded).isEqualTo(diagnostics);
        assertThat(WayangA2uiHttpOperationalDiagnostics.fromMap(flatReadinessMap)).isEqualTo(diagnostics);
        assertThat(WayangA2uiHttpOperationalDiagnostics.fromJson(diagnostics.toJson())).isEqualTo(diagnostics);
        assertThatThrownBy(() -> WayangA2uiHttpOperationalDiagnostics.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP operational diagnostics JSON must not be blank");
    }

    @Test
    void decodesEmptyStoredDiagnosticsThroughExplicitReadinessFallback() {
        WayangA2uiHttpOperationalDiagnostics diagnostics = WayangA2uiHttpOperationalDiagnostics.fromMap(Map.of());
        WayangA2uiHttpOperationalDiagnosticsSummary summary =
                WayangA2uiHttpOperationalDiagnosticsSummary.empty();

        assertThat(diagnostics).isEqualTo(WayangA2uiHttpOperationalDiagnostics.empty());
        assertThat(diagnostics.summary()).isEqualTo(summary);
        assertThat(WayangA2uiHttpOperationalDiagnosticsSummary.from(null)).isEqualTo(summary);
        assertThat(WayangA2uiHttpOperationalDiagnosticsSummary.fromMap(Map.of())).isEqualTo(summary);
        assertThat(diagnostics.passed()).isFalse();
        assertThat(summary.issueCodes()).containsExactly("binding_report_probe_failed");
        assertThat(summary.actionBindingPassed()).isTrue();
        assertThat(summary.smokePassed()).isTrue();
        assertThat(summary.attributes())
                .containsEntry("readinessId", WayangA2uiHttpReadinessProbeResult.READINESS_ID);
    }

    @Test
    void surfacesActionBindingFailuresThroughAggregateDiagnostics() {
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(Set.of("custom.allowed"), Set.of(), Map.of());
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(
                policy,
                WayangA2uiSurfaceRegistry.readOnly(),
                WayangA2uiActionHandlers.builder().build());
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(
                new WayangA2uiTransportAdapter(new WayangA2uiSession(router)));

        WayangA2uiHttpOperationalDiagnostics diagnostics = adapter.operationalDiagnostics();

        assertThat(diagnostics.passed()).isFalse();
        assertThat(diagnostics.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(diagnostics.bindingReportPassed()).isTrue();
        assertThat(diagnostics.actionBindingPassed()).isFalse();
        assertThat(diagnostics.actionBindingProbe().missingHandlerActions()).containsExactly("custom.allowed");
        assertThat(diagnostics.summary().issueCodes()).containsExactly("action_binding_probe_failed");
        assertThat(diagnostics.summary().missingActionHandlerCount()).isEqualTo(1);
        assertThat(diagnostics.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "action_binding_probe_failed")
                        .containsEntry("statusCode", 200)
                        .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_EXCHANGE));
        assertThat(TransportMaps.copyMapList(
                diagnostics.standardReadiness().toMap().get("issues")))
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "http")
                        .containsEntry("code", "action_binding_probe_failed"));
        assertThat(TransportMaps.copyMapList(
                TransportMaps.copyMap(diagnostics.toMap().get("readinessProbe")).get("issues")))
                .hasSize(1);
    }
}
