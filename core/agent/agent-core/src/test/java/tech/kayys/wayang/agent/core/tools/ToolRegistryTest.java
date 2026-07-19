package tech.kayys.wayang.agent.core.tools;

import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.List;
import java.util.Map;

class ToolRegistryTest {

    @Test
    void testRegistryDiscoveryAndLookup() {
        Tool tool = Mockito.mock(Tool.class);
        Mockito.when(tool.id()).thenReturn("mock-tool");
        Mockito.when(tool.description()).thenReturn("Mock Tool");
        Mockito.when(tool.inputSchema()).thenReturn(Map.of("arg", "string"));

        ToolRegistry registry = new ToolRegistry();
        registry.register(tool);

        Assertions.assertTrue(registry.getTool("mock-tool").isPresent());
        Assertions.assertEquals(1, registry.listTools().size());
    }

    @Test
    void testExecuteToolSuccess() {
        Tool tool = Mockito.mock(Tool.class);
        Mockito.when(tool.id()).thenReturn("mock-tool");
        Mockito.when(tool.description()).thenReturn("Mock Tool");
        Mockito.when(tool.inputSchema()).thenReturn(Map.of());
        Mockito.when(tool.execute(Mockito.anyMap(), Mockito.any()))
                .thenReturn(ToolResult.success(Map.of("status", "success")));

        ToolRegistry registry = new ToolRegistry();
        registry.register(tool);
        Map<String, Object> result = registry.executeTool("mock-tool", Map.of("x", 1), Map.of())
                .await().indefinitely();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        Assertions.assertEquals("success", data.get("status"));
    }

    @Test
    void testExecuteToolNotFound() {
        ToolRegistry registry = new ToolRegistry();

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registry.executeTool("missing-tool", Map.of(), Map.of()).await().indefinitely());
        Assertions.assertTrue(ex.getMessage().contains("Tool not found"));
    }
}
