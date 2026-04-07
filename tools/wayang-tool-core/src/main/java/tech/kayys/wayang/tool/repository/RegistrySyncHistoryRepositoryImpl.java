package tech.kayys.wayang.tool.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.util.List;

@ApplicationScoped
public class RegistrySyncHistoryRepositoryImpl implements PanacheRepository<RegistrySyncHistory>, RegistrySyncHistoryRepository {

    @Override
    public Uni<RegistrySyncHistory> save(RegistrySyncHistory history) {
        return persist(history);
    }

    @Override
    public Uni<List<RegistrySyncHistory>> listByRequestId(String requestId, int limit) {
        return find("requestId = ?1 order by startedAt desc", requestId)
                .page(0, Math.max(1, limit))
                .list();
    }
}

