package tech.kayys.wayang.tool.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.tool.entity.OpenApiSource;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OpenApiSourceRepositoryImpl implements PanacheRepository<OpenApiSource>, OpenApiSourceRepository {

    @Override
    public Uni<OpenApiSource> save(OpenApiSource source) {
        return persist(source);
    }

    @Override
    public Uni<OpenApiSource> findById(UUID sourceId) {
        return find("sourceId", sourceId).firstResult();
    }

    @Override
    public Uni<List<OpenApiSource>> listSyncCandidates(String requestId) {
        return find("requestId = ?1 and enabled = true and sourceType in (?2, ?3, ?4)",
                requestId,
                tech.kayys.wayang.tool.dto.SourceType.URL,
                tech.kayys.wayang.tool.dto.SourceType.OPENAPI_3_URL,
                tech.kayys.wayang.tool.dto.SourceType.SWAGGER_2_URL).list();
    }

    @Override
    public Uni<OpenApiSource> findByRequestIdAndSourceId(String requestId, UUID sourceId) {
        return find("requestId = ?1 and sourceId = ?2", requestId, sourceId).firstResult();
    }

    @Override
    public Uni<List<OpenApiSource>> listScheduledCandidates() {
        return find("enabled = true and syncSchedule is not null").list();
    }
}
