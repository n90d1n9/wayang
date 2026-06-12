package tech.kayys.wayang.agent.core.skills.loader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Public factories and metadata keys for read-only skill execution outcomes.
 */
public final class SkillExecutionOutcomes {

    public static final String KEY_FAILURE_TYPE = "failureType";
    public static final String FAILURE_SKILL_NOT_FOUND = "skill_not_found";
    public static final String FAILURE_SKILL_NOT_LOADED = "skill_not_loaded";
    public static final String FAILURE_PARAMETER_VALIDATION = "parameter_validation";
    public static final String FAILURE_INVALID_INPUT = "invalid_input";
    public static final String FAILURE_SKILL_LAYOUT = "skill_layout";
    public static final String FAILURE_PROCESS_EXIT = "process_exit";
    public static final String FAILURE_TIMEOUT = "timeout";
    public static final String FAILURE_EXECUTION_ERROR = "execution_error";

    private SkillExecutionOutcomes() {
    }

    public static SkillExecutionOutcome success(
            String skillName,
            String output,
            long executionTimeMs,
            Map<String, Object> metadata) {
        return new ImmutableSkillExecutionOutcome(
                skillName,
                output,
                executionTimeMs,
                true,
                null,
                metadata);
    }

    public static SkillExecutionOutcome failure(
            String skillName,
            String output,
            long executionTimeMs,
            String error,
            Map<String, Object> metadata) {
        return new ImmutableSkillExecutionOutcome(
                skillName,
                output,
                executionTimeMs,
                false,
                error,
                metadata);
    }

    public static SkillExecutionOutcome skillNotLoaded(String skillName) {
        return failure(
                skillName,
                null,
                0,
                "Skill not loaded in orchestrator context: " + skillName,
                failureMetadata(SkillFailureType.SKILL_NOT_LOADED));
    }

    public static Optional<SkillFailureType> failureType(SkillExecutionOutcome outcome) {
        if (outcome == null || outcome.metadata() == null) {
            return Optional.empty();
        }
        Object value = outcome.metadata().get(KEY_FAILURE_TYPE);
        if (!(value instanceof String code)) {
            return Optional.empty();
        }
        return SkillFailureType.fromCode(code);
    }

    public static Map<String, Object> failureMetadata(SkillFailureType failureType) {
        return failureMetadata(failureType, Map.of());
    }

    public static Map<String, Object> failureMetadata(SkillFailureType failureType, String key, Object value) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put(key, value);
        return failureMetadata(failureType, attributes);
    }

    public static Map<String, Object> failureMetadata(SkillFailureType failureType, Map<String, Object> attributes) {
        Objects.requireNonNull(failureType, "failureType");
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (key != null && value != null && !KEY_FAILURE_TYPE.equals(key)) {
                    metadata.put(key, value);
                }
            });
        }
        metadata.put(KEY_FAILURE_TYPE, failureType.code());
        return Map.copyOf(metadata);
    }

    private record ImmutableSkillExecutionOutcome(
            String skillName,
            String output,
            long executionTimeMs,
            boolean success,
            String error,
            Map<String, Object> metadata
    ) implements SkillExecutionOutcome {
        private ImmutableSkillExecutionOutcome {
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
