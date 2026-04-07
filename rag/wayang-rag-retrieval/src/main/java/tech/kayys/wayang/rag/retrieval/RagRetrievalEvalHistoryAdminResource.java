package tech.kayys.wayang.rag.retrieval;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.AdminProtected;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/admin/eval/retrieval")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@AdminProtected
public class RagRetrievalEvalHistoryAdminResource {

    @Inject
    RagRetrievalEvalHistoryService service;

    @GET
    @Path("/history")
    public List<RagRetrievalEvalRun> history(
            @QueryParam("tenantId") String tenantId,
            @QueryParam("datasetName") String datasetName,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        return service.history(tenantId, datasetName, limit);
    }

    @GET
    @Path("/trend")
    public RagRetrievalEvalTrendResponse trend(
            @QueryParam("tenantId") String tenantId,
            @QueryParam("datasetName") String datasetName,
            @QueryParam("window") @DefaultValue("20") int window) {
        return service.trend(tenantId, datasetName, window);
    }
}
