package tech.kayys.golok.tools.spi;

import java.util.Map;
import java.util.Optional;

/**
 * Result of a tool execution.
 *
 * <p>
 * Contains:
 * <ul>
 * <li>Success/failure status</li>
 * <li>Output data (text or structured)</li>
 * <li>Error message (if failed)</li>
 * <li>Metadata (duration, truncation info, etc.)</li>
 * </ul>
 *
 * @author golok Team
 * @version 2.0.0
 */
public record ToolResult(
        /**
         * Whether the tool executed successfully.
         */
        boolean success,

        /**
         * Output data from the tool.
         * Can be String for text output or Map/List for structured data.
         */
        Object data,

        /**
         * Error message if execution failed.
         */
        String error,

        /**
         * Additional metadata (duration, truncation, etc.).
         */
        Map<String, Object> metadata) {

    /**
     * Create a successful result with text output.
     *
     * @param output the text output
     * @return tool result
     */
    public static ToolResult success(String output) {
        return new ToolResult(true, output, null, Map.of());
    }

    /**
     * Create a successful result with structured data.
     *
     * @param data the output data
     * @return tool result
     */
    public static ToolResult success(Object data) {
        return new ToolResult(true, data, null, Map.of());
    }

    /**
     * Create a successful result with metadata.
     *
     * @param data     the output data
     * @param metadata additional metadata
     * @return tool result
     */
    public static ToolResult success(Object data, Map<String, Object> metadata) {
        return new ToolResult(true, data, null, metadata);
    }

    /**
     * Create a failed result with error message.
     *
     * @param error the error message
     * @return tool result
     */
    public static ToolResult error(String error) {
        return new ToolResult(false, null, error, Map.of());
    }

    /**
     * Create a failed result with error message and metadata.
     *
     * @param error    the error message
     * @param metadata additional metadata
     * @return tool result
     */
    public static ToolResult error(String error, Map<String, Object> metadata) {
        return new ToolResult(false, null, error, metadata);
    }

    /**
     * Get the output as a string.
     *
     * @return optional string output
     */
    public Optional<String> output() {
        if (data instanceof String str) {
            return Optional.of(str);
        }
        return Optional.empty();
    }

    /**
     * Get the output data cast to a specific type.
     *
     * @param type the expected type
     * @param <T>  the type
     * @return optional typed data
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> data(Class<T> type) {
        if (data == null) {
            return Optional.empty();
        }
        if (type.isInstance(data)) {
            return Optional.of(type.cast(data));
        }
        throw new ClassCastException(
                "Cannot cast " + data.getClass().getName() + " to " + type.getName());
    }

    /**
     * Get a metadata value by key.
     *
     * @param key  the metadata key
     * @param type the expected type
     * @param <T>  the type
     * @return optional metadata value
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> metadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Check if the output was truncated.
     *
     * @return true if truncated
     */
    public boolean isTruncated() {
        return metadata("truncated", Boolean.class).orElse(false);
    }

    /**
     * Get the execution duration in milliseconds.
     *
     * @return duration in ms
     */
    public long durationMs() {
        return metadata("durationMs", Long.class).orElse(0L);
    }
}
