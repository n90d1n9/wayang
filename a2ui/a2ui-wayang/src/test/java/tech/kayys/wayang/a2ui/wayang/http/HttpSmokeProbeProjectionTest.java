package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarios;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeSummary;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractSmokeHttpResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractSmokeTransportResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.failedContractSmokeTransportResponse;

class HttpSmokeProbeProjectionTest {

    @Test
    void projectsOrderedSummaryEnvelopeAndRecordDelegates() {
        WayangA2uiHttpSmokeSummary summary = WayangA2uiHttpSmokeSummary.from(contractSmokeTransportResponse());

        Map<String, Object> values = HttpSmokeProbeProjection.summary(summary);

        assertThat(summary.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                WayangA2uiTransportFields.PASSED,
                WayangA2uiTransportFields.EXIT_CODE,
                WayangA2uiTransportFields.SUITE_ID,
                WayangA2uiTransportFields.SCENARIO_COUNT,
                WayangA2uiTransportFields.ISSUE_COUNT,
                WayangA2uiTransportFields.ROUTE_COUNT,
                "smokeResult",
                "successfulExit",
                "issues",
                WayangA2uiTransportFields.METADATA,
                WayangA2uiTransportFields.BODY);
        assertThat(values)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 0)
                .containsEntry(WayangA2uiTransportFields.SUITE_ID, WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry(WayangA2uiTransportFields.ROUTE_COUNT, 6)
                .containsEntry("successfulExit", true);
    }

    @Test
    void projectsOrderedProbeEnvelopeAndRecordDelegates() {
        WayangA2uiHttpSmokeProbeResult probe = WayangA2uiHttpSmokeProbeResult.from(contractSmokeHttpResponse());

        Map<String, Object> values = HttpSmokeProbeProjection.probe(probe);

        assertThat(probe.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "statusCode",
                "httpSuccessful",
                "routeOperation",
                "allow",
                WayangA2uiTransportFields.OUTCOME,
                "smokeRoute",
                WayangA2uiTransportFields.PASSED,
                WayangA2uiTransportFields.EXIT_CODE,
                "summary",
                "headers");
        assertThat(values)
                .containsEntry("statusCode", 200)
                .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_SMOKE)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 0);
    }

    @Test
    void projectsSmokeIssuesWithDefaultSourceAttribution() {
        Map<String, Object> suiteReport = Map.of(
                "issues",
                List.of(Map.of("field", "routeCount")));
        Map<String, Object> expectationResult = Map.of(
                "validationIssues",
                List.of(
                        Map.of("field", "scenarioCount"),
                        Map.of("source", "custom", "field", "exitCode")));

        List<Map<String, Object>> issues = HttpSmokeProbeProjection.issues(
                suiteReport,
                expectationResult);

        assertThat(issues).hasSize(3);
        assertThat(issues.getFirst())
                .containsEntry("source", "suite")
                .containsEntry("field", "routeCount");
        assertThat(issues.get(1))
                .containsEntry("source", "expectation")
                .containsEntry("field", "scenarioCount");
        assertThat(issues.getLast())
                .containsEntry("source", "custom")
                .containsEntry("field", "exitCode");
    }

    @Test
    void failedSummaryUsesProjectedIssues() {
        WayangA2uiHttpSmokeSummary summary = WayangA2uiHttpSmokeSummary.from(failedContractSmokeTransportResponse());

        assertThat(summary.successfulExit()).isFalse();
        assertThat(summary.issueCount()).isEqualTo(1);
        assertThat(summary.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "expectation")
                        .containsEntry("field", "scenarioCount"));
        assertThat(HttpSmokeProbeProjection.summary(summary))
                .containsEntry(WayangA2uiTransportFields.PASSED, false)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 1)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 1);
    }
}
