package tech.kayys.wayang.skill;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;

public final class WayangSkillJsonSchema {

    private WayangSkillJsonSchema() {
    }

   public  static boolean matches(WayangContractDescriptor contract) {
        return WayangSkillContract.SCHEMA.equals(contract.schema());
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
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
