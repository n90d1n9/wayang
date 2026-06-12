package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;

import java.util.Objects;
import java.util.Optional;

/**
 * Applies JSON-RPC method preflight checks before dispatch execution.
 */
final class WayangA2aJsonRpcMethodPreflightPolicy {

    private final WayangA2aSendMessagePreflight sendMessagePreflight;
    private final WayangA2aTenantGuard tenantGuard;
    private final WayangA2aCapabilityGuard capabilityGuard;

    private WayangA2aJsonRpcMethodPreflightPolicy(
            WayangA2aSendMessagePreflight sendMessagePreflight,
            WayangA2aTenantGuard tenantGuard,
            WayangA2aCapabilityGuard capabilityGuard) {
        this.sendMessagePreflight = Objects.requireNonNull(sendMessagePreflight, "sendMessagePreflight");
        this.tenantGuard = Objects.requireNonNull(tenantGuard, "tenantGuard");
        this.capabilityGuard = Objects.requireNonNull(capabilityGuard, "capabilityGuard");
    }

    static WayangA2aJsonRpcMethodPreflightPolicy fromAgentCard(A2aAgentCard agentCard) {
        A2aAgentCard resolved = Objects.requireNonNull(agentCard, "agentCard");
        return new WayangA2aJsonRpcMethodPreflightPolicy(
                WayangA2aSendMessagePreflight.fromAgentCard(resolved),
                WayangA2aTenantGuard.fromAgentCard(resolved),
                WayangA2aCapabilityGuard.forJsonRpc(resolved));
    }

    Result validate(
            WayangA2aJsonRpcRequest request,
            WayangA2aJsonRpcMethodDispatchTable.Entry entry) {
        WayangA2aJsonRpcRequest resolvedRequest = Objects.requireNonNull(request, "request");
        WayangA2aJsonRpcMethodDispatchTable.Entry resolvedEntry = Objects.requireNonNull(entry, "entry");
        WayangA2aSendMessagePreflight.JsonRpcResult sendMessage =
                sendMessagePreflight.validateJsonRpc(resolvedRequest);
        Optional<WayangA2aJsonRpcError> error = validateTenant(resolvedRequest, sendMessage)
                .or(() -> capabilityGuard.validateJsonRpc(resolvedEntry.operation()))
                .or(sendMessage::error);
        return new Result(sendMessage, error);
    }

    private Optional<WayangA2aJsonRpcError> validateTenant(
            WayangA2aJsonRpcRequest request,
            WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
        if (preflight.sendRequest().isPresent()) {
            return tenantGuard.validateJsonRpc(preflight.sendRequest().orElseThrow());
        }
        return tenantGuard.validateJsonRpc(request);
    }

    record Result(
            WayangA2aSendMessagePreflight.JsonRpcResult sendMessage,
            Optional<WayangA2aJsonRpcError> error) {

        Result {
            sendMessage = sendMessage == null
                    ? WayangA2aSendMessagePreflight.JsonRpcResult.empty()
                    : sendMessage;
            error = error == null ? Optional.empty() : error;
        }
    }
}
