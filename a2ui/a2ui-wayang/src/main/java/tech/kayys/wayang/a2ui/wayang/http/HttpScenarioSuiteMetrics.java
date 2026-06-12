package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioResult;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * Shared aggregate metrics for A2UI HTTP scenario suite results.
 */
public final class HttpScenarioSuiteMetrics {

    public static int scenarioCount(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).size();
    }

    public static long passedScenarioCount(List<WayangA2uiHttpScenarioResult> results) {
        return reports(results).stream()
                .filter(WayangA2uiHttpScenarioReport::passed)
                .count();
    }

    public static long failedScenarioCount(List<WayangA2uiHttpScenarioResult> results) {
        return scenarioCount(results) - passedScenarioCount(results);
    }

    public static long exchangeCount(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream()
                .mapToLong(WayangA2uiHttpScenarioResult::exchangeCount)
                .sum();
    }

    public static long successfulCount(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream()
                .mapToLong(WayangA2uiHttpScenarioResult::successfulCount)
                .sum();
    }

    public static long clientErrorCount(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream()
                .mapToLong(WayangA2uiHttpScenarioResult::clientErrorCount)
                .sum();
    }

    public static long serverErrorCount(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream()
                .mapToLong(WayangA2uiHttpScenarioResult::serverErrorCount)
                .sum();
    }

    public static long handledCount(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream()
                .mapToLong(WayangA2uiHttpScenarioResult::handledCount)
                .sum();
    }

    public static long rejectedCount(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream()
                .mapToLong(WayangA2uiHttpScenarioResult::rejectedCount)
                .sum();
    }

    public static boolean hasTransportErrors(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream().anyMatch(WayangA2uiHttpScenarioResult::hasTransportErrors);
    }

    public static long issueCount(List<WayangA2uiHttpScenarioReport> reports) {
        return safeReports(reports).stream()
                .mapToLong(WayangA2uiHttpScenarioReport::issueCount)
                .sum();
    }

    public static List<String> scenarioIds(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream()
                .map(WayangA2uiHttpScenarioResult::scenarioId)
                .toList();
    }

    public static List<WayangA2uiHttpScenarioReport> reports(List<WayangA2uiHttpScenarioResult> results) {
        return results(results).stream()
                .map(WayangA2uiHttpScenarioResult::report)
                .toList();
    }

    public static List<Map<String, Object>> scenarioMaps(List<WayangA2uiHttpScenarioReport> reports) {
        return safeReports(reports).stream()
                .map(WayangA2uiHttpScenarioReport::toMap)
                .toList();
    }

    public static List<Map<String, Object>> issues(List<WayangA2uiHttpScenarioReport> reports) {
        return safeReports(reports).stream()
                .flatMap(report -> report.issues().stream())
                .toList();
    }

    private static List<WayangA2uiHttpScenarioResult> results(List<WayangA2uiHttpScenarioResult> results) {
        return RecordCollections.nonNullList(results);
    }

    private static List<WayangA2uiHttpScenarioReport> safeReports(List<WayangA2uiHttpScenarioReport> reports) {
        return RecordCollections.nonNullList(reports);
    }

    private HttpScenarioSuiteMetrics() {
    }
}
