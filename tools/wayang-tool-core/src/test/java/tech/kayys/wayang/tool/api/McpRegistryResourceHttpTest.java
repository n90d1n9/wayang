package tech.kayys.wayang.tool.api;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.dto.McpRegistryImportResponse;
import tech.kayys.wayang.tool.dto.McpServerConfigRequest;
import tech.kayys.wayang.tool.dto.McpServerRegistryResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;
import tech.kayys.wayang.tool.service.McpRegistryService;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestSecurity(user = "tester", roles = "user")
class McpRegistryResourceHttpTest {

    @InjectMock
    ToolRequestContext requestContext;

    @InjectMock
    McpRegistryService registryService;

    @BeforeEach
    void setup() {
        when(requestContext.getCurrentRequestId()).thenReturn("tenant-http");
    }

    @Test
    void importEndpointReturnsOkPayload() {
        when(registryService.importFromJson(eq("tenant-http"), any()))
                .thenReturn(Uni.createFrom().item(new McpRegistryImportResponse(
                        2,
                        List.of("alpha", "beta"))));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "sourceType": "RAW",
                          "source": "{\\"mcpServers\\":{}}"
                        }
                        """)
                .when()
                .post("/api/v1/mcp/registry/import")
                .then()
                .statusCode(200)
                .body("importedCount", equalTo(2))
                .body("serverNames.size()", equalTo(2));
    }

    @Test
    void listServersEndpointReturnsServerArray() {
        when(registryService.listServers("tenant-http"))
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

        given()
                .when()
                .get("/api/v1/mcp/registry/servers")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].name", equalTo("image-downloader"))
                .body("[0].transport", equalTo("stdio"));
    }

    @Test
    void upsertEndpointReturnsSavedServer() {
        when(registryService.upsertServer(eq("tenant-http"), eq("image-downloader"), any(McpServerConfigRequest.class)))
                .thenReturn(Uni.createFrom().item(new McpServerRegistryResponse(
                        "image-downloader",
                        "stdio",
                        "node",
                        null,
                        "[\"/tmp/index.js\"]",
                        "{\"ENV\":\"test\"}",
                        "manual://api",
                        "PT15M",
                        true)));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "transport": "stdio",
                          "command": "node",
                          "args": ["/tmp/index.js"],
                          "env": { "ENV": "test" },
                          "enabled": true,
                          "source": "manual://api",
                          "syncSchedule": "PT15M"
                        }
                        """)
                .when()
                .post("/api/v1/mcp/registry/servers/image-downloader")
                .then()
                .statusCode(200)
                .body("name", equalTo("image-downloader"))
                .body("enabled", equalTo(true))
                .body("syncSchedule", equalTo("PT15M"));
    }

    @Test
    void deleteEndpointReturnsNotFoundWhenMissing() {
        when(registryService.removeServer("tenant-http", "missing-server"))
                .thenReturn(Uni.createFrom().item(false));

        given()
                .when()
                .delete("/api/v1/mcp/registry/servers/missing-server")
                .then()
                .statusCode(404);
    }
}
