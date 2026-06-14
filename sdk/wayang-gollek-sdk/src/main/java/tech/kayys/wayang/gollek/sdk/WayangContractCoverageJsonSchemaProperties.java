package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangContractCoverageJsonSchemaProperties {

    private WayangContractCoverageJsonSchemaProperties() {
    }

    static Map<String, Object> properties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("totalContracts", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("totalCommands", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("commandLinkedContracts", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("commandlessContracts", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("incompleteContracts", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("commandContractLinks", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("commandlessEntries", WayangJsonSchemaDocuments.arrayProperty(entryProperty()));
        properties.put("incompleteEntries", WayangJsonSchemaDocuments.arrayProperty(entryProperty()));
        return properties;
    }

    private static Map<String, Object> entryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("schema", WayangJsonSchemaDocuments.stringProperty());
        properties.put("version", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("envelope", WayangJsonSchemaDocuments.stringProperty());
        properties.put("domain", WayangJsonSchemaDocuments.stringProperty());
        properties.put("jsonSchemaId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("declaredCommandIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("linkedCommandIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("unlinkedCommandIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("undeclaredLinkedCommandIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("commandLinked", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("commandless", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("complete", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "schema",
                        "version",
                        "envelope",
                        "domain",
                        "jsonSchemaId",
                        "declaredCommandIds",
                        "linkedCommandIds",
                        "unlinkedCommandIds",
                        "undeclaredLinkedCommandIds",
                        "commandLinked",
                        "commandless",
                        "complete"),
                true,
                properties);
    }
}
