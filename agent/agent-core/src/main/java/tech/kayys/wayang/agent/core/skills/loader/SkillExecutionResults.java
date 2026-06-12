package tech.kayys.wayang.agent.core.skills.loader;

import java.util.Collection;
import java.util.Map;

/**
 * Builds immutable filesystem skill execution results and metadata.
 */
final class SkillExecutionResults {

    private final String skillName;
    private final long startedAtMs;

    private SkillExecutionResults(String skillName, long startedAtMs) {
        this.skillName = skillName;
        this.startedAtMs = startedAtMs;
    }

    static SkillExecutionResults started(String skillName) {
        return new SkillExecutionResults(skillName, System.currentTimeMillis());
    }

    SkillExecutor.SkillExecutionResult success(String output, Map<String, Object> metadata) {
        return new SkillExecutor.SkillExecutionResult(
                skillName,
                output,
                elapsedMs(),
                true,
                null,
                metadata);
    }

    SkillExecutor.SkillExecutionResult skillNotFound() {
        return failure(
                null,
                "Skill not found: " + skillName,
                SkillExecutionOutcomes.failureMetadata(SkillFailureType.SKILL_NOT_FOUND));
    }

    SkillExecutor.SkillExecutionResult parameterValidationFailure(Collection<String> errors) {
        Collection<String> safeErrors = errors == null ? java.util.List.of() : errors;
        return failure(
                null,
                "Parameter validation failed: " + String.join("; ", safeErrors),
                SkillExecutionOutcomes.failureMetadata(
                        SkillFailureType.PARAMETER_VALIDATION,
                        SkillExecutionMetadata.KEY_ERROR_COUNT,
                        safeErrors.size()));
    }

    SkillExecutor.SkillExecutionResult invalidInput(String message) {
        return failure(null, message, SkillExecutionOutcomes.failureMetadata(SkillFailureType.INVALID_INPUT));
    }

    SkillExecutor.SkillExecutionResult layoutFailure(String message, Map<String, Object> metadata) {
        return failure(null, message, metadata);
    }

    SkillExecutor.SkillExecutionResult processFailure(SkillProcessRunner.ProcessFailureException error) {
        return failure(error.result().output(), error.getMessage(), error.metadata());
    }

    SkillExecutor.SkillExecutionResult timeout(String message, int timeoutSeconds) {
        return failure(
                null,
                message,
                SkillExecutionOutcomes.failureMetadata(
                        SkillFailureType.TIMEOUT,
                        SkillExecutionMetadata.KEY_TIMEOUT_SECONDS,
                        timeoutSeconds));
    }

    SkillExecutor.SkillExecutionResult executionError(Exception error) {
        return failure(
                null,
                error.getMessage(),
                SkillExecutionOutcomes.failureMetadata(
                        SkillFailureType.EXECUTION_ERROR,
                        SkillExecutionMetadata.KEY_EXCEPTION_TYPE,
                        error.getClass().getName()));
    }

    private SkillExecutor.SkillExecutionResult failure(String output, String error, Map<String, Object> metadata) {
        return new SkillExecutor.SkillExecutionResult(
                skillName,
                output,
                elapsedMs(),
                false,
                error,
                metadata);
    }

    private long elapsedMs() {
        return System.currentTimeMillis() - startedAtMs;
    }
}
