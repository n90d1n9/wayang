package tech.kayys.wayang.gollek.sdk;

import java.util.List;

final class WayangStandardAlignmentJsonSchema {

    private WayangStandardAlignmentJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return WayangStandardAlignmentContract.SCHEMA.equals(contract.schema())
                && WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH.equals(contract.envelope());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                "Wayang standard-alignment health envelope",
                List.of("product", "health"),
                WayangStandardAlignmentJsonSchemaProperties.properties());
    }
}
