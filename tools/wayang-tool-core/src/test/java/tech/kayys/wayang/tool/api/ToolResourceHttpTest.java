package tech.kayys.wayang.tool.api;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.dto.CapabilityLevel;
import tech.kayys.wayang.tool.dto.InvocationStatus;
import tech.kayys.wayang.tool.dto.RequestContext;
import tech.kayys.wayang.tool.dto.ToolExecutionRequest;
import tech.kayys.wayang.tool.dto.ToolExecutionResult;
import tech.kayys.wayang.tool.dto.ToolGenerationResult;
import tech.kayys.wayang.tool.entity.McpTool;
import tech.kayys.wayang.tool.repository.ToolRepository;
import tech.kayys.wayang.tool.service.ToolExecutor;
import tech.kayys.wayang.tool.service.ToolGenerationService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestSecurity(user = "tester", roles = "user")
class ToolResourceHttpTest {

    @InjectMock
    ToolGenerationService toolGenerationService;

    @InjectMock
    ToolExecutor toolExecutor;

    @InjectMock
    ToolRepository toolRepository;

    @InjectMock
    RequestContext requestContext;

    @BeforeEach
    void setup() {
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-http");
    }

    @Test
    void getFormatsReturnsSupportedMap() {
        given()
                .when()
                .get("/api/v1/mcp/tools/formats")
                .then()
                .statusCode(200)
                .body("$", hasKey("OPENAPI_3"))
                .body("$", hasKey("SWAGGER_2"));
    }

    @Test
    void openApiGenerationEndpointReturnsMappedPayload() {
        when(toolGenerationService.generateTools(any())).thenReturn(Uni.createFrom().item(
                new ToolGenerationResult(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "acme",
                        2,
                        List.of("tool-a", "tool-b"),
                        List.of("warning"))));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "namespace": "acme",
                          "sourceType": "OPENAPI_3_RAW",
                          "source": "raw-openapi-spec"
                        }
                        """)
                .when()
                .post("/api/v1/mcp/tools/openapi")
                .then()
                .statusCode(200)
                .body("sourceId", equalTo("11111111-1111-1111-1111-111111111111"))
                .body("namespace", equalTo("acme"))
                .body("toolsGenerated", equalTo(2))
                .body("toolIds.size()", equalTo(2));
    }

    @Test
    void executeEndpointReturnsSuccessPayload() {
        when(toolExecutor.execute(any(ToolExecutionRequest.class)))
                .thenReturn(Uni.createFrom().item(
                        ToolExecutionResult.success("weather-tool", Map.of("temp", 29), 10)));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "arguments": { "city": "Jakarta" },
                          "context": { "traceId": "abc" }
                        }
                        """)
                .when()
                .post("/api/v1/mcp/tools/weather-tool/execute")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("output.temp", equalTo(29));
    }

    @Test
    void listToolsEndpointReturnsMappedTools() {
        McpTool tool = new McpTool();
        tool.setToolId("tool-1");
        tool.setName("Weather Tool");
        tool.setDescription("Reads weather");
        tool.setCapabilities(Set.of("http"));
        tool.setCapabilityLevel(CapabilityLevel.READ_ONLY);
        tool.setReadOnly(true);
        tool.setTags(Set.of("public"));
        when(toolRepository.searchTools(any(), any())).thenReturn(Uni.createFrom().item(List.of(tool)));

        given()
                .when()
                .get("/api/v1/mcp/tools")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].toolId", equalTo("tool-1"))
                .body("[0].capabilityLevel", equalTo("READ_ONLY"));
    }

    @Test
    void getToolEndpointReturnsNotFoundWhenMissing() {
        when(toolRepository.findByRequestIdAndToolId("tenant-http", "missing"))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .get("/api/v1/mcp/tools/missing")
                .then()
                .statusCode(404);
    }

    @Test
    void executeEndpointReturnsFailurePayload() {
        when(toolExecutor.execute(any(ToolExecutionRequest.class)))
                .thenReturn(Uni.createFrom().item(
                        ToolExecutionResult.failure(
                                "tool-2",
                                InvocationStatus.VALIDATION_ERROR,
                                "invalid input",
                                5,
                                Map.of("errorCode", "TOOL_400", "retryable", false))));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "arguments": {}
                        }
                        """)
                .when()
                .post("/api/v1/mcp/tools/tool-2/execute")
                .then()
                .statusCode(400)
                .body("status", equalTo("failure"))
                .body("error", equalTo("invalid input"))
                .body("errorCode", equalTo("TOOL_400"))
                .body("retryable", equalTo(false))
                .body("executionTimeMs", notNullValue());
    }
}
