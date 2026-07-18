package tech.kayys.wayang.tool.mcp;

import java.util.List;

public record McpServerLifecycleImpact(
        McpServerRegistryEntry server,
        List<String> importedToolIds,
        List<String> activeToolIds,
        List<String> staleToolIds,
        List<String> serverDisabledToolIds,
        List<String> retiredToolIds,
        List<String> disableAffectedToolIds,
        List<String> enableAffectedToolIds,
        List<String> retireAffectedToolIds) {

    public McpServerLifecycleImpact {
        importedToolIds = copy(importedToolIds);
        activeToolIds = copy(activeToolIds);
        staleToolIds = copy(staleToolIds);
        serverDisabledToolIds = copy(serverDisabledToolIds);
        retiredToolIds = copy(retiredToolIds);
        disableAffectedToolIds = copy(disableAffectedToolIds);
        enableAffectedToolIds = copy(enableAffectedToolIds);
        retireAffectedToolIds = copy(retireAffectedToolIds);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
