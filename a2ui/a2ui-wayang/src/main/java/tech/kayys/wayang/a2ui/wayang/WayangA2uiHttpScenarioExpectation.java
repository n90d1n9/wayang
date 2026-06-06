package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Optional expectations for validating a single A2UI HTTP scenario result.
 */
public record WayangA2uiHttpScenarioExpectation(
        String id,
        Boolean expectedPassed,
        Integer expectedExchangeCount,
        Long expectedIssueCount,
        Boolean allowTransportErrors,
        List<Integer> expectedStatusCodes,
        List<String> expectedOutcomes,
        List<String> expectedRouteOperations,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioExpectation {
        id = id == null || id.isBlank() ? "a2ui-http-scenario-expectation" : id.trim();
        expectedExchangeCount = expectedExchangeCount == null ? null : Math.max(0, expectedExchangeCount);
        expectedIssueCount = expectedIssueCount == null ? null : Math.max(0L, expectedIssueCount);
        allowTransportErrors = allowTransportErrors == null ? Boolean.FALSE : allowTransportErrors;
        expectedStatusCodes = expectedStatusCodes == null ? List.of() : List.copyOf(expectedStatusCodes);
        expectedOutcomes = WayangA2uiDecodeCollections.nonBlankTexts(expectedOutcomes);
        expectedRouteOperations = WayangA2uiDecodeCollections.nonBlankTexts(expectedRouteOperations);
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpScenarioExpectation pass() {
        return new WayangA2uiHttpScenarioExpectation(
                "a2ui-http-scenario-pass",
                true,
                null,
                0L,
                false,
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    public WayangA2uiHttpScenarioExpectation withExpectedExchangeCount(int value) {
        return new WayangA2uiHttpScenarioExpectation(
                id,
                expectedPassed,
                value,
                expectedIssueCount,
                allowTransportErrors,
                expectedStatusCodes,
                expectedOutcomes,
                expectedRouteOperations,
                attributes);
    }

    public WayangA2uiHttpScenarioExpectation withExpectedPassed(boolean value) {
        return new WayangA2uiHttpScenarioExpectation(
                id,
                value,
                expectedExchangeCount,
                expectedIssueCount,
                allowTransportErrors,
                expectedStatusCodes,
                expectedOutcomes,
                expectedRouteOperations,
                attributes);
    }

    public WayangA2uiHttpScenarioExpectation withExpectedIssueCount(long value) {
        return new WayangA2uiHttpScenarioExpectation(
                id,
                expectedPassed,
                expectedExchangeCount,
                value,
                allowTransportErrors,
                expectedStatusCodes,
                expectedOutcomes,
                expectedRouteOperations,
                attributes);
    }

    public WayangA2uiHttpScenarioExpectation allowingTransportErrors(boolean value) {
        return new WayangA2uiHttpScenarioExpectation(
                id,
                expectedPassed,
                expectedExchangeCount,
                expectedIssueCount,
                value,
                expectedStatusCodes,
                expectedOutcomes,
                expectedRouteOperations,
                attributes);
    }

    public WayangA2uiHttpScenarioExpectation withExpectedStatusCodes(List<Integer> values) {
        return new WayangA2uiHttpScenarioExpectation(
                id,
                expectedPassed,
                expectedExchangeCount,
                expectedIssueCount,
                allowTransportErrors,
                values,
                expectedOutcomes,
                expectedRouteOperations,
                attributes);
    }

    public WayangA2uiHttpScenarioExpectation withExpectedOutcomes(List<String> values) {
        return new WayangA2uiHttpScenarioExpectation(
                id,
                expectedPassed,
                expectedExchangeCount,
                expectedIssueCount,
                allowTransportErrors,
                expectedStatusCodes,
                values,
                expectedRouteOperations,
                attributes);
    }

    public WayangA2uiHttpScenarioExpectation withExpectedRouteOperations(List<String> values) {
        return new WayangA2uiHttpScenarioExpectation(
                id,
                expectedPassed,
                expectedExchangeCount,
                expectedIssueCount,
                allowTransportErrors,
                expectedStatusCodes,
                expectedOutcomes,
                values,
                attributes);
    }

    public WayangA2uiHttpExpectationResult validate(WayangA2uiHttpScenarioResult result) {
        return validate(result == null ? null : result.report());
    }

    public WayangA2uiHttpExpectationResult validate(WayangA2uiHttpScenarioReport report) {
        if (report == null) {
            return WayangA2uiHttpExpectationResult.of(
                    "a2ui-http-scenario",
                    id,
                    List.of(WayangA2uiHttpExpectationIssue.of(
                            "a2ui-http-scenario",
                            "report",
                            "present",
                            "null",
                            "Scenario report is required for expectation validation.")),
                    attributes);
        }
        List<WayangA2uiHttpExpectationIssue> issues = new ArrayList<>();
        String targetId = report.scenarioId();
        compare(issues, targetId, "passed", expectedPassed, report.passed());
        compare(issues, targetId, "exchangeCount", expectedExchangeCount, report.exchangeCount());
        compare(issues, targetId, "issueCount", expectedIssueCount, (long) report.issueCount());
        if (!allowTransportErrors && report.transportErrors()) {
            issues.add(WayangA2uiHttpExpectationIssue.of(
                    targetId,
                    "transportErrors",
                    false,
                    true,
                    "Scenario produced transport errors."));
        }
        compareList(issues, targetId, "statusCodes", expectedStatusCodes, report.statusCodes());
        compareList(issues, targetId, "outcomes", expectedOutcomes, report.outcomes());
        compareList(issues, targetId, "routeOperations", expectedRouteOperations, routeOperations(report));
        return WayangA2uiHttpExpectationResult.of(targetId, id, issues, attributes);
    }

    private static void compare(
            List<WayangA2uiHttpExpectationIssue> issues,
            String targetId,
            String field,
            Object expected,
            Object actual) {
        if (expected != null && !expected.equals(actual)) {
            issues.add(WayangA2uiHttpExpectationIssue.of(
                    targetId,
                    field,
                    expected,
                    actual,
                    "Expected " + field + " to equal " + expected + "."));
        }
    }

    private static void compareList(
            List<WayangA2uiHttpExpectationIssue> issues,
            String targetId,
            String field,
            List<?> expected,
            List<?> actual) {
        if (expected != null && !expected.isEmpty() && !expected.equals(actual)) {
            issues.add(WayangA2uiHttpExpectationIssue.of(
                    targetId,
                    field,
                    expected,
                    actual,
                    "Expected " + field + " to match exactly."));
        }
    }

    private static List<String> routeOperations(WayangA2uiHttpScenarioReport report) {
        return report.exchanges().stream()
                .map(WayangA2uiHttpScenarioExpectation::routeOperation)
                .toList();
    }

    private static String routeOperation(Map<String, Object> exchange) {
        Object response = exchange.get("response");
        if (response instanceof Map<?, ?> responseMap) {
            Object routeOperation = responseMap.get("routeOperation");
            return routeOperation == null ? "" : String.valueOf(routeOperation);
        }
        return "";
    }

}
