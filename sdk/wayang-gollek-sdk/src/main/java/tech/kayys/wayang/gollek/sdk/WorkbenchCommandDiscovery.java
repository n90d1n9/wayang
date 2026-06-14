package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record WorkbenchCommandDiscovery(
        WorkbenchCommandQuery query,
        List<WorkbenchCommand> commands,
        List<String> categories,
        Map<String, Integer> categoryCounts,
        List<String> commandIds,
        int totalCommands) {

    public WorkbenchCommandDiscovery {
        query = query == null ? WorkbenchCommandQuery.all() : query;
        commands = SdkLists.copy(commands);
        categories = SdkLists.copy(categories);
        categoryCounts = SdkCounts.copyPositiveTextKeys(categoryCounts);
        commandIds = SdkLists.copy(commandIds);
        totalCommands = Math.max(0, totalCommands);
    }

    public static WorkbenchCommandDiscovery of(
            WorkbenchCommandQuery query,
            List<WorkbenchCommand> commands,
            int totalCommands) {
        List<WorkbenchCommand> matches = SdkLists.copy(commands);
        WorkbenchCommandIndex index = WorkbenchCommandIndex.of(matches);
        return new WorkbenchCommandDiscovery(
                query,
                matches,
                index.categories(),
                index.categoryCounts(),
                index.commandIds(),
                totalCommands);
    }

    public int matchingCommands() {
        return commands.size();
    }

    public boolean empty() {
        return commands.isEmpty();
    }

    public List<WorkbenchCommandCategorySummary> categorySummaries() {
        return index().categorySummaries();
    }

    public List<String> contractJsonSchemaIds() {
        return index().contractJsonSchemaIds();
    }

    public Map<String, Integer> contractJsonSchemaIdCounts() {
        return index().contractJsonSchemaIdCounts();
    }

    public List<WayangContractKey> contractKeys() {
        return index().contractKeys();
    }

    public Map<WayangContractKey, Integer> contractKeyCounts() {
        return index().contractKeyCounts();
    }

    public List<WorkbenchCommandContractSummary> contractSummaries() {
        return index().contractSummaries();
    }

    public List<WorkbenchCommandContractSummary> contractSummariesForKey(WayangContractKey key) {
        return index().contractSummariesForKey(key);
    }

    public Optional<WorkbenchCommandContractSummary> contractSummaryByJsonSchemaId(String jsonSchemaId) {
        return index().contractSummaryByJsonSchemaId(jsonSchemaId);
    }

    public List<String> commandIdsForContractKey(WayangContractKey key) {
        return index().commandIdsForContractKey(key);
    }

    private WorkbenchCommandIndex index() {
        return WorkbenchCommandIndex.of(commands);
    }
}
