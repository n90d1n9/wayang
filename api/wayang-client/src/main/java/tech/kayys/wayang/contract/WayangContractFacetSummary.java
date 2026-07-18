package tech.kayys.wayang.contract;

import java.util.List;

public record WayangContractFacetSummary(
        String name,
        int count,
        List<String> schemas,
        List<String> domains,
        List<String> envelopes,
        List<String> jsonSchemaIds,
        List<String> commandIds) {

    public WayangContractFacetSummary {
        name = SdkText.trimToEmpty(name);
        count = Math.max(0, count);
        schemas = SdkLists.copy(schemas);
        domains = SdkLists.copy(domains);
        envelopes = SdkLists.copy(envelopes);
        jsonSchemaIds = SdkLists.copy(jsonSchemaIds);
        commandIds = SdkLists.copy(commandIds);
    }
}
