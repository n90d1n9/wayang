package tech.kayys.gamelan.tool.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Task list / TODO tracker — lets the agent manage work across multi-step tasks.
 *
 * <h2>Why this matters</h2>
 * Claude Code, Cursor, and Qwen-Agent all maintain a visible task list during
 * complex multi-file operations. Users can see progress. The agent can check
 * off tasks and recover if interrupted. Without this, long-running tasks like
 * "add tests to all 20 service classes" produce no progress feedback and are
 * unrecoverable if interrupted.
 *
 * <pre>{@code
 * <!-- Create tasks -->
 * <tool_call>
 *   <n>todo</n>
 *   <action>add</action>
 *   <title>Add unit tests to UserService</title>
 *   <priority>high</priority>
 * </tool_call>
 *
 * <!-- Mark done -->
 * <tool_call>
 *   <n>todo</n>
 *   <action>done</action>
 *   <id>1</id>
 * </tool_call>
 *
 * <!-- List all -->
 * <tool_call>
 *   <n>todo</n>
 *   <action>list</action>
 * </tool_call>
 * }</pre>
 */
@ApplicationScoped
public class TodoTool implements ToolHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Path TODO_DIR =
            Path.of(System.getProperty("user.home"), ".gamelan", "todos");

    private final Map<Integer, TodoItem> items = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private Path todoFile;

    @PostConstruct
    void init() {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        todoFile = TODO_DIR.resolve(project + "-todos.json");
        load();
    }

    @Override public String toolName() { return "todo"; }

    @Override public String description() {
        return "Manage a task list for the current session. "
                + "Actions: add, done, list, clear. "
                + "Use this to track multi-step work and show progress to the user.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "action   - add | done | list | clear | remove",
                "title    - Task description (for add)",
                "id       - Task ID (for done/remove)",
                "priority - high | medium | low (for add, default: medium)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        return switch (call.param("action", "list")) {
            case "add"    -> doAdd(call);
            case "done"   -> doDone(call);
            case "remove" -> doRemove(call);
            case "list"   -> doList();
            case "clear"  -> doClear();
            default -> ToolResult.failure(toolName(),
                    "Unknown action. Use: add, done, list, clear, remove");
        };
    }

    private ToolResult doAdd(ToolCall call) {
        String title = call.param("title").strip();
        if (title.isBlank()) return ToolResult.failure(toolName(), "'title' is required for add");
        String priority = call.param("priority", "medium").toLowerCase();
        int id = nextId.getAndIncrement();
        items.put(id, new TodoItem(id, title, false, priority, Instant.now()));
        persist();
        return ToolResult.success(toolName(), "Added task #" + id + ": " + title);
    }

    private ToolResult doDone(ToolCall call) {
        int id = parseId(call);
        if (id < 0) return ToolResult.failure(toolName(), "'id' is required and must be a number");
        TodoItem item = items.get(id);
        if (item == null) return ToolResult.failure(toolName(), "Task #" + id + " not found");
        items.put(id, new TodoItem(id, item.title(), true, item.priority(), item.createdAt()));
        persist();
        return ToolResult.success(toolName(), "✓ Done: #" + id + " " + item.title());
    }

    private ToolResult doRemove(ToolCall call) {
        int id = parseId(call);
        if (id < 0) return ToolResult.failure(toolName(), "'id' is required");
        TodoItem removed = items.remove(id);
        if (removed == null) return ToolResult.failure(toolName(), "Task #" + id + " not found");
        persist();
        return ToolResult.success(toolName(), "Removed task #" + id);
    }

    private ToolResult doList() {
        if (items.isEmpty()) return ToolResult.success(toolName(), "No tasks. Use todo add to create one.");
        StringBuilder sb = new StringBuilder();
        long done    = items.values().stream().filter(TodoItem::done).count();
        long pending = items.size() - done;
        sb.append(String.format("Tasks (%d pending, %d done):\n\n", pending, done));

        // Group by priority then done status
        items.values().stream()
            .sorted(Comparator.comparing(TodoItem::done)
                .thenComparing(t -> priorityOrder(t.priority())))
            .forEach(t -> {
                String icon = t.done() ? "✓" : priorityIcon(t.priority());
                sb.append(String.format("  %s #%d [%s] %s%n",
                        icon, t.id(), t.priority(), t.title()));
            });
        return ToolResult.success(toolName(), sb.toString());
    }

    private ToolResult doClear() {
        items.clear();
        nextId.set(1);
        persist();
        return ToolResult.success(toolName(), "All tasks cleared.");
    }

    private int parseId(ToolCall call) {
        try { return Integer.parseInt(call.param("id").strip()); }
        catch (NumberFormatException e) { return -1; }
    }

    private int priorityOrder(String p) {
        return switch (p) { case "high" -> 0; case "medium" -> 1; default -> 2; };
    }

    private String priorityIcon(String p) {
        return switch (p) { case "high" -> "!"; case "medium" -> "·"; default -> " "; };
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(todoFile)) return;
        try {
            List<TodoItem> loaded = MAPPER.readValue(todoFile.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, TodoItem.class));
            loaded.forEach(t -> {
                items.put(t.id(), t);
                if (t.id() >= nextId.get()) nextId.set(t.id() + 1);
            });
        } catch (IOException e) { /* start fresh */ }
    }

    private void persist() {
        try {
            Files.createDirectories(TODO_DIR);
            MAPPER.writerWithDefaultPrettyPrinter()
                  .writeValue(todoFile.toFile(), new ArrayList<>(items.values()));
        } catch (IOException ignored) {}
    }

    public record TodoItem(int id, String title, boolean done,
                           String priority, Instant createdAt) {}
}
