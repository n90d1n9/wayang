package tech.kayys.wayang.gollek.cli;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TestJsonSchemaValidator {

    private TestJsonSchemaValidator() {
    }

    static List<String> validate(Map<String, Object> schema, Object value) {
        List<String> errors = new ArrayList<>();
        validate("$", schema, value, errors);
        return errors;
    }

    @SuppressWarnings("unchecked")
    private static void validate(String path, Map<String, Object> schema, Object value, List<String> errors) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        Object oneOf = schema.get("oneOf");
        if (oneOf instanceof List<?> alternatives) {
            int matches = 0;
            for (Object alternative : alternatives) {
                if (alternative instanceof Map<?, ?> alternativeSchema) {
                    List<String> alternativeErrors = new ArrayList<>();
                    validate(path, (Map<String, Object>) alternativeSchema, value, alternativeErrors);
                    if (alternativeErrors.isEmpty()) {
                        matches++;
                    }
                }
            }
            if (matches != 1) {
                errors.add(path + " should match exactly one schema alternative but matched " + matches);
                return;
            }
        }
        if (schema.containsKey("const") && !jsonEquals(schema.get("const"), value)) {
            errors.add(path + " should equal const " + schema.get("const") + " but was " + value);
            return;
        }
        Object type = schema.get("type");
        if (type != null && !matchesType(type, value)) {
            errors.add(path + " should be " + typeName(type) + " but was " + valueType(value));
            return;
        }
        validateNumberBounds(path, schema, value, errors);
        if (value instanceof Map<?, ?> object) {
            validateObject(path, schema, (Map<String, Object>) object, errors);
        }
        if (value instanceof List<?> array && schema.get("items") instanceof Map<?, ?> items) {
            for (int i = 0; i < array.size(); i++) {
                validate(path + "[" + i + "]", (Map<String, Object>) items, array.get(i), errors);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void validateObject(
            String path,
            Map<String, Object> schema,
            Map<String, Object> object,
            List<String> errors) {
        if (schema.get("required") instanceof List<?> required) {
            for (Object key : required) {
                if (key instanceof String name && !object.containsKey(name)) {
                    errors.add(path + " is missing required property '" + name + "'");
                }
            }
        }
        Map<String, Object> properties = schema.get("properties") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            Object propertySchema = properties.get(entry.getKey());
            if (propertySchema instanceof Map<?, ?> nestedSchema) {
                validate(path + "." + entry.getKey(), (Map<String, Object>) nestedSchema, entry.getValue(), errors);
                continue;
            }
            Object additionalProperties = schema.get("additionalProperties");
            if (Boolean.FALSE.equals(additionalProperties)) {
                errors.add(path + " has unexpected property '" + entry.getKey() + "'");
            } else if (additionalProperties instanceof Map<?, ?> additionalSchema) {
                validate(
                        path + "." + entry.getKey(),
                        (Map<String, Object>) additionalSchema,
                        entry.getValue(),
                        errors);
            }
        }
    }

    private static void validateNumberBounds(
            String path,
            Map<String, Object> schema,
            Object value,
            List<String> errors) {
        if (!(value instanceof Number number)) {
            return;
        }
        BigDecimal actual = decimal(number);
        if (schema.get("minimum") instanceof Number minimum && actual.compareTo(decimal(minimum)) < 0) {
            errors.add(path + " should be >= " + minimum + " but was " + value);
        }
        if (schema.get("maximum") instanceof Number maximum && actual.compareTo(decimal(maximum)) > 0) {
            errors.add(path + " should be <= " + maximum + " but was " + value);
        }
    }

    private static boolean matchesType(Object type, Object value) {
        if (type instanceof String name) {
            return matchesSingleType(name, value);
        }
        if (type instanceof List<?> names) {
            return names.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .anyMatch(name -> matchesSingleType(name, value));
        }
        return true;
    }

    private static boolean matchesSingleType(String type, Object value) {
        return switch (type) {
            case "array" -> value instanceof List<?>;
            case "boolean" -> value instanceof Boolean;
            case "integer" -> value instanceof Number number && decimal(number).stripTrailingZeros().scale() <= 0;
            case "null" -> value == null;
            case "number" -> value instanceof Number;
            case "object" -> value instanceof Map<?, ?>;
            case "string" -> value instanceof String;
            default -> true;
        };
    }

    private static boolean jsonEquals(Object expected, Object actual) {
        if (expected instanceof Number expectedNumber && actual instanceof Number actualNumber) {
            return decimal(expectedNumber).compareTo(decimal(actualNumber)) == 0;
        }
        return expected == null ? actual == null : expected.equals(actual);
    }

    private static BigDecimal decimal(Number value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    private static String typeName(Object type) {
        if (type instanceof List<?> list) {
            return String.join("|", list.stream()
                    .map(String::valueOf)
                    .toList());
        }
        return String.valueOf(type);
    }

    private static String valueType(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof List<?>) {
            return "array";
        }
        if (value instanceof Map<?, ?>) {
            return "object";
        }
        return value.getClass().getSimpleName();
    }
}
