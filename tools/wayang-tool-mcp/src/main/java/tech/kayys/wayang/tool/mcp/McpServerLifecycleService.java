package tech.kayys.wayang.tool.mcp;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.ToolRepository;
import tech.kayys.wayang.tool.service.EditionModeService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class McpServerLifecycleService {

    static final String SERVER_DISABLED_TAG = McpToolLifecycle.SERVER_DISABLED_TAG;
    static final String SERVER_RETIRED_TAG = McpToolLifecycle.SERVER_RETIRED_TAG;

    @Inject
    McpServerRegistryRepository serverRegistryRepository;

    @Inject
    ToolRepository toolRepository;

    @Inject
    EditionModeService editionModeService;

    @WithTransaction
    public Uni<McpServerLifecycleResult> setEnabled(
            String requestId,
            String serverName,
            boolean enabled) {
        if (!isAvailable() || serverName == null || serverName.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        return serverRegistryRepository.findByRequestIdAndName(requestId, serverName)
                .flatMap(server -> {
                    if (server == null) {
                        return Uni.createFrom().nullItem();
                    }
                    server.setEnabled(enabled);
                    server.setUpdatedAt(Instant.now());
                    return cascadeToolState(requestId, serverName, enabled)
                            .flatMap(changes -> serverRegistryRepository.update(server)
                                    .map(updated -> new McpServerLifecycleResult(
                                            McpServerRegistryEntries.from(updated),
                                            changes.disabledToolIds(),
                                            changes.reactivatedToolIds())));
                });
    }

    public Uni<McpServerLifecycleImpact> impact(String requestId, String serverName) {
        if (!isAvailable() || serverName == null || serverName.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        return serverRegistryRepository.findByRequestIdAndName(requestId, serverName)
                .flatMap(server -> {
                    if (server == null) {
                        return Uni.createFrom().nullItem();
                    }
                    if (toolRepository == null) {
                        return Uni.createFrom().item(emptyImpact(server));
                    }
                    return toolRepository.findByRequestId(requestId)
                            .map(tools -> impact(server, serverName, tools));
                });
    }

    @WithTransaction
    public Uni<McpServerRetirementResult> retire(
            String requestId,
            String serverName) {
        if (!isAvailable() || serverName == null || serverName.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        return serverRegistryRepository.findByRequestIdAndName(requestId, serverName)
                .flatMap(server -> {
                    if (server == null) {
                        return Uni.createFrom().nullItem();
                    }
                    McpServerRegistryEntry entry = McpServerRegistryEntries.from(server);
                    return retireMatchingTools(requestId, serverName)
                            .flatMap(retiredToolIds -> serverRegistryRepository
                                    .deleteByRequestIdAndName(requestId, serverName)
                                    .map(deleted -> deleted
                                            ? new McpServerRetirementResult(entry, retiredToolIds)
                                            : null));
                });
    }

    private McpServerLifecycleImpact emptyImpact(McpServerRegistry server) {
        return new McpServerLifecycleImpact(
                McpServerRegistryEntries.from(server),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private McpServerLifecycleImpact impact(
            McpServerRegistry server,
            String serverName,
            List<tech.kayys.wayang.tool.entity.McpTool> tools) {
        LifecycleImpactAccumulator impact = new LifecycleImpactAccumulator();
        tools.stream()
                .filter(tool -> belongsToServer(tool, serverName))
                .sorted(Comparator.comparing(tool -> sortKey(tool.getToolId())))
                .forEach(impact::add);
        return impact.snapshot(McpServerRegistryEntries.from(server));
    }

    private boolean isAvailable() {
        return serverRegistryRepository != null
                && (editionModeService == null || editionModeService.supportsMcpRegistryDatabase());
    }

    private Uni<LifecycleToolChanges> cascadeToolState(
            String requestId,
            String serverName,
            boolean enabled) {
        if (toolRepository == null) {
            return Uni.createFrom().item(LifecycleToolChanges.empty());
        }
        return toolRepository.findByRequestId(requestId)
                .flatMap(tools -> updateMatchingTools(serverName, enabled, tools));
    }

    private Uni<LifecycleToolChanges> updateMatchingTools(
            String serverName,
            boolean enabled,
            List<tech.kayys.wayang.tool.entity.McpTool> tools) {
        Uni<MutableLifecycleToolChanges> chain = Uni.createFrom().item(new MutableLifecycleToolChanges());
        for (tech.kayys.wayang.tool.entity.McpTool tool : tools) {
            if (!belongsToServer(tool, serverName) || !shouldUpdateTool(tool, enabled)) {
                continue;
            }
            chain = chain.flatMap(changes -> {
                applyToolState(tool, enabled);
                return toolRepository.update(tool)
                        .map(updated -> {
                            changes.add(updated.getToolId(), enabled);
                            return changes;
                        });
            });
        }
        return chain.map(MutableLifecycleToolChanges::snapshot);
    }

    private Uni<List<String>> retireMatchingTools(String requestId, String serverName) {
        if (toolRepository == null) {
            return Uni.createFrom().item(List.of());
        }
        return toolRepository.findByRequestId(requestId)
                .flatMap(tools -> {
                    Uni<List<String>> chain = Uni.createFrom().item(new ArrayList<String>());
                    for (tech.kayys.wayang.tool.entity.McpTool tool : tools) {
                        if (!belongsToServer(tool, serverName) || McpToolLifecycle.isRetired(tool)) {
                            continue;
                        }
                        chain = chain.flatMap(retiredToolIds -> {
                            retireTool(tool);
                            return toolRepository.update(tool)
                                    .map(updated -> {
                                        retiredToolIds.add(updated.getToolId());
                                        return retiredToolIds;
                                    });
                        });
                    }
                    return chain.map(List::copyOf);
                });
    }

    private boolean shouldUpdateTool(tech.kayys.wayang.tool.entity.McpTool tool, boolean enabled) {
        return enabled ? McpToolLifecycle.isServerDisabled(tool) && !McpToolLifecycle.isStale(tool) : tool.isEnabled();
    }

    private void applyToolState(tech.kayys.wayang.tool.entity.McpTool tool, boolean enabled) {
        tool.setEnabled(enabled);
        tool.setUpdatedAt(Instant.now());
        tool.setTags(enabled
                ? McpToolLifecycle.withoutValue(tool.getTags(), SERVER_DISABLED_TAG)
                : McpToolLifecycle.withValue(tool.getTags(), SERVER_DISABLED_TAG));
    }

    private void retireTool(tech.kayys.wayang.tool.entity.McpTool tool) {
        tool.setEnabled(false);
        tool.setUpdatedAt(Instant.now());
        tool.setTags(McpToolLifecycle.withValues(
                McpToolLifecycle.withoutValue(tool.getTags(), SERVER_DISABLED_TAG),
                McpToolLifecycle.STALE_TAG,
                SERVER_RETIRED_TAG));
        tool.setCapabilities(McpToolLifecycle.withValues(tool.getCapabilities(), McpToolLifecycle.STALE_TAG));
    }

    private boolean belongsToServer(tech.kayys.wayang.tool.entity.McpTool tool, String serverName) {
        return McpToolLifecycle.belongsToServer(tool, serverName);
    }

    private String sortKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record LifecycleToolChanges(
            List<String> disabledToolIds,
            List<String> reactivatedToolIds) {

        private static LifecycleToolChanges empty() {
            return new LifecycleToolChanges(List.of(), List.of());
        }
    }

    private static final class MutableLifecycleToolChanges {
        private final List<String> disabledToolIds = new ArrayList<>();
        private final List<String> reactivatedToolIds = new ArrayList<>();

        private void add(String toolId, boolean enabled) {
            if (enabled) {
                reactivatedToolIds.add(toolId);
            } else {
                disabledToolIds.add(toolId);
            }
        }

        private LifecycleToolChanges snapshot() {
            return new LifecycleToolChanges(
                    List.copyOf(disabledToolIds),
                    List.copyOf(reactivatedToolIds));
        }
    }

    private final class LifecycleImpactAccumulator {
        private final List<String> importedToolIds = new ArrayList<>();
        private final List<String> activeToolIds = new ArrayList<>();
        private final List<String> staleToolIds = new ArrayList<>();
        private final List<String> serverDisabledToolIds = new ArrayList<>();
        private final List<String> retiredToolIds = new ArrayList<>();
        private final List<String> disableAffectedToolIds = new ArrayList<>();
        private final List<String> enableAffectedToolIds = new ArrayList<>();
        private final List<String> retireAffectedToolIds = new ArrayList<>();

        private void add(tech.kayys.wayang.tool.entity.McpTool tool) {
            String toolId = tool.getToolId();
            importedToolIds.add(toolId);
            if (tool.isEnabled()) {
                activeToolIds.add(toolId);
                disableAffectedToolIds.add(toolId);
            }
            if (McpToolLifecycle.isStale(tool)) {
                staleToolIds.add(toolId);
            }
            if (McpToolLifecycle.isServerDisabled(tool)) {
                serverDisabledToolIds.add(toolId);
                if (!McpToolLifecycle.isStale(tool)) {
                    enableAffectedToolIds.add(toolId);
                }
            }
            if (McpToolLifecycle.isRetired(tool)) {
                retiredToolIds.add(toolId);
            } else {
                retireAffectedToolIds.add(toolId);
            }
        }

        private McpServerLifecycleImpact snapshot(McpServerRegistryEntry server) {
            return new McpServerLifecycleImpact(
                    server,
                    importedToolIds,
                    activeToolIds,
                    staleToolIds,
                    serverDisabledToolIds,
                    retiredToolIds,
                    disableAffectedToolIds,
                    enableAffectedToolIds,
                    retireAffectedToolIds);
        }
    }
}
