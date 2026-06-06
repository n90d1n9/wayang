package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Optional expectations for validating an A2UI HTTP scenario suite result.
 */
public record WayangA2uiHttpScenarioSuiteExpectation(
        String id,
        Boolean expectedPassed,
        Integer expectedScenarioCount,
        Long expectedIssueCount,
        Boolean allowTransportErrors,
        List<String> expectedScenarioIds,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioSuiteExpectation {
        id = id == null || id.isBlank() ? "a2ui-http-suite-expectation" : id.trim();
        expectedScenarioCount = expectedScenarioCount == null ? null : Math.max(0, expectedScenarioCount);
        expectedIssueCount = expectedIssueCount == null ? null : Math.max(0L, expectedIssueCount);
        allowTransportErrors = allowTransportErrors == null ? Boolean.FALSE : allowTransportErrors;
        expectedScenarioIds = expectedScenarioIds == null
                ? List.of()
                : expectedScenarioIds.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .toList();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpScenarioSuiteExpectation pass() {
        return new WayangA2uiHttpScenarioSuiteExpectation(
                "a2ui-http-suite-pass",
                true,
                null,
                0L,
                false,
                List.of(),
                Map.of());
    }

    public WayangA2uiHttpScenarioSuiteExpectation withExpectedScenarioCount(int value) {
        return new WayangA2uiHttpScenarioSuiteExpectation(
                id,
                expectedPassed,
                value,
                expectedIssueCount,
                allowTransportErrors,
                expectedScenarioIds,
                attributes);
    }

    public WayangA2uiHttpScenarioSuiteExpectation withExpectedPassed(boolean value) {
        return new WayangA2uiHttpScenarioSuiteExpectation(
                id,
                value,
                expectedScenarioCount,
                expectedIssueCount,
                allowTransportErrors,
                expectedScenarioIds,
                attributes);
    }

    public WayangA2uiHttpScenarioSuiteExpectation withExpectedIssueCount(long value) {
        return new WayangA2uiHttpScenarioSuiteExpectation(
                id,
                expectedPassed,
                expectedScenarioCount,
                value,
                allowTransportErrors,
                expectedScenarioIds,
                attributes);
    }

    public WayangA2uiHttpScenarioSuiteExpectation allowingTransportErrors(boolean value) {
        return new WayangA2uiHttpScenarioSuiteExpectation(
                id,
                expectedPassed,
                expectedScenarioCount,
                expectedIssueCount,
                value,
                expectedScenarioIds,
                attributes);
    }

    public WayangA2uiHttpScenarioSuiteExpectation withExpectedScenarioIds(List<String> values) {
        return new WayangA2uiHttpScenarioSuiteExpectation(
                id,
                expectedPassed,
                expectedScenarioCount,
                expectedIssueCount,
                allowTransportErrors,
                values,
                attributes);
    }

    public WayangA2uiHttpExpectationResult validate(WayangA2uiHttpScenarioSuiteResult result) {
        return validate(result == null ? null : result.report());
    }

    public WayangA2uiHttpExpectationResult validate(WayangA2uiHttpScenarioSuiteReport report) {
        if (report == null) {
            return WayangA2uiHttpExpectationResult.of(
                    "a2ui-http-suite",
                    id,
                    List.of(WayangA2uiHttpExpectationIssue.of(
                            "a2ui-http-suite",
                            "report",
                            "present",
                            "null",
                            "Suite report is required for expectation validation.")),
                    attributes);
        }
        List<WayangA2uiHttpExpectationIssue> issues = new ArrayList<>();
        String targetId = report.suiteId();
        compare(issues, targetId, "passed", expectedPassed, report.passed());
        compare(issues, targetId, "scenarioCount", expectedScenarioCount, report.scenarioCount());
        compare(issues, targetId, "issueCount", expectedIssueCount, report.issueCount());
        if (!allowTransportErrors && report.transportErrors()) {
            issues.add(WayangA2uiHttpExpectationIssue.of(
                    targetId,
                    "transportErrors",
                    false,
                    true,
                    "Suite produced transport errors."));
        }
        compareList(issues, targetId, "scenarioIds", expectedScenarioIds, report.scenarioIds());
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
}
