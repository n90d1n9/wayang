package tech.kayys.gollek.skills.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents validation results for a skill.
 */
public final class ValidationResult {

    private final String skillName;
    private final List<String> errors;
    private final List<String> warnings;
    private final boolean valid;

    private ValidationResult(String skillName, List<String> errors, List<String> warnings) {
        this.skillName = Objects.requireNonNull(skillName);
        this.errors = new ArrayList<>(Objects.requireNonNull(errors));
        this.warnings = new ArrayList<>(Objects.requireNonNull(warnings));
        this.valid = errors.isEmpty();
    }

    public static ValidationResult valid(String skillName) {
        return new ValidationResult(skillName, List.of(), List.of());
    }

    public static ValidationResult invalid(String skillName, List<String> errors) {
        return new ValidationResult(skillName, errors, List.of());
    }

    public static ValidationResult withWarnings(String skillName, List<String> warnings) {
        return new ValidationResult(skillName, List.of(), warnings);
    }

    public static ValidationResult mixed(String skillName, List<String> errors, List<String> warnings) {
        return new ValidationResult(skillName, errors, warnings);
    }

    public String getSkillName() {
        return skillName;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "skillName='" + skillName + '\'' +
                ", errors=" + errors +
                ", warnings=" + warnings +
                ", valid=" + valid +
                '}';
    }
}
