package tech.kayys.wayang.gollek.sdk;

import java.util.Locale;

public enum AgentSkillState {
    ACTIVE,
    PREVIEW,
    DISABLED,
    DEPRECATED;

    public boolean availableForRuns() {
        return this == ACTIVE || this == PREVIEW;
    }

    public static AgentSkillState from(String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            return ACTIVE;
        }
        return valueOf(normalized.replace('-', '_').toUpperCase(Locale.ROOT));
    }
}
