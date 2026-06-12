package tech.kayys.wayang.tool.impl;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.spi.ToolExecutor;
import tech.kayys.wayang.tool.spi.ToolRegistry;
import tech.kayys.wayang.tools.spi.ToolContext;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Default implementation of ToolExecutor.
 */
@ApplicationScoped
public class DefaultToolExecutor implements ToolExecutor {

    @Inject
    ToolRegistry toolRegistry;

    @Override
    public Uni<Map<String, Object>> execute(String toolId, Map<String, Object> arguments, Map<String, Object> context) {
        Map<String, Object> safeArguments = arguments != null ? arguments : Map.of();
        ToolContext toolContext = new ToolContext(
                toolId,
                safeArguments,
                Path.of(System.getProperty("user.dir")),
                System.getenv(),
                Duration.ofSeconds(30),
                false,
                context != null ? context : Map.of());

        return toolRegistry.getTool(toolId)
                .flatMap(tool -> tool.executeAsync(safeArguments, toolContext))
                .map(result -> result.toMap());
    }
}
