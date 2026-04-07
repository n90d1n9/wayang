package tech.kayys.gollek.agent.skills.validation;

import java.util.List;

/**
 * Result of SKILL.md validation.
 *
 * @param isValid whether the skill is valid
 * @param errors list of validation errors
 * @param warnings list of validation warnings
 */
public record ValidationResult(
        boolean isValid,
        List<String> errors,
        List<String> warnings) {

    /**
     * Get formatted error messages.
     */
    public String formatErrors() {
        if (errors.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String error : errors) {
            sb.append("  ❌ ").append(error).append("\n");
        }
        return sb.toString();
    }

    /**
     * Get formatted warning messages.
     */
    public String formatWarnings() {
        if (warnings.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String warning : warnings) {
            sb.append("  ⚠️  ").append(warning).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isValid) {
            sb.append("✅ Valid skill\n");
        } else {
            sb.append("❌ Invalid skill\n");
        }
        if (!errors.isEmpty()) {
            sb.append("\nErrors:\n").append(formatErrors());
        }
        if (!warnings.isEmpty()) {
            sb.append("\nWarnings:\n").append(formatWarnings());
        }
        return sb.toString();
    }
}
