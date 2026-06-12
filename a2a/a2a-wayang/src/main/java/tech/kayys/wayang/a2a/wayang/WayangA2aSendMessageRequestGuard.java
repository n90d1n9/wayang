package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates SendMessage payload semantics before Wayang task execution.
 */
final class WayangA2aSendMessageRequestGuard {

    private static final String TEXT_PLAIN = "text/plain";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private final WayangA2aSkillRouting skillRouting;

    private WayangA2aSendMessageRequestGuard(A2aAgentCard agentCard) {
        this.skillRouting = WayangA2aSkillRouting.fromAgentCard(Objects.requireNonNull(agentCard, "agentCard"));
    }

    static WayangA2aSendMessageRequestGuard fromAgentCard(A2aAgentCard agentCard) {
        return new WayangA2aSendMessageRequestGuard(agentCard);
    }

    Optional<WayangA2aHttpResponse> validateHttp(A2aSendMessageRequest request) {
        if (request.message().role() != A2aRole.ROLE_USER) {
            return Optional.of(WayangA2aHttpResponse.error(
                    400,
                    "invalid_message_role",
                    "SendMessage requests require a user message role.",
                    roleMetadata(request.message().role())));
        }
        List<String> unsupportedSkillIds = skillRouting.unsupportedSkillIds(request);
        if (!unsupportedSkillIds.isEmpty()) {
            return Optional.of(WayangA2aHttpResponse.error(
                    400,
                    "skill_not_supported",
                    "Requested skills are not advertised by Agent Card: "
                            + String.join(", ", unsupportedSkillIds) + ".",
                    skillMetadata(request, unsupportedSkillIds)));
        }
        List<String> unsupportedInputModes = unsupportedInputModes(request);
        if (!unsupportedInputModes.isEmpty()) {
            return Optional.of(WayangA2aHttpResponse.error(
                    400,
                    "unsupported_input_mode",
                    "Message input modes are not supported: " + String.join(", ", unsupportedInputModes) + ".",
                    inputModeMetadata(request, unsupportedInputModes)));
        }
        return Optional.empty();
    }

    Optional<WayangA2aJsonRpcError> validateJsonRpc(A2aSendMessageRequest request) {
        if (request.message().role() != A2aRole.ROLE_USER) {
            return Optional.of(WayangA2aJsonRpcError.invalidParams(
                    "SendMessage requests require a user message role."));
        }
        List<String> unsupportedSkillIds = skillRouting.unsupportedSkillIds(request);
        if (!unsupportedSkillIds.isEmpty()) {
            return Optional.of(WayangA2aJsonRpcError.invalidParams(
                    "Requested skills are not advertised by Agent Card: "
                            + String.join(", ", unsupportedSkillIds) + "."));
        }
        List<String> unsupportedInputModes = unsupportedInputModes(request);
        if (!unsupportedInputModes.isEmpty()) {
            return Optional.of(WayangA2aJsonRpcError.invalidParams(
                    "Message input modes are not supported: "
                            + String.join(", ", unsupportedInputModes) + "."));
        }
        return Optional.empty();
    }

    private List<String> unsupportedInputModes(A2aSendMessageRequest request) {
        return inputModes(request).stream()
                .filter(mode -> !WayangA2aMediaTypes.supports(skillRouting.inputModes(request), mode))
                .distinct()
                .toList();
    }

    private static List<String> inputModes(A2aSendMessageRequest request) {
        return request.message().parts().stream()
                .map(WayangA2aSendMessageRequestGuard::inputMode)
                .distinct()
                .toList();
    }

    private static String inputMode(A2aPart part) {
        if (part.text() != null) {
            return TEXT_PLAIN;
        }
        if (part.dataPresent()) {
            return APPLICATION_JSON;
        }
        String mediaType = WayangA2aMaps.optional(part.mediaType());
        return mediaType == null ? APPLICATION_OCTET_STREAM : mediaType;
    }

    private static Map<String, Object> roleMetadata(A2aRole role) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("role", role.value());
        metadata.put("expectedRole", A2aRole.ROLE_USER.value());
        return WayangA2aMaps.copyMap(metadata);
    }

    private Map<String, Object> skillMetadata(
            A2aSendMessageRequest request,
            List<String> unsupportedSkillIds) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestedSkillIds", skillRouting.requestedSkillIds(request));
        metadata.put("supportedSkillIds", skillRouting.supportedSkillIds());
        metadata.put("unsupportedSkillIds", unsupportedSkillIds);
        return WayangA2aMaps.copyMap(metadata);
    }

    private Map<String, Object> inputModeMetadata(
            A2aSendMessageRequest request,
            List<String> unsupportedInputModes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("inputModes", inputModes(request));
        metadata.put("supportedInputModes", skillRouting.inputModes(request));
        metadata.put("unsupportedInputModes", unsupportedInputModes);
        return WayangA2aMaps.copyMap(metadata);
    }

}
