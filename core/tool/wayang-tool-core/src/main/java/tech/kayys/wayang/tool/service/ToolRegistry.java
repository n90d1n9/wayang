package tech.kayys.wayang.tool.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.entity.McpTool;
import tech.kayys.wayang.tool.repository.ToolRepository;

@ApplicationScoped
public class ToolRegistry {

    @Inject
    ToolRepository toolRepository;

    public Uni<McpTool> resolveTool(String toolId, String requestId) {
        return toolRepository.findByRequestIdAndToolId(requestId, toolId);
    }
}
