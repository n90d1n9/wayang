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

@Path("/admin/eval/retrieval/guardrails")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@AdminProtected
public class RagRetrievalEvalGuardrailAdminResource {

    @Inject
    RagRetrievalEvalGuardrailService service;

    @GET
    public RagRetrievalEvalGuardrailStatus evaluate(
            @QueryParam("tenantId") String tenantId,
            @QueryParam("datasetName") String datasetName,
            @QueryParam("window") @DefaultValue("0") int window) {
        Integer override = window <= 0 ? null : window;
        return service.evaluate(tenantId, datasetName, override);
    }
}
