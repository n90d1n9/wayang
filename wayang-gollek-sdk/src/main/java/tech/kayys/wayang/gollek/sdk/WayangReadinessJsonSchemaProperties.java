package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangReadinessJsonSchemaProperties {

    private WayangReadinessJsonSchemaProperties() {
    }

    static Map<String, Object> reportProperties() {
        return properties(WayangJsonSchemaDocuments.openObjectProperty());
    }

    static Map<String, Object> aggregateProperties() {
        return properties(aggregateAttributesProperty());
    }

    private static Map<String, Object> properties(Map<String, Object> attributes) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("readinessId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("ready", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("exitCode", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("probes", WayangJsonSchemaDocuments.arrayProperty(probeProperty()));
        properties.put("issues", WayangJsonSchemaDocuments.arrayProperty(issueProperty()));
        properties.put("attributes", attributes);
        return properties;
    }

    private static Map<String, Object> probeProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("probe", WayangJsonSchemaDocuments.stringProperty());
        properties.put("required", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("passed", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("attributes", WayangJsonSchemaDocuments.openObjectProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("probe", "required", "passed", "issueCount", "attributes"),
                true,
                properties);
    }

    private static Map<String, Object> issueProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("code", WayangJsonSchemaDocuments.stringProperty());
        properties.put("source", WayangJsonSchemaDocuments.stringProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("componentReadinessId", WayangJsonSchemaDocuments.stringProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("code", "source", "message"),
                true,
                properties);
    }

    private static Map<String, Object> aggregateAttributesProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("readinessProfileId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("readinessProfileDefault", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("readinessProfileProduction", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("readinessProfileComponentIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("componentCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("readyComponentCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("failedComponentCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("componentReadinessIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("failedReadinessIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("componentSummaries", WayangJsonSchemaDocuments.arrayProperty(componentSummaryProperty()));
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "componentCount",
                        "readyComponentCount",
                        "failedComponentCount",
                        "componentReadinessIds",
                        "failedReadinessIds",
                        "componentSummaries"),
                true,
                properties);
    }

    private static Map<String, Object> componentSummaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("readinessId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("ready", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("exitCode", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("readinessId", "ready", "exitCode", "issueCount"),
                true,
                properties);
    }
}
