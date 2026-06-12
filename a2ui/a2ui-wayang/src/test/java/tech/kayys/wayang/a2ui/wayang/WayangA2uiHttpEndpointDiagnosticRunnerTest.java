package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointDiagnosticRunnerTest {

    @Test
    @SuppressWarnings("unchecked")
    void runsDefaultDiagnosticsAndExposesReportAndSummaryJson() {
        WayangA2uiHttpEndpointDiagnosticConfig config =
                WayangA2uiHttpEndpointDiagnosticConfig.discoveryOnly()
                        .withDefaultAttributes(Map.of("tenant", "demo"));
        WayangA2uiHttpEndpointDiagnosticRunner runner =
                WayangA2uiHttpEndpointDiagnosticRunner.of(errorEndpoint(), config);

        WayangA2uiHttpEndpointDiagnosticRun run = runner.runDefault();
        Map<String, Object> runMap = run.toMap();
        String runJson = run.toJson();

        assertThat(runner.config()).isEqualTo(config);
        assertThat(run.passed()).isTrue();
        assertThat(run.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_SUCCESS);
        assertThat(run.summary().exchangeCount()).isEqualTo(2);
        assertThat(run.summary().issueCount()).isZero();
        assertThat(run.summaryJson())
                .contains("\"successfulExit\":true")
                .contains("\"tenant\":\"demo\"");
        assertThat(run.reportJson())
                .contains("\"diagnosticsId\":\"" + WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID + "\"")
                .contains("\"passed\":true");
        assertThat(runJson).startsWith("{\"summary\":");
        assertThat(runJson.indexOf("\"report\"")).isGreaterThan(runJson.indexOf("\"summary\""));
        assertThat((Map<String, Object>) runMap.get("summary"))
                .containsEntry("passed", true)
                .containsEntry("exchangeCount", 2);
        assertThat((Map<String, Object>) runMap.get("report"))
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
    }

    @Test
    void runsPlanJsonForAdapterEntrypoints() {
        WayangA2uiHttpEndpointDiagnosticPlan plan =
                WayangA2uiHttpEndpointDiagnosticPlan.fromMap(Map.of(
                        "diagnosticsId",
                        "runner-plan",
                        "config",
                        Map.of(
                                "profile",
                                "discovery",
                                "attributes",
                                Map.of("tenant", "demo")),
                        "requests",
                        List.of(
                                Map.of(
                                        "method",
                                        "GET",
                                        "path",
                                        "/api/a2ui/route-catalog?tenant=demo"),
                                Map.of(
                                        "method",
                                        "GET",
                                        "path",
                                        "/api/a2ui/exchange")),
                        "attributes",
                        Map.of("source", "runner")));
        WayangA2uiHttpEndpointDiagnosticRunner runner =
                WayangA2uiHttpEndpointDiagnosticRunner.of(errorEndpoint());

        WayangA2uiHttpEndpointDiagnosticRun run = runner.runPlanJson(plan.toJson());

        assertThat(run.diagnosticsId()).isEqualTo("runner-plan");
        assertThat(run.passed()).isFalse();
        assertThat(run.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(run.summary().issueCategories())
                .containsExactly(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH);
        assertThat(run.summary().errorCodes()).containsExactly("method_not_allowed");
        assertThat(run.report().issueCount()).isEqualTo(1);
        assertThat(run.toJson())
                .contains("\"summary\"")
                .contains("\"report\"")
                .contains("\"runner-plan\"");
    }

    @Test
    void runsRequestMapsForDynamicHarnesses() {
        WayangA2uiHttpEndpointDiagnosticRunner runner =
                WayangA2uiHttpEndpointDiagnosticRunner.of(errorEndpoint());

        WayangA2uiHttpEndpointDiagnosticRun run = runner.runRequestMaps(
                "mapped-runner",
                List.of(
                        Map.of(
                                "method",
                                "GET",
                                "path",
                                "/api/a2ui/route-catalog",
                                "attributes",
                                Map.of("traceId", "trace-1")),
                        Map.of(
                                "method",
                                "GET",
                                "path",
                                "/api/a2ui/missing")),
                Map.of("source", "maps"));

        assertThat(run.diagnosticsId()).isEqualTo("mapped-runner");
        assertThat(run.passed()).isFalse();
        assertThat(run.summary().exchangeCount()).isEqualTo(2);
        assertThat(run.summary().issueCategories())
                .containsExactly(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH);
        assertThat(run.summary().attributes()).containsEntry("source", "maps");
    }

    private static WayangA2uiHttpEndpointBinding errorEndpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
