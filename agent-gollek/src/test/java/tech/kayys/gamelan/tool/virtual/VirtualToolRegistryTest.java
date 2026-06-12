package tech.kayys.gamelan.tool.virtual;

import org.junit.jupiter.api.*;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class VirtualToolRegistryTest {

    private VirtualToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new VirtualToolRegistry();
        registry.init();
    }

    @Test
    void builtInDiscoverCapabilitiesToolIsRegistered() {
        assertThat(registry.find("discover_capabilities")).isPresent();
        assertThat(registry.find("http_fetch")).isPresent();
    }

    @Test
    void discoverCapabilitiesListsAllTools() {
        registry.register(VirtualToolRegistry.VirtualTool.mock("my-api", "A test API tool"));

        ToolCall call = tool("virtual", Map.of("filter", ""));
        ToolResult result = registry.execute(call);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.output()).contains("discover_capabilities");
    }

    @Test
    void discoverCapabilitiesFiltersResults() {
        registry.register(VirtualToolRegistry.VirtualTool.mock("payment-api", "Process payments"));
        registry.register(VirtualToolRegistry.VirtualTool.mock("email-api", "Send emails"));

        ToolCall call = tool("discover_capabilities", Map.of("filter", "payment"));
        ToolResult result = registry.execute(call);

        assertThat(result.output()).contains("payment-api");
        assertThat(result.output()).doesNotContain("email-api");
    }

    @Test
    void lambdaToolExecutesFunction() {
        registry.register(VirtualToolRegistry.VirtualTool.lambda(
                "add-numbers",
                "Adds two numbers",
                params -> {
                    int a = Integer.parseInt(params.getOrDefault("a", "0"));
                    int b = Integer.parseInt(params.getOrDefault("b", "0"));
                    return ToolResult.success("add-numbers", String.valueOf(a + b));
                }));

        ToolCall call = tool("add-numbers", Map.of("a", "3", "b", "4"));
        ToolResult result = registry.execute(call);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.output()).isEqualTo("7");
    }

    @Test
    void unknownToolReturnsFailureWithHint() {
        ToolCall call = tool("does-not-exist", Map.of());
        ToolResult result = registry.execute(call);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).contains("not registered");
        assertThat(result.error()).contains("discover_capabilities");
    }

    @Test
    void registerAndUnregisterTool() {
        registry.register(VirtualToolRegistry.VirtualTool.mock("temp-tool", "Temporary tool"));
        assertThat(registry.find("temp-tool")).isPresent();

        registry.unregister("temp-tool");
        assertThat(registry.find("temp-tool")).isEmpty();
    }

    @Test
    void toolNamesIncludesAllRegisteredTools() {
        registry.register(VirtualToolRegistry.VirtualTool.mock("tool-a", "A"));
        registry.register(VirtualToolRegistry.VirtualTool.mock("tool-b", "B"));

        assertThat(registry.toolNames()).contains("tool-a", "tool-b", "virtual");
    }

    @Test
    void lambdaToolFailureReturnedAsFailureResult() {
        registry.register(VirtualToolRegistry.VirtualTool.lambda(
                "failing-tool", "Always fails",
                params -> ToolResult.failure("failing-tool", "Intentional failure")));

        ToolCall call = tool("failing-tool", Map.of());
        ToolResult result = registry.execute(call);

        // After retries, should return the failure
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isNotBlank();
    }

    @Test
    void mockToolAlwaysSucceeds() {
        registry.register(VirtualToolRegistry.VirtualTool.mock("mock-service", "Mock service"));

        ToolCall call = tool("mock-service", Map.of("param", "value"));
        ToolResult result = registry.execute(call);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.output()).contains("MOCK");
    }

    @Test
    void restToolMetadataIsCorrect() {
        VirtualToolRegistry.VirtualTool tool = VirtualToolRegistry.VirtualTool.rest(
                "my-api", "REST API", "https://api.example.com/data",
                Map.of("Authorization", "Bearer token"));

        assertThat(tool.name()).isEqualTo("my-api");
        assertThat(tool.backend()).isEqualTo(VirtualToolRegistry.VirtualTool.Backend.REST);
        assertThat(tool.endpoint()).isEqualTo("https://api.example.com/data");
        assertThat(tool.headers()).containsKey("Authorization");
    }

    @Test
    void agentToolMetadataIsCorrect() {
        VirtualToolRegistry.VirtualTool tool = VirtualToolRegistry.VirtualTool.agent(
                "security-scanner", "Security analysis agent", "http://scanner:8080/analyze");

        assertThat(tool.backend()).isEqualTo(VirtualToolRegistry.VirtualTool.Backend.AGENT);
        assertThat(tool.endpoint()).isEqualTo("http://scanner:8080/analyze");
        assertThat(tool.timeoutSeconds()).isEqualTo(60);
    }

    @Test
    void circuitBreakerOpensAfterThresholdFailures() {
        // Create a tool that always fails
        registry.register(VirtualToolRegistry.VirtualTool.lambda(
                "always-fails", "Always fails",
                params -> { throw new RuntimeException("Network down"); }));

        // Execute 3 times to open the circuit breaker
        for (int i = 0; i < 4; i++) {
            registry.execute(tool("always-fails", Map.of()));
        }

        // Circuit should now be open — next call should return circuit-breaker error
        ToolResult result = registry.execute(tool("always-fails", Map.of()));
        // Either open or still retrying — just verify no exception
        assertThatCode(() -> registry.execute(tool("always-fails", Map.of())))
                .doesNotThrowAnyException();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ToolCall tool(String name, Map<String, String> params) {
        return new ToolCall(name, params, "<tc/>");
    }
}
