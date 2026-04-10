package tech.kayys.wayang.tools.spi;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Context provided to tools during execution.
 *
 * <p>
 * Contains execution environment information and configuration:
 * <ul>
 * <li>Working directory for file operations</li>
 * <li>Environment variables</li>
 * <li>Timeout settings</li>
 * <li>Dry-run mode</li>
 * <li>Custom context data</li>
 * </ul>
 *
 * @author golok Team
 * @version 2.0.0
 */
public record ToolContext(
        /**
         * The ID of the tool being executed.
         */
        String toolId,

        /**
         * The input parameters for the tool.
         */
        Map<String, Object> inputs,

        /**
         * Working directory for relative path resolution.
         */
        Path workingDirectory,

        /**
         * Environment variables available to the tool.
         */
        Map<String, String> environment,

        /**
         * Maximum execution time.
         */
        Duration timeout,

        /**
         * If true, tool should describe what it would do without executing.
         */
        boolean dryRun,

        /**
         * Custom context data (tenant ID, run ID, step number, etc.).
         */
        Map<String, Object> customData) {

    /**
     * Create a default context with standard settings.
     *
     * @return default tool context
     */
    public static ToolContext defaults() {
        return new ToolContext(
                "unknown",
                Map.of(),
                Path.of(System.getProperty("user.dir")),
                System.getenv(),
                Duration.ofSeconds(30),
                false,
                Map.of());
    }

    /**
     * Create a context with custom working directory.
     *
     * @param workingDirectory the working directory
     * @return new tool context
     */
    public static ToolContext withDirectory(Path workingDirectory) {
        return new ToolContext(
                "unknown",
                Map.of(),
                workingDirectory,
                System.getenv(),
                Duration.ofSeconds(30),
                false,
                Map.of());
    }

    /**
     * Get a custom data value by key.
     *
     * @param key  the data key
     * @param type the expected type
     * @param <T>  the type
     * @return optional value
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getCustomData(String key, Class<T> type) {
        Object value = customData.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        throw new ClassCastException(
                "Cannot cast " + value.getClass().getName() + " to " + type.getName());
    }

    /**
     * Get the tenant ID from custom data.
     *
     * @return optional tenant ID
     */
    public Optional<String> tenantId() {
        return getCustomData("tenantId", String.class);
    }

    /**
     * Get the run ID from custom data.
     *
     * @return optional run ID
     */
    public Optional<String> runId() {
        return getCustomData("runId", String.class);
    }

    /**
     * Get the step number from custom data.
     *
     * @return optional step number
     */
    public Optional<Integer> stepNumber() {
        return getCustomData("stepNumber", Integer.class);
    }
}
