package tech.kayys.wayang.prompt.core;

import java.util.Objects;

/**
 * ============================================================================
 * ValidationWarning — structured warning from template validation.
 * ============================================================================
 *
 * Returned by {@link PromptTemplate#getValidationWarnings()} to surface issues
 * that don't block template creation but should be fixed by the author.
 */
public final class ValidationWarning {

    private final ValidationWarningType type;
    private final String variableName;
    private final String message;

    public ValidationWarning(ValidationWarningType type, String variableName, String message) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.variableName = variableName; // may be null for warnings that don't refer to a specific variable
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    public ValidationWarningType getType() {
        return type;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValidationWarning that)) return false;
        return type == that.type &&
                Objects.equals(variableName, that.variableName) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, variableName, message);
    }

    @Override
    public String toString() {
        return "ValidationWarning{type=%s, variable='%s', message='%s'}"
                .formatted(type, variableName, message);
    }
}