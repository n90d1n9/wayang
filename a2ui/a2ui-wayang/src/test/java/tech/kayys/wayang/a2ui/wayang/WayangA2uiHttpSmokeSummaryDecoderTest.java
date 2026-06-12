package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractSmokeTransportResponse;

class WayangA2uiHttpSmokeSummaryDecoderTest {

    @Test
    void decodesStoredSummaryMapsWithStringyFields() {
        WayangA2uiHttpSmokeSummary summary =
                WayangA2uiHttpSmokeSummaryDecoder.fromMap(storedSummaryValues());

        assertThat(summary.passed()).isTrue();
        assertThat(summary.successfulExit()).isTrue();
        assertThat(summary.exitCode()).isZero();
        assertThat(summary.suiteId()).isEqualTo(WayangA2uiHttpScenarios.SMOKE_SUITE_ID);
        assertThat(summary.scenarioCount()).isEqualTo(3);
        assertThat(summary.issueCount()).isEqualTo(1);
        assertThat(summary.routeCount()).isEqualTo(6);
        assertThat(summary.smokeResult()).isTrue();
        assertThat(summary.issues()).containsExactly(Map.of("field", "scenarioCount"));
        assertThat(summary.metadata()).containsEntry("source", "store");
        assertThat(summary.body()).containsEntry("kind", "stored");
    }

    @Test
    void decodesTransportAndRawResultBodies() {
        WayangA2uiHttpSmokeSummary transportSummary =
                WayangA2uiHttpSmokeSummaryDecoder.from(contractSmokeTransportResponse());
        WayangA2uiHttpSmokeSummary rawResultSummary =
                WayangA2uiHttpSmokeSummaryDecoder.fromResultJson("""
                        {
                          "passed": true,
                          "exitCode": 0,
                          "suiteReport": {
                            "suiteId": "a2ui-http-smoke-suite",
                            "scenarioCount": 3,
                            "issueCount": 0
                          },
                          "expectationResult": {"issueCount": 0, "validationIssues": []},
                          "attributes": {"routeCount": 6}
                        }
                        """);

        assertThat(transportSummary.smokeResult()).isTrue();
        assertThat(transportSummary.routeCount()).isEqualTo(6);
        assertThat(rawResultSummary.smokeResult()).isFalse();
        assertThat(rawResultSummary.successfulExit()).isTrue();
        assertThat(rawResultSummary.suiteId()).isEqualTo(WayangA2uiHttpScenarios.SMOKE_SUITE_ID);
        assertThat(rawResultSummary.scenarioCount()).isEqualTo(3);
        assertThat(rawResultSummary.routeCount()).isEqualTo(6);
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = storedSummaryValues();

        assertThat(WayangA2uiHttpSmokeSummary.fromMap(values))
                .isEqualTo(WayangA2uiHttpSmokeSummaryDecoder.fromMap(values));
        assertThat(WayangA2uiHttpSmokeSummary.from(contractSmokeTransportResponse()))
                .isEqualTo(WayangA2uiHttpSmokeSummaryDecoder.from(contractSmokeTransportResponse()));
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiHttpSmokeSummary decoded =
                WayangA2uiHttpSmokeSummaryDecoder.fromJson("""
                        {
                          "passed": "true",
                          "exitCode": "0",
                          "suiteId": "a2ui-http-smoke",
                          "scenarioCount": "3",
                          "routeCount": "6",
                          "smokeResult": "true"
                        }
                        """);

        assertThat(decoded.successfulExit()).isTrue();
        assertThat(decoded.smokeResult()).isTrue();
        assertThatThrownBy(() -> WayangA2uiHttpSmokeSummaryDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP smoke summary JSON must not be blank");
    }

    private static Map<String, Object> storedSummaryValues() {
        return Map.ofEntries(
                Map.entry(WayangA2uiTransportFields.PASSED, "true"),
                Map.entry(WayangA2uiTransportFields.EXIT_CODE, "0"),
                Map.entry(WayangA2uiTransportFields.SUITE_ID, WayangA2uiHttpScenarios.SMOKE_SUITE_ID),
                Map.entry(WayangA2uiTransportFields.SCENARIO_COUNT, "3"),
                Map.entry(WayangA2uiTransportFields.ISSUE_COUNT, "1"),
                Map.entry(WayangA2uiTransportFields.ROUTE_COUNT, "6"),
                Map.entry("smokeResult", "true"),
                Map.entry("issues", List.of(Map.of("field", "scenarioCount"))),
                Map.entry(WayangA2uiTransportFields.METADATA, Map.of("source", "store")),
                Map.entry(WayangA2uiTransportFields.BODY, Map.of("kind", "stored")));
    }
}
