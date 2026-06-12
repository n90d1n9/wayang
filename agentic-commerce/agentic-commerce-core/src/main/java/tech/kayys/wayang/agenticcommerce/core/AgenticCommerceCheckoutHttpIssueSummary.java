package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flattened checkout HTTP smoke issue for operational summaries.
 */
public record AgenticCommerceCheckoutHttpIssueSummary(
        String source,
        String stepId,
        String operation,
        String code,
        String field,
        String message,
        String expected,
        String actual,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpIssueSummary {
        source = AgenticCommerceValues.textValue(source);
        stepId = AgenticCommerceValues.textValue(stepId);
        operation = AgenticCommerceValues.textValue(operation);
        code = AgenticCommerceValues.textValue(code);
        field = AgenticCommerceValues.textValue(field);
        message = AgenticCommerceValues.textValue(message);
        expected = AgenticCommerceValues.textValue(expected);
        actual = AgenticCommerceValues.textValue(actual);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCheckoutHttpIssueSummary fromIssue(
            String source,
            String stepId,
            String operation,
            AgenticCommerceValidationIssue issue) {
        AgenticCommerceValidationIssue resolved = issue == null
                ? AgenticCommerceValidationIssue.of("", "", "", "", "")
                : issue;
        return new AgenticCommerceCheckoutHttpIssueSummary(
                source,
                stepId,
                operation,
                resolved.code(),
                resolved.field(),
                resolved.message(),
                resolved.expected(),
                resolved.actual(),
                Map.of());
    }

    public static AgenticCommerceCheckoutHttpIssueSummary fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceCheckoutHttpIssueSummary("", "", "", "", "", "", "", "", Map.of());
        }
        return new AgenticCommerceCheckoutHttpIssueSummary(
                AgenticCommerceValues.text(values, "source"),
                AgenticCommerceValues.text(values, "stepId", "step_id"),
                AgenticCommerceValues.text(values, "operation"),
                AgenticCommerceValues.text(values, "code"),
                AgenticCommerceValues.text(values, "field"),
                AgenticCommerceValues.text(values, "message"),
                AgenticCommerceValues.text(values, "expected"),
                AgenticCommerceValues.text(values, "actual"),
                AgenticCommerceValues.metadata(
                        values,
                        "source",
                        "stepId",
                        "step_id",
                        "operation",
                        "code",
                        "field",
                        "message",
                        "expected",
                        "actual"));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "source", source);
        AgenticCommerceValues.putText(values, "stepId", stepId);
        AgenticCommerceValues.putText(values, "operation", operation);
        AgenticCommerceValues.putText(values, "code", code);
        AgenticCommerceValues.putText(values, "field", field);
        AgenticCommerceValues.putText(values, "message", message);
        AgenticCommerceValues.putText(values, "expected", expected);
        AgenticCommerceValues.putText(values, "actual", actual);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
