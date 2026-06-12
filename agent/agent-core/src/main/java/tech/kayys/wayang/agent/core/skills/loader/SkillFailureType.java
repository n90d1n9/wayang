package tech.kayys.wayang.agent.core.skills.loader;

import java.util.Arrays;
import java.util.Optional;

/**
 * Stable failure taxonomy for skill execution outcomes.
 */
public enum SkillFailureType {
    SKILL_NOT_FOUND(SkillExecutionOutcomes.FAILURE_SKILL_NOT_FOUND),
    SKILL_NOT_LOADED(SkillExecutionOutcomes.FAILURE_SKILL_NOT_LOADED),
    PARAMETER_VALIDATION(SkillExecutionOutcomes.FAILURE_PARAMETER_VALIDATION),
    INVALID_INPUT(SkillExecutionOutcomes.FAILURE_INVALID_INPUT),
    SKILL_LAYOUT(SkillExecutionOutcomes.FAILURE_SKILL_LAYOUT),
    PROCESS_EXIT(SkillExecutionOutcomes.FAILURE_PROCESS_EXIT),
    TIMEOUT(SkillExecutionOutcomes.FAILURE_TIMEOUT),
    EXECUTION_ERROR(SkillExecutionOutcomes.FAILURE_EXECUTION_ERROR);

    private final String code;

    SkillFailureType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<SkillFailureType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst();
    }
}
