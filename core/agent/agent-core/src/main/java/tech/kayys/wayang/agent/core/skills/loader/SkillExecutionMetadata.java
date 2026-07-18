package tech.kayys.wayang.agent.core.skills.loader;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Stable metadata keys and typed readers for skill execution outcomes.
 */
public final class SkillExecutionMetadata {

    public static final String KEY_ERROR_COUNT = "errorCount";
    public static final String KEY_EXCEPTION_TYPE = "exceptionType";
    public static final String KEY_EXIT_CODE = "exitCode";
    public static final String KEY_LAYOUT_ERROR = "layoutError";
    public static final String KEY_OUTPUT_CHARS = "outputChars";
    public static final String KEY_OUTPUT_TRUNCATED = "outputTruncated";
    public static final String KEY_TIMEOUT_SECONDS = "timeoutSeconds";

    private SkillExecutionMetadata() {
    }

    public static OptionalInt errorCount(SkillExecutionOutcome outcome) {
        return intValue(value(outcome, KEY_ERROR_COUNT));
    }

    public static Optional<String> exceptionType(SkillExecutionOutcome outcome) {
        return stringValue(value(outcome, KEY_EXCEPTION_TYPE));
    }

    public static OptionalInt exitCode(SkillExecutionOutcome outcome) {
        return intValue(value(outcome, KEY_EXIT_CODE));
    }

    public static Optional<String> layoutError(SkillExecutionOutcome outcome) {
        return stringValue(value(outcome, KEY_LAYOUT_ERROR));
    }

    public static OptionalLong outputChars(SkillExecutionOutcome outcome) {
        return longValue(value(outcome, KEY_OUTPUT_CHARS));
    }

    public static Optional<Boolean> outputTruncated(SkillExecutionOutcome outcome) {
        Object value = value(outcome, KEY_OUTPUT_TRUNCATED);
        return value instanceof Boolean typed ? Optional.of(typed) : Optional.empty();
    }

    public static OptionalInt timeoutSeconds(SkillExecutionOutcome outcome) {
        return intValue(value(outcome, KEY_TIMEOUT_SECONDS));
    }

    private static Object value(SkillExecutionOutcome outcome, String key) {
        if (outcome == null) {
            return null;
        }
        return value(outcome.metadata(), key);
    }

    private static Object value(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }

    private static OptionalInt intValue(Object value) {
        return value instanceof Number number ? OptionalInt.of(number.intValue()) : OptionalInt.empty();
    }

    private static OptionalLong longValue(Object value) {
        return value instanceof Number number ? OptionalLong.of(number.longValue()) : OptionalLong.empty();
    }

    private static Optional<String> stringValue(Object value) {
        return value instanceof String text ? Optional.of(text) : Optional.empty();
    }
}
