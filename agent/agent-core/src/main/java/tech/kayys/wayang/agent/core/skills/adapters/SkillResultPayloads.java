package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shared payload mapping for skill execution results exposed through adapters.
 */
final class SkillResultPayloads {

    static final String ERROR_EXECUTION_FAILED = "Skill execution failed";
    static final String ERROR_NO_RESULT = "Skill execution returned no result";
    static final String KEY_OBSERVATION = "observation";
    static final String KEY_OUTPUTS = "outputs";
    static final String KEY_STATUS = "status";
    static final String STATUS_UNKNOWN = "UNKNOWN";

    private SkillResultPayloads() {
    }

    static Map<String, Object> resultData(SkillResult result, String observationKey) {
        Map<String, Object> data = new LinkedHashMap<>();
        putResultData(data, result, observationKey);
        return Map.copyOf(data);
    }

    static void putResultData(Map<String, Object> target, SkillResult result, String observationKey) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(observationKey, "observationKey");
        if (result.observation() != null) {
            target.put(observationKey, result.observation());
        }
        target.put(KEY_STATUS, statusName(result));
        if (result.hasOutputs()) {
            target.put(KEY_OUTPUTS, result.getOutputs());
        }
    }

    static String failureMessage(SkillResult result) {
        if (result == null) {
            return ERROR_NO_RESULT;
        }
        if (result.observation() != null) {
            return result.observation();
        }
        if (result.error() != null) {
            return errorMessage(result.error());
        }
        return ERROR_EXECUTION_FAILED;
    }

    static String statusName(SkillResult result) {
        return result == null || result.status() == null ? STATUS_UNKNOWN : result.status().name();
    }

    static String errorMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return ERROR_EXECUTION_FAILED;
        }
        return error.getMessage();
    }
}
