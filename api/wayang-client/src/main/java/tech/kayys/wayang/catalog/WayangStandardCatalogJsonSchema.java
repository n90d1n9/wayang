package tech.kayys.wayang.catalog;

import java.util.List;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;

public final class WayangStandardCatalogJsonSchema {

    private WayangStandardCatalogJsonSchema() {
    }

    public static boolean matches(WayangContractDescriptor contract) {
        return WayangStandardCatalogContract.SCHEMA.equals(contract.schema())
                && WayangStandardCatalogContract.STANDARDS_CATALOG.equals(contract.envelope());
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
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
