package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.service.EditionModeService;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class McpServerRegistryInspectionService {

    @Inject
    McpServerRegistryRepository serverRegistryRepository;

    @Inject
    EditionModeService editionModeService;

    public Uni<List<McpServerRegistryEntry>> list(
            String requestId,
            Boolean enabled,
            String transport) {
        if (!isAvailable()) {
            return Uni.createFrom().item(List.of());
        }
        return serverRegistryRepository.listByRequestId(requestId)
                .map(servers -> servers.stream()
                        .filter(server -> matchesEnabled(server, enabled))
                        .filter(server -> matchesTransport(server, transport))
                        .sorted(Comparator.comparing(server -> sortKey(server.getName())))
                        .map(McpServerRegistryEntries::from)
                        .toList());
    }

    public Uni<McpServerRegistryEntry> get(String requestId, String serverName) {
        if (!isAvailable() || serverName == null || serverName.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        return serverRegistryRepository.findByRequestIdAndName(requestId, serverName)
                .map(server -> server == null ? null : McpServerRegistryEntries.from(server));
    }

    private boolean isAvailable() {
        return serverRegistryRepository != null
                && (editionModeService == null || editionModeService.supportsMcpRegistryDatabase());
    }

    private boolean matchesEnabled(McpServerRegistry server, Boolean enabled) {
        return enabled == null || server.isEnabled() == enabled;
    }

    private boolean matchesTransport(McpServerRegistry server, String transport) {
        return McpServerTransports.matches(server.getTransport(), transport);
    }

    private String sortKey(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
