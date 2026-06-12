package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangCommandJsonSchemaProperties {

    private WayangCommandJsonSchemaProperties() {
    }

    static Map<String, Object> commandQueryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("surfaceId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("profileId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("resolvedSurfaceId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("category", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("commandId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("contractJsonSchemaId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("filtered", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "surfaceId",
                        "profileId",
                        "resolvedSurfaceId",
                        "category",
                        "commandId",
                        "contractJsonSchemaId",
                        "filtered"),
                true,
                properties);
    }

    static Map<String, Object> categorySummaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("count", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("commandIds", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(List.of("name", "count", "commandIds"), true, properties);
    }

    static Map<String, Object> contractSummaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("jsonSchemaId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("schema", WayangJsonSchemaDocuments.stringProperty());
        properties.put("version", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("envelope", WayangJsonSchemaDocuments.stringProperty());
        properties.put("count", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("commandIds", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("jsonSchemaId", "schema", "version", "envelope", "count", "commandIds"),
                true,
                properties);
    }

    static Map<String, Object> commandProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", WayangJsonSchemaDocuments.stringProperty());
        properties.put("title", WayangJsonSchemaDocuments.stringProperty());
        properties.put("command", WayangJsonSchemaDocuments.stringProperty());
        properties.put("category", WayangJsonSchemaDocuments.stringProperty());
        properties.put("description", WayangJsonSchemaDocuments.stringProperty());
        properties.put("surfaceIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("localOnly", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("contracts", WayangJsonSchemaDocuments.arrayProperty(commandContractProperty()));
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("id", "title", "command", "category", "description", "surfaceIds", "localOnly"),
                true,
                properties);
    }

    private static Map<String, Object> commandContractProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("schema", WayangJsonSchemaDocuments.stringProperty());
        properties.put("version", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("envelope", WayangJsonSchemaDocuments.stringProperty());
        properties.put("jsonSchemaId", WayangJsonSchemaDocuments.stringProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("schema", "version", "envelope", "jsonSchemaId"),
                true,
                properties);
    }
}
