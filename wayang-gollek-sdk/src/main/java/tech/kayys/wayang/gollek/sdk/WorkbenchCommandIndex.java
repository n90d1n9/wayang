package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WorkbenchCommandIndex {

    private final List<WorkbenchCommand> commands;
    private final Map<String, WorkbenchCommand> commandsById;
    private final Map<String, List<WorkbenchCommand>> commandsByCategory;

    private WorkbenchCommandIndex(List<WorkbenchCommand> commands) {
        this.commands = SdkLists.copy(commands);
        this.commandsById = byId(this.commands);
        this.commandsByCategory = byCategory(this.commands);
    }

    public static WorkbenchCommandIndex of(List<WorkbenchCommand> commands) {
        return new WorkbenchCommandIndex(commands);
    }

    public List<WorkbenchCommand> commands() {
        return commands;
    }

    public List<String> commandIds() {
        return SdkFacets.values(commands, WorkbenchCommand::id);
    }

    public List<String> categories() {
        return SdkFacets.values(commands, WorkbenchCommand::category);
    }

    public Map<String, Integer> categoryCounts() {
        return SdkFacets.counts(commands, WorkbenchCommand::category);
    }

    public List<WorkbenchCommandCategorySummary> categorySummaries() {
        return commandsByCategory.entrySet().stream()
                .map(entry -> new WorkbenchCommandCategorySummary(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(WorkbenchCommand::id)
                                .toList()))
                .toList();
    }

    public Map<String, WorkbenchCommand> commandsById() {
        return commandsById;
    }

    public Map<String, List<WorkbenchCommand>> commandsByCategory() {
        return commandsByCategory;
    }

    public List<WorkbenchCommand> commandsForQuery(WorkbenchCommandQuery query) {
        WorkbenchCommandQuery normalized = query == null ? WorkbenchCommandQuery.all() : query;
        List<WorkbenchCommand> filtered = commands;
        String resolvedSurfaceId = normalized.resolvedSurfaceId();
        if (resolvedSurfaceId != null) {
            filtered = commandsForSurface(filtered, resolvedSurfaceId);
        }
        if (normalized.hasCategory()) {
            filtered = commandsForCategory(filtered, normalized.category());
        }
        if (normalized.hasCommandId()) {
            filtered = commandsForId(filtered, normalized.commandId());
        }
        if (normalized.hasContractJsonSchemaId()) {
            filtered = commandsForContractJsonSchemaId(filtered, normalized.contractJsonSchemaId());
        }
        return filtered;
    }

    public List<WorkbenchCommand> commandsForSurface(String surfaceId) {
        return commandsForSurface(commands, surfaceId);
    }

    public List<WorkbenchCommand> commandsForCategory(String category) {
        return commandsForCategory(commands, category);
    }

    public List<WorkbenchCommand> commandsForId(String commandId) {
        return commandsForId(commands, commandId);
    }

    public Optional<WorkbenchCommand> findCommand(String commandId) {
        String normalized = requireNonBlank("Wayang command id", commandId);
        return Optional.ofNullable(commandsById.get(normalized));
    }

    public List<WorkbenchCommand> commandsForContractKey(WayangContractKey key) {
        return commandsForContractKey(commands, key);
    }

    public List<WorkbenchCommand> commandsForContractJsonSchemaId(String jsonSchemaId) {
        return commandsForContractJsonSchemaId(commands, jsonSchemaId);
    }

    private List<WorkbenchCommand> commandsForSurface(List<WorkbenchCommand> source, String surfaceId) {
        String normalized = WayangProductCatalog.requireKnownSurfaceId(surfaceId);
        return source.stream()
                .filter(command -> supportsSurface(command, normalized))
                .toList();
    }

    private List<WorkbenchCommand> commandsForCategory(List<WorkbenchCommand> source, String category) {
        String normalized = requireNonBlank("Wayang command category", category);
        List<WorkbenchCommand> matches = source.stream()
                .filter(command -> command.category().equalsIgnoreCase(normalized))
                .toList();
        if (!matches.isEmpty()) {
            return matches;
        }
        throw new IllegalArgumentException("Unknown Wayang command category '" + normalized
                + "'. Known categories: "
                + String.join(", ", SdkFacets.values(source, WorkbenchCommand::category)));
    }

    private List<WorkbenchCommand> commandsForId(List<WorkbenchCommand> source, String commandId) {
        String normalized = requireNonBlank("Wayang command id", commandId);
        return List.of(source.stream()
                .filter(command -> command.id().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Wayang command id '" + normalized
                        + "'. Known command ids: "
                        + String.join(", ", SdkFacets.values(source, WorkbenchCommand::id)))));
    }

    private List<WorkbenchCommand> commandsForContractKey(List<WorkbenchCommand> source, WayangContractKey key) {
        if (key == null) {
            return List.of();
        }
        return source.stream()
                .filter(command -> command.contracts().stream().anyMatch(key::matches))
                .toList();
    }

    private List<WorkbenchCommand> commandsForContractJsonSchemaId(
            List<WorkbenchCommand> source,
            String jsonSchemaId) {
        String normalized = SdkText.trimToEmpty(jsonSchemaId);
        if (normalized.isEmpty()) {
            return List.of();
        }
        Optional<WayangContractKey> key = WayangContractKey.parseJsonSchemaId(normalized);
        return source.stream()
                .filter(command -> command.contracts().stream()
                        .anyMatch(contract -> matchesContractJsonSchemaId(contract, normalized, key.orElse(null))))
                .toList();
    }

    public List<String> contractJsonSchemaIds() {
        Map<String, Integer> counts = contractJsonSchemaIdCounts();
        return counts.isEmpty() ? List.of() : List.copyOf(counts.keySet());
    }

    public Map<String, Integer> contractJsonSchemaIdCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (WorkbenchCommand command : commands) {
            for (WorkbenchCommandContract contract : command.contracts()) {
                counts.merge(contract.jsonSchemaId(), 1, Integer::sum);
            }
        }
        return SdkCounts.copyPositiveTextKeys(counts);
    }

    public List<WayangContractKey> contractKeys() {
        Map<WayangContractKey, Integer> counts = contractKeyCounts();
        return counts.isEmpty() ? List.of() : List.copyOf(counts.keySet());
    }

    public Map<WayangContractKey, Integer> contractKeyCounts() {
        Map<WayangContractKey, Integer> counts = new LinkedHashMap<>();
        for (WorkbenchCommand command : commands) {
            for (WorkbenchCommandContract contract : command.contracts()) {
                counts.merge(contract.key(), 1, Integer::sum);
            }
        }
        return SdkCounts.copyPositiveKeys(counts);
    }

    public List<WorkbenchCommandContractSummary> contractSummaries() {
        Map<String, ContractAccumulator> summaries = new LinkedHashMap<>();
        for (WorkbenchCommand command : commands) {
            for (WorkbenchCommandContract contract : command.contracts()) {
                summaries.computeIfAbsent(contract.jsonSchemaId(), ignored -> new ContractAccumulator(contract))
                        .addCommandId(command.id());
            }
        }
        return summaries.values().stream()
                .map(ContractAccumulator::toSummary)
                .toList();
    }

    public List<WorkbenchCommandContractSummary> contractSummariesForKey(WayangContractKey key) {
        if (key == null) {
            return List.of();
        }
        return contractSummaries().stream()
                .filter(summary -> summary.key().equals(key))
                .toList();
    }

    public Optional<WorkbenchCommandContractSummary> contractSummaryByJsonSchemaId(String jsonSchemaId) {
        String normalized = SdkText.trimToEmpty(jsonSchemaId);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        Optional<WayangContractKey> key = WayangContractKey.parseJsonSchemaId(normalized);
        List<WorkbenchCommandContractSummary> summaries = contractSummaries();
        if (key.isPresent()) {
            Optional<WorkbenchCommandContractSummary> exactKeyMatch = summaries.stream()
                    .filter(summary -> summary.key().equals(key.get()))
                    .filter(summary -> summary.jsonSchemaId().equals(normalized))
                    .findFirst();
            if (exactKeyMatch.isPresent()) {
                return exactKeyMatch;
            }
            Optional<WorkbenchCommandContractSummary> keyMatch = summaries.stream()
                    .filter(summary -> summary.key().equals(key.get()))
                    .findFirst();
            if (keyMatch.isPresent()) {
                return keyMatch;
            }
        }
        return summaries.stream()
                .filter(summary -> summary.jsonSchemaId().equals(normalized))
                .findFirst();
    }

    public List<String> commandIdsForContractKey(WayangContractKey key) {
        if (key == null) {
            return List.of();
        }
        return SdkFacets.flatValues(contractSummariesForKey(key), WorkbenchCommandContractSummary::commandIds);
    }

    private static Map<String, WorkbenchCommand> byId(List<WorkbenchCommand> commands) {
        Map<String, WorkbenchCommand> values = new LinkedHashMap<>();
        commands.forEach(command -> values.putIfAbsent(command.id(), command));
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }

    private static Map<String, List<WorkbenchCommand>> byCategory(List<WorkbenchCommand> commands) {
        Map<String, List<WorkbenchCommand>> grouped = new LinkedHashMap<>();
        commands.forEach(command -> grouped.computeIfAbsent(command.category(), ignored -> new ArrayList<>())
                .add(command));
        if (grouped.isEmpty()) {
            return Map.of();
        }
        Map<String, List<WorkbenchCommand>> copy = new LinkedHashMap<>();
        grouped.forEach((category, values) -> copy.put(category, List.copyOf(values)));
        return Collections.unmodifiableMap(copy);
    }

    private static boolean supportsSurface(WorkbenchCommand command, String surfaceId) {
        return command.surfaceIds().isEmpty() || command.surfaceIds().contains(surfaceId);
    }

    private static boolean matchesContractJsonSchemaId(
            WorkbenchCommandContract contract,
            String jsonSchemaId,
            WayangContractKey key) {
        if (key != null && key.matches(contract)) {
            return true;
        }
        return contract.jsonSchemaId().equals(jsonSchemaId);
    }

    private static String requireNonBlank(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    private static final class ContractAccumulator {
        private final WorkbenchCommandContract contract;
        private final List<String> commandIds = new ArrayList<>();

        private ContractAccumulator(WorkbenchCommandContract contract) {
            this.contract = contract;
        }

        private void addCommandId(String commandId) {
            if (!commandIds.contains(commandId)) {
                commandIds.add(commandId);
            }
        }

        private WorkbenchCommandContractSummary toSummary() {
            return new WorkbenchCommandContractSummary(
                    contract.jsonSchemaId(),
                    contract.schema(),
                    contract.version(),
                    contract.envelope(),
                    commandIds.size(),
                    commandIds);
        }
    }
}
