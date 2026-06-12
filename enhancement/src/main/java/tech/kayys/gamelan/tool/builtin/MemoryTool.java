package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Explicit memory management tool — lets the LLM save and retrieve facts
 * across sessions.
 *
 * <pre>{@code
 * <!-- Save a preference -->
 * <tool_call>
 *   <n>remember</n>
 *   <key>code-style</key>
 *   <value>This project uses Google Java Style Guide, 2-space indent</value>
 *   <type>PREFERENCE</type>
 * </tool_call>
 *
 * <!-- Look up what we know -->
 * <tool_call>
 *   <n>recall</n>
 *   <query>code style</query>
 * </tool_call>
 *
 * <!-- Forget an entry -->
 * <tool_call>
 *   <n>forget</n>
 *   <key>code-style</key>
 * </tool_call>
 * }</pre>
 */
@ApplicationScoped
public class MemoryTool implements ToolHandler {

    @Inject AgentMemory memory;

    @Override public String toolName() { return "remember"; }

    @Override public List<String> toolNames() {
        return List.of("remember", "recall", "forget", "memory_list");
    }

    @Override public String description() {
        return "Persist facts, preferences, and decisions across sessions. "
                + "Use `remember` to save, `recall` to search, `forget` to delete, "
                + "`memory_list` to show all.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "key   - Unique identifier for the memory entry",
                "value - Content to remember (for remember)",
                "type  - FACT | PREFERENCE | DECISION | COMMAND (optional, auto-detected)",
                "query - Search term (for recall)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        return switch (call.name()) {
            case "remember"     -> doRemember(call);
            case "recall"       -> doRecall(call);
            case "forget"       -> doForget(call);
            case "memory_list"  -> doList();
            default -> ToolResult.failure(toolName(), "Unknown operation: " + call.name());
        };
    }

    private ToolResult doRemember(ToolCall call) {
        String key   = call.param("key");
        String value = call.param("value");
        if (key.isBlank())   return ToolResult.failure("remember", "'key' is required");
        if (value.isBlank()) return ToolResult.failure("remember", "'value' is required");

        AgentMemory.MemoryType type = AgentMemory.MemoryType.FACT;
        String typeStr = call.param("type", "").toUpperCase();
        try {
            if (!typeStr.isBlank()) type = AgentMemory.MemoryType.valueOf(typeStr);
        } catch (IllegalArgumentException ignored) {}

        memory.remember(key, value, type);
        return ToolResult.success("remember",
                "Remembered [" + type + "]: " + key + " = " + value);
    }

    private ToolResult doRecall(ToolCall call) {
        String query = call.param("query", "").toLowerCase();
        List<AgentMemory.MemoryEntry> results = memory.relevant().stream()
                .filter(e -> query.isBlank()
                        || e.key().toLowerCase().contains(query)
                        || e.value().toLowerCase().contains(query))
                .toList();

        if (results.isEmpty()) return ToolResult.success("recall", "No memories found for: " + query);

        String formatted = results.stream()
                .map(e -> "[" + e.type() + "] " + e.key() + ": " + e.value())
                .collect(Collectors.joining("\n"));
        return ToolResult.success("recall",
                results.size() + " memories found:\n\n" + formatted);
    }

    private ToolResult doForget(ToolCall call) {
        String key = call.param("key");
        if (key.isBlank()) return ToolResult.failure("forget", "'key' is required");
        memory.forget(key);
        return ToolResult.success("forget", "Forgotten: " + key);
    }

    private ToolResult doList() {
        List<AgentMemory.MemoryEntry> all = memory.all();
        if (all.isEmpty()) return ToolResult.success("memory_list", "No memories stored.");
        String formatted = all.stream()
                .map(e -> String.format("[%s|%s] %s: %s", e.type(), e.project(), e.key(), e.value()))
                .collect(Collectors.joining("\n"));
        return ToolResult.success("memory_list", all.size() + " memories:\n\n" + formatted);
    }
}
