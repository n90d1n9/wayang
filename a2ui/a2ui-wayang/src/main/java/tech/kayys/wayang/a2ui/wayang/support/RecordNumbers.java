package tech.kayys.wayang.a2ui.wayang.support;

/**
 * Small numeric normalization helpers for A2UI record constructors.
 */
public final class RecordNumbers {

    private RecordNumbers() {
    }

    public static int nonNegative(int value) {
        return Math.max(0, value);
    }

    public static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    public static Integer nullableNonNegative(Integer value) {
        return value == null ? null : nonNegative(value);
    }

    public static Long nullableNonNegative(Long value) {
        return value == null ? null : nonNegative(value);
    }

    public static int oneBased(int value) {
        return Math.max(1, value);
    }
}
