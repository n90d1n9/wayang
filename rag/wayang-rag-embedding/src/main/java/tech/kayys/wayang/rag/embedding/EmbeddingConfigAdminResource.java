package tech.kayys.wayang.rag.embedding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/admin/embedding/config")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AdminProtected
public class EmbeddingConfigAdminResource {

    @Inject
    EmbeddingConfigAdminService service;

    @GET
    public EmbeddingConfigStatus status() {
        return service.status();
    }

    @POST
    @Path("/reload")
    public EmbeddingConfigStatus reload() {
        return service.reload();
    }
}
