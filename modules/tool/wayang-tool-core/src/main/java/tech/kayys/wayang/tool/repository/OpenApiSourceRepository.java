package tech.kayys.wayang.tool.repository;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.OpenApiSource;

import java.util.List;
import java.util.UUID;

public interface OpenApiSourceRepository {

    Uni<OpenApiSource> save(OpenApiSource source);

    Uni<OpenApiSource> findById(UUID sourceId);

    Uni<List<OpenApiSource>> listSyncCandidates(String requestId);

    Uni<OpenApiSource> findByRequestIdAndSourceId(String requestId, UUID sourceId);

    Uni<List<OpenApiSource>> listScheduledCandidates();
}
