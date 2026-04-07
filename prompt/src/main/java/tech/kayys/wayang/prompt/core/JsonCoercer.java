package tech.kayys.wayang.prompt.core;

/**
 * ============================================================================
 * JsonCoercer — strategy for converting objects to JSON strings.
 * ============================================================================
 *
 * Used by the renderer to serialize complex objects (lists, maps, custom types)
 * to their JSON representation when the variable type is JSON.
 */
@FunctionalInterface
public interface JsonCoercer {
    /**
     * Converts the given value to its JSON string representation.
     *
     * @param value The value to convert
     * @return The JSON string representation
     */
    String toJson(Object value);
}