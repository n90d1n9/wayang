package tech.kayys.wayang.rag.slo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.rag.runtime.AdminProtected;

@Path("/admin/observability/slo")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AdminProtected
public class RagSloAdminResource {

    @Inject
    RagSloAdminService service;

    @GET
    public RagSloStatus status() {
        return service.status();
    }
}
