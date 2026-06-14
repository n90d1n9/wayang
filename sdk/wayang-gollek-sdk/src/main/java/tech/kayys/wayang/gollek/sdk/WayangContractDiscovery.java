package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record WayangContractDiscovery(
        WayangContractQuery query,
        List<WayangContractDescriptor> contracts,
        int totalContracts) {

    public WayangContractDiscovery {
        query = query == null ? WayangContractQuery.all() : query;
        contracts = SdkLists.copy(contracts);
        totalContracts = Math.max(0, totalContracts);
    }

    public static WayangContractDiscovery of(
            WayangContractQuery query,
            List<WayangContractDescriptor> contracts,
            int totalContracts) {
        return new WayangContractDiscovery(query, contracts, totalContracts);
    }

    public int matchingContracts() {
        return contracts.size();
    }

    public boolean empty() {
        return contracts.isEmpty();
    }

    public List<String> schemas() {
        return index().schemas();
    }

    public Map<String, Integer> schemaCounts() {
        return index().schemaCounts();
    }

    public List<WayangContractFacetSummary> schemaSummaries() {
        return index().schemaSummaries();
    }

    public List<String> domains() {
        return index().domains();
    }

    public Map<String, Integer> domainCounts() {
        return index().domainCounts();
    }

    public List<WayangContractFacetSummary> domainSummaries() {
        return index().domainSummaries();
    }

    public List<String> envelopes() {
        return index().envelopes();
    }

    public List<WayangContractFacetSummary> envelopeSummaries() {
        return index().envelopeSummaries();
    }

    public List<String> jsonSchemaIds() {
        return index().jsonSchemaIds();
    }

    public List<WayangContractKey> keys() {
        return index().keys();
    }

    public Map<WayangContractKey, WayangContractDescriptor> contractsByKey() {
        return index().contractsByKey();
    }

    public Optional<WayangContractDescriptor> contractByKey(WayangContractKey key) {
        return index().contractByKey(key);
    }

    public Optional<WayangContractDescriptor> contractByJsonSchemaId(String jsonSchemaId) {
        return index().contractByJsonSchemaId(jsonSchemaId);
    }

    public List<String> commandIds() {
        return index().commandIds();
    }

    public Map<String, Integer> commandIdCounts() {
        return index().commandIdCounts();
    }

    private WayangContractIndex index() {
        return WayangContractIndex.of(contracts);
    }
}
