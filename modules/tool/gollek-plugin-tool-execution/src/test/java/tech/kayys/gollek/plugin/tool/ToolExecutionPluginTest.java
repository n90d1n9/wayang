/*
 * MIT License
 *
 * Copyright (c) 2026 Kayys.tech
 *
 * @author Bhangun
 */

package tech.kayys.gollek.plugin.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.gollek.core.execution.ExecutionContext;
import tech.kayys.gollek.spi.plugin.PluginContext;
import tech.kayys.gollek.spi.registry.ToolRegistry;
import tech.kayys.gollek.spi.tool.Tool;
import tech.kayys.gollek.spi.tool.ToolCall;
import tech.kayys.gollek.tool.dto.ToolExecutionResult;
import tech.kayys.gollek.tool.dto.InvocationStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ToolExecutionPlugin}.
 */
@ExtendWith(MockitoExtension.class)
class ToolExecutionPluginTest {

    @Mock
    PluginContext pluginContext;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ToolRegistry toolRegistry;

    @Mock
    Tool tool;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    ToolExecutionPlugin plugin;

    @BeforeEach
    void setUp() {
        // Default behavior
    }

    @Test
    void initialize_loadsConfig() {
        when(pluginContext.getConfig("enabled")).thenReturn(Optional.of("false"));
        plugin.initialize(pluginContext);

        // Should be disabled based on config
        assertFalse(plugin.shouldExecute(executionContext));
    }

    @Test
    void execute_noToolCalls_doesNothing() throws Exception {
        when(executionContext.getVariable(eq("detectedToolCalls"), eq(List.class)))
                .thenReturn(Optional.empty());

        plugin.execute(executionContext, null);

        verifyNoInteractions(toolRegistry);
    }

    @Test
    void execute_runsDetectedTools() throws Exception {
        // Mock a tool call
        ToolCall call = mock(ToolCall.class);
        ToolCall.Function function = mock(ToolCall.Function.class);

        when(call.getId()).thenReturn("call_123");
        when(call.getFunction()).thenReturn(function);
        when(function.getName()).thenReturn("search");
        when(function.getArguments()).thenReturn("{\"query\": \"gollek\"}");

        when(executionContext.getVariable(eq("detectedToolCalls"), eq(List.class)))
                .thenReturn(Optional.of(List.of(call)));

        when(executionContext.getVariable(eq("conversationHistory"), eq(List.class)))
                .thenReturn(Optional.of(new java.util.ArrayList<>()));

        // Mock ToolRegistry to return a mock Tool
        when(toolRegistry.getTool("search")).thenReturn(io.smallrye.mutiny.Uni.createFrom().item(tool));

        // Mock ObjectMapper
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("query", "gollek"));

        // Mock tool execution
        when(tool.execute(anyMap(), anyMap()))
                .thenReturn(io.smallrye.mutiny.Uni.createFrom().item(Map.of("result", "Gollek is awesome")));

        plugin.execute(executionContext, null);

        // Verify tool execution
        verify(tool, times(1)).execute(anyMap(), anyMap());

        // Verify results stored in context
        verify(executionContext).putVariable(eq("toolResults"), anyList());
        verify(executionContext).putVariable(eq("hasToolResults"), eq(true));
        verify(executionContext).putVariable(eq("reasoningLoopContinue"), eq(true));
    }
}
