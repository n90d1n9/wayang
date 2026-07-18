package tech.kayys.wayang.api.rest;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangPlatformApi;

import java.util.Map;

@Path("/api/v1/platform")
@Produces(MediaType.APPLICATION_JSON)
public class PlatformResource {

    private final WayangGollekSdk sdk = tech.kayys.wayang.gollek.sdk.Wayang.local();

    @GET
    @Path("/status")
    public Response getStatus() {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangPlatformApi platformApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).platform();
        return Response.ok(platformApi.statusEnvelope(platformApi.status())).build();
    }

    @GET
    @Path("/readiness")
    public Response getReadiness() {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangPlatformApi platformApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).platform();
        return Response.ok(platformApi.readinessEnvelope(platformApi.readiness())).build();
    }

    @GET
    @Path("/readiness/{profileId}")
    public Response getReadinessProfile(@PathParam("profileId") String profileId) {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangPlatformApi platformApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).platform();
        return Response.ok(platformApi.readinessEnvelope(platformApi.readiness(profileId))).build();
    }

    @GET
    @Path("/catalog")
    public Response getProductCatalog() {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangPlatformApi platformApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).platform();
        return Response.ok(platformApi.productCatalogJson()).build();
    }

    @GET
    @Path("/surfaces/{surfaceId}/profiles")
    public Response getProfilesForSurface(@PathParam("surfaceId") String surfaceId) {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangPlatformApi platformApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).platform();
        return Response.ok(platformApi.profilesJson(surfaceId, platformApi.productProfilesForSurface(surfaceId))).build();
    }

    @GET
    @Path("/profiles/{profileId}")
    public Response getProfileDetail(@PathParam("profileId") String profileId) {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangPlatformApi platformApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).platform();
        return Response.ok(platformApi.profileDetailJson(platformApi.productProfile(profileId))).build();
    }

    @GET
    @Path("/boundaries")
    public Response getSdkBoundaryCatalog() {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangPlatformApi platformApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).platform();
        return Response.ok(platformApi.sdkBoundaryCatalogJson()).build();
    }

    @GET
    @Path("/boundaries/{boundaryId}")
    public Response getSdkBoundaryDetail(@PathParam("boundaryId") String boundaryId) {
        if (sdk == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "SDK not initialized")).build();
        }
        WayangPlatformApi platformApi = tech.kayys.wayang.gollek.sdk.WayangClient.of(sdk).platform();
        return Response.ok(platformApi.sdkBoundaryJson(boundaryId)).build();
    }
}
