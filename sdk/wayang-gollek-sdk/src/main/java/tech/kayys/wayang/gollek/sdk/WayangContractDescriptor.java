package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record WayangContractDescriptor(
        String schema,
        int version,
        String envelope,
        String domain,
        String description,
        List<String> commandIds,
        List<String> commands) {

    public WayangContractDescriptor {
        schema = SdkText.trimToEmpty(schema);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
        domain = SdkText.trimToEmpty(domain);
        description = SdkText.trimToEmpty(description);
        commandIds = SdkLists.copy(commandIds);
        commands = SdkLists.copy(commands);
    }

    public String jsonSchemaId() {
        return key().jsonSchemaId();
    }

    public WayangContractKey key() {
        return WayangContractKey.from(this);
    }
}
