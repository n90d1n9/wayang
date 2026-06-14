package tech.kayys.wayang.gollek.sdk;

import java.util.List;

final class WayangStandardCatalogJsonSchema {

    private WayangStandardCatalogJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return WayangStandardCatalogContract.SCHEMA.equals(contract.schema())
                && WayangStandardCatalogContract.STANDARDS_CATALOG.equals(contract.envelope());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                "Wayang standards catalog envelope",
                required(),
                WayangStandardCatalogJsonSchemaProperties.properties());
    }

    private static List<String> required() {
        return List.of(
                "product",
                "totalStandards",
                "standardIds",
                "names",
                "versions",
                "bindings",
                "bindingCounts",
                "specUrls",
                "standards");
    }
}
