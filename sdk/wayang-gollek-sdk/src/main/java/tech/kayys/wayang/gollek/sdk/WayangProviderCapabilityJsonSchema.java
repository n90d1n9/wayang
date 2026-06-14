package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

final class WayangProviderCapabilityJsonSchema {

    private WayangProviderCapabilityJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return WayangProviderCapabilityContract.SCHEMA.equals(contract.schema());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                required(contract.envelope()),
                properties(contract.envelope()));
    }

    private static List<String> required(String envelope) {
        return switch (envelope) {
            case WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY ->
                    WayangProviderCapabilityJsonSchemaProperties.discoveryRequired();
            case WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL ->
                    WayangProviderCapabilityJsonSchemaProperties.detailRequired();
            default -> List.of();
        };
    }

    private static Map<String, Object> properties(String envelope) {
        return switch (envelope) {
            case WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY ->
                    WayangProviderCapabilityJsonSchemaProperties.discoveryProperties();
            case WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL ->
                    WayangProviderCapabilityJsonSchemaProperties.detailProperties();
            default -> Map.of();
        };
    }
}
