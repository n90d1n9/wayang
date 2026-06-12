package tech.kayys.wayang.agenticcommerce.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact checkout HTTP smoke result summary for CLI, REST, and CI probes.
 */
public record AgenticCommerceCheckoutHttpSmokeSummary(
        boolean passed,
        boolean failed,
        int exitCode,
        boolean successfulExit,
        String scenarioId,
        String expectationId,
        boolean scenarioValid,
        boolean scenarioSuccessful,
        boolean expectationValid,
        int exchangeCount,
        int scenarioIssueCount,
        int expectationIssueCount,
        int issueCount,
        int routeCount,
        List<Integer> statusCodes,
        List<String> operations,
        List<String> stepIds,
        long transportErrorCount,
        List<AgenticCommerceCheckoutHttpIssueSummary> issues,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpSmokeSummary {
        scenarioId = AgenticCommerceValues.textValue(scenarioId);
        expectationId = AgenticCommerceValues.textValue(expectationId);
        exchangeCount = Math.max(0, exchangeCount);
        scenarioIssueCount = Math.max(0, scenarioIssueCount);
        expectationIssueCount = Math.max(0, expectationIssueCount);
        issueCount = Math.max(0, issueCount);
        routeCount = Math.max(0, routeCount);
        statusCodes = statusCodes == null
                ? List.of()
                : statusCodes.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(status -> Math.max(0, status))
                        .toList();
        operations = AgenticCommerceValues.strings(operations);
        stepIds = AgenticCommerceValues.strings(stepIds);
        transportErrorCount = Math.max(0, transportErrorCount);
        issues = issues == null
                ? List.of()
                : issues.stream()
                        .filter(java.util.Objects::nonNull)
                        .toList();
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCheckoutHttpSmokeSummary from(
            AgenticCommerceCheckoutHttpSmokeResult result) {
        java.util.Objects.requireNonNull(result, "result");
        List<AgenticCommerceCheckoutHttpIssueSummary> issues = new ArrayList<>();
        result.scenarioResult().issues().forEach(issue -> issues.add(
                AgenticCommerceCheckoutHttpIssueSummary.fromIssue("scenario", "", "", issue)));
        result.scenarioResult().exchanges().forEach(exchange -> exchange.issues().forEach(issue -> issues.add(
                AgenticCommerceCheckoutHttpIssueSummary.fromIssue(
                        "exchange",
                        exchange.step().id(),
                        exchange.operation(),
                        issue))));
        result.expectationResult().issues().forEach(issue -> issues.add(
                AgenticCommerceCheckoutHttpIssueSummary.fromIssue("expectation", "", "", issue)));
        return new AgenticCommerceCheckoutHttpSmokeSummary(
                result.passed(),
                result.failed(),
                result.exitCode(),
                result.successfulExit(),
                result.scenarioResult().scenario().id(),
                result.expectationResult().expectation().id(),
                result.scenarioResult().valid(),
                result.scenarioResult().successful(),
                result.expectationResult().valid(),
                result.scenarioResult().exchangeCount(),
                result.scenarioIssueCount(),
                result.expectationIssueCount(),
                result.issueCount(),
                routeCount(result.metadata()),
                result.scenarioResult().exchanges().stream()
                        .map(exchange -> exchange.result().response().statusCode())
                        .toList(),
                result.scenarioResult().exchanges().stream()
                        .map(AgenticCommerceCheckoutHttpExchange::operation)
                        .toList(),
                result.scenarioResult().exchanges().stream()
                        .map(exchange -> exchange.step().id())
                        .toList(),
                result.scenarioResult().exchanges().stream()
                        .filter(exchange -> !exchange.transportError().isBlank())
                        .count(),
                issues,
                result.metadata());
    }

    public static AgenticCommerceCheckoutHttpSmokeSummary fromJson(String json) {
        return fromMap(AgenticCommerceJson.readObject(json));
    }

    public static AgenticCommerceCheckoutHttpSmokeSummary fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceCheckoutHttpSmokeSummary(
                    false,
                    true,
                    1,
                    false,
                    "",
                    "",
                    false,
                    false,
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    List.of(),
                    0,
                    List.of(),
                    Map.of());
        }
        Map<String, Object> scenarioResult = AgenticCommerceValues.map(values, "scenarioResult", "scenario_result");
        Map<String, Object> expectationResult = AgenticCommerceValues.map(values, "expectationResult", "expectation_result");
        Map<String, Object> scenarioSummary = AgenticCommerceValues.map(expectationResult, "scenarioSummary", "scenario_summary");
        Map<String, Object> metadata = AgenticCommerceValues.map(values, "metadata");
        List<Map<String, Object>> exchangeMaps = AgenticCommerceValues.maps(scenarioResult, "exchanges");
        List<Integer> statusCodes = intList(scenarioSummary, "statusCodes", "status_codes");
        if (statusCodes.isEmpty()) {
            statusCodes = intList(values, "statusCodes", "status_codes");
        }
        if (statusCodes.isEmpty()) {
            statusCodes = exchangeMaps.stream()
                    .map(exchange -> intValue(exchange, "actualStatusCode", "actual_status_code"))
                    .toList();
        }
        List<String> operations = AgenticCommerceValues.stringList(scenarioSummary, "operations");
        if (operations.isEmpty()) {
            operations = AgenticCommerceValues.stringList(values, "operations");
        }
        if (operations.isEmpty()) {
            operations = exchangeMaps.stream()
                    .map(exchange -> AgenticCommerceValues.text(exchange, "operation"))
                    .filter(operation -> !operation.isBlank())
                    .toList();
        }
        List<String> stepIds = AgenticCommerceValues.stringList(scenarioSummary, "stepIds", "step_ids");
        if (stepIds.isEmpty()) {
            stepIds = AgenticCommerceValues.stringList(values, "stepIds", "step_ids");
        }
        if (stepIds.isEmpty()) {
            stepIds = exchangeMaps.stream()
                    .map(exchange -> AgenticCommerceValues.text(exchange, "stepId", "step_id"))
                    .filter(stepId -> !stepId.isBlank())
                    .toList();
        }
        List<AgenticCommerceCheckoutHttpIssueSummary> issues = issuesFromMap(
                scenarioResult,
                expectationResult,
                exchangeMaps,
                values);
        int scenarioIssueCount = intValue(values, "scenarioIssueCount", "scenario_issue_count");
        int expectationIssueCount = intValue(values, "expectationIssueCount", "expectation_issue_count");
        int issueCount = intValue(values, "issueCount", "issue_count");
        if (issueCount == 0 && !issues.isEmpty()) {
            issueCount = issues.size();
        }
        return new AgenticCommerceCheckoutHttpSmokeSummary(
                bool(values, "passed"),
                bool(values, "failed"),
                intValue(values, "exitCode", "exit_code"),
                bool(values, "successfulExit", "successful_exit"),
                AgenticCommerceValues.text(values, "scenarioId", "scenario_id"),
                AgenticCommerceValues.text(values, "expectationId", "expectation_id"),
                bool(values, "scenarioValid", "scenario_valid"),
                bool(values, "scenarioSuccessful", "scenario_successful"),
                bool(values, "expectationValid", "expectation_valid"),
                intValue(values, "exchangeCount", "exchange_count"),
                scenarioIssueCount,
                expectationIssueCount,
                issueCount,
                routeCount(metadata),
                statusCodes,
                operations,
                stepIds,
                transportErrorCount(values, scenarioSummary),
                issues,
                metadata);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed);
        values.put("failed", failed);
        values.put("exitCode", exitCode);
        values.put("successfulExit", successfulExit);
        AgenticCommerceValues.putText(values, "scenarioId", scenarioId);
        AgenticCommerceValues.putText(values, "expectationId", expectationId);
        values.put("scenarioValid", scenarioValid);
        values.put("scenarioSuccessful", scenarioSuccessful);
        values.put("expectationValid", expectationValid);
        values.put("exchangeCount", exchangeCount);
        values.put("scenarioIssueCount", scenarioIssueCount);
        values.put("expectationIssueCount", expectationIssueCount);
        values.put("issueCount", issueCount);
        values.put("routeCount", routeCount);
        AgenticCommerceValues.putList(values, "statusCodes", statusCodes);
        AgenticCommerceValues.putStringList(values, "operations", operations);
        AgenticCommerceValues.putStringList(values, "stepIds", stepIds);
        values.put("transportErrorCount", transportErrorCount);
        AgenticCommerceValues.putList(values, "issues", issues.stream()
                .map(AgenticCommerceCheckoutHttpIssueSummary::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }

    private static List<AgenticCommerceCheckoutHttpIssueSummary> issuesFromMap(
            Map<String, Object> scenarioResult,
            Map<String, Object> expectationResult,
            List<Map<String, Object>> exchangeMaps,
            Map<?, ?> values) {
        List<AgenticCommerceCheckoutHttpIssueSummary> issues = new ArrayList<>();
        AgenticCommerceValues.maps(scenarioResult, "issues").stream()
                .map(issue -> issueSummary("scenario", "", "", issue))
                .forEach(issues::add);
        exchangeMaps.forEach(exchange -> {
            String stepId = AgenticCommerceValues.text(exchange, "stepId", "step_id");
            String operation = AgenticCommerceValues.text(exchange, "operation");
            AgenticCommerceValues.maps(exchange, "issues").stream()
                    .map(issue -> issueSummary("exchange", stepId, operation, issue))
                    .forEach(issues::add);
        });
        AgenticCommerceValues.maps(expectationResult, "issues").stream()
                .map(issue -> issueSummary("expectation", "", "", issue))
                .forEach(issues::add);
        if (issues.isEmpty()) {
            AgenticCommerceValues.maps(values, "issues").stream()
                    .map(AgenticCommerceCheckoutHttpIssueSummary::fromMap)
                    .forEach(issues::add);
        }
        return List.copyOf(issues);
    }

    private static AgenticCommerceCheckoutHttpIssueSummary issueSummary(
            String source,
            String stepId,
            String operation,
            Map<String, Object> issue) {
        Map<String, Object> values = new LinkedHashMap<>(issue);
        values.put("source", source);
        if (!stepId.isBlank()) {
            values.put("stepId", stepId);
        }
        if (!operation.isBlank()) {
            values.put("operation", operation);
        }
        return AgenticCommerceCheckoutHttpIssueSummary.fromMap(values);
    }

    private static List<Integer> intList(Map<?, ?> values, String... keys) {
        Object raw = AgenticCommerceValues.first(values, keys);
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(AgenticCommerceCheckoutHttpSmokeSummary::toInt)
                    .toList();
        }
        return List.of();
    }

    private static int routeCount(Map<?, ?> metadata) {
        int routeCount = intValue(metadata, "routeCount", "route_count");
        return routeCount > 0 ? routeCount : AgenticCommerceRouteCatalog.checkoutCatalog().routeCount();
    }

    private static long transportErrorCount(Map<?, ?> values, Map<?, ?> scenarioSummary) {
        long direct = longValue(values, "transportErrorCount", "transport_error_count");
        return direct > 0 ? direct : longValue(scenarioSummary, "transportErrorCount", "transport_error_count");
    }

    private static int intValue(Map<?, ?> values, String... keys) {
        return toInt(AgenticCommerceValues.first(values, keys));
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = AgenticCommerceValues.textValue(value);
        if (text.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static long longValue(Map<?, ?> values, String... keys) {
        Object raw = AgenticCommerceValues.first(values, keys);
        if (raw instanceof Number number) {
            return number.longValue();
        }
        String text = AgenticCommerceValues.textValue(raw);
        if (text.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean bool(Map<?, ?> values, String... keys) {
        Object raw = AgenticCommerceValues.first(values, keys);
        if (raw instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(AgenticCommerceValues.textValue(raw));
    }
}
