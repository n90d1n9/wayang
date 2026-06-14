package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

final class WayangSkillJsonSchema {

    private WayangSkillJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return WayangSkillContract.SCHEMA.equals(contract.schema());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                required(contract.envelope()),
                properties(contract.envelope()));
    }

    private static List<String> required(String envelope) {
        return switch (envelope) {
            case WayangSkillContract.SKILL_DISCOVERY -> WayangSkillJsonSchemaProperties.discoveryRequired();
            case WayangSkillContract.SKILL_DETAIL -> WayangSkillJsonSchemaProperties.detailRequired();
            default -> List.of();
        };
    }

    private static Map<String, Object> properties(String envelope) {
        return switch (envelope) {
            case WayangSkillContract.SKILL_DISCOVERY -> WayangSkillJsonSchemaProperties.discoveryProperties();
            case WayangSkillContract.SKILL_DETAIL -> WayangSkillJsonSchemaProperties.detailProperties();
            default -> Map.of();
        };
    }
}
