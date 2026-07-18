package tech.kayys.gollek.agent.skills.validation;

import java.util.List;

/**
 * Validation result for skill validation.
 *
 * @param status validation status
 * @param skillId skill being validated
 * @param errors list of validation errors
 * @param warnings list of validation warnings
 * @param info list of informational messages
 */
public record ValidationResult(
    ValidationStatus status,
    String skillId,
    List<String> errors,
    List<String> warnings,
    List<String> info
) {
    public static ValidationResult valid(String skillId) {
        return new ValidationResult(ValidationStatus.VALID, skillId, List.of(), List.of(), List.of());
    }

    public static ValidationResult invalid(String skillId, List<String> errors) {
        return new ValidationResult(ValidationStatus.INVALID, skillId, errors, List.of(), List.of());
    }

    public static ValidationResult withWarnings(String skillId, List<String> warnings) {
        return new ValidationResult(ValidationStatus.WARNING, skillId, List.of(), warnings, List.of());
    }

    public boolean isValid() {
        return status == ValidationStatus.VALID || status == ValidationStatus.WARNING;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
