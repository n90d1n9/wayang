package tech.kayys.wayang.tool.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.dto.RegistryScheduleResponse;
import tech.kayys.wayang.tool.dto.RegistrySyncHistoryResponse;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.OpenApiSourceRepository;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RegistryScheduleService {

    @Inject
    OpenApiSourceRepository openApiSourceRepository;

    @Inject
    McpServerRegistryRepository mcpServerRegistryRepository;

    @Inject
    RegistrySyncHistoryRepository historyRepository;

    @Inject
    EditionModeService editionModeService;

    public Uni<RegistryScheduleResponse> setOpenApiSchedule(String requestId, UUID sourceId, String interval) {
        RegistrySyncService.parseInterval(interval);
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() ->
                openApiSourceRepository.findByRequestIdAndSourceId(requestId, sourceId)
                        .flatMap(source -> {
                            if (source == null) {
                                return Uni.createFrom().nullItem();
                            }
                            source.setSyncSchedule(interval);
                            source.setUpdatedAt(java.time.Instant.now());
                            return openApiSourceRepository.save(source)
                                    .map(saved -> new RegistryScheduleResponse(
                                            "OPENAPI",
                                            String.valueOf(saved.getSourceId()),
                                            saved.getSyncSchedule(),
                                            saved.getLastSyncAt()));
                        }));
    }

    public Uni<RegistryScheduleResponse> setMcpSchedule(String requestId, String serverName, String interval) {
        if (!editionModeService.supportsMcpRegistryDatabase()) {
            return Uni.createFrom().failure(new jakarta.ws.rs.ForbiddenException(
                    "MCP registry database mode is enterprise-only. Use file-based MCP config in community mode."));
        }
        RegistrySyncService.parseInterval(interval);
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() ->
                mcpServerRegistryRepository.findByRequestIdAndName(requestId, serverName)
                        .flatMap(server -> {
                            if (server == null) {
                                return Uni.createFrom().nullItem();
                            }
                            server.setSyncSchedule(interval);
                            server.setUpdatedAt(java.time.Instant.now());
                            return mcpServerRegistryRepository.save(server)
                                    .map(saved -> new RegistryScheduleResponse(
                                            "MCP",
                                            saved.getName(),
                                            saved.getSyncSchedule(),
                                            saved.getLastSyncAt()));
                        }));
    }

    public Uni<List<RegistrySyncHistoryResponse>> listHistory(String requestId, int limit) {
        return historyRepository.listByRequestId(requestId, Math.max(1, limit))
                .map(items -> items.stream()
                        .map(item -> new RegistrySyncHistoryResponse(
                                item.getSourceKind(),
                                item.getSourceRef(),
                                item.getStatus(),
                                item.getMessage(),
                                item.getItemsAffected(),
                                item.getStartedAt(),
                                item.getFinishedAt()))
                        .toList());
    }
}
