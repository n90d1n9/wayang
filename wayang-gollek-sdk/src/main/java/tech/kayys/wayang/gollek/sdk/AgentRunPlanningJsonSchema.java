package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

final class AgentRunPlanningJsonSchema {

    private AgentRunPlanningJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return AgentRunPlanningContract.SCHEMA.equals(contract.schema());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                required(contract.envelope()),
                properties(contract));
    }

    private static List<String> required(String envelope) {
        return switch (envelope) {
            case AgentRunPlanningContract.RUN_PREFLIGHT -> AgentRunPlanningJsonSchemaProperties.preflightRequired();
            case AgentRunPlanningContract.RUN_PREVIEW -> AgentRunPlanningJsonSchemaProperties.previewRequired();
            default -> List.of("contract");
        };
    }

    private static Map<String, Object> properties(WayangContractDescriptor contract) {
        return switch (contract.envelope()) {
            case AgentRunPlanningContract.RUN_PREFLIGHT ->
                    AgentRunPlanningJsonSchemaProperties.preflightProperties(contract);
            case AgentRunPlanningContract.RUN_PREVIEW -> AgentRunPlanningJsonSchemaProperties.previewProperties(contract);
            default -> Map.of("contract", WayangJsonSchemaDocuments.contractProperty(contract));
        };
    }
}
