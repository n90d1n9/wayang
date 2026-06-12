package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Non-blocking problem found while discovering or reading a standard-alignment provider.
 */
public record WayangStandardAlignmentProviderIssue(
        String providerId,
        String providerClass,
        String message) {

    public WayangStandardAlignmentProviderIssue {
        providerId = SdkText.trimToDefault(providerId, "unknown");
        providerClass = SdkText.trimToEmpty(providerClass);
        message = SdkText.trimToDefault(message, "Provider failed.");
    }

    public static WayangStandardAlignmentProviderIssue from(
            String providerId,
            String providerClass,
            Throwable failure) {
        return new WayangStandardAlignmentProviderIssue(
                providerId,
                providerClass,
                failure == null ? "" : failure.getMessage());
    }

    public String recommendation() {
        return "Review standard-alignment provider " + providerId + ": " + message;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerId", providerId);
        values.put("providerClass", providerClass);
        values.put("message", message);
        return SdkMaps.orderedCopy(values);
    }
}
