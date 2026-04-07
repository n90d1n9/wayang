package tech.kayys.wayang.rag.slo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.rag.runtime.AdminProtected;

@Path("/admin/observability/slo/alerts")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AdminProtected
public class RagSloAlertAdminResource {

    @Inject
    RagSloAlertService service;

    @GET
    public RagSloAlertState state() {
        return service.evaluate();
    }

    @GET
    @Path("/snooze")
    public RagSloAlertSnoozeStatus snoozeStatus() {
        return service.snoozeStatus();
    }

    @POST
    @Path("/snooze")
    public RagSloAlertSnoozeStatus snooze(RagSloAlertSnoozeRequest request) {
        return service.snooze(request);
    }

    @POST
    @Path("/snooze/clear")
    public RagSloAlertSnoozeStatus clearSnooze() {
        return service.clearSnooze();
    }
}
