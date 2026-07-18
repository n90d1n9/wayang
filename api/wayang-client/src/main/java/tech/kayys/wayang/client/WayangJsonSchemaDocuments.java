package tech.kayys.wayang.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;

final public class WayangJsonSchemaDocuments {

    private static final String DIALECT = "https://json-schema.org/draft/2020-12/schema";

    private WayangJsonSchemaDocuments() {
    }

    public static Map<String, Object> baseDocument(WayangContractDescriptor contract, String title) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("$schema", DIALECT);
        document.put("$id", contract.jsonSchemaId());
        document.put("title", title);
        document.put("description", contract.description());
        return document;
    }

    public static WayangContractJsonSchema envelopeSchema(
            WayangContractDescriptor contract,
            List<String> required,
            Map<String, Object> properties) {
        return envelopeSchema(contract, "Wayang " + contract.envelope() + " envelope", required, properties);
    }

    public static WayangContractJsonSchema envelopeSchema(
            WayangContractDescriptor contract,
            String title,
            List<String> required,
            Map<String, Object> properties) {
        return objectSchema(contract, title, required, true, properties);
    }

    public static WayangContractJsonSchema objectSchema(
            WayangContractDescriptor contract,
            String title,
            List<String> required,
            boolean additionalProperties,
            Map<String, Object> properties) {
        Map<String, Object> document = baseDocument(contract, title);
        document.put("type", "object");
        document.put("additionalProperties", additionalProperties);
        document.put("required", required == null ? List.of() : List.copyOf(required));
        document.put("properties", properties == null ? Map.of() : new LinkedHashMap<>(properties));
        putWayangMetadata(document, contract);
        return new WayangContractJsonSchema(contract, contract.jsonSchemaId(), document);
    }

    public static void putWayangMetadata(Map<String, Object> document, WayangContractDescriptor contract) {
        document.put("x-wayang-schema", contract.schema());
        document.put("x-wayang-version", contract.version());
        document.put("x-wayang-envelope", contract.envelope());
        document.put("x-wayang-domain", contract.domain());
        document.put("x-wayang-commandIds", contract.commandIds());
    }

    public static Map<String, Object> objectProperty(
            List<String> required,
            boolean additionalProperties,
            Map<String, Object> properties) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "object");
        values.put("required", required);
        values.put("additionalProperties", additionalProperties);
        values.put("properties", properties);
        return values;
    }

    public static Map<String, Object> arrayProperty(Map<String, Object> items) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "array");
        values.put("items", items);
        return values;
    }

    public static Map<String, Object> oneOfProperty(List<Map<String, Object>> alternatives) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("oneOf", alternatives);
        return values;
    }

    public static Map<String, Object> stringArrayProperty() {
        return arrayProperty(stringProperty());
    }

    public static Map<String, Object> countMapProperty() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "object");
        values.put("additionalProperties", nonNegativeIntegerProperty());
        return values;
    }

    public static Map<String, Object> openObjectProperty() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "object");
        values.put("additionalProperties", true);
        return values;
    }

    public static Map<String, Object> stringProperty() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "string");
        return values;
    }

    public static Map<String, Object> nullableStringProperty() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", List.of("string", "null"));
        return values;
    }

    public static Map<String, Object> booleanProperty() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "boolean");
        return values;
    }

    public static Map<String, Object> nonNegativeIntegerProperty() {
        return integerProperty(0);
    }

    public static Map<String, Object> integerProperty() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "integer");
        return values;
    }

    public static Map<String, Object> positiveIntegerProperty() {
        return integerProperty(1);
    }

    public static Map<String, Object> constValue(Object value) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("const", value);
        return values;
    }

    public static Map<String, Object> contractProperty(WayangContractDescriptor contract) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "object");
        values.put("required", List.of("schema", "version", "envelope"));
        values.put("additionalProperties", false);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("schema", constValue(contract.schema()));
        properties.put("version", constValue(contract.version()));
        properties.put("envelope", constValue(contract.envelope()));
        values.put("properties", properties);
        return values;
    }

    private static Map<String, Object> integerProperty(int minimum) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "integer");
        values.put("minimum", minimum);
        return values;
    }
}
