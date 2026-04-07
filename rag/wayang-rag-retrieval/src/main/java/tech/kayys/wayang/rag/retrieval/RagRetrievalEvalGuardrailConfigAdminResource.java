package tech.kayys.wayang.rag.retrieval;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.AdminProtected;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/admin/eval/retrieval/guardrails/config")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AdminProtected
public class RagRetrievalEvalGuardrailConfigAdminResource {

    @Inject
    RagRetrievalEvalGuardrailConfigAdminService service;

    @GET
    public RagRetrievalEvalGuardrailConfigStatus status() {
        return service.status();
    }

    @PUT
    public RagRetrievalEvalGuardrailConfigStatus update(RagRetrievalEvalGuardrailConfigUpdate update) {
        return service.update(update);
    }

    @POST
    @Path("/reload")
    public RagRetrievalEvalGuardrailConfigStatus reload() {
        return service.reload();
    }
}
