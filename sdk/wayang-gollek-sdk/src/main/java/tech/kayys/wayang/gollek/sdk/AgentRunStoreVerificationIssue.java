package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One operator-facing issue found while verifying an agent run-store snapshot.
 */
public record AgentRunStoreVerificationIssue(
        String severity,
        String code,
        String message) {

    public static final String SEVERITY_ERROR = "error";
    public static final String SEVERITY_WARNING = "warning";

    public AgentRunStoreVerificationIssue {
        severity = SdkText.trimToDefault(severity, SEVERITY_ERROR);
        code = SdkText.trimToDefault(code, "run-store.issue");
        message = SdkText.trimToEmpty(message);
    }

    public static AgentRunStoreVerificationIssue error(String code, String message) {
        return new AgentRunStoreVerificationIssue(SEVERITY_ERROR, code, message);
    }

    public static AgentRunStoreVerificationIssue warning(String code, String message) {
        return new AgentRunStoreVerificationIssue(SEVERITY_WARNING, code, message);
    }

    public boolean error() {
        return SEVERITY_ERROR.equalsIgnoreCase(severity);
    }

    public boolean warning() {
        return SEVERITY_WARNING.equalsIgnoreCase(severity);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("severity", severity);
        values.put("code", code);
        values.put("message", message);
        return SdkMaps.copy(values);
    }
}
