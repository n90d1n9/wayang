package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Optional;

public record WayangContractCommandCoverageReport(
        int totalContracts,
        int totalCommands,
        List<WayangContractCommandCoverageEntry> entries) {

    public WayangContractCommandCoverageReport {
        totalContracts = Math.max(0, totalContracts);
        totalCommands = Math.max(0, totalCommands);
        entries = SdkLists.copy(entries);
    }

    public int commandLinkedContracts() {
        return (int) entries.stream()
                .filter(WayangContractCommandCoverageEntry::commandLinked)
                .count();
    }

    public int commandlessContracts() {
        return (int) entries.stream()
                .filter(WayangContractCommandCoverageEntry::commandless)
                .count();
    }

    public int incompleteContracts() {
        return (int) entries.stream()
                .filter(entry -> !entry.complete())
                .count();
    }

    public int commandContractLinks() {
        return entries.stream()
                .mapToInt(entry -> entry.linkedCommandIds().size())
                .sum();
    }

    public List<WayangContractCommandCoverageEntry> commandLinkedEntries() {
        return entries.stream()
                .filter(WayangContractCommandCoverageEntry::commandLinked)
                .toList();
    }

    public List<WayangContractCommandCoverageEntry> commandlessEntries() {
        return entries.stream()
                .filter(WayangContractCommandCoverageEntry::commandless)
                .toList();
    }

    public List<WayangContractCommandCoverageEntry> incompleteEntries() {
        return entries.stream()
                .filter(entry -> !entry.complete())
                .toList();
    }

    public Optional<WayangContractCommandCoverageEntry> entryForKey(WayangContractKey key) {
        if (key == null) {
            return Optional.empty();
        }
        return entries.stream()
                .filter(entry -> entry.key().equals(key))
                .findFirst();
    }

    public Optional<WayangContractCommandCoverageEntry> entryForJsonSchemaId(String jsonSchemaId) {
        String normalized = SdkText.trimToEmpty(jsonSchemaId);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return WayangContractKey.parseJsonSchemaId(normalized)
                .flatMap(this::entryForKey)
                .or(() -> entries.stream()
                        .filter(entry -> entry.jsonSchemaId().equals(normalized))
                        .findFirst());
    }
}
