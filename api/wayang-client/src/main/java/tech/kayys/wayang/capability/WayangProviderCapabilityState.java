package tech.kayys.wayang.capability;

import java.util.Locale;

public enum WayangProviderCapabilityState {
    AVAILABLE,
    PREVIEW,
    DISABLED,
    DEPRECATED;

    public boolean available() {
        return this == AVAILABLE || this == PREVIEW;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static WayangProviderCapabilityState from(String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            return AVAILABLE;
        }
        return valueOf(normalized.replace('-', '_').toUpperCase(Locale.ROOT));
    }
}
