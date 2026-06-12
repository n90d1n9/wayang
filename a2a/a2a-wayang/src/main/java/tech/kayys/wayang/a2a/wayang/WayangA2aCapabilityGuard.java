package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Enforces optional A2A capabilities advertised by the Agent Card.
 */
final class WayangA2aCapabilityGuard {

    private final A2aAgentCard agentCard;
    private final boolean extendedAgentCardConfigured;

    private WayangA2aCapabilityGuard(A2aAgentCard agentCard, boolean extendedAgentCardConfigured) {
        this.agentCard = Objects.requireNonNull(agentCard, "agentCard");
        this.extendedAgentCardConfigured = extendedAgentCardConfigured;
    }

    static WayangA2aCapabilityGuard forHttp(A2aAgentCard publicAgentCard, A2aAgentCard extendedAgentCard) {
        return new WayangA2aCapabilityGuard(publicAgentCard, extendedAgentCard != null);
    }

    static WayangA2aCapabilityGuard forJsonRpc(A2aAgentCard agentCard) {
        return new WayangA2aCapabilityGuard(agentCard, true);
    }

    Optional<WayangA2aHttpResponse> validateHttp(String operation) {
        if (WayangA2aJsonRpcMethods.requiresPushNotificationCapability(operation)
                && !agentCard.capabilities().pushNotifications()) {
            return Optional.of(WayangA2aHttpResponse.error(
                    400,
                    "push_notification_not_supported",
                    "A2A agent does not support push notifications.",
                    metadata(operation, "pushNotifications")));
        }
        if (WayangA2aJsonRpcMethods.streaming(operation) && !agentCard.capabilities().streaming()) {
            return Optional.of(WayangA2aHttpResponse.error(
                    400,
                    "unsupported_operation",
                    "A2A agent does not support streaming operation: " + operation + ".",
                    metadata(operation, "streaming")));
        }
        if (WayangA2aJsonRpcMethods.requiresExtendedAgentCardCapability(operation)) {
            if (!agentCard.capabilities().extendedAgentCard()) {
                return Optional.of(WayangA2aHttpResponse.error(
                        400,
                        "unsupported_operation",
                        "A2A agent does not support authenticated extended Agent Cards.",
                        metadata(operation, "extendedAgentCard")));
            }
            if (!extendedAgentCardConfigured) {
                return Optional.of(WayangA2aHttpResponse.error(
                        400,
                        "extended_agent_card_not_configured",
                        "A2A agent declares extended Agent Card support but no extended card is configured.",
                        metadata(operation, "extendedAgentCard")));
            }
        }
        return Optional.empty();
    }

    Optional<WayangA2aJsonRpcError> validateJsonRpc(String operation) {
        if (WayangA2aJsonRpcMethods.requiresPushNotificationCapability(operation)
                && !agentCard.capabilities().pushNotifications()) {
            return Optional.of(WayangA2aJsonRpcError.pushNotificationNotSupported());
        }
        if (WayangA2aJsonRpcMethods.streaming(operation) && !agentCard.capabilities().streaming()) {
            return Optional.of(WayangA2aJsonRpcError.unsupportedOperation(
                    "A2A agent does not support streaming operation: " + operation + "."));
        }
        if (WayangA2aJsonRpcMethods.requiresExtendedAgentCardCapability(operation)) {
            if (!agentCard.capabilities().extendedAgentCard()) {
                return Optional.of(WayangA2aJsonRpcError.unsupportedOperation(
                        "A2A agent does not support authenticated extended Agent Cards."));
            }
            if (!extendedAgentCardConfigured) {
                return Optional.of(WayangA2aJsonRpcError.extendedAgentCardNotConfigured());
            }
        }
        return Optional.empty();
    }

    private static Map<String, Object> metadata(String operation, String capability) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("operation", operation == null ? "" : operation);
        metadata.put("capability", capability);
        return WayangA2aMaps.copyMap(metadata);
    }
}
