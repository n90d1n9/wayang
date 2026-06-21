package tech.kayys.wayang.api.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangCommandApi;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;

import java.util.Map;

@Path("/api/v1/commands")
@Produces(MediaType.APPLICATION_JSON)
public class CommandResource {

    private final WayangGollekSdk sdk = tech.kayys.wayang.gollek.sdk.Wayang.local();

    @GET
    @Path("/discover")
    public Response discover(
            @QueryParam("surfaceId") String surfaceId,
            @QueryParam("category") String category,
            @QueryParam("commandId") String commandId) {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangCommandApi commandApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).commands();
        WorkbenchCommandQuery query = WorkbenchCommandQuery.of(surfaceId, category, commandId);
        return Response.ok(commandApi.discoveryJson(query)).build();
    }

    @GET
    @Path("/index")
    public Response index(
            @QueryParam("surfaceId") String surfaceId,
            @QueryParam("category") String category,
            @QueryParam("commandId") String commandId) {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangCommandApi commandApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).commands();
        WorkbenchCommandQuery query = WorkbenchCommandQuery.of(surfaceId, category, commandId);
        return Response.ok(commandApi.indexJson(query)).build();
    }

    @GET
    @Path("/workbench")
    public Response workbench(
            @QueryParam("surfaceId") String surfaceId,
            @QueryParam("category") String category,
            @QueryParam("commandId") String commandId) {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangCommandApi commandApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).commands();
        WorkbenchCommandQuery query = WorkbenchCommandQuery.of(surfaceId, category, commandId);
        return Response.ok(commandApi.workbenchJson(query)).build();
    }
}
