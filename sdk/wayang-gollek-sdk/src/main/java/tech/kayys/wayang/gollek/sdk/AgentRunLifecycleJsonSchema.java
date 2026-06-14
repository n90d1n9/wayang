package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

final class AgentRunLifecycleJsonSchema {

    private AgentRunLifecycleJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return AgentRunLifecycleContract.SCHEMA.equals(contract.schema());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                required(contract.envelope()),
                properties(contract));
    }

    private static List<String> required(String envelope) {
        return switch (envelope) {
            case AgentRunLifecycleContract.RUN_RESULT -> AgentRunLifecycleJsonSchemaProperties.resultRequired();
            case AgentRunLifecycleContract.RUN_STATUS -> AgentRunLifecycleJsonSchemaProperties.statusEnvelopeRequired();
            case AgentRunLifecycleContract.RUN_EVENTS -> AgentRunLifecycleJsonSchemaProperties.eventsRequired(true);
            case AgentRunLifecycleContract.RUN_EVENTS_STATS ->
                    AgentRunLifecycleJsonSchemaProperties.eventsRequired(false);
            case AgentRunLifecycleContract.RUN_EVENTS_FOLLOW ->
                    AgentRunLifecycleJsonSchemaProperties.eventsFollowRequired();
            case AgentRunLifecycleContract.RUN_INSPECT -> AgentRunLifecycleJsonSchemaProperties.inspectionRequired();
            case AgentRunLifecycleContract.RUN_LIST -> AgentRunLifecycleJsonSchemaProperties.historyRequired(true);
            case AgentRunLifecycleContract.RUN_STATS -> AgentRunLifecycleJsonSchemaProperties.historyRequired(false);
            case AgentRunLifecycleContract.RUN_WAIT -> AgentRunLifecycleJsonSchemaProperties.waitRequired();
            case AgentRunLifecycleContract.RUN_CANCEL -> AgentRunLifecycleJsonSchemaProperties.cancelRequired();
            case AgentRunLifecycleContract.RUN_FORGET -> AgentRunLifecycleJsonSchemaProperties.forgetRequired();
            case AgentRunLifecycleContract.RUN_STORE -> AgentRunLifecycleJsonSchemaProperties.runStoreRequired();
            case AgentRunLifecycleContract.RUN_STORE_VERIFICATION ->
                    AgentRunLifecycleJsonSchemaProperties.runStoreVerificationRequired();
            case AgentRunLifecycleContract.RUN_STORE_COMPACTION_PREVIEW ->
                    AgentRunLifecycleJsonSchemaProperties.runStoreCompactionPreviewRequired();
            case AgentRunLifecycleContract.RUN_STORE_COMPACTION ->
                    AgentRunLifecycleJsonSchemaProperties.runStoreCompactionResultRequired();
            default -> List.of("contract");
        };
    }

    private static Map<String, Object> properties(WayangContractDescriptor contract) {
        return switch (contract.envelope()) {
            case AgentRunLifecycleContract.RUN_RESULT -> AgentRunLifecycleJsonSchemaProperties.resultProperties(contract);
            case AgentRunLifecycleContract.RUN_STATUS ->
                    AgentRunLifecycleJsonSchemaProperties.statusEnvelopeProperties(contract);
            case AgentRunLifecycleContract.RUN_EVENTS -> AgentRunLifecycleJsonSchemaProperties.eventsProperties(contract, true);
            case AgentRunLifecycleContract.RUN_EVENTS_STATS ->
                    AgentRunLifecycleJsonSchemaProperties.eventsProperties(contract, false);
            case AgentRunLifecycleContract.RUN_EVENTS_FOLLOW ->
                    AgentRunLifecycleJsonSchemaProperties.eventsFollowProperties(contract);
            case AgentRunLifecycleContract.RUN_INSPECT -> AgentRunLifecycleJsonSchemaProperties.inspectionProperties(contract);
            case AgentRunLifecycleContract.RUN_LIST -> AgentRunLifecycleJsonSchemaProperties.historyProperties(contract, true);
            case AgentRunLifecycleContract.RUN_STATS -> AgentRunLifecycleJsonSchemaProperties.historyProperties(contract, false);
            case AgentRunLifecycleContract.RUN_WAIT -> AgentRunLifecycleJsonSchemaProperties.waitProperties(contract);
            case AgentRunLifecycleContract.RUN_CANCEL -> AgentRunLifecycleJsonSchemaProperties.cancelProperties(contract);
            case AgentRunLifecycleContract.RUN_FORGET -> AgentRunLifecycleJsonSchemaProperties.forgetProperties(contract);
            case AgentRunLifecycleContract.RUN_STORE -> AgentRunLifecycleJsonSchemaProperties.runStoreProperties(contract);
            case AgentRunLifecycleContract.RUN_STORE_VERIFICATION ->
                    AgentRunLifecycleJsonSchemaProperties.runStoreVerificationProperties(contract);
            case AgentRunLifecycleContract.RUN_STORE_COMPACTION_PREVIEW ->
                    AgentRunLifecycleJsonSchemaProperties.runStoreCompactionPreviewProperties(contract);
            case AgentRunLifecycleContract.RUN_STORE_COMPACTION ->
                    AgentRunLifecycleJsonSchemaProperties.runStoreCompactionResultProperties(contract);
            default -> AgentRunLifecycleJsonSchemaProperties.contractOnlyProperties(contract);
        };
    }
}
