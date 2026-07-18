package tech.kayys.wayang.command;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;

public  final class WayangCommandDiscoveryJsonSchema {

    private WayangCommandDiscoveryJsonSchema() {
    }

    public static boolean matches(WayangContractDescriptor contract) {
        return WayangCommandDiscoveryContract.SCHEMA.equals(contract.schema())
                && WayangCommandDiscoveryContract.COMMANDS_DISCOVERY.equals(contract.envelope());
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                "Wayang command discovery envelope",
                required(),
                properties());
    }

    private static List<String> required() {
        return List.of(
                "product",
                "surfaceId",
                "profileId",
                "resolvedSurfaceId",
                "category",
                "commandId",
                "contractJsonSchemaId",
                "query",
                "totalCommands",
                "matchingCommands",
                "categories",
                "categoryCounts",
                "categorySummaries",
                "contractJsonSchemaIds",
                "contractJsonSchemaIdCounts",
                "contractSummaries",
                "commandIds");
    }

    private static Map<String, Object> properties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("surfaceId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("profileId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("resolvedSurfaceId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("category", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("commandId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("contractJsonSchemaId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("query", WayangCommandJsonSchemaProperties.commandQueryProperty());
        properties.put("totalCommands", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("matchingCommands", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("categories", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("categoryCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("categorySummaries", WayangJsonSchemaDocuments.arrayProperty(
                WayangCommandJsonSchemaProperties.categorySummaryProperty()));
        properties.put("contractJsonSchemaIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("contractJsonSchemaIdCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("contractSummaries", WayangJsonSchemaDocuments.arrayProperty(
                WayangCommandJsonSchemaProperties.contractSummaryProperty()));
        properties.put("commandIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("commands", WayangJsonSchemaDocuments.arrayProperty(
                WayangCommandJsonSchemaProperties.commandProperty()));
        return properties;
    }
}
