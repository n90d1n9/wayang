package tech.kayys.wayang.capability;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;

public  final class WayangProviderCapabilityJsonSchema {

    private WayangProviderCapabilityJsonSchema() {
    }

    public static boolean matches(WayangContractDescriptor contract) {
        return WayangProviderCapabilityContract.SCHEMA.equals(contract.schema());
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
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
