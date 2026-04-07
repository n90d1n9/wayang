package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/admin/rag/plugins")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@AdminProtected
public class RagPluginAdminResource {

    @Inject
    RagPluginAdminService service;

    @GET
    public RagPluginAdminStatus status(@QueryParam("tenantId") String tenantId) {
        return service.status(tenantId);
    }
}
