package tech.kayys.golok.tools.spi;

import java.util.List;
import java.util.Map;

/**
 * JSON Schema definition for tool input/output parameters.
 *
 * <p>
 * This record implements JSON Schema Draft 7 format for describing
 * the structure of tool parameters and results.
 *
 * <h3>Usage:</h3>
 * 
 * <pre>{@code
 * JsonSchema schema = JsonSchema.object(Map.of(
 *         "path", JsonSchema.string("File path", true),
 *         "count", JsonSchema.number("Number of results", false)));
 * }</pre>
 *
 * @author golok Team
 * @version 2.0.0
 */
public record JsonSchema(
        /**
         * The type of this schema (string, number, boolean, object, array).
         */
        String type,

        /**
         * Description of this field.
         */
        String description,

        /**
         * Whether this field is required.
         */
        boolean required,

        /**
         * Properties for object type.
         */
        Map<String, JsonSchema> properties,

        /**
         * Required fields for object type.
         */
        List<String> requiredFields,

        /**
         * Items schema for array type.
         */
        JsonSchema items,

        /**
         * Default value for this field.
         */
        Object defaultValue,

        /**
         * Enum values for string type.
         */
        List<Object> enumValues) {

    /**
     * Create a string schema.
     *
     * @param description field description
     * @return string schema
     */
    public static JsonSchema string(String description) {
        return new JsonSchema("string", description, false, Map.of(), List.of(), null, null, null);
    }

    /**
     * Create a string schema.
     *
     * @param description field description
     * @param required    whether required
     * @return string schema
     */
    public static JsonSchema string(String description, boolean required) {
        return new JsonSchema("string", description, required, Map.of(), List.of(), null, null, null);
    }

    /**
     * Create a string schema with enum values.
     *
     * @param description field description
     * @param required    whether required
     * @param values      allowed values
     * @return string schema
     */
    public static JsonSchema enumString(String description, boolean required, Object... values) {
        return new JsonSchema("string", description, required, Map.of(), List.of(), null, null, List.of(values));
    }

    /**
     * Create a number schema.
     *
     * @param description field description
     * @return number schema
     */
    public static JsonSchema number(String description) {
        return new JsonSchema("number", description, false, Map.of(), List.of(), null, null, null);
    }

    /**
     * Create a number schema.
     *
     * @param description field description
     * @param required    whether required
     * @return number schema
     */
    public static JsonSchema number(String description, boolean required) {
        return new JsonSchema("number", description, required, Map.of(), List.of(), null, null, null);
    }

    /**
     * Create an integer schema.
     *
     * @param description field description
     * @return integer schema
     */
    public static JsonSchema integer(String description) {
        return new JsonSchema("integer", description, false, Map.of(), List.of(), null, null, null);
    }

    /**
     * Create an integer schema.
     *
     * @param description field description
     * @param required    whether required
     * @return integer schema
     */
    public static JsonSchema integer(String description, boolean required) {
        return new JsonSchema("integer", description, required, Map.of(), List.of(), null, null, null);
    }

    /**
     * Create a boolean schema.
     *
     * @param description field description
     * @return boolean schema
     */
    public static JsonSchema bool(String description) {
        return new JsonSchema("boolean", description, false, Map.of(), List.of(), null, null, null);
    }

    /**
     * Create a boolean schema.
     *
     * @param description field description
     * @param required    whether required
     * @return boolean schema
     */
    public static JsonSchema bool(String description, boolean required) {
        return new JsonSchema("boolean", description, required, Map.of(), List.of(), null, null, null);
    }

    /**
     * Create an object schema.
     *
     * @param properties field properties
     * @return object schema
     */
    public static JsonSchema object(Map<String, JsonSchema> properties) {
        return new JsonSchema("object", null, false, properties, List.of(), null, null, null);
    }

    /**
     * Create an object schema with required fields.
     *
     * @param properties     field properties
     * @param requiredFields list of required field names
     * @return object schema
     */
    public static JsonSchema object(Map<String, JsonSchema> properties, List<String> requiredFields) {
        return new JsonSchema("object", null, false, properties, requiredFields, null, null, null);
    }

    /**
     * Create an array schema.
     *
     * @param items schema of array items
     * @return array schema
     */
    public static JsonSchema array(JsonSchema items) {
        return new JsonSchema("array", null, false, Map.of(), List.of(), items, null, null);
    }

    /**
     * Create an array schema.
     *
     * @param items       schema of array items
     * @param description array description
     * @return array schema
     */
    public static JsonSchema array(JsonSchema items, String description) {
        return new JsonSchema("array", description, false, Map.of(), List.of(), items, null, null);
    }

    /**
     * Convert this schema to a Map representation.
     *
     * @return schema as map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap() {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("type", type);

        if (description != null) {
            result.put("description", description);
        }

        if (properties != null && !properties.isEmpty()) {
            Map<String, Object> propsMap = new java.util.LinkedHashMap<>();
            properties.forEach((k, v) -> propsMap.put(k, v.toMap()));
            result.put("properties", propsMap);
        }

        if (requiredFields != null && !requiredFields.isEmpty()) {
            result.put("required", requiredFields);
        }

        if (items != null) {
            result.put("items", items.toMap());
        }

        if (enumValues != null && !enumValues.isEmpty()) {
            result.put("enum", enumValues);
        }

        if (defaultValue != null) {
            result.put("default", defaultValue);
        }

        return result;
    }
}
