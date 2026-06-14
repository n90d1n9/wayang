package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangWorkbenchJsonSchema {

    private WayangWorkbenchJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return WayangWorkbenchContract.SCHEMA.equals(contract.schema())
                && WayangWorkbenchContract.WORKBENCH_DISCOVERY.equals(contract.envelope());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                "Wayang workbench discovery envelope",
                required(),
                properties());
    }

    private static List<String> required() {
        return List.of(
                "product",
                "status",
                "catalog",
                "commandQuery",
                "commandPalette",
                "commands",
                "nextActions");
    }

    private static Map<String, Object> properties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("status", WayangPlatformJsonSchemaProperties.statusProperty());
        properties.put("catalog", WayangPlatformJsonSchemaProperties.catalogProperty());
        properties.put("commandQuery", WayangCommandJsonSchemaProperties.commandQueryProperty());
        properties.put("commandPalette", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("commands", WayangJsonSchemaDocuments.arrayProperty(
                WayangCommandJsonSchemaProperties.commandProperty()));
        properties.put("nextActions", WayangJsonSchemaDocuments.stringArrayProperty());
        return properties;
    }
}
