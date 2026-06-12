package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiBridgeResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRequest;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRouteCatalog;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioExchange;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpScenarioSuiteMetricsTest {

    @Test
    void aggregatesScenarioSuiteMetricsAndFlattenedReports() {
        WayangA2uiHttpScenarioResult passed = scenarioResult(
                "passed",
                successfulExchange(1),
                successfulExchange(2));
        WayangA2uiHttpScenarioResult failed = scenarioResult(
                "failed",
                errorExchange(1, "missing"));
        List<WayangA2uiHttpScenarioResult> results = List.of(passed, failed);
        List<WayangA2uiHttpScenarioReport> reports = HttpScenarioSuiteMetrics.reports(results);

        assertThat(HttpScenarioSuiteMetrics.scenarioCount(results)).isEqualTo(2);
        assertThat(HttpScenarioSuiteMetrics.passedScenarioCount(results)).isEqualTo(1L);
        assertThat(HttpScenarioSuiteMetrics.failedScenarioCount(results)).isEqualTo(1L);
        assertThat(HttpScenarioSuiteMetrics.exchangeCount(results)).isEqualTo(3L);
        assertThat(HttpScenarioSuiteMetrics.successfulCount(results)).isEqualTo(2L);
        assertThat(HttpScenarioSuiteMetrics.clientErrorCount(results)).isEqualTo(1L);
        assertThat(HttpScenarioSuiteMetrics.serverErrorCount(results)).isZero();
        assertThat(HttpScenarioSuiteMetrics.handledCount(results)).isZero();
        assertThat(HttpScenarioSuiteMetrics.rejectedCount(results)).isEqualTo(1L);
        assertThat(HttpScenarioSuiteMetrics.hasTransportErrors(results)).isTrue();
        assertThat(HttpScenarioSuiteMetrics.scenarioIds(results)).containsExactly("passed", "failed");
        assertThat(HttpScenarioSuiteMetrics.issueCount(reports)).isEqualTo(1L);
        assertThat(HttpScenarioSuiteMetrics.scenarioMaps(reports))
                .extracting(scenario -> scenario.get("scenarioId"))
                .containsExactly("passed", "failed");
        assertThat(HttpScenarioSuiteMetrics.issues(reports))
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("scenarioId", "failed")
                        .containsEntry("path", "/missing"));
    }

    @Test
    void treatsNullInputsAsEmpty() {
        assertThat(HttpScenarioSuiteMetrics.scenarioCount(null)).isZero();
        assertThat(HttpScenarioSuiteMetrics.passedScenarioCount(null)).isZero();
        assertThat(HttpScenarioSuiteMetrics.reports(null)).isEmpty();
        assertThat(HttpScenarioSuiteMetrics.issueCount(null)).isZero();
        assertThat(HttpScenarioSuiteMetrics.scenarioMaps(null)).isEmpty();
        assertThat(HttpScenarioSuiteMetrics.issues(null)).isEmpty();
    }

    @Test
    void ignoresNullScenarioResultsAndReports() {
        WayangA2uiHttpScenarioResult passed = scenarioResult("passed", successfulExchange(1));
        WayangA2uiHttpScenarioResult failed = scenarioResult("failed", errorExchange(1, "missing"));
        List<WayangA2uiHttpScenarioResult> results = new ArrayList<>();
        results.add(passed);
        results.add(null);
        results.add(failed);
        List<WayangA2uiHttpScenarioReport> reports = new ArrayList<>();
        reports.add(passed.report());
        reports.add(null);
        reports.add(failed.report());

        assertThat(HttpScenarioSuiteMetrics.scenarioCount(results)).isEqualTo(2);
        assertThat(HttpScenarioSuiteMetrics.failedScenarioCount(results)).isEqualTo(1L);
        assertThat(HttpScenarioSuiteMetrics.reports(results))
                .extracting(WayangA2uiHttpScenarioReport::scenarioId)
                .containsExactly("passed", "failed");
        assertThat(HttpScenarioSuiteMetrics.issueCount(reports)).isEqualTo(1L);
        assertThat(HttpScenarioSuiteMetrics.scenarioMaps(reports))
                .extracting(scenario -> scenario.get("scenarioId"))
                .containsExactly("passed", "failed");
    }

    private static WayangA2uiHttpScenarioResult scenarioResult(
            String id,
            WayangA2uiHttpScenarioExchange... exchanges) {
        return new WayangA2uiHttpScenarioResult(id, List.of(exchanges), Map.of());
    }

    private static WayangA2uiHttpScenarioExchange successfulExchange(int index) {
        WayangA2uiHttpResponse response = WayangA2uiHttpResponse.fromBridge(
                        WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.from(
                                WayangA2uiHttpRouteCatalog.defaultCatalog())))
                .withRoute(WayangA2uiHttpRoute.routeCatalog());
        return new WayangA2uiHttpScenarioExchange(
                index,
                WayangA2uiHttpRequest.routeCatalog(),
                response);
    }

    private static WayangA2uiHttpScenarioExchange errorExchange(int index, String path) {
        WayangA2uiHttpResponse response = WayangA2uiHttpResponse.error(404, "not_found", "Missing");
        return new WayangA2uiHttpScenarioExchange(
                index,
                WayangA2uiHttpRequest.get("/" + path),
                response);
    }
}
