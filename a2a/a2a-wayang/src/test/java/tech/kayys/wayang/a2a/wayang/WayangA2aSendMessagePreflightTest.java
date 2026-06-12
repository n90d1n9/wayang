package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageConfiguration;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSendMessagePreflightTest {

    private final WayangA2aSendMessagePreflight preflight = WayangA2aSendMessagePreflight.fromAgentCard(card());

    @Test
    void validatesHttpRequestThenConfigurationFromOnePreflightBoundary() {
        WayangA2aHttpRequest agentRole = WayangA2aHttpRequest.sendMessage(request(
                "message-role",
                A2aRole.ROLE_AGENT,
                List.of(A2aPart.text("ping")),
                null).toJson());
        WayangA2aHttpRequest unsupportedOutput = WayangA2aHttpRequest.sendMessage(request(
                "message-output",
                A2aRole.ROLE_USER,
                List.of(A2aPart.text("ping")),
                new A2aSendMessageConfiguration(List.of("application/json"), Map.of(), null, null)).toJson());

        WayangA2aHttpResponse roleRejected = preflight
                .validateHttp(agentRole, A2aProtocol.OPERATION_SEND_MESSAGE)
                .error()
                .orElseThrow();
        WayangA2aHttpResponse outputRejected = preflight
                .validateHttp(unsupportedOutput, A2aProtocol.OPERATION_SEND_MESSAGE)
                .error()
                .orElseThrow();

        assertThat(errorCode(roleRejected)).isEqualTo("invalid_message_role");
        assertThat(errorCode(outputRejected)).isEqualTo("unsupported_output_mode");
        assertThat(errorMetadata(roleRejected).keySet()).containsExactly("role", "expectedRole");
        assertThat(errorMetadata(outputRejected).keySet()).containsExactly(
                "acceptedOutputModes",
                "supportedOutputModes",
                "unsupportedOutputModes");
    }

    @Test
    void projectsHttpPreflightRequestErrorMetadataInStableOrder() {
        WayangA2aHttpRequest unsupportedSkills = WayangA2aHttpRequest.sendMessage(request(
                "message-skills",
                A2aRole.ROLE_USER,
                List.of(A2aPart.text("ping")),
                null,
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("rag", "tools")),
                Map.of()).toJson());
        WayangA2aHttpRequest unsupportedInput = WayangA2aHttpRequest.sendMessage(request(
                "message-input",
                A2aRole.ROLE_USER,
                List.of(A2aPart.data(Map.of("prompt", "ping"))),
                null).toJson());

        WayangA2aHttpResponse skillRejected = preflight
                .validateHttp(unsupportedSkills, A2aProtocol.OPERATION_SEND_MESSAGE)
                .error()
                .orElseThrow();
        WayangA2aHttpResponse inputRejected = preflight
                .validateHttp(unsupportedInput, A2aProtocol.OPERATION_SEND_MESSAGE)
                .error()
                .orElseThrow();

        assertThat(errorCode(skillRejected)).isEqualTo("skill_not_supported");
        assertThat(errorMetadata(skillRejected).keySet()).containsExactly(
                "requestedSkillIds",
                "supportedSkillIds",
                "unsupportedSkillIds");
        assertThat(errorMetadata(skillRejected))
                .containsEntry("requestedSkillIds", List.of("rag", "tools"))
                .containsEntry("supportedSkillIds", List.of("chat"))
                .containsEntry("unsupportedSkillIds", List.of("rag", "tools"));
        assertThat(errorCode(inputRejected)).isEqualTo("unsupported_input_mode");
        assertThat(errorMetadata(inputRejected).keySet()).containsExactly(
                "inputModes",
                "supportedInputModes",
                "unsupportedInputModes");
        assertThat(errorMetadata(inputRejected))
                .containsEntry("inputModes", List.of("application/json"))
                .containsEntry("supportedInputModes", List.of("text/plain"))
                .containsEntry("unsupportedInputModes", List.of("application/json"));
    }

    @Test
    void ignoresMalformedPayloadsSoDispatchersCanUseNormalParseErrors() {
        WayangA2aHttpRequest malformed = WayangA2aHttpRequest.sendMessage("{");

        WayangA2aSendMessagePreflight.HttpResult result = preflight.validateHttp(
                malformed,
                A2aProtocol.OPERATION_SEND_MESSAGE);

        assertThat(result.error()).isEmpty();
        assertThat(result.request()).isSameAs(malformed);
    }

    @Test
    void enrichesValidHttpRequestsWithParsedSendMessageRequest() {
        WayangA2aHttpRequest request = WayangA2aHttpRequest.sendMessage(request(
                "message-valid",
                A2aRole.ROLE_USER,
                List.of(A2aPart.text("ping")),
                null).toJson());

        WayangA2aSendMessagePreflight.HttpResult result = preflight.validateHttp(
                request,
                A2aProtocol.OPERATION_SEND_MESSAGE);

        assertThat(result.error()).isEmpty();
        assertThat(result.request()).isNotSameAs(request);
        assertThat(result.request().sendMessageRequest()).isSameAs(result.request().sendMessageRequest());
        assertThat(result.request().sendMessageRequest().message().messageId()).isEqualTo("message-valid");
    }

    @Test
    void exposesParsedJsonRpcSendMessageRequest() {
        WayangA2aJsonRpcRequest request = WayangA2aJsonRpcRequest.of(
                "jsonrpc-valid",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                request(
                        "message-jsonrpc-valid",
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        null).toMap());

        WayangA2aSendMessagePreflight.JsonRpcResult result = preflight.validateJsonRpc(request);

        assertThat(result.error()).isEmpty();
        assertThat(result.sendRequest()).isPresent();
        assertThat(result.sendRequest().orElseThrow().message().messageId()).isEqualTo("message-jsonrpc-valid");
    }

    @SuppressWarnings("unchecked")
    private static String errorCode(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return String.valueOf(((Map<String, Object>) payload.get("error")).get("code"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> errorMetadata(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        Map<String, Object> error = (Map<String, Object>) payload.get("error");
        return WayangA2aMaps.copyMap((Map<?, ?>) error.get("metadata"));
    }

    private static A2aSendMessageRequest request(
            String messageId,
            A2aRole role,
            List<A2aPart> parts,
            A2aSendMessageConfiguration configuration) {
        return request(messageId, role, parts, configuration, Map.of(), Map.of());
    }

    private static A2aSendMessageRequest request(
            String messageId,
            A2aRole role,
            List<A2aPart> parts,
            A2aSendMessageConfiguration configuration,
            Map<String, Object> requestMetadata,
            Map<String, Object> messageMetadata) {
        return new A2aSendMessageRequest(
                null,
                new A2aMessage(
                        messageId,
                        "context-" + messageId,
                        "task-" + messageId,
                        role,
                        parts,
                        messageMetadata,
                        List.of(),
                        List.of()),
                configuration,
                requestMetadata);
    }

    private static A2aAgentCard card() {
        return new A2aAgentCard(
                "Wayang",
                "A2A endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                null,
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }
}
