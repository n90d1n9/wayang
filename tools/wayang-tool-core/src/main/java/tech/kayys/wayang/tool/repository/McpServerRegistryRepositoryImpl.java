package tech.kayys.wayang.tool.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.util.List;

@ApplicationScoped
public class McpServerRegistryRepositoryImpl implements PanacheRepository<McpServerRegistry>, McpServerRegistryRepository {

    @Override
    public Uni<McpServerRegistry> findByRequestIdAndName(String requestId, String name) {
        return find("requestId = ?1 and name = ?2", requestId, name).firstResult();
    }

    @Override
    public Uni<List<McpServerRegistry>> listByRequestId(String requestId) {
        return find("requestId", requestId).list();
    }

    @Override
    public Uni<List<McpServerRegistry>> listScheduledCandidates() {
        return find("enabled = true and syncSchedule is not null").list();
    }

    @Override
    public Uni<McpServerRegistry> save(McpServerRegistry entity) {
        return persist(entity);
    }

    @Override
    public Uni<Boolean> deleteByRequestIdAndName(String requestId, String name) {
        return delete("requestId = ?1 and name = ?2", requestId, name)
                .map(count -> count > 0);
    }
}
