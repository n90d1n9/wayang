package tech.kayys.wayang.agent.core.skills.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Normalized skill input schema used by validators and tool projections.
 */
public final class SkillParameterSchema {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final List<String> PARAMETER_SCHEMA_KEYS = List.of(
            "parameters",
            "parameterSchema",
            "parameter_schema",
            SkillMetadataKeys.KEY_INPUT_SCHEMA,
            "input_schema",
            "schema");
    private static final Set<String> SCHEMA_ENVELOPE_METADATA_KEYS = Set.of(
            SkillMetadataKeys.KEY_CATEGORY,
            "description",
            "metadata",
            "name",
            "strict",
            "title",
            "type",
            SkillMetadataKeys.KEY_VERSION);
    private static final Set<String> RESERVED_SCHEMA_KEYS = Set.of(
            "$defs",
            "$schema",
            "additionalProperties",
            "definitions",
            "description",
            "properties",
            "required",
            "title",
            "type");

    private final Map<String, Map<String, Object>> properties;
    private final Set<String> required;
    private final boolean additionalPropertiesAllowed;

    private SkillParameterSchema(
            Map<String, Map<String, Object>> properties,
            Set<String> required,
            boolean additionalPropertiesAllowed) {
        this.properties = deepImmutableParameterMap(properties);
        this.required = Collections.unmodifiableSet(new LinkedHashSet<>(required));
        this.additionalPropertiesAllowed = additionalPropertiesAllowed;
    }

    public static SkillParameterSchema empty() {
        return new SkillParameterSchema(Map.of(), Set.of(), true);
    }

    public static SkillParameterSchema resolve(Object source) {
        if (source == null) {
            return empty();
        }
        if (source instanceof SkillManifest manifest) {
            return fromManifest(manifest);
        }
        if (source instanceof String json) {
            return parseJsonParameterSchema(json);
        }
        if (source instanceof Map<?, ?> map) {
            return normalizeSchemaMap(toStringObjectMap(map));
        }

        throw new SkillParameterSchemaException(
                "unsupported parameter schema source: " + source.getClass().getSimpleName());
    }

    public static SkillParameterSchema resolveOrEmpty(Object source, List<String> warnings) {
        if (source != null
                && !(source instanceof SkillManifest)
                && !(source instanceof String)
                && !(source instanceof Map<?, ?>)) {
            if (warnings != null) {
                warnings.add("Unsupported parameter schema source: " + source.getClass().getSimpleName());
            }
            return empty();
        }
        return resolve(source);
    }

    public static SkillParameterSchema fromManifest(SkillManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        Object metadataSchema = firstSchemaCandidate(manifest.getRawMetadata());
        if (metadataSchema == null) {
            metadataSchema = firstSchemaCandidate(manifest.getMetadata());
        }
        if (metadataSchema != null) {
            return normalizeSchemaSource(metadataSchema);
        }

        String legacySchema = manifest.getAllowedToolsString();
        if (looksLikeJsonObject(legacySchema)) {
            return parseJsonParameterSchema(legacySchema);
        }

        return empty();
    }

    public Map<String, Map<String, Object>> properties() {
        return properties;
    }

    public Set<String> required() {
        return required;
    }

    public boolean additionalPropertiesAllowed() {
        return additionalPropertiesAllowed;
    }

    public boolean isEmpty() {
        return properties.isEmpty() && required.isEmpty();
    }

    public Map<String, Object> toJsonSchema() {
        if (isEmpty()) {
            return Map.of();
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", jsonSchemaProperties());
        if (!required.isEmpty()) {
            schema.put("required", List.copyOf(required));
        }
        if (!additionalPropertiesAllowed) {
            schema.put("additionalProperties", false);
        }
        return Collections.unmodifiableMap(schema);
    }

    public ParameterValidation validate(Map<String, Object> providedParameters) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Object> inputs = providedParameters != null ? providedParameters : Map.of();

        if (isEmpty()) {
            return new ParameterValidation(errors, warnings);
        }

        for (String paramName : required) {
            if (!inputs.containsKey(paramName)) {
                errors.add("Missing required parameter: " + paramName);
            }
        }

        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            String paramName = entry.getKey();
            Map<String, Object> paramSchema = properties.get(paramName);
            if (paramSchema == null) {
                if (additionalPropertiesAllowed) {
                    warnings.add("Parameter '" + paramName + "' not defined in skill schema");
                } else {
                    errors.add("Unexpected parameter: " + paramName);
                }
                continue;
            }

            try {
                validateParameterValue(paramName, entry.getValue(), paramSchema, errors);
            } catch (SkillParameterSchemaException e) {
                errors.add("Invalid parameter schema: " + e.getMessage());
            }
        }

        return new ParameterValidation(errors, warnings);
    }

    private static SkillParameterSchema normalizeSchemaSource(Object source) {
        if (source == null) {
            return empty();
        }
        if (source instanceof String json) {
            return parseJsonParameterSchema(json);
        }
        if (source instanceof Map<?, ?> map) {
            return normalizeSchemaMap(toStringObjectMap(map));
        }
        throw new SkillParameterSchemaException("schema source must be a JSON object, map, or JSON string");
    }

    private static SkillParameterSchema parseJsonParameterSchema(String json) {
        if (json == null || json.isBlank()) {
            return empty();
        }
        try {
            return normalizeSchemaMap(JSON.readValue(json, MAP_TYPE));
        } catch (JsonProcessingException e) {
            throw new SkillParameterSchemaException("failed to parse JSON: " + e.getOriginalMessage(), e);
        }
    }

    private static SkillParameterSchema normalizeSchemaMap(Map<String, Object> schema) {
        if (schema.isEmpty()) {
            return empty();
        }

        Object envelope = firstSchemaCandidate(schema);
        if (envelope != null && shouldUnwrapSchemaEnvelope(schema)) {
            return normalizeSchemaSource(envelope);
        }

        validateRootType(schema.get("type"));

        LinkedHashSet<String> required = new LinkedHashSet<>(parseRequired(schema.get("required")));
        boolean additionalPropertiesAllowed = parseAdditionalProperties(schema.get("additionalProperties"));
        Map<String, Object> rawProperties = rawProperties(schema);
        Map<String, Map<String, Object>> properties = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : rawProperties.entrySet()) {
            String paramName = entry.getKey();
            Map<String, Object> paramSchema = normalizePropertySchema(paramName, entry.getValue());
            if (Boolean.TRUE.equals(paramSchema.get("required"))) {
                required.add(paramName);
            }
            properties.put(paramName, immutableMap(paramSchema));
        }

        for (String requiredParam : required) {
            properties.putIfAbsent(requiredParam, Map.of());
        }

        return new SkillParameterSchema(properties, required, additionalPropertiesAllowed);
    }

    private static void validateRootType(Object type) {
        if (type == null) {
            return;
        }
        List<String> declaredTypes = declaredTypes(type);
        if (!declaredTypes.isEmpty() && !declaredTypes.contains("object")) {
            throw new SkillParameterSchemaException("parameter schema root type must be object");
        }
    }

    private static Map<String, Object> rawProperties(Map<String, Object> schema) {
        Object properties = schema.get("properties");
        if (properties != null) {
            if (properties instanceof Map<?, ?> propertyMap) {
                return toStringObjectMap(propertyMap);
            }
            throw new SkillParameterSchemaException("'properties' must be an object");
        }

        Map<String, Object> raw = new LinkedHashMap<>();
        schema.forEach((key, value) -> {
            if (!RESERVED_SCHEMA_KEYS.contains(key)) {
                raw.put(key, value);
            }
        });
        return raw;
    }

    private static Set<String> parseRequired(Object required) {
        if (required == null) {
            return Set.of();
        }
        if (required instanceof Collection<?> values) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (Object value : values) {
                if (!(value instanceof String paramName) || paramName.isBlank()) {
                    throw new SkillParameterSchemaException("'required' must contain non-blank parameter names");
                }
                result.add(paramName);
            }
            return result;
        }
        throw new SkillParameterSchemaException("'required' must be an array of parameter names");
    }

    private static boolean parseAdditionalProperties(Object additionalProperties) {
        if (additionalProperties == null) {
            return true;
        }
        if (additionalProperties instanceof Boolean allowed) {
            return allowed;
        }
        if (additionalProperties instanceof Map<?, ?>) {
            return true;
        }
        throw new SkillParameterSchemaException("'additionalProperties' must be boolean or object");
    }

    private static Map<String, Object> normalizePropertySchema(String paramName, Object propertySchema) {
        if (propertySchema == null) {
            return Map.of();
        }
        if (propertySchema instanceof String type) {
            return Map.of("type", type);
        }
        if (propertySchema instanceof Map<?, ?> map) {
            return toStringObjectMap(map);
        }
        if (propertySchema instanceof Boolean allowed) {
            if (allowed) {
                return Map.of();
            }
            throw new SkillParameterSchemaException("parameter '" + paramName + "' cannot use boolean false schema");
        }
        throw new SkillParameterSchemaException("parameter '" + paramName + "' schema must be an object or type string");
    }

    private static void validateParameterValue(String paramName, Object value, Map<String, Object> paramSchema,
                                               List<String> errors) {
        List<String> types = declaredTypes(paramSchema.get("type"));
        if (!types.isEmpty() && types.stream().noneMatch(type -> matchesType(type, value))) {
            errors.add("Parameter '" + paramName + "' must be " + String.join("|", types)
                    + " but got " + typeName(value));
            return;
        }

        Object enumValues = paramSchema.get("enum");
        if (enumValues instanceof Collection<?> allowedValues && !allowedValues.contains(value)) {
            errors.add("Parameter '" + paramName + "' must be one of " + allowedValues);
        } else if (enumValues != null && !(enumValues instanceof Collection<?>)) {
            errors.add("Parameter '" + paramName + "' has invalid enum schema");
        }
    }

    private static List<String> declaredTypes(Object type) {
        if (type == null) {
            return List.of();
        }
        if (type instanceof String value) {
            if (value.isBlank()) {
                throw new SkillParameterSchemaException("'type' must not be blank");
            }
            return List.of(value);
        }
        if (type instanceof Collection<?> values) {
            List<String> types = new ArrayList<>();
            for (Object value : values) {
                if (!(value instanceof String typeName) || typeName.isBlank()) {
                    throw new SkillParameterSchemaException("'type' array must contain non-blank type names");
                }
                types.add(typeName);
            }
            return List.copyOf(types);
        }
        throw new SkillParameterSchemaException("'type' must be a string or array of strings");
    }

    private static boolean matchesType(String type, Object value) {
        return switch (type) {
            case "array" -> value instanceof Collection<?> || (value != null && value.getClass().isArray());
            case "boolean" -> value instanceof Boolean;
            case "integer" -> isInteger(value);
            case "null" -> value == null;
            case "number" -> value instanceof Number;
            case "object" -> value instanceof Map<?, ?>;
            case "string" -> value instanceof CharSequence;
            default -> throw new SkillParameterSchemaException("unsupported JSON schema type: " + type);
        };
    }

    private static boolean isInteger(Object value) {
        if (!(value instanceof Number number)) {
            return false;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof java.math.BigInteger) {
            return true;
        }
        double asDouble = number.doubleValue();
        return !Double.isNaN(asDouble) && !Double.isInfinite(asDouble) && asDouble % 1 == 0;
    }

    private static Object firstSchemaCandidate(Map<String, Object> map) {
        for (String key : PARAMETER_SCHEMA_KEYS) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static boolean shouldUnwrapSchemaEnvelope(Map<String, Object> schema) {
        if (schema.containsKey("properties")) {
            return false;
        }
        if (schema.size() == 1) {
            return true;
        }
        if (schema.containsKey("inputSchema")
                || schema.containsKey("input_schema")
                || schema.containsKey("parameterSchema")
                || schema.containsKey("parameter_schema")) {
            return true;
        }
        return schema.keySet().stream().allMatch(key -> PARAMETER_SCHEMA_KEYS.contains(key)
                || SCHEMA_ENVELOPE_METADATA_KEYS.contains(key));
    }

    private static boolean looksLikeJsonObject(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    private Map<String, Object> jsonSchemaProperties() {
        Map<String, Object> projected = new LinkedHashMap<>();
        properties.forEach((key, value) -> projected.put(key, jsonSchemaProperty(value)));
        return Collections.unmodifiableMap(projected);
    }

    private static Map<String, Object> jsonSchemaProperty(Map<String, Object> property) {
        Map<String, Object> copy = new LinkedHashMap<>(property);
        if (copy.get("required") instanceof Boolean) {
            copy.remove("required");
        }
        return immutableMap(copy);
    }

    private static Map<String, Object> toStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (!(key instanceof String stringKey) || stringKey.isBlank()) {
                throw new SkillParameterSchemaException("schema keys must be non-blank strings");
            }
            result.put(stringKey, value);
        });
        return result;
    }

    private static Map<String, Map<String, Object>> deepImmutableParameterMap(
            Map<String, Map<String, Object>> source) {
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, immutableMap(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, immutableValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> copy.put(String.valueOf(key), immutableValue(nestedValue)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(SkillParameterSchema::immutableValue)
                    .toList();
        }
        return value;
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    public record ParameterValidation(List<String> errors, List<String> warnings) {
        public ParameterValidation {
            errors = List.copyOf(errors);
            warnings = List.copyOf(warnings);
        }

        public boolean valid() {
            return errors.isEmpty();
        }
    }

    public static class SkillParameterSchemaException extends IllegalArgumentException {
        public SkillParameterSchemaException(String message) {
            super(message);
        }

        public SkillParameterSchemaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
