package tech.kayys.wayang.rag.embedding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/admin/embedding/schema")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AdminProtected
public class EmbeddingSchemaAdminResource {

    @Inject
    EmbeddingSchemaAdminService service;

    @Inject
    EmbeddingSchemaHistoryCompactorJob compactorJob;

    @GET
    @Path("/{tenantId}")
    public EmbeddingSchemaContract status(@PathParam("tenantId") String tenantId) {
        return service.status(tenantId);
    }

    @GET
    @Path("/{tenantId}/history")
    public List<EmbeddingSchemaMigrationStatus> history(
            @PathParam("tenantId") String tenantId,
            @QueryParam("limit") Integer limit) {
        return service.history(tenantId, limit == null ? 20 : limit);
    }

    @POST
    @Path("/{tenantId}/history/compact")
    public EmbeddingSchemaHistoryCompactionStatus compactHistory(
            @PathParam("tenantId") String tenantId,
            EmbeddingSchemaHistoryCompactionRequest request) {
        return service.compactHistory(tenantId, request);
    }

    @GET
    @Path("/history/compaction/status")
    public EmbeddingSchemaHistoryCompactorStatus compactionStatus() {
        return compactorJob.status();
    }

    @POST
    @Path("/migrate")
    public EmbeddingSchemaMigrationStatus migrate(EmbeddingSchemaMigrationRequest request) {
        return service.migrate(request);
    }
}
