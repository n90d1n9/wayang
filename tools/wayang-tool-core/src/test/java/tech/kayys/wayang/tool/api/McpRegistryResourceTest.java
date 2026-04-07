package tech.kayys.wayang.tool.api;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.ForbiddenException;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.dto.McpRegistryImportRequest;
import tech.kayys.wayang.tool.dto.McpRegistryImportResponse;
import tech.kayys.wayang.tool.dto.McpServerConfigRequest;
import tech.kayys.wayang.tool.dto.McpServerRegistryResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;
import tech.kayys.wayang.tool.service.McpRegistryService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpRegistryResourceTest {

    @Test
    void listServersPropagatesForbiddenWhenCommunityMode() {
        ToolRequestContext requestContext = mock(ToolRequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("community");

        McpRegistryService service = mock(McpRegistryService.class);
        when(service.listServers("community"))
                .thenReturn(Uni.createFrom().failure(new ForbiddenException("enterprise-only")));

        McpRegistryResource resource = new McpRegistryResource();
        resource.requestContext = requestContext;
        resource.registryService = service;

        assertThrows(ForbiddenException.class, () -> resource.listRegistryServers().await().indefinitely());
    }

    @Test
    void listServersReturnsPayloadWhenEnterpriseMode() {
        ToolRequestContext requestContext = mock(ToolRequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("enterprise");

        McpRegistryService service = mock(McpRegistryService.class);
        when(service.listServers("enterprise"))
                .thenReturn(Uni.createFrom().item(List.of(
                        new McpServerRegistryResponse(
                                "image-downloader",
                                "stdio",
                                "node",
                                null,
                                "[\"/tmp/index.js\"]",
                                "{}",
                                "manual://api",
                                null,
                                true))));

        McpRegistryResource resource = new McpRegistryResource();
        resource.requestContext = requestContext;
        resource.registryService = service;

        List<McpServerRegistryResponse> list = resource.listRegistryServers().await().indefinitely();
        assertEquals(1, list.size());
        assertEquals("image-downloader", list.getFirst().name());
    }

    @Test
    void removeReturnsNotFoundWhenServiceReportsFalse() {
        ToolRequestContext requestContext = mock(ToolRequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("enterprise");

        McpRegistryService service = mock(McpRegistryService.class);
        when(service.removeServer("enterprise", "missing"))
                .thenReturn(Uni.createFrom().item(false));

        McpRegistryResource resource = new McpRegistryResource();
        resource.requestContext = requestContext;
        resource.registryService = service;

        RestResponse<Void> response = resource.deleteRegistryServer("missing").await().indefinitely();
        assertEquals(404, response.getStatus());
    }

    @Test
    void importReturnsOkWhenServiceAcceptsRequest() {
        ToolRequestContext requestContext = mock(ToolRequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("enterprise");

        McpRegistryService service = mock(McpRegistryService.class);
        when(service.importFromJson("enterprise", new McpRegistryImportRequest("RAW", "{\"mcpServers\":{}}", null)))
                .thenReturn(Uni.createFrom().item(new McpRegistryImportResponse(0, List.of())));

        McpRegistryResource resource = new McpRegistryResource();
        resource.requestContext = requestContext;
        resource.registryService = service;

        RestResponse<McpRegistryImportResponse> response = resource.importRegistry(
                new McpRegistryImportRequest("RAW", "{\"mcpServers\":{}}", null))
                .await().indefinitely();
        assertEquals(200, response.getStatus());
    }

    @Test
    void upsertReturnsOkWhenServiceAcceptsRequest() {
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("enterprise");

        McpRegistryService service = mock(McpRegistryService.class);
        McpServerConfigRequest request = new McpServerConfigRequest(
                "stdio", "node", null, List.of("/tmp/index.js"), Map.of(), true, "manual://api", null);
        when(service.upsertServer("enterprise", "image-downloader", request))
                .thenReturn(Uni.createFrom().item(new McpServerRegistryResponse(
                        "image-downloader",
                        "stdio",
                        "node",
                        null,
                        "[\"/tmp/index.js\"]",
                        "{}",
                        "manual://api",
                        null,
                        true)));

        McpRegistryResource resource = new McpRegistryResource();
        resource.requestContext = requestContext;
        resource.registryService = service;

        RestResponse<McpServerRegistryResponse> response = resource.upsertRegistryServer("image-downloader", request)
                .await().indefinitely();
        assertEquals(200, response.getStatus());
    }
}
