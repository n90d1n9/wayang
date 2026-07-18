package tech.kayys.wayang.workbench;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.client.WayangPlatformJsonSchemaProperties;
import tech.kayys.wayang.command.WayangCommandJsonSchemaProperties;
import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;


public final class WayangWorkbenchJsonSchema {

    private WayangWorkbenchJsonSchema() {
    }

    public static boolean matches(WayangContractDescriptor contract) {
        return WayangWorkbenchContract.SCHEMA.equals(contract.schema())
                && WayangWorkbenchContract.WORKBENCH_DISCOVERY.equals(contract.envelope());
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
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
