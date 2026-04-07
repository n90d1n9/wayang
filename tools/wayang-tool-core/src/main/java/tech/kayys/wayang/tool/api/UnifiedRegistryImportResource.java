package tech.kayys.wayang.tool.api;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.tool.dto.RegistrySyncResponse;
import tech.kayys.wayang.tool.dto.RegistryScheduleRequest;
import tech.kayys.wayang.tool.dto.RegistryScheduleResponse;
import tech.kayys.wayang.tool.dto.RegistrySyncHistoryResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;
import tech.kayys.wayang.tool.dto.UnifiedRegistryImportRequest;
import tech.kayys.wayang.tool.dto.UnifiedRegistryImportResponse;
import tech.kayys.wayang.tool.service.UnifiedRegistryImportService;
import tech.kayys.wayang.tool.service.RegistrySyncService;
import tech.kayys.wayang.tool.service.UnifiedRegistryQueryService;
import tech.kayys.wayang.tool.dto.UnifiedRegistrySnapshotResponse;
import tech.kayys.wayang.tool.service.RegistryScheduleService;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/registry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Unified Registry", description = "Low-code unified import for OpenAPI/OAS and MCP JSON")
public class UnifiedRegistryImportResource {

    @Inject
    ToolRequestContext requestContext;

    @Inject
    UnifiedRegistryImportService importService;

    @Inject
    UnifiedRegistryQueryService queryService;

    @Inject
    RegistrySyncService syncService;

    @Inject
    RegistryScheduleService scheduleService;

    @POST
    @Path("/import")
    @Operation(summary = "Auto-detect and import OpenAPI/OAS (tool registry) or MCP JSON (MCP registry)")
    public Uni<RestResponse<UnifiedRegistryImportResponse>> importUnified(@Valid UnifiedRegistryImportRequest request) {
        String requestId = requestContext.getCurrentRequestId();
        String userId = "current-user";
        return importService.importSource(requestId, userId, request)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/snapshot")
    @Operation(summary = "Get unified tenant snapshot of generated tools and MCP server registry")
    public Uni<UnifiedRegistrySnapshotResponse> snapshot(@QueryParam("namespace") String namespace) {
        String requestId = requestContext.getCurrentRequestId();
        return queryService.snapshot(requestId, namespace);
    }

    @POST
    @Path("/sync")
    @Operation(summary = "Sync URL-based OpenAPI sources and MCP registry sources into DB")
    public Uni<RestResponse<RegistrySyncResponse>> sync(
            @QueryParam("openapi") @DefaultValue("true") boolean openApi,
            @QueryParam("mcp") @DefaultValue("true") boolean mcp) {
        String requestId = requestContext.getCurrentRequestId();
        String userId = "system-sync";
        return syncService.syncTenant(requestId, userId, openApi, mcp)
                .map(RestResponse::ok);
    }

    @POST
    @Path("/schedule/openapi/{sourceId}")
    @Operation(summary = "Set schedule interval for one OpenAPI source (e.g. PT15M or 15m)")
    public Uni<RestResponse<RegistryScheduleResponse>> setOpenApiSchedule(
            @PathParam("sourceId") String sourceId,
            @Valid RegistryScheduleRequest request) {
        String requestId = requestContext.getCurrentRequestId();
        return scheduleService.setOpenApiSchedule(requestId, UUID.fromString(sourceId), request.interval())
                .map(response -> response != null ? RestResponse.ok(response) : RestResponse.notFound());
    }

    @POST
    @Path("/schedule/mcp/{name}")
    @Operation(summary = "Set schedule interval for one MCP server entry (e.g. PT15M or 15m)")
    public Uni<RestResponse<RegistryScheduleResponse>> setMcpSchedule(
            @PathParam("name") String name,
            @Valid RegistryScheduleRequest request) {
        String requestId = requestContext.getCurrentRequestId();
        return scheduleService.setMcpSchedule(requestId, name, request.interval())
                .map(response -> response != null ? RestResponse.ok(response) : RestResponse.notFound());
    }

    @GET
    @Path("/sync/history")
    @Operation(summary = "Get registry sync history for this tenant")
    public Uni<List<RegistrySyncHistoryResponse>> history(
            @QueryParam("limit") @DefaultValue("50") int limit) {
        String requestId = requestContext.getCurrentRequestId();
        return scheduleService.listHistory(requestId, limit);
    }
}
