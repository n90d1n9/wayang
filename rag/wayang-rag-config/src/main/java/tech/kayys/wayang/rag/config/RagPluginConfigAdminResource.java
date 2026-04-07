package tech.kayys.wayang.rag.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.rag.runtime.AdminProtected;

@Path("/admin/rag/plugins/config")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AdminProtected
public class RagPluginConfigAdminResource {

    @Inject
    RagPluginConfigAdminService service;

    @GET
    public RagPluginConfigStatus status() {
        return service.status();
    }

    @PUT
    public RagPluginConfigStatus update(RagPluginConfigUpdate update) {
        return service.update(update);
    }

    @POST
    @Path("/reload")
    public RagPluginConfigStatus reload() {
        return service.reload();
    }
}
