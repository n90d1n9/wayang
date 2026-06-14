package tech.kayys.wayang.gollek.sdk;

public final class AgentRunOutcomes {

    public static final String TERMINAL = "terminal";
    public static final String TIMEOUT = "timeout";
    public static final String MAX_POLLS = "max-polls";
    public static final String FORGOTTEN = "forgotten";
    public static final String CANCELLED = "cancelled";
    public static final String NOT_CANCELLABLE = "not-cancellable";
    public static final String NOT_FOUND = "not-found";
    public static final String UNKNOWN = "unknown";
    public static final String EMPTY = "empty";
    public static final String PENDING = "pending";

    private static final String[] KNOWN_OUTCOMES = {
            TERMINAL,
            TIMEOUT,
            MAX_POLLS,
            FORGOTTEN,
            CANCELLED,
            NOT_CANCELLABLE,
            NOT_FOUND,
            UNKNOWN,
            EMPTY,
            PENDING
    };

    private AgentRunOutcomes() {
    }

    public static String[] knownOutcomeNames() {
        return KNOWN_OUTCOMES.clone();
    }

    public static boolean known(String value) {
        String normalized = normalizeValue(value);
        for (String outcome : KNOWN_OUTCOMES) {
            if (outcome.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizeOrDefault(String value, String fallback) {
        String normalized = normalizeValue(value);
        if (known(normalized)) {
            return normalized;
        }
        String normalizedFallback = normalizeValue(fallback);
        return known(normalizedFallback) ? normalizedFallback : UNKNOWN;
    }

    private static String normalizeValue(String value) {
        return SdkText.trimToEmpty(value)
                .toLowerCase()
                .replace('_', '-')
                .replace(' ', '-');
    }
}
