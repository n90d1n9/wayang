package tech.kayys.wayang.tool.api;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.tool.dto.McpRegistryImportRequest;
import tech.kayys.wayang.tool.dto.McpRegistryImportResponse;
import tech.kayys.wayang.tool.dto.McpServerConfigRequest;
import tech.kayys.wayang.tool.dto.McpServerRegistryResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;
import tech.kayys.wayang.tool.service.McpRegistryService;

import java.util.List;

@Path("/api/v1/mcp/registry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "MCP Registry", description = "Enterprise MCP registry management")
public class McpRegistryResource {

    @Inject
    ToolRequestContext requestContext;

    @Inject
    McpRegistryService registryService;

    @POST
    @Path("/import")
    @Operation(summary = "Import MCP server definitions from JSON payload, URL, or file")
    public Uni<RestResponse<McpRegistryImportResponse>> importRegistry(@Valid McpRegistryImportRequest request) {
        String requestId = requestContext.getCurrentRequestId();
        return registryService.importFromJson(requestId, request)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/servers")
    @Operation(summary = "List tenant MCP server registry")
    public Uni<List<McpServerRegistryResponse>> listRegistryServers() {
        String requestId = requestContext.getCurrentRequestId();
        return registryService.listServers(requestId);
    }

    @POST
    @Path("/servers/{name}")
    @Operation(summary = "Create or update one MCP server configuration")
    public Uni<RestResponse<McpServerRegistryResponse>> upsertRegistryServer(
            @PathParam("name") String name,
            @Valid McpServerConfigRequest request) {
        String requestId = requestContext.getCurrentRequestId();
        return registryService.upsertServer(requestId, name, request)
                .map(RestResponse::ok);
    }

    @DELETE
    @Path("/servers/{name}")
    @Operation(summary = "Remove one MCP server definition from tenant registry")
    public Uni<RestResponse<Void>> deleteRegistryServer(@PathParam("name") String name) {
        String requestId = requestContext.getCurrentRequestId();
        return registryService.removeServer(requestId, name)
                .map(deleted -> deleted ? RestResponse.ok() : RestResponse.notFound());
    }
}
