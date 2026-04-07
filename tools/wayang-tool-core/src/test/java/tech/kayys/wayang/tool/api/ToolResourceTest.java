package tech.kayys.wayang.tool.api;

import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tech.kayys.wayang.error.WayangException;
import tech.kayys.wayang.tool.dto.GenerateToolsRequest;
import tech.kayys.wayang.tool.dto.InvocationStatus;
import tech.kayys.wayang.tool.dto.OpenApiToolRequest;
import tech.kayys.wayang.tool.dto.RequestContext;
import tech.kayys.wayang.tool.dto.SourceType;
import tech.kayys.wayang.tool.dto.ToolDetailResponse;
import tech.kayys.wayang.tool.dto.ToolExecuteRequest;
import tech.kayys.wayang.tool.dto.ToolExecutionRequest;
import tech.kayys.wayang.tool.dto.ToolExecutionResponse;
import tech.kayys.wayang.tool.dto.ToolExecutionResult;
import tech.kayys.wayang.tool.dto.ToolGenerationResponse;
import tech.kayys.wayang.tool.dto.ToolGenerationResult;
import tech.kayys.wayang.tool.dto.ToolMetadataResponse;
import tech.kayys.wayang.tool.dto.ToolUpdateRequest;
import tech.kayys.wayang.tool.dto.CapabilityLevel;
import tech.kayys.wayang.tool.entity.McpTool;
import tech.kayys.wayang.tool.registry.SpecFormatRegistry;
import tech.kayys.wayang.tool.repository.ToolRepository;
import tech.kayys.wayang.tool.service.ToolExecutor;
import tech.kayys.wayang.tool.service.ToolGenerationService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolResourceTest {

    @Test
    void getSupportedFormatsReturnsKnownFormats() {
        ToolResource resource = baseResource();
        resource.specFormatRegistry = new SpecFormatRegistry();

        Map<String, SpecFormatRegistry.SpecFormatInfo> formats = resource.getSupportedFormats();

        assertTrue(formats.containsKey("OPENAPI_3"));
        assertTrue(formats.containsKey("SWAGGER_2"));
    }

    @Test
    void generateFromOpenApiMapsGenerationResult() {
        ToolGenerationService generator = mock(ToolGenerationService.class);
        ToolResource resource = baseResource();
        resource.toolGenerator = generator;

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-a");
        resource.requestContext = requestContext;

        when(generator.generateTools(any())).thenReturn(Uni.createFrom().item(
                new ToolGenerationResult(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "acme",
                        2,
                        List.of("t1", "t2"),
                        List.of("warning"))));

        RestResponse<ToolGenerationResponse> response = resource.generateFromOpenApi(
                new OpenApiToolRequest("acme", "OPENAPI_3_RAW", "{\"openapi\":\"3.0.0\"}", null, Map.of()))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals("acme", response.getEntity().namespace());
        assertEquals(2, response.getEntity().toolsGenerated());
        assertEquals("11111111-1111-1111-1111-111111111111", response.getEntity().sourceId());

        ArgumentCaptor<GenerateToolsRequest> captor = ArgumentCaptor.forClass(GenerateToolsRequest.class);
        org.mockito.Mockito.verify(generator).generateTools(captor.capture());
        assertEquals("tenant-a", captor.getValue().requestId());
        assertEquals(SourceType.OPENAPI_3_RAW, captor.getValue().sourceType());
    }

    @Test
    void generateFromOpenApiWrapsFailure() {
        ToolGenerationService generator = mock(ToolGenerationService.class);
        when(generator.generateTools(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("bad spec")));

        ToolResource resource = baseResource();
        resource.toolGenerator = generator;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-a");
        resource.requestContext = requestContext;

        WayangException error = assertThrows(
                WayangException.class,
                () -> resource.generateFromOpenApi(
                        new OpenApiToolRequest("acme", "OPENAPI_3_RAW", "{}", null, Map.of()))
                        .await().indefinitely());
        assertTrue(error.getMessage().contains("bad spec"));
    }

    @Test
    void listToolsMapsRepositoryEntities() {
        ToolRepository repository = mock(ToolRepository.class);
        McpTool tool = mock(McpTool.class);
        when(tool.getToolId()).thenReturn("tool-1");
        when(tool.getName()).thenReturn("Weather");
        when(tool.getDescription()).thenReturn("Weather API");
        when(tool.getCapabilities()).thenReturn(Set.of("http"));
        when(tool.getCapabilityLevel()).thenReturn(CapabilityLevel.READ_ONLY);
        when(tool.isReadOnly()).thenReturn(true);
        when(tool.getTags()).thenReturn(Set.of("public"));

        when(repository.searchTools(any(), any())).thenReturn(Uni.createFrom().item(List.of(tool)));

        ToolResource resource = baseResource();
        resource.mcpToolRepository = repository;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-b");
        resource.requestContext = requestContext;

        List<ToolMetadataResponse> response = resource.listTools(null, null, null, null, null).await().indefinitely();

        assertEquals(1, response.size());
        assertEquals("tool-1", response.getFirst().toolId());
        assertEquals("READ_ONLY", response.getFirst().capabilityLevel());
        assertTrue(response.getFirst().readOnly());
    }

    @Test
    void getToolReturnsNotFoundWhenMissing() {
        ToolRepository repository = mock(ToolRepository.class);
        when(repository.findByRequestIdAndToolId("tenant-c", "missing"))
                .thenReturn(Uni.createFrom().nullItem());

        ToolResource resource = baseResource();
        resource.mcpToolRepository = repository;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-c");
        resource.requestContext = requestContext;

        RestResponse<ToolDetailResponse> response = resource.getTool("missing").await().indefinitely();
        assertEquals(404, response.getStatus());
    }

    @Test
    void getToolReturnsDetailWhenFound() {
        ToolRepository repository = mock(ToolRepository.class);
        McpTool tool = new McpTool();
        tool.setToolId("tool-42");
        tool.setName("Translator");
        tool.setDescription("Translate text");
        tool.setInputSchema(Map.of("type", "object"));
        tool.setOutputSchema(Map.of("type", "object"));
        tool.setCapabilities(Set.of("http", "json"));
        tool.setCapabilityLevel(CapabilityLevel.READ_ONLY);
        tool.setEnabled(true);
        tool.setReadOnly(false);
        when(repository.findByRequestIdAndToolId("tenant-g", "tool-42"))
                .thenReturn(Uni.createFrom().item(tool));

        ToolResource resource = baseResource();
        resource.mcpToolRepository = repository;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-g");
        resource.requestContext = requestContext;

        RestResponse<ToolDetailResponse> response = resource.getTool("tool-42").await().indefinitely();
        assertEquals(200, response.getStatus());
        assertEquals("tool-42", response.getEntity().toolId());
        assertEquals("READ_ONLY", response.getEntity().capabilityLevel());
        assertEquals(0L, response.getEntity().totalInvocations());
    }

    @Test
    void executeToolReturnsSuccessResponse() {
        ToolExecutor executor = mock(ToolExecutor.class);
        when(executor.execute(any())).thenReturn(Uni.createFrom().item(
                ToolExecutionResult.success("weather-tool", Map.of("temp", 30), 20)));

        ToolResource resource = baseResource();
        resource.toolExecutor = executor;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-d");
        resource.requestContext = requestContext;

        RestResponse<ToolExecutionResponse> response = resource.executeTool(
                "weather-tool",
                new ToolExecuteRequest(Map.of("city", "Jakarta"), Map.of("trace", "1")))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals("success", response.getEntity().status());
        assertEquals(30, response.getEntity().output().get("temp"));
    }

    @Test
    void executeToolReturnsFailureResponseWithMetadata() {
        ToolExecutor executor = mock(ToolExecutor.class);
        when(executor.execute(any())).thenReturn(Uni.createFrom().item(
                ToolExecutionResult.failure(
                        "weather-tool",
                        InvocationStatus.VALIDATION_ERROR,
                        "city is required",
                        7,
                        Map.of("errorCode", "TOOL_400", "retryable", false))));

        ToolResource resource = baseResource();
        resource.toolExecutor = executor;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-e");
        resource.requestContext = requestContext;

        RestResponse<ToolExecutionResponse> response = resource.executeTool(
                "weather-tool",
                new ToolExecuteRequest(Map.of(), Map.of()))
                .await().indefinitely();

        assertEquals(400, response.getStatus());
        assertEquals("failure", response.getEntity().status());
        assertEquals("city is required", response.getEntity().error());
        assertEquals("TOOL_400", response.getEntity().errorCode());
        assertFalse(response.getEntity().retryable());

        ArgumentCaptor<ToolExecutionRequest> captor = ArgumentCaptor.forClass(ToolExecutionRequest.class);
        org.mockito.Mockito.verify(executor).execute(captor.capture());
        assertEquals("tenant-e", captor.getValue().requestId());
        assertEquals("weather-tool", captor.getValue().toolId());
    }

    @Test
    void deleteToolReturnsNotFoundWhenMissing() {
        ToolRepository repository = mock(ToolRepository.class);
        when(repository.findByRequestIdAndToolId("tenant-f", "missing"))
                .thenReturn(Uni.createFrom().nullItem());

        ToolResource resource = baseResource();
        resource.mcpToolRepository = repository;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-f");
        resource.requestContext = requestContext;

        RestResponse<Void> response = resource.deleteTool("missing").await().indefinitely();
        assertEquals(404, response.getStatus());
    }

    @Test
    void updateToolPersistsChangesWhenFound() {
        ToolRepository repository = mock(ToolRepository.class);
        McpTool tool = new McpTool();
        tool.setToolId("tool-88");
        tool.setDescription("old");
        tool.setEnabled(true);
        tool.setTags(Set.of("legacy"));
        when(repository.findByRequestIdAndToolId("tenant-h", "tool-88"))
                .thenReturn(Uni.createFrom().item(tool));
        when(repository.update(any())).thenReturn(Uni.createFrom().item(tool));

        ToolResource resource = baseResource();
        resource.mcpToolRepository = repository;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-h");
        resource.requestContext = requestContext;

        RestResponse<Void> response = resource.updateTool(
                "tool-88",
                new ToolUpdateRequest(false, "new-desc", Set.of("stable", "v2")))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertFalse(tool.isEnabled());
        assertEquals("new-desc", tool.getDescription());
        assertEquals(Set.of("stable", "v2"), tool.getTags());
    }

    @Test
    void deleteToolReturnsOkWhenRepositoryDeletes() {
        ToolRepository repository = mock(ToolRepository.class);
        McpTool tool = new McpTool();
        tool.setToolId("tool-99");
        when(repository.findByRequestIdAndToolId("tenant-i", "tool-99"))
                .thenReturn(Uni.createFrom().item(tool));
        when(repository.deleteById("tool-99")).thenReturn(Uni.createFrom().item(true));

        ToolResource resource = baseResource();
        resource.mcpToolRepository = repository;
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-i");
        resource.requestContext = requestContext;

        RestResponse<Void> response = resource.deleteTool("tool-99").await().indefinitely();

        assertEquals(200, response.getStatus());
        assertNotNull(response);
    }

    private ToolResource baseResource() {
        ToolResource resource = new ToolResource();
        resource.toolGenerator = mock(ToolGenerationService.class);
        resource.toolExecutor = mock(ToolExecutor.class);
        resource.mcpToolRepository = mock(ToolRepository.class);
        resource.specFormatRegistry = new SpecFormatRegistry();
        return resource;
    }
}
