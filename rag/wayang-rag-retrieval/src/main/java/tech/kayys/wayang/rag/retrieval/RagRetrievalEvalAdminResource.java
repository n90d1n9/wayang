package tech.kayys.wayang.rag.retrieval;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.AdminProtected;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/admin/eval/retrieval")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AdminProtected
public class RagRetrievalEvalAdminResource {

    @Inject
    RagRetrievalEvalService service;

    @POST
    public RagRetrievalEvalResponse evaluate(RagRetrievalEvalRequest request) {
        return service.evaluate(request);
    }
}
