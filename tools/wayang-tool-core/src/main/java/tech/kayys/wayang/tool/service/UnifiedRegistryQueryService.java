package tech.kayys.wayang.tool.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.dto.McpServerRegistryResponse;
import tech.kayys.wayang.tool.dto.ToolMetadataResponse;
import tech.kayys.wayang.tool.dto.UnifiedRegistrySnapshotResponse;
import tech.kayys.wayang.tool.entity.McpTool;
import tech.kayys.wayang.tool.repository.ToolRepository;

import java.util.List;

@ApplicationScoped
public class UnifiedRegistryQueryService {

    @Inject
    ToolRepository toolRepository;

    @Inject
    McpRegistryService mcpRegistryService;

    @Inject
    EditionModeService editionModeService;

    public Uni<UnifiedRegistrySnapshotResponse> snapshot(String requestId, String namespace) {
        Uni<List<McpTool>> toolsUni = namespace == null || namespace.isBlank()
                ? toolRepository.findByRequestId(requestId)
                : toolRepository.findByRequestIdAndNamespace(requestId, namespace);

        Uni<List<McpServerRegistryResponse>> mcpUni = editionModeService.supportsMcpRegistryDatabase()
                ? mcpRegistryService.listServers(requestId)
                : Uni.createFrom().item(List.of());

        return Uni.combine().all().unis(toolsUni, mcpUni).asTuple()
                .map(tuple -> {
                    List<ToolMetadataResponse> tools = tuple.getItem1().stream()
                            .map(this::mapTool)
                            .toList();
                    return new UnifiedRegistrySnapshotResponse(tools, tuple.getItem2());
                });
    }

    private ToolMetadataResponse mapTool(McpTool tool) {
        String capability = tool.getCapabilityLevel() != null ? tool.getCapabilityLevel().name() : null;
        return new ToolMetadataResponse(
                tool.getToolId(),
                tool.getName(),
                tool.getDescription(),
                tool.getCapabilities(),
                capability,
                tool.isReadOnly(),
                tool.getTags());
    }
}
