package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.error.WayangException;
import tech.kayys.wayang.tool.dto.GenerateToolsRequest;
import tech.kayys.wayang.tool.dto.InvocationStatus;
import tech.kayys.wayang.tool.dto.SourceType;
import tech.kayys.wayang.tool.dto.ToolExecutionRequest;
import tech.kayys.wayang.tool.dto.ToolExecutionResult;
import tech.kayys.wayang.tool.dto.ToolGenerationResult;
import tech.kayys.wayang.tool.service.ToolExecutor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpResourceTest {

    @Test
    void generateToolsReturnsOkOnSuccess() {
        McpResource resource = new McpResource();
        resource.toolGenerator = request -> Uni.createFrom().item(
                new ToolGenerationResult(
                        UUID.randomUUID(),
                        request.namespace(),
                        2,
                        List.of("tool-a", "tool-b"),
                        List.of()));
        resource.toolExecutor = new StubToolExecutor(
                Uni.createFrom().item(ToolExecutionResult.success("tool-a", Map.of("ok", true), 10)));

        GenerateToolsRequest request = new GenerateToolsRequest(
                "req-1",
                "default",
                SourceType.OPENAPI_3_RAW,
                "{}",
                null,
                "user-1",
                Map.of());

        RestResponse<ToolGenerationResult> response = resource.generateTools(request)
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals("default", response.getEntity().namespace());
        assertEquals(2, response.getEntity().toolsGenerated());
    }

    @Test
    void generateToolsWrapsFailureAsWayangException() {
        McpResource resource = new McpResource();
        resource.toolGenerator = request -> Uni.createFrom().failure(new RuntimeException("bad source"));
        resource.toolExecutor = new StubToolExecutor(
                Uni.createFrom().item(ToolExecutionResult.success("tool-a", Map.of(), 1)));

        GenerateToolsRequest request = new GenerateToolsRequest(
                "req-2",
                "default",
                SourceType.OPENAPI_3_RAW,
                "{}",
                null,
                "user-2",
                Map.of());

        WayangException error = assertThrows(
                WayangException.class,
                () -> resource.generateTools(request).await().indefinitely());

        assertTrue(error.getMessage().contains("Generation failed: bad source"));
        assertNotNull(error.getErrorCode());
    }

    @Test
    void executeToolReturnsOkOnSuccess() {
        McpResource resource = new McpResource();
        resource.toolGenerator = request -> Uni.createFrom().item(
                new ToolGenerationResult(UUID.randomUUID(), "default", 1, List.of("tool-a"), List.of()));
        resource.toolExecutor = new StubToolExecutor(
                Uni.createFrom().item(ToolExecutionResult.success("tool-a", Map.of("status", "done"), 15)));

        ToolExecutionRequest request = new ToolExecutionRequest(
                "req-3",
                "user-3",
                "tool-a",
                Map.of("q", "ai"),
                "run-1",
                "agent-1",
                Map.of());

        RestResponse<ToolExecutionResult> response = resource.executeTool(request)
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(InvocationStatus.SUCCESS, response.getEntity().status());
        assertEquals("done", response.getEntity().output().get("status"));
    }

    @Test
    void executeToolWrapsFailureAsWayangException() {
        McpResource resource = new McpResource();
        resource.toolGenerator = request -> Uni.createFrom().item(
                new ToolGenerationResult(UUID.randomUUID(), "default", 0, List.of(), List.of()));
        resource.toolExecutor = new StubToolExecutor(
                Uni.createFrom().failure(new RuntimeException("executor failure")));

        ToolExecutionRequest request = new ToolExecutionRequest(
                "req-4",
                "user-4",
                "tool-x",
                Map.of(),
                "run-2",
                "agent-2",
                Map.of());

        WayangException error = assertThrows(
                WayangException.class,
                () -> resource.executeTool(request).await().indefinitely());

        assertTrue(error.getMessage().contains("Execution failed: executor failure"));
        assertNotNull(error.getErrorCode());
    }

    @Test
    void listToolsReturnsEmptyList() {
        McpResource resource = new McpResource();
        resource.toolGenerator = request -> Uni.createFrom().item(
                new ToolGenerationResult(UUID.randomUUID(), "default", 0, List.of(), List.of()));
        resource.toolExecutor = new StubToolExecutor(
                Uni.createFrom().item(ToolExecutionResult.success("tool-a", Map.of(), 1)));

        RestResponse<List<McpTool>> response = resource.listTools(null, null)
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity().isEmpty());
    }

    @Test
    void getToolReturnsNotFound() {
        McpResource resource = new McpResource();
        resource.toolGenerator = request -> Uni.createFrom().item(
                new ToolGenerationResult(UUID.randomUUID(), "default", 0, List.of(), List.of()));
        resource.toolExecutor = new StubToolExecutor(
                Uni.createFrom().item(ToolExecutionResult.success("tool-a", Map.of(), 1)));

        RestResponse<McpTool> response = resource.getTool("missing-tool")
                .await().indefinitely();

        assertEquals(404, response.getStatus());
    }

    static class StubToolExecutor extends ToolExecutor {
        private final Uni<ToolExecutionResult> result;

        StubToolExecutor(Uni<ToolExecutionResult> result) {
            this.result = result;
        }

        @Override
        public Uni<ToolExecutionResult> execute(ToolExecutionRequest request) {
            return result;
        }
    }
}
