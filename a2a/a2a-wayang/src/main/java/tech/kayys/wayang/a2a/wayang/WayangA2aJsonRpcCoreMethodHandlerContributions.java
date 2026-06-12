package tech.kayys.wayang.a2a.wayang;

import java.util.List;

/**
 * Stable identities for built-in A2A JSON-RPC method handler contributors.
 */
final class WayangA2aJsonRpcCoreMethodHandlerContributions {

    static final String MODULE_ID = "wayang-a2a-wayang";
    static final String TAG_A2A_JSON_RPC = "a2a.jsonrpc";

    static final String GROUP_SEND_MESSAGE = "send-message";
    static final String GROUP_TASK = "task";
    static final String GROUP_AGENT_CARD = "agent-card";

    static final String PROVIDER_SEND_MESSAGE = "wayang.a2a.jsonrpc.send-message";
    static final String PROVIDER_TASK = "wayang.a2a.jsonrpc.task";
    static final String PROVIDER_AGENT_CARD = "wayang.a2a.jsonrpc.agent-card";

    private WayangA2aJsonRpcCoreMethodHandlerContributions() {
    }

    static WayangA2aJsonRpcMethodHandlerContribution sendMessage() {
        return core(
                PROVIDER_SEND_MESSAGE,
                List.of(TAG_A2A_JSON_RPC, GROUP_SEND_MESSAGE, "streaming"));
    }

    static WayangA2aJsonRpcMethodHandlerContribution task() {
        return core(
                PROVIDER_TASK,
                List.of(TAG_A2A_JSON_RPC, GROUP_TASK, "push-config", "subscription"));
    }

    static WayangA2aJsonRpcMethodHandlerContribution agentCard() {
        return core(
                PROVIDER_AGENT_CARD,
                List.of(TAG_A2A_JSON_RPC, GROUP_AGENT_CARD));
    }

    private static WayangA2aJsonRpcMethodHandlerContribution core(
            String providerId,
            List<String> capabilityTags) {
        return new WayangA2aJsonRpcMethodHandlerContribution(providerId, MODULE_ID, capabilityTags, 0);
    }
}
