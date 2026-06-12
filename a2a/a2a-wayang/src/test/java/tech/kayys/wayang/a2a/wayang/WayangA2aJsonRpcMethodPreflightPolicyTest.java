package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodPreflightPolicyTest {

    @Test
    void exposesParsedSendMessageRequestForDispatchReuse() {
        WayangA2aJsonRpcMethodPreflightPolicy policy =
                WayangA2aJsonRpcMethodPreflightPolicy.fromAgentCard(card("tenant-a", capabilities(true, true)));
        WayangA2aJsonRpcRequest request = WayangA2aJsonRpcRequest.of(
                "send",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                sendRequest("tenant-a", A2aRole.ROLE_USER).toMap());

        WayangA2aJsonRpcMethodPreflightPolicy.Result result =
                policy.validate(request, entry(WayangA2aJsonRpcMethods.SEND_MESSAGE));

        assertThat(result.error()).isEmpty();
        assertThat(result.sendMessage().sendRequest())
                .hasValueSatisfying(send -> assertThat(send.message().messageId()).isEqualTo("message-tenant-a"));
    }

    @Test
    void validatesTenantBeforeCapabilityPolicy() {
        WayangA2aJsonRpcMethodPreflightPolicy policy =
                WayangA2aJsonRpcMethodPreflightPolicy.fromAgentCard(card("tenant-a", capabilities(false, true)));
        WayangA2aJsonRpcRequest request = WayangA2aJsonRpcRequest.of(
                "tenant-first",
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                sendRequest("tenant-b", A2aRole.ROLE_USER).toMap());

        WayangA2aJsonRpcMethodPreflightPolicy.Result result =
                policy.validate(request, entry(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE));

        assertThat(result.error())
                .hasValueSatisfying(error -> assertThat(error)
                        .returns(WayangA2aJsonRpcError.INVALID_PARAMS, WayangA2aJsonRpcError::code)
                        .returns("A2A tenant is not advertised by Agent Card: tenant-b.",
                                WayangA2aJsonRpcError::message));
    }

    @Test
    void validatesCapabilityBeforeSendMessagePreflightError() {
        WayangA2aJsonRpcMethodPreflightPolicy policy =
                WayangA2aJsonRpcMethodPreflightPolicy.fromAgentCard(card("tenant-a", capabilities(false, true)));
        WayangA2aJsonRpcRequest request = WayangA2aJsonRpcRequest.of(
                "capability-first",
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                sendRequest("tenant-a", A2aRole.ROLE_AGENT).toMap());

        WayangA2aJsonRpcMethodPreflightPolicy.Result result =
                policy.validate(request, entry(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE));

        assertThat(result.error())
                .hasValueSatisfying(error -> assertThat(error)
                        .returns(WayangA2aJsonRpcError.UNSUPPORTED_OPERATION, WayangA2aJsonRpcError::code)
                        .returns("A2A agent does not support streaming operation: SendStreamingMessage.",
                                WayangA2aJsonRpcError::message));
    }

    @Test
    void returnsSendMessagePreflightErrorWhenGuardsPass() {
        WayangA2aJsonRpcMethodPreflightPolicy policy =
                WayangA2aJsonRpcMethodPreflightPolicy.fromAgentCard(card("tenant-a", capabilities(true, true)));
        WayangA2aJsonRpcRequest request = WayangA2aJsonRpcRequest.of(
                "send-preflight",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                sendRequest("tenant-a", A2aRole.ROLE_AGENT).toMap());

        WayangA2aJsonRpcMethodPreflightPolicy.Result result =
                policy.validate(request, entry(WayangA2aJsonRpcMethods.SEND_MESSAGE));

        assertThat(result.error())
                .hasValueSatisfying(error -> assertThat(error.code()).isEqualTo(WayangA2aJsonRpcError.INVALID_PARAMS));
    }

    private static WayangA2aJsonRpcMethodDispatchTable.Entry entry(String method) {
        return new WayangA2aJsonRpcMethodDispatchTable.Entry(
                WayangA2aJsonRpcMethods.requireDescriptor(method),
                WayangA2aJsonRpcMethods.methodGroup(method).orElse("unassigned"),
                (request, preflight) -> null);
    }

    private static A2aSendMessageRequest sendRequest(String tenant, A2aRole role) {
        return new A2aSendMessageRequest(
                tenant,
                new A2aMessage(
                        "message-" + tenant,
                        "context-" + tenant,
                        "task-" + tenant,
                        role,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                null,
                Map.of());
    }

    private static A2aAgentCard card(String tenant, A2aAgentCapabilities capabilities) {
        return new A2aAgentCard(
                "wayang-preflight-agent",
                "A JSON-RPC preflight policy test card",
                List.of(new A2aAgentInterface(
                        "https://wayang.test/a2a/" + tenant,
                        A2aProtocol.BINDING_HTTP_JSON,
                        tenant,
                        A2aProtocol.VERSION)),
                null,
                "1.0.0",
                null,
                capabilities,
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "Answer prompts", List.of("chat"))),
                List.of(),
                null);
    }

    private static A2aAgentCapabilities capabilities(boolean streaming, boolean pushNotifications) {
        return new A2aAgentCapabilities(streaming, pushNotifications, List.of(), true);
    }
}
