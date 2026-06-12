package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared validation primitives for skill store config records.
 */
final class SkillStoreConfigValidation {

    private SkillStoreConfigValidation() {
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private final List<String> errors = new ArrayList<>();

        Builder requireDirectory(Path directory, String message) {
            return require(directory != null, message);
        }

        Builder requireDirectoryWhen(boolean condition, Path directory, String message) {
            return condition ? requireDirectory(directory, message) : this;
        }

        Builder requireText(String value, String message) {
            return require(value != null && !value.isBlank(), message);
        }

        Builder requireTextWhen(boolean condition, String value, String message) {
            return condition ? requireText(value, message) : this;
        }

        Builder requirePair(Object primary, Object fallback, String message) {
            return require(primary != null && fallback != null, message);
        }

        Builder requirePairWhen(boolean condition, Object primary, Object fallback, String message) {
            return condition ? requirePair(primary, fallback, message) : this;
        }

        Builder require(boolean condition, String message) {
            if (!condition) {
                errors.add(message);
            }
            return this;
        }

        SkillStoreConfigValidationResult result() {
            return errors.isEmpty()
                    ? SkillStoreConfigValidationResult.valid()
                    : new SkillStoreConfigValidationResult(errors);
        }
    }
}
