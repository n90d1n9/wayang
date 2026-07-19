package tech.kayys.wayang.command;

import tech.kayys.wayang.client.SdkText;

public record WayangCommandDiscoveryContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.command.discovery";
    public static final int VERSION = 1;
    public static final String COMMANDS_DISCOVERY = "commands-discovery";

    public WayangCommandDiscoveryContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangCommandDiscoveryContract commandsDiscovery() {
        return new WayangCommandDiscoveryContract(SCHEMA, VERSION, COMMANDS_DISCOVERY);
    }
}
