package tech.kayys.wayang.agent.skill;

import java.util.Locale;

import tech.kayys.wayang.client.SdkText;

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
