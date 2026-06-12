package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangContractEnvelopeJsonSchema {

    private WayangContractEnvelopeJsonSchema() {
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                List.of("contract"),
                properties(contract));
    }

    private static Map<String, Object> properties(WayangContractDescriptor contract) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", WayangJsonSchemaDocuments.contractProperty(contract));
        return values;
    }
}
