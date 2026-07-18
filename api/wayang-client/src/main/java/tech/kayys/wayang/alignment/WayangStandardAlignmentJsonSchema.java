package tech.kayys.wayang.alignment;

import java.util.List;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;

public final class WayangStandardAlignmentJsonSchema {

    private WayangStandardAlignmentJsonSchema() {
    }

    public static boolean matches(WayangContractDescriptor contract) {
        return WayangStandardAlignmentContract.SCHEMA.equals(contract.schema())
                && WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH.equals(contract.envelope());
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                "Wayang standard-alignment health envelope",
                List.of("product", "health"),
                WayangStandardAlignmentJsonSchemaProperties.properties());
    }
}
