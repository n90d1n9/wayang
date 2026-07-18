package tech.kayys.wayang.client;


public record ComponentStatus(
        String name,
        String role,
        String state,
        String endpoint,
        int healthPercent) {

    public ComponentStatus {
        name = SdkText.trimToEmpty(name);
        role = SdkText.trimToEmpty(role);
        state = SdkText.trimToDefault(state, "unknown");
        endpoint = SdkText.trimToEmpty(endpoint);
        healthPercent = Math.max(0, Math.min(100, healthPercent));
    }
}
