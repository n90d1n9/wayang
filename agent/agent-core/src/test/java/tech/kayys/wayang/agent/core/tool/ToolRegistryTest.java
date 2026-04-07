package tech.kayys.wayang.agent.core.tool;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.kayys.wayang.tool.spi.Tool;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;

class ToolRegistryTest {

    @Test
    void testRegistryDiscoveryAndLookup() {
        Tool tool = Mockito.mock(Tool.class);
        Mockito.when(tool.id()).thenReturn("mock-tool");
        Mockito.when(tool.description()).thenReturn("Mock Tool");
        Mockito.when(tool.inputSchema()).thenReturn(Map.of("arg", "string"));
        Mockito.when(tool.execute(anyMap(), anyMap())).thenReturn(Uni.createFrom().item(Map.of("ok", true)));

        @SuppressWarnings("unchecked")
        Instance<Tool> instances = Mockito.mock(Instance.class);
        Mockito.when(instances.iterator()).thenReturn(List.of(tool).iterator());

        ToolRegistry registry = new ToolRegistry(instances);

        Assertions.assertTrue(registry.getTool("mock-tool").isPresent());
        Assertions.assertEquals(1, registry.listTools().size());
    }

    @Test
    void testExecuteToolSuccess() {
        Tool tool = Mockito.mock(Tool.class);
        Mockito.when(tool.id()).thenReturn("mock-tool");
        Mockito.when(tool.description()).thenReturn("Mock Tool");
        Mockito.when(tool.inputSchema()).thenReturn(Map.of());
        Mockito.when(tool.execute(anyMap(), anyMap()))
                .thenReturn(Uni.createFrom().item(Map.of("status", "success")));

        @SuppressWarnings("unchecked")
        Instance<Tool> instances = Mockito.mock(Instance.class);
        Mockito.when(instances.iterator()).thenReturn(List.of(tool).iterator());

        ToolRegistry registry = new ToolRegistry(instances);
        Map<String, Object> result = registry.executeTool("mock-tool", Map.of("x", 1), Map.of())
                .await().indefinitely();

        Assertions.assertEquals("success", result.get("status"));
    }

    @Test
    void testExecuteToolNotFound() {
        @SuppressWarnings("unchecked")
        Instance<Tool> instances = Mockito.mock(Instance.class);
        Mockito.when(instances.iterator()).thenReturn(List.<Tool>of().iterator());
        ToolRegistry registry = new ToolRegistry(instances);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registry.executeTool("missing-tool", Map.of(), Map.of()).await().indefinitely());
        Assertions.assertTrue(ex.getMessage().contains("Tool not found"));
    }
}
