package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of one checkout HTTP harness step.
 */
public record AgenticCommerceCheckoutHttpExchange(
        AgenticCommerceCheckoutHttpScenarioStep step,
        AgenticCommerceValidationReport requestValidation,
        AgenticCommerceCheckoutHttpResult result,
        String transportError,
        List<AgenticCommerceValidationIssue> issues,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpExchange {
        step = Objects.requireNonNull(step, "step");
        requestValidation = Objects.requireNonNull(requestValidation, "requestValidation");
        result = Objects.requireNonNull(result, "result");
        transportError = AgenticCommerceValues.textValue(transportError);
        issues = issues == null
                ? List.of()
                : issues.stream()
                        .filter(Objects::nonNull)
                        .toList();
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public boolean successful() {
        return valid()
                && transportError.isBlank()
                && result.response().statusCode() == step.expectedStatusCode()
                && result.successful() == step.expectedSuccessful();
    }

    public int issueCount() {
        return issues.size();
    }

    public String operation() {
        String resultOperation = result.operation();
        return resultOperation.isBlank() ? step.operation() : resultOperation;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("stepId", step.id());
        values.put("operation", operation());
        values.put("valid", valid());
        values.put("successful", successful());
        values.put("expectedStatusCode", step.expectedStatusCode());
        values.put("actualStatusCode", result.response().statusCode());
        values.put("expectedSuccessful", step.expectedSuccessful());
        values.put("actualSuccessful", result.successful());
        values.put("issueCount", issueCount());
        values.put("requestValidation", requestValidation.toMap());
        values.put("result", result.toMap());
        AgenticCommerceValues.putText(values, "transportError", transportError);
        AgenticCommerceValues.putList(values, "issues", issues.stream()
                .map(AgenticCommerceValidationIssue::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
