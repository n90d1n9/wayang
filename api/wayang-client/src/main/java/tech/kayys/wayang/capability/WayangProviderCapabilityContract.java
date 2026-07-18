package tech.kayys.wayang.capability;

public record WayangProviderCapabilityContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.provider.capability";
    public static final int VERSION = 1;
    public static final String PROVIDER_CAPABILITY_DISCOVERY = "provider-capability-discovery";
    public static final String PROVIDER_CAPABILITY_DETAIL = "provider-capability-detail";

    public WayangProviderCapabilityContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangProviderCapabilityContract providerCapabilityDiscovery() {
        return new WayangProviderCapabilityContract(SCHEMA, VERSION, PROVIDER_CAPABILITY_DISCOVERY);
    }

    public static WayangProviderCapabilityContract providerCapabilityDetail() {
        return new WayangProviderCapabilityContract(SCHEMA, VERSION, PROVIDER_CAPABILITY_DETAIL);
    }
}
