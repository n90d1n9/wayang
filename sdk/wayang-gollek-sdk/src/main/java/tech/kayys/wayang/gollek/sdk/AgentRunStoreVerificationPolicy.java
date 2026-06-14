package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operator policy that decides whether run-store verification warnings should fail automation.
 */
public record AgentRunStoreVerificationPolicy(boolean failOnWarnings) {

    public static AgentRunStoreVerificationPolicy lenient() {
        return new AgentRunStoreVerificationPolicy(false);
    }

    public static AgentRunStoreVerificationPolicy strict() {
        return new AgentRunStoreVerificationPolicy(true);
    }

    public static AgentRunStoreVerificationPolicy fromStrict(boolean strict) {
        return strict ? strict() : lenient();
    }

    public static AgentRunStoreVerificationPolicy fromMode(String mode) {
        String normalized = SdkText.trimToEmpty(mode)
                .toLowerCase()
                .replace('_', '-');
        if (normalized.isEmpty()
                || "default".equals(normalized)
                || "lenient".equals(normalized)
                || "warn".equals(normalized)
                || "warnings".equals(normalized)) {
            return lenient();
        }
        if ("strict".equals(normalized)
                || "fail-on-warnings".equals(normalized)
                || "warnings-as-errors".equals(normalized)
                || "error-on-warnings".equals(normalized)) {
            return strict();
        }
        throw new IllegalArgumentException(
                "Unsupported run-store verification policy '" + mode
                        + "'. Use lenient or strict.");
    }

    public static AgentRunStoreVerificationPolicy fromMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return lenient();
        }
        Object mode = values.get("mode");
        if (mode != null && !SdkText.trimToEmpty(String.valueOf(mode)).isEmpty()) {
            return fromMode(String.valueOf(mode));
        }
        Object failOnWarnings = values.get("failOnWarnings");
        return failOnWarnings == null
                ? lenient()
                : fromStrict(Boolean.parseBoolean(String.valueOf(failOnWarnings)));
    }

    public String mode() {
        return failOnWarnings ? "strict" : "lenient";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", mode());
        values.put("failOnWarnings", failOnWarnings);
        return AgentRunEnvelopeMaps.copy(values);
    }
}
