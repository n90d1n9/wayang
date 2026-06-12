package tech.kayys.wayang.a2a.core;

import java.util.Arrays;

/**
 * ProtoJSON role values used by A2A messages.
 */
public enum A2aRole {
    ROLE_UNSPECIFIED,
    ROLE_USER,
    ROLE_AGENT;

    public String value() {
        return name();
    }

    public static A2aRole fromValue(String value) {
        if (value == null || value.isBlank()) {
            return ROLE_UNSPECIFIED;
        }
        return Arrays.stream(values())
                .filter(role -> role.name().equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported A2A role: " + value));
    }
}
