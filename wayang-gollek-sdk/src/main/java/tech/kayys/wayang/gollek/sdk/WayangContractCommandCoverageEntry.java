package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.List;

public record WayangContractCommandCoverageEntry(
        WayangContractDescriptor contract,
        List<String> declaredCommandIds,
        List<String> linkedCommandIds) {

    public WayangContractCommandCoverageEntry {
        contract = contract == null ? WayangContractDescriptors.empty() : contract;
        declaredCommandIds = SdkLists.copy(declaredCommandIds);
        linkedCommandIds = SdkLists.copy(linkedCommandIds);
    }

    public WayangContractKey key() {
        return contract.key();
    }

    public String schema() {
        return contract.schema();
    }

    public int version() {
        return contract.version();
    }

    public String envelope() {
        return contract.envelope();
    }

    public String domain() {
        return contract.domain();
    }

    public String jsonSchemaId() {
        return contract.jsonSchemaId();
    }

    public boolean commandLinked() {
        return !linkedCommandIds.isEmpty();
    }

    public boolean commandless() {
        return declaredCommandIds.isEmpty() && linkedCommandIds.isEmpty();
    }

    public boolean complete() {
        return unlinkedCommandIds().isEmpty() && undeclaredLinkedCommandIds().isEmpty();
    }

    public List<String> unlinkedCommandIds() {
        return difference(declaredCommandIds, linkedCommandIds);
    }

    public List<String> undeclaredLinkedCommandIds() {
        return difference(linkedCommandIds, declaredCommandIds);
    }

    private static List<String> difference(List<String> source, List<String> known) {
        List<String> missing = new ArrayList<>();
        for (String value : source) {
            if (!known.contains(value)) {
                missing.add(value);
            }
        }
        return missing.isEmpty() ? List.of() : List.copyOf(missing);
    }
}
