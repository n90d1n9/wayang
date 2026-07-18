package tech.kayys.wayang.workbench;

import java.util.List;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.contract.WayangContractKey;

public record WorkbenchCommandContractSummary(
        String jsonSchemaId,
        String schema,
        int version,
        String envelope,
        int count,
        List<String> commandIds) {

    public WorkbenchCommandContractSummary {
        jsonSchemaId = SdkText.trimToEmpty(jsonSchemaId);
        schema = SdkText.trimToEmpty(schema);
        version = Math.max(0, version);
        envelope = SdkText.trimToEmpty(envelope);
        count = Math.max(0, count);
        commandIds = SdkLists.copy(commandIds);
    }

    public WayangContractKey key() {
        return WayangContractKey.of(schema, version, envelope);
    }
}
