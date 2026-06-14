package tech.kayys.wayang.gollek.sdk;

public final class AgentRunStates {

    private AgentRunStates() {
    }

    public static AgentRunState parseOptional(String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return AgentRunState.valueOf(normalized.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown run state '" + normalized + "'. Known states: "
                            + String.join(", ", knownStateNames()),
                    e);
        }
    }

    public static AgentRunState parseOrDefault(String value, AgentRunState fallback) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            return fallback == null ? AgentRunState.UNKNOWN : fallback;
        }
        try {
            return AgentRunState.valueOf(normalized.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return fallback == null ? AgentRunState.UNKNOWN : fallback;
        }
    }

    public static String wireName(AgentRunState state) {
        AgentRunState normalized = state == null ? AgentRunState.UNKNOWN : state;
        return normalized.name().toLowerCase().replace('_', '-');
    }

    public static String[] knownStateNames() {
        AgentRunState[] states = AgentRunState.values();
        String[] names = new String[states.length];
        for (int i = 0; i < states.length; i++) {
            names[i] = wireName(states[i]);
        }
        return names;
    }
}
