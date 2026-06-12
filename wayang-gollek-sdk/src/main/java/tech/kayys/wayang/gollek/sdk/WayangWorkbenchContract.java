package tech.kayys.wayang.gollek.sdk;

public record WayangWorkbenchContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.workbench.discovery";
    public static final int VERSION = 1;
    public static final String WORKBENCH_DISCOVERY = "workbench-discovery";

    public WayangWorkbenchContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangWorkbenchContract workbenchDiscovery() {
        return new WayangWorkbenchContract(SCHEMA, VERSION, WORKBENCH_DISCOVERY);
    }
}
