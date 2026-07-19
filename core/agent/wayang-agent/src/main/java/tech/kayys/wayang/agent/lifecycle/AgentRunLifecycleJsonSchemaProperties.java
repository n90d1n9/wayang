package tech.kayys.wayang.agent.lifecycle;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.contract.WayangContractDescriptor;

/**
 * Deprecated: Use {@link AgentRunSchemaHandler}, {@link AgentRunStateProperties}, and {@link AgentRunEventProperties} instead.
 * This class is maintained for backward compatibility and delegates all methods.
 */
@Deprecated(forRemoval = true)
final class AgentRunLifecycleJsonSchemaProperties {

    private AgentRunLifecycleJsonSchemaProperties() {
    }

    // Result envelope

    public static List<String> resultRequired() {
        return AgentRunStateProperties.resultRequired();
    }

    public static Map<String, Object> resultProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.resultProperties(contract);
    }

    // Status envelope

    public static List<String> statusEnvelopeRequired() {
        return AgentRunStateProperties.statusEnvelopeRequired();
    }

    public static Map<String, Object> statusEnvelopeProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.statusEnvelopeProperties(contract);
    }

    // Events envelopes

    public static List<String> eventsRequired(boolean includeEvents) {
        return AgentRunEventProperties.eventsRequired(includeEvents);
    }

    public static Map<String, Object> eventsProperties(WayangContractDescriptor contract, boolean includeEvents) {
        return AgentRunEventProperties.eventsProperties(contract, includeEvents);
    }

    public static List<String> eventsFollowRequired() {
        return AgentRunEventProperties.eventsFollowRequired();
    }

    public static Map<String, Object> eventsFollowProperties(WayangContractDescriptor contract) {
        return AgentRunEventProperties.eventsFollowProperties(contract);
    }

    // Inspection envelope

    public static List<String> inspectionRequired() {
        return AgentRunStateProperties.inspectionRequired();
    }

    public static Map<String, Object> inspectionProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.inspectionProperties(contract);
    }

    // History/List envelopes

    public static List<String> historyRequired(boolean includeRuns) {
        return AgentRunEventProperties.historyRequired(includeRuns);
    }

    public static Map<String, Object> historyProperties(WayangContractDescriptor contract, boolean includeRuns) {
        return AgentRunEventProperties.historyProperties(contract, includeRuns);
    }

    // Wait envelope

    public static List<String> waitRequired() {
        return AgentRunStateProperties.waitRequired();
    }

    public static Map<String, Object> waitProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.waitProperties(contract);
    }

    // Cancel envelope

    public static List<String> cancelRequired() {
        return AgentRunStateProperties.cancelRequired();
    }

    public static Map<String, Object> cancelProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.cancelProperties(contract);
    }

    // Forget envelope

    public static List<String> forgetRequired() {
        return AgentRunStateProperties.forgetRequired();
    }

    public static Map<String, Object> forgetProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.forgetProperties(contract);
    }

    // Run store envelopes

    public static List<String> runStoreRequired() {
        return AgentRunStateProperties.runStoreRequired();
    }

    public static Map<String, Object> runStoreProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.runStoreProperties(contract);
    }

    public static List<String> runStoreVerificationRequired() {
        return AgentRunStateProperties.runStoreVerificationRequired();
    }

    public static Map<String, Object> runStoreVerificationProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.runStoreVerificationProperties(contract);
    }

    public static List<String> runStoreCompactionPreviewRequired() {
        return AgentRunStateProperties.runStoreCompactionPreviewRequired();
    }

    public static Map<String, Object> runStoreCompactionPreviewProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.runStoreCompactionPreviewProperties(contract);
    }

    public static List<String> runStoreCompactionResultRequired() {
        return AgentRunStateProperties.runStoreCompactionResultRequired();
    }

    public static Map<String, Object> runStoreCompactionResultProperties(WayangContractDescriptor contract) {
        return AgentRunStateProperties.runStoreCompactionResultProperties(contract);
    }

    // Main handler

    public static Map<String, Object> contractOnlyProperties(WayangContractDescriptor contract) {
        return AgentRunSchemaHandler.contractOnlyProperties(contract);
    }
}
