package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aSendMessageConfiguration;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates SendMessage configuration against advertised Agent Card support.
 */
final class WayangA2aSendMessageConfigurationGuard {

    private final A2aAgentCard agentCard;
    private final WayangA2aSkillRouting skillRouting;

    private WayangA2aSendMessageConfigurationGuard(A2aAgentCard agentCard) {
        this.agentCard = Objects.requireNonNull(agentCard, "agentCard");
        this.skillRouting = WayangA2aSkillRouting.fromAgentCard(this.agentCard);
    }

    static WayangA2aSendMessageConfigurationGuard fromAgentCard(A2aAgentCard agentCard) {
        return new WayangA2aSendMessageConfigurationGuard(agentCard);
    }

    Optional<WayangA2aHttpResponse> validateHttp(A2aSendMessageRequest request) {
        A2aSendMessageConfiguration config = request.configuration();
        if (config == null) {
            return Optional.empty();
        }
        if (!config.taskPushNotificationConfig().isEmpty() && !agentCard.capabilities().pushNotifications()) {
            return Optional.of(WayangA2aHttpResponse.error(
                    400,
                    "push_notification_not_supported",
                    "A2A agent does not support push notifications.",
                    Map.of("capability", "pushNotifications")));
        }
        List<String> supportedOutputModes = skillRouting.outputModes(request);
        List<String> unsupportedOutputModes = unsupportedOutputModes(config, supportedOutputModes);
        if (!unsupportedOutputModes.isEmpty()) {
            return Optional.of(WayangA2aHttpResponse.error(
                    400,
                    "unsupported_output_mode",
                    "Accepted output modes are not supported: " + String.join(", ", unsupportedOutputModes) + ".",
                    outputModeMetadata(config, supportedOutputModes, unsupportedOutputModes)));
        }
        return Optional.empty();
    }

    Optional<WayangA2aJsonRpcError> validateJsonRpc(A2aSendMessageRequest request) {
        A2aSendMessageConfiguration config = request.configuration();
        if (config == null) {
            return Optional.empty();
        }
        if (!config.taskPushNotificationConfig().isEmpty() && !agentCard.capabilities().pushNotifications()) {
            return Optional.of(WayangA2aJsonRpcError.pushNotificationNotSupported());
        }
        List<String> unsupportedOutputModes = unsupportedOutputModes(config, skillRouting.outputModes(request));
        if (!unsupportedOutputModes.isEmpty()) {
            return Optional.of(WayangA2aJsonRpcError.invalidParams(
                    "Accepted output modes are not supported: "
                            + String.join(", ", unsupportedOutputModes) + "."));
        }
        return Optional.empty();
    }

    private List<String> unsupportedOutputModes(
            A2aSendMessageConfiguration config,
            List<String> supportedOutputModes) {
        if (config.acceptedOutputModes().isEmpty()) {
            return List.of();
        }
        return WayangA2aMediaTypes.intersects(supportedOutputModes, config.acceptedOutputModes())
                ? List.of()
                : config.acceptedOutputModes();
    }

    private static Map<String, Object> outputModeMetadata(
            A2aSendMessageConfiguration config,
            List<String> supportedOutputModes,
            List<String> unsupportedOutputModes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("acceptedOutputModes", config.acceptedOutputModes());
        metadata.put("supportedOutputModes", supportedOutputModes);
        metadata.put("unsupportedOutputModes", unsupportedOutputModes);
        return WayangA2aMaps.copyMap(metadata);
    }

}
