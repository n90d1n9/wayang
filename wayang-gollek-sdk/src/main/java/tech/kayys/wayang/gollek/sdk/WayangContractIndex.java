package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class WayangContractIndex {

    private final List<WayangContractDescriptor> contracts;
    private final Map<WayangContractKey, WayangContractDescriptor> contractsByKey;
    private final Map<WayangContractKey, List<WayangContractDescriptor>> groupedByKey;
    private final Map<String, WayangContractDescriptor> contractsByJsonSchemaId;

    private WayangContractIndex(List<WayangContractDescriptor> contracts) {
        this.contracts = SdkLists.copy(contracts);
        this.groupedByKey = groupByKey(this.contracts);
        this.contractsByKey = firstByKey(groupedByKey);
        this.contractsByJsonSchemaId = byJsonSchemaId(this.contracts);
    }

    public static WayangContractIndex of(List<WayangContractDescriptor> contracts) {
        return new WayangContractIndex(contracts);
    }

    public List<WayangContractDescriptor> contracts() {
        return contracts;
    }

    public List<WayangContractKey> keys() {
        return contractsByKey.isEmpty() ? List.of() : List.copyOf(contractsByKey.keySet());
    }

    public List<String> jsonSchemaIds() {
        return contractsByJsonSchemaId.isEmpty() ? List.of() : List.copyOf(contractsByJsonSchemaId.keySet());
    }

    public List<String> schemas() {
        return SdkFacets.values(contracts, WayangContractDescriptor::schema);
    }

    public Map<String, Integer> schemaCounts() {
        return SdkFacets.counts(contracts, WayangContractDescriptor::schema);
    }

    public List<WayangContractFacetSummary> schemaSummaries() {
        return summariesBy(WayangContractDescriptor::schema);
    }

    public List<String> domains() {
        return SdkFacets.values(contracts, WayangContractDescriptor::domain);
    }

    public Map<String, Integer> domainCounts() {
        return SdkFacets.counts(contracts, WayangContractDescriptor::domain);
    }

    public List<WayangContractFacetSummary> domainSummaries() {
        return summariesBy(WayangContractDescriptor::domain);
    }

    public List<String> envelopes() {
        return contracts.stream()
                .map(WayangContractDescriptor::envelope)
                .toList();
    }

    public List<WayangContractFacetSummary> envelopeSummaries() {
        return summariesBy(WayangContractDescriptor::envelope);
    }

    public List<String> commandIds() {
        return SdkFacets.flatValues(contracts, WayangContractDescriptor::commandIds);
    }

    public Map<String, Integer> commandIdCounts() {
        return SdkFacets.flatCounts(contracts, WayangContractDescriptor::commandIds);
    }

    public Map<WayangContractKey, WayangContractDescriptor> contractsByKey() {
        return contractsByKey;
    }

    public Map<String, WayangContractDescriptor> contractsByJsonSchemaId() {
        return contractsByJsonSchemaId;
    }

    public Optional<WayangContractDescriptor> contractByKey(WayangContractKey key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(contractsByKey.get(key));
    }

    public Optional<WayangContractDescriptor> contractByJsonSchemaId(String jsonSchemaId) {
        String normalized = SdkText.trimToEmpty(jsonSchemaId);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        Optional<WayangContractKey> key = WayangContractKey.parseJsonSchemaId(normalized);
        if (key.isPresent()) {
            Optional<WayangContractDescriptor> contract = contractByKey(key.get());
            if (contract.isPresent()) {
                return contract;
            }
        }
        return Optional.ofNullable(contractsByJsonSchemaId.get(normalized));
    }

    public List<WayangContractDescriptor> contractsForQuery(WayangContractQuery query) {
        WayangContractQuery normalized = query == null ? WayangContractQuery.all() : query;
        List<WayangContractDescriptor> filtered = contracts;
        if (normalized.hasSchema()) {
            filtered = contractsForSchema(filtered, normalized.schema());
        }
        if (normalized.hasEnvelope()) {
            filtered = contractsForEnvelope(filtered, normalized.envelope());
        }
        if (normalized.hasCommandId()) {
            filtered = contractsForCommandId(filtered, normalized.commandId());
        }
        if (normalized.hasDomain()) {
            filtered = contractsForDomain(filtered, normalized.domain());
        }
        if (normalized.hasJsonSchemaId()) {
            filtered = contractsForJsonSchemaId(filtered, normalized.jsonSchemaId());
        }
        return filtered;
    }

    public List<WayangContractDescriptor> contractsForSchema(String schema) {
        return contractsForSchema(contracts, schema);
    }

    public List<WayangContractDescriptor> contractsForEnvelope(String envelope) {
        return contractsForEnvelope(contracts, envelope);
    }

    public List<WayangContractDescriptor> contractsForCommandId(String commandId) {
        return contractsForCommandId(contracts, commandId);
    }

    public List<WayangContractDescriptor> contractsForDomain(String domain) {
        return contractsForDomain(contracts, domain);
    }

    public List<WayangContractDescriptor> contractsForJsonSchemaId(String jsonSchemaId) {
        return contractsForJsonSchemaId(contracts, jsonSchemaId);
    }

    public List<WayangContractKey> duplicateKeys() {
        return duplicatesByKey().keySet().stream().toList();
    }

    public Map<WayangContractKey, List<WayangContractDescriptor>> duplicatesByKey() {
        Map<WayangContractKey, List<WayangContractDescriptor>> duplicates = new LinkedHashMap<>();
        groupedByKey.forEach((key, values) -> {
            if (values.size() > 1) {
                duplicates.put(key, values);
            }
        });
        return duplicates.isEmpty() ? Map.of() : Collections.unmodifiableMap(duplicates);
    }

    private List<WayangContractFacetSummary> summariesBy(Function<WayangContractDescriptor, String> facet) {
        Map<String, List<WayangContractDescriptor>> grouped = new LinkedHashMap<>();
        contracts.forEach(contract -> grouped.computeIfAbsent(facet.apply(contract), ignored -> new ArrayList<>())
                .add(contract));
        return grouped.entrySet().stream()
                .map(entry -> summary(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static WayangContractFacetSummary summary(String name, List<WayangContractDescriptor> contracts) {
        return new WayangContractFacetSummary(
                name,
                contracts.size(),
                SdkFacets.values(contracts, WayangContractDescriptor::schema),
                SdkFacets.values(contracts, WayangContractDescriptor::domain),
                SdkFacets.values(contracts, WayangContractDescriptor::envelope),
                SdkFacets.values(contracts, WayangContractDescriptor::jsonSchemaId),
                SdkFacets.flatValues(contracts, WayangContractDescriptor::commandIds));
    }

    private static List<WayangContractDescriptor> contractsForSchema(
            List<WayangContractDescriptor> source,
            String schema) {
        String normalized = SdkText.trimToEmpty(schema);
        if (normalized.isEmpty()) {
            return source;
        }
        return source.stream()
                .filter(contract -> contract.schema().equals(normalized))
                .toList();
    }

    private static List<WayangContractDescriptor> contractsForEnvelope(
            List<WayangContractDescriptor> source,
            String envelope) {
        String normalized = SdkText.trimToEmpty(envelope);
        if (normalized.isEmpty()) {
            return source;
        }
        return source.stream()
                .filter(contract -> contract.envelope().equals(normalized))
                .toList();
    }

    private static List<WayangContractDescriptor> contractsForCommandId(
            List<WayangContractDescriptor> source,
            String commandId) {
        String normalized = SdkText.trimToEmpty(commandId);
        if (normalized.isEmpty()) {
            return source;
        }
        return source.stream()
                .filter(contract -> contract.commandIds().contains(normalized))
                .toList();
    }

    private static List<WayangContractDescriptor> contractsForDomain(
            List<WayangContractDescriptor> source,
            String domain) {
        String normalized = SdkText.trimToEmpty(domain);
        if (normalized.isEmpty()) {
            return source;
        }
        return source.stream()
                .filter(contract -> contract.domain().equals(normalized))
                .toList();
    }

    private static List<WayangContractDescriptor> contractsForJsonSchemaId(
            List<WayangContractDescriptor> source,
            String jsonSchemaId) {
        String normalized = SdkText.trimToEmpty(jsonSchemaId);
        if (normalized.isEmpty()) {
            return source;
        }
        WayangContractKey key = WayangContractKey.parseJsonSchemaId(normalized).orElse(null);
        return source.stream()
                .filter(contract -> matchesJsonSchemaId(contract, normalized, key))
                .toList();
    }

    private static boolean matchesJsonSchemaId(
            WayangContractDescriptor contract,
            String jsonSchemaId,
            WayangContractKey key) {
        if (key != null && key.matches(contract)) {
            return true;
        }
        return contract.jsonSchemaId().equals(jsonSchemaId);
    }

    private static Map<WayangContractKey, List<WayangContractDescriptor>> groupByKey(
            List<WayangContractDescriptor> contracts) {
        Map<WayangContractKey, List<WayangContractDescriptor>> grouped = new LinkedHashMap<>();
        contracts.forEach(contract -> grouped.computeIfAbsent(contract.key(), ignored -> new ArrayList<>())
                .add(contract));
        if (grouped.isEmpty()) {
            return Map.of();
        }
        Map<WayangContractKey, List<WayangContractDescriptor>> copy = new LinkedHashMap<>();
        grouped.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        return Collections.unmodifiableMap(copy);
    }

    private static Map<WayangContractKey, WayangContractDescriptor> firstByKey(
            Map<WayangContractKey, List<WayangContractDescriptor>> grouped) {
        if (grouped.isEmpty()) {
            return Map.of();
        }
        Map<WayangContractKey, WayangContractDescriptor> values = new LinkedHashMap<>();
        grouped.forEach((key, contracts) -> values.put(key, contracts.get(0)));
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, WayangContractDescriptor> byJsonSchemaId(
            List<WayangContractDescriptor> contracts) {
        Map<String, WayangContractDescriptor> values = new LinkedHashMap<>();
        contracts.forEach(contract -> values.putIfAbsent(contract.jsonSchemaId(), contract));
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }
}
