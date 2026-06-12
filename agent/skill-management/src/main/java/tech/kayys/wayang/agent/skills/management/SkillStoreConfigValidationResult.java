package tech.kayys.wayang.agent.skills.management;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Structured validation result for skill store configuration.
 */
public record SkillStoreConfigValidationResult(List<String> errors) {

    public SkillStoreConfigValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static SkillStoreConfigValidationResult valid() {
        return new SkillStoreConfigValidationResult(List.of());
    }

    public static SkillStoreConfigValidationResult error(String error) {
        return new SkillStoreConfigValidationResult(error == null || error.isBlank() ? List.of() : List.of(error));
    }

    public static SkillStoreConfigValidationResult combine(SkillStoreConfigValidationResult... results) {
        if (results == null || results.length == 0) {
            return valid();
        }
        return new SkillStoreConfigValidationResult(Arrays.stream(results)
                .filter(Objects::nonNull)
                .flatMap(result -> result.errors().stream())
                .toList());
    }

    public boolean validConfiguration() {
        return errors.isEmpty();
    }

    public String message() {
        return String.join("; ", errors);
    }

    public void throwIfInvalid() {
        if (!validConfiguration()) {
            throw new IllegalArgumentException(message());
        }
    }
}
