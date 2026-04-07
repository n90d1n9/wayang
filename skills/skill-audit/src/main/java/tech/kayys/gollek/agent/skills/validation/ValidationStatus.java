package tech.kayys.gollek.agent.skills.validation;

/**
 * Validation status for skill validation results.
 */
public enum ValidationStatus {
    VALID("Skill passed all validations"),
    INVALID("Skill failed validation"),
    WARNING("Skill has warnings but is valid"),
    ERROR("Skill has errors");

    private final String description;

    ValidationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
