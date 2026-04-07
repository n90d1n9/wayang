package tech.kayys.wayang.tool.repository;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.util.List;

public interface McpServerRegistryRepository {

    Uni<McpServerRegistry> findByRequestIdAndName(String requestId, String name);

    Uni<List<McpServerRegistry>> listByRequestId(String requestId);

    Uni<List<McpServerRegistry>> listScheduledCandidates();

    Uni<McpServerRegistry> save(McpServerRegistry entity);

    Uni<Boolean> deleteByRequestIdAndName(String requestId, String name);
}
