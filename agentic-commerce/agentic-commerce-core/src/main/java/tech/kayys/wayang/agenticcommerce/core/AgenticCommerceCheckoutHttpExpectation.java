package tech.kayys.wayang.agenticcommerce.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reusable expectation for checkout HTTP harness scenario results.
 */
public record AgenticCommerceCheckoutHttpExpectation(
        String id,
        boolean expectedValid,
        boolean expectedSuccessful,
        int expectedExchangeCount,
        int expectedIssueCount,
        boolean allowTransportErrors,
        List<Integer> expectedStatusCodes,
        List<String> expectedOperations,
        List<String> expectedStepIds,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpExpectation {
        id = AgenticCommerceValues.textValue(id);
        if (id.isBlank()) {
            throw new IllegalArgumentException("Agentic Commerce checkout expectation id must not be blank");
        }
        expectedExchangeCount = expectedExchangeCount < 0 ? -1 : expectedExchangeCount;
        expectedIssueCount = expectedIssueCount < 0 ? -1 : expectedIssueCount;
        expectedStatusCodes = expectedStatusCodes == null
                ? List.of()
                : expectedStatusCodes.stream()
                        .filter(Objects::nonNull)
                        .map(status -> Math.max(100, status))
                        .toList();
        expectedOperations = AgenticCommerceValues.strings(expectedOperations);
        expectedStepIds = AgenticCommerceValues.strings(expectedStepIds);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCheckoutHttpExpectation smoke() {
        return new AgenticCommerceCheckoutHttpExpectation(
                "agentic-commerce-checkout-smoke-expectation",
                true,
                true,
                5,
                0,
                false,
                List.of(201, 200, 200, 200, 200),
                List.of(
                        AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                        AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                        AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                        AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                        AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION),
                List.of("create", "retrieve", "update", "complete", "cancel"),
                Map.of("fixture", true));
    }

    public AgenticCommerceCheckoutHttpExpectationResult validate(
            AgenticCommerceCheckoutHttpScenarioResult result) {
        AgenticCommerceCheckoutHttpScenarioResult resolved = Objects.requireNonNull(result, "result");
        List<AgenticCommerceValidationIssue> issues = new ArrayList<>();
        expectBoolean(issues, "scenario_valid", "valid", expectedValid, resolved.valid());
        expectBoolean(issues, "scenario_successful", "successful", expectedSuccessful, resolved.successful());
        if (expectedExchangeCount >= 0 && resolved.exchangeCount() != expectedExchangeCount) {
            issues.add(issue(
                    "unexpected_exchange_count",
                    "exchangeCount",
                    "Checkout HTTP scenario exchange count did not match the expectation.",
                    String.valueOf(expectedExchangeCount),
                    String.valueOf(resolved.exchangeCount())));
        }
        if (expectedIssueCount >= 0 && resolved.issueCount() != expectedIssueCount) {
            issues.add(issue(
                    "unexpected_issue_count",
                    "issueCount",
                    "Checkout HTTP scenario issue count did not match the expectation.",
                    String.valueOf(expectedIssueCount),
                    String.valueOf(resolved.issueCount())));
        }
        if (!allowTransportErrors) {
            List<String> transportErrorStepIds = resolved.exchanges().stream()
                    .filter(exchange -> !exchange.transportError().isBlank())
                    .map(exchange -> exchange.step().id())
                    .toList();
            if (!transportErrorStepIds.isEmpty()) {
                issues.add(issue(
                        "transport_errors_not_allowed",
                        "transportError",
                        "Checkout HTTP scenario included transport errors.",
                        "no transport errors",
                        String.valueOf(transportErrorStepIds)));
            }
        }
        expectSequence(
                issues,
                "unexpected_status_codes",
                "statusCodes",
                "Checkout HTTP status-code sequence did not match the expectation.",
                expectedStatusCodes,
                resolved.exchanges().stream()
                        .map(exchange -> exchange.result().response().statusCode())
                        .toList());
        expectSequence(
                issues,
                "unexpected_operations",
                "operations",
                "Checkout HTTP operation sequence did not match the expectation.",
                expectedOperations,
                resolved.exchanges().stream()
                        .map(AgenticCommerceCheckoutHttpExchange::operation)
                        .toList());
        expectSequence(
                issues,
                "unexpected_step_ids",
                "stepIds",
                "Checkout HTTP step-id sequence did not match the expectation.",
                expectedStepIds,
                resolved.exchanges().stream()
                        .map(exchange -> exchange.step().id())
                        .toList());
        return new AgenticCommerceCheckoutHttpExpectationResult(this, resolved, issues, metadata());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("expectedValid", expectedValid);
        values.put("expectedSuccessful", expectedSuccessful);
        values.put("expectedExchangeCount", expectedExchangeCount);
        values.put("expectedIssueCount", expectedIssueCount);
        values.put("allowTransportErrors", allowTransportErrors);
        AgenticCommerceValues.putList(values, "expectedStatusCodes", expectedStatusCodes);
        AgenticCommerceValues.putStringList(values, "expectedOperations", expectedOperations);
        AgenticCommerceValues.putStringList(values, "expectedStepIds", expectedStepIds);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }

    private static void expectBoolean(
            List<AgenticCommerceValidationIssue> issues,
            String codeSuffix,
            String field,
            boolean expected,
            boolean actual) {
        if (expected != actual) {
            issues.add(issue(
                    "unexpected_" + codeSuffix,
                    field,
                    "Checkout HTTP scenario boolean state did not match the expectation.",
                    String.valueOf(expected),
                    String.valueOf(actual)));
        }
    }

    private static void expectSequence(
            List<AgenticCommerceValidationIssue> issues,
            String code,
            String field,
            String message,
            List<?> expected,
            List<?> actual) {
        if (!expected.isEmpty() && !expected.equals(actual)) {
            issues.add(issue(code, field, message, String.valueOf(expected), String.valueOf(actual)));
        }
    }

    private static AgenticCommerceValidationIssue issue(
            String code,
            String field,
            String message,
            String expected,
            String actual) {
        return AgenticCommerceValidationIssue.of(code, field, message, expected, actual);
    }
}
