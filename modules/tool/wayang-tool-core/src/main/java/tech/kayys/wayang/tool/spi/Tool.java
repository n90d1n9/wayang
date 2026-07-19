package tech.kayys.wayang.tool.spi;

import io.smallrye.mutiny.Uni;

import java.util.Map;

/**
 * Legacy tool SPI interface (pre-2.0).
 *
 * <p>This interface is maintained for backward compatibility. New tool implementations
 * should use {@link tech.kayys.wayang.tools.spi.Tool} instead.
 *
 * <p>Use {@code LegacyWayangToolAdapter} to bridge from this interface to the new one.
 *
 * @deprecated Use {@code tech.kayys.wayang.tools.spi.Tool}
 */
@Deprecated(since = "2.0", forRemoval = true)
public interface Tool {

    /**
     * Unique tool identifier.
     *
     * @return tool ID
     */
    String id();

    /**
     * Human-readable tool name.
     *
     * @return tool name
     */
    String name();

    /**
     * Description for the LLM to understand when to use this tool.
     *
     * @return tool description
     */
    String description();

    /**
     * JSON Schema describing the tool's input parameters.
     *
     * @return input schema
     */
    Map<String, Object> inputSchema();

    /**
     * Execute the tool asynchronously with the given parameters.
     *
     * @param params  input parameters as key-value pairs
     * @param context execution context map
     * @return Uni containing the result map
     */
    Uni<Map<String, Object>> execute(Map<String, Object> params, Map<String, Object> context);
}
