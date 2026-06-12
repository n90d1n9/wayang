package tech.kayys.wayang.tools.spi;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ToolRegistry}.
 *
 * <p>Features:
 * <ul>
 * <li>CDI-based auto-discovery of tools</li>
 * <li>Tool grouping by category</li>
 * <li>Enable/disable functionality</li>
 * </ul>
 *
 * @author Wayang Team
 * @version 2.0.0
 */
@ApplicationScoped
public class DefaultToolRegistry implements ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultToolRegistry.class);

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final Map<String, String> toolGroups = new ConcurrentHashMap<>();
    private final Set<String> enabledTools = ConcurrentHashMap.newKeySet();

    /**
     * Constructor with auto-discovery of tools via CDI.
     */
    @Inject
    public DefaultToolRegistry(Instance<Tool> toolInstances) {
        log.info("Initializing DefaultToolRegistry with auto-discovery");
        
        int count = 0;
        for (Tool tool : toolInstances) {
            register(tool);
            count++;
        }
        
        log.info("DefaultToolRegistry initialized with {} tools", count);
    }

    @Override
    public void register(Tool tool) {
        tools.put(tool.id(), tool);
        enabledTools.add(tool.id());
        log.debug("Registered tool: {}", tool.id());
    }

    @Override
    public void register(Tool tool, String group) {
        register(tool);
        toolGroups.put(tool.id(), group);
        log.debug("Registered tool {} in group {}", tool.id(), group);
    }

    @Override
    public Optional<Tool> get(String toolId) {
        return Optional.ofNullable(tools.get(toolId));
    }

    @Override
    public boolean has(String toolId) {
        return tools.containsKey(toolId);
    }

    @Override
    public List<Tool> all() {
        return List.copyOf(tools.values());
    }

    @Override
    public List<Tool> byGroup(String group) {
        return tools.entrySet().stream()
                .filter(e -> group.equals(toolGroups.get(e.getKey())))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> groups() {
        return new ArrayList<>(new HashSet<>(toolGroups.values()));
    }

    @Override
    public void enable(String toolId) {
        if (has(toolId)) {
            enabledTools.add(toolId);
            log.info("Enabled tool: {}", toolId);
        }
    }

    @Override
    public void disable(String toolId) {
        enabledTools.remove(toolId);
        log.info("Disabled tool: {}", toolId);
    }

    @Override
    public boolean isEnabled(String toolId) {
        return enabledTools.contains(toolId);
    }

    @Override
    public int count() {
        return tools.size();
    }

    @Override
    public int activeCount() {
        return enabledTools.size();
    }

    @Override
    public Uni<Map<String, Object>> execute(String toolId,
                                             Map<String, Object> params,
                                             ToolContext context) {
        // Check if tool exists
        if (!has(toolId)) {
            return Uni.createFrom().failure(new ToolNotFoundException(toolId));
        }

        // Check if tool is enabled
        if (!isEnabled(toolId)) {
            return Uni.createFrom().failure(new ToolDisabledException(toolId));
        }

        // Execute tool directly
        return get(toolId)
            .map(tool -> tool.executeAsync(params, context))
            .orElseThrow(() -> new ToolNotFoundException(toolId))
            .map(ToolResult::toMap);
    }

    @Override
    public List<Map<String, Object>> toToolDefinitions() {
        return enabledTools.stream()
                .map(tools::get)
                .filter(Objects::nonNull)
                .map(tool -> Map.of(
                    "name", tool.id(),
                    "description", tool.description(),
                    "parameters", tool.inputSchema()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public String listTools() {
        StringBuilder sb = new StringBuilder();
        sb.append("Registered Tools (").append(count()).append(" total, ")
          .append(activeCount()).append(" active):\n");

        Map<String, List<Tool>> grouped = tools.values().stream()
                .collect(Collectors.groupingBy(t -> 
                    toolGroups.getOrDefault(t.id(), "uncategorized")));

        grouped.forEach((group, toolList) -> {
            sb.append("\n").append(group.toUpperCase()).append(":\n");
            toolList.forEach(tool -> {
                String status = isEnabled(tool.id()) ? "✓" : "✗";
                sb.append("  ").append(status).append(" ")
                  .append(tool.id()).append(" - ")
                  .append(tool.description()).append("\n");
            });
        });

        return sb.toString();
    }

    /**
     * Tool not found exception.
     */
    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String toolId) {
            super("Tool not found: " + toolId);
        }
    }

    /**
     * Tool disabled exception.
     */
    public static class ToolDisabledException extends RuntimeException {
        public ToolDisabledException(String toolId) {
            super("Tool is disabled: " + toolId);
        }
    }
}
