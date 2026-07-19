package tech.kayys.wayang.agent.lifecycle;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;

/**
 * Main orchestrator for agent run lifecycle JSON schema property resolution.
 * Routes contract descriptors to specialized state and event property handlers.
 */
final class AgentRunSchemaHandler {

    private AgentRunSchemaHandler() {
    }

    // Main entry point

    static Map<String, Object> schemaProperties(WayangContractDescriptor contract, String envelope) {
        return switch (envelope) {
            // Result envelope
            case AgentRunLifecycleContract.RUN_RESULT -> AgentRunStateProperties.resultProperties(contract);

            // Status envelope
            case AgentRunLifecycleContract.RUN_STATUS -> AgentRunStateProperties.statusEnvelopeProperties(contract);

            // Events envelopes
            case AgentRunLifecycleContract.RUN_EVENTS -> AgentRunEventProperties.eventsProperties(contract, true);
            case AgentRunLifecycleContract.RUN_EVENTS_STATS -> AgentRunEventProperties.eventsProperties(contract, false);
            case AgentRunLifecycleContract.RUN_EVENTS_FOLLOW -> AgentRunEventProperties.eventsFollowProperties(contract);

            // Inspection envelope
            case AgentRunLifecycleContract.RUN_INSPECT -> AgentRunStateProperties.inspectionProperties(contract);

            // Run list/history envelopes
            case AgentRunLifecycleContract.RUN_LIST -> AgentRunEventProperties.historyProperties(contract, true);
            case AgentRunLifecycleContract.RUN_STATS -> AgentRunEventProperties.historyProperties(contract, false);

            // Wait envelope
            case AgentRunLifecycleContract.RUN_WAIT -> AgentRunStateProperties.waitProperties(contract);

            // Cancel envelope
            case AgentRunLifecycleContract.RUN_CANCEL -> AgentRunStateProperties.cancelProperties(contract);

            // Forget envelope
            case AgentRunLifecycleContract.RUN_FORGET -> AgentRunStateProperties.forgetProperties(contract);

            // Run store envelopes
            case AgentRunLifecycleContract.RUN_STORE -> AgentRunStateProperties.runStoreProperties(contract);
            case AgentRunLifecycleContract.RUN_STORE_VERIFICATION -> AgentRunStateProperties.runStoreVerificationProperties(contract);
            case AgentRunLifecycleContract.RUN_STORE_COMPACTION_PREVIEW -> AgentRunStateProperties.runStoreCompactionPreviewProperties(contract);
            case AgentRunLifecycleContract.RUN_STORE_COMPACTION -> AgentRunStateProperties.runStoreCompactionResultProperties(contract);

            default -> throw new IllegalArgumentException("Unknown lifecycle contract envelope: " + envelope);
        };
    }

    // Shared contract-only properties

    static Map<String, Object> contractOnlyProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("contract", WayangJsonSchemaDocuments.contractProperty(contract));
        return properties;
    }

    // Required properties routing

    static java.util.List<String> requiredProperties(String envelope, Object... params) {
        return switch (envelope) {
            case AgentRunLifecycleContract.RUN_RESULT -> AgentRunStateProperties.resultRequired();
            case AgentRunLifecycleContract.RUN_STATUS -> AgentRunStateProperties.statusEnvelopeRequired();
            case AgentRunLifecycleContract.RUN_EVENTS -> AgentRunEventProperties.eventsRequired(true);
            case AgentRunLifecycleContract.RUN_EVENTS_STATS -> AgentRunEventProperties.eventsRequired(false);
            case AgentRunLifecycleContract.RUN_EVENTS_FOLLOW -> AgentRunEventProperties.eventsFollowRequired();
            case AgentRunLifecycleContract.RUN_INSPECT -> AgentRunStateProperties.inspectionRequired();
            case AgentRunLifecycleContract.RUN_LIST -> AgentRunEventProperties.historyRequired(true);
            case AgentRunLifecycleContract.RUN_STATS -> AgentRunEventProperties.historyRequired(false);
            case AgentRunLifecycleContract.RUN_WAIT -> AgentRunStateProperties.waitRequired();
            case AgentRunLifecycleContract.RUN_CANCEL -> AgentRunStateProperties.cancelRequired();
            case AgentRunLifecycleContract.RUN_FORGET -> AgentRunStateProperties.forgetRequired();
            case AgentRunLifecycleContract.RUN_STORE -> AgentRunStateProperties.runStoreRequired();
            case AgentRunLifecycleContract.RUN_STORE_VERIFICATION -> AgentRunStateProperties.runStoreVerificationRequired();
            case AgentRunLifecycleContract.RUN_STORE_COMPACTION_PREVIEW -> AgentRunStateProperties.runStoreCompactionPreviewRequired();
            case AgentRunLifecycleContract.RUN_STORE_COMPACTION -> AgentRunStateProperties.runStoreCompactionResultRequired();
            default -> throw new IllegalArgumentException("Unknown lifecycle contract envelope: " + envelope);
        };
    }
}
