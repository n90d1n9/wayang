# Gollek SDK Migration Summary

## Overview
This document describes the migration from `AnthropicClient` to `GollekLocalClient` (gollek-sdk-java-local) in the agent orchestration module.

## Changes Made

### 1. AgentLoop.java
**File**: `src/main/java/tech/kayys/wayang/agent/orchestration/AgentLoop.java`

#### Key Changes:
- **Replaced AnthropicClient with GollekLocalClient**
  - Changed from HTTP-based Anthropic API calls to local Gollek SDK inference
  - Client initialization: `new AnthropicClient(config)` → `new LocalGollekSdk()`
  
- **Updated message format**
  - From: `List<Map<String, Object>>` (Anthropic format)
  - To: `List<Message>` (Gollek SPI format)
  
- **Updated tool definitions**
  - From: `registry.toClaudeToolDefinitions()` returning Claude-specific format
  - To: `registry.toGollekToolDefinitions()` returning `List<ToolDefinition>`
  
- **Updated inference call**
  - From: `client.sendMessage(messages, tools, systemPrompt)` returning `JsonNode`
  - To: `callGollekBlocking(messages, tools, systemPrompt)` returning `InferenceResponse`
  
- **Updated response handling**
  - Text extraction: `AnthropicClient.extractText(response)` → `response.getContent()`
  - Stop reason: `AnthropicClient.stopReason(response)` → `response.getFinishReason().name()`
  - Tool calls: `AnthropicClient.toolUseBlocks(response)` → `response.getToolCalls()`
  
- **Updated tool execution**
  - Renamed `executeSingleTool(JsonNode)` → `executeSingleToolGollek(InferenceResponse.ToolCall)`
  - Renamed `executeToolsParallel(List<JsonNode>)` → `executeToolsParallelGollek(List<InferenceResponse.ToolCall>)`
  - Tool input now comes from `ToolCall.arguments()` instead of `JsonNode.path("input")`
  
- **Added helper methods**
  - `callGollekBlocking()`: Constructs `InferenceRequest` and calls Gollek SDK
  - `convertToJsonNode()`: Converts `Map<String, Object>` to `JsonNode` for tool compatibility

#### Benefits:
✅ **Local inference**: Uses Gollek's local model execution instead of cloud API  
✅ **Type safety**: Uses strongly-typed Gollek SPI classes instead of JsonNode  
✅ **Multi-model support**: Can leverage any model supported by Gollek (GGUF, ONNX, etc.)  
✅ **Streaming ready**: Infrastructure in place for streaming support  
✅ **Better error handling**: Structured `FinishReason` enum instead of string comparison  

### 2. ConversationMemory.java
**File**: `src/main/java/tech/kayys/wayang/agent/orchestration/ConversationMemory.java`

#### Key Changes:
- **Updated message storage**
  - From: `Deque<Turn>` with custom `Turn` record containing `Map<String, Object>`
  - To: `Deque<Message>` using Gollek's `Message` class
  
- **Updated message creation methods**
  - `addUser(String)`: Now creates `Message.user(text)`
  - `addAssistant(List<Map<String, Object>>)`: Changed to `addAssistant(String)` creating `Message.assistant(content)`
  - `addToolResult(String, String)`: Now creates `Message.tool(toolCallId, content)`
  
- **Updated getMessages()**
  - Returns: `List<Message>` instead of `List<Map<String, Object>>`

#### Benefits:
✅ **Type safety**: Uses strongly-typed Message class with Role enum  
✅ **Simpler API**: Factory methods like `Message.user()`, `Message.assistant()`, `Message.tool()`  
✅ **Consistent format**: Aligns with Gollek's message format throughout  

### 3. ToolRegistry.java (NEW)
**File**: `src/main/java/tech/kayys/wayang/agent/orchestration/ToolRegistry.java`

#### New Implementation:
- **Purpose**: Bridge between Golok tools and Gollek inference
- **Key features**:
  - Loads tools via ServiceLoader mechanism
  - Provides `toGollekToolDefinitions()` returning `List<ToolDefinition>`
  - Defines `AgentTool` interface for tool implementations
  - Converts tool schemas to Gollek's expected format

#### AgentTool Interface:
```java
public interface AgentTool {
    String name();
    String description();
    String schema();
    String execute(JsonNode input);
}
```

#### Benefits:
✅ **Standardized tool interface**: Clear contract for tool implementations  
✅ **Auto-discovery**: ServiceLoader-based plugin system  
✅ **Schema conversion**: Automatic conversion to Gollek ToolDefinition format  

## Migration Guide

### For Existing Code Using AgentLoop:

#### Before (Anthropic):
```java
AgentConfig config = AgentConfig.builder()
    .apiKey("sk-ant-...")
    .model("claude-sonnet-4-20250514")
    .build();
    
AgentLoop loop = new AgentLoop(config);
String result = loop.run("Write a Java class that...");
```

#### After (Gollek):
```java
AgentConfig config = AgentConfig.builder()
    .model("hf:Qwen/Qwen2.5-0.5B-Instruct")  // Use any Gollek-supported model
    .build();
    
AgentLoop loop = new AgentLoop(config);
String result = loop.run("Write a Java class that...");
```

### Key Differences:
1. **No API key required**: Local models don't need API keys (but you can still configure one for cloud providers)
2. **Model flexibility**: Can use any model supported by Gollek (GGUF, HuggingFace, ONNX, etc.)
3. **Better tool support**: Native tool calling with structured responses
4. **Streaming ready**: Infrastructure supports streaming (implementation pending)

## Dependencies

The module now requires:
- `gollek-sdk-java-local`: Local Gollek SDK implementation
- `gollek-spi-inference`: Inference SPI (InferenceRequest, InferenceResponse)
- `gollek-spi`: Message class and tool definitions
- `golok-tools-spi`: Tool registry and tool definitions

## Next Steps

### Recommended Improvements:
1. **Implement streaming**: Use `client.streamCompletion()` for real-time token output
2. **Add model configuration**: Update `AgentConfig` to support Gollek-specific settings (provider selection, quantization, etc.)
3. **Implement checkpointing**: Complete `saveCheckpoint()` and `loadCheckpoint()` methods
4. **Add usage metrics**: Extract token usage from `InferenceResponse` (inputTokens, outputTokens, durationMs)
5. **Multi-provider support**: Allow switching between local and cloud providers dynamically
6. **Tool result format**: Consider returning structured results instead of plain strings

### Testing:
- Unit tests for message conversion
- Integration tests with actual Gollek models
- Tool execution tests with real tools
- Performance benchmarks (local vs cloud)

## Compatibility Notes

- **Message format**: Gollek's `Message` class uses `Role` enum (SYSTEM, USER, ASSISTANT, TOOL) instead of string roles
- **Tool calls**: Use `InferenceResponse.ToolCall` record with `name()` and `arguments()` instead of JSON parsing
- **Finish reasons**: Use `FinishReason` enum (STOP, TOOL_CALLS, LENGTH, ERROR) instead of string comparison
- **No API key validation**: Local Gollek doesn't validate API keys, but cloud providers might

## Troubleshooting

### Common Issues:

1. **Model not found**: Ensure the model is available in Gollek's registry. Use `hf:` prefix for HuggingFace models.
   
2. **Tool not found**: Verify tools are registered via ServiceLoader and implement `AgentTool` interface.

3. **Schema errors**: Check that tool schemas are valid JSON and match Gollek's expected format.

4. **Performance**: Local models may be slower than cloud APIs. Consider using quantized GGUF models for better performance.

## Conclusion

The migration to Gollek SDK provides:
- ✅ **Vendor independence**: No longer tied to Anthropic's API
- ✅ **Local execution**: Run models locally for privacy and cost savings
- ✅ **Multi-model support**: Access to hundreds of models via Gollek
- ✅ **Better type safety**: Strongly-typed SPI classes
- ✅ **Extensibility**: Easy to add new tools and features

The agent orchestration is now fully integrated with the Gollek ecosystem while maintaining backward compatibility with existing tool implementations.
