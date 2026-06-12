package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentExtension;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpOperationPreflightPolicyTest {

    private static final String REQUIRED_EXTENSION = "https://wayang.test/extensions/human-approval/v1";

    @Test
    void returnsRouteGuardErrorBeforeOperationPreflight() {
        WayangA2aHttpOperationPreflightPolicy policy = policy(cardForTenant("Wayang", "tenant-a"));
        WayangA2aHttpRequest request = new WayangA2aHttpRequest(
                "POST",
                "/message:send",
                sendRequest("tenant-b").toJson(),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE,
                        WayangA2aHttpResponse.HEADER_ACCEPT, A2aProtocol.EVENT_STREAM_MEDIA_TYPE),
                Map.of());

        WayangA2aHttpOperationPreflightPolicy.Result result =
                policy.validate(request, route(A2aProtocol.OPERATION_SEND_MESSAGE));

        assertThat(result.error()).isPresent();
        assertThat(errorCode(result.error().orElseThrow())).isEqualTo("not_acceptable");
    }

    @Test
    void exposesParsedSendMessageRequestForDispatchReuse() {
        WayangA2aHttpOperationPreflightPolicy policy = policy(cardForTenant("Wayang", "tenant-a"));

        WayangA2aHttpOperationPreflightPolicy.Result result = policy.validate(
                WayangA2aHttpRequest.sendMessage(sendRequest("tenant-a").toJson()),
                route(A2aProtocol.OPERATION_SEND_MESSAGE));

        assertThat(result.error()).isEmpty();
        assertThat(result.request().attributes().get(WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE))
                .isInstanceOf(A2aSendMessageRequest.class);
        assertThat(result.request().sendMessageRequest().tenant()).isEqualTo("tenant-a");
    }

    @Test
    void validatesTenantBeforeCapabilityPolicy() {
        WayangA2aHttpOperationPreflightPolicy policy = policy(cardForTenant("Wayang", "tenant-a"));

        WayangA2aHttpOperationPreflightPolicy.Result result = policy.validate(
                WayangA2aHttpRequest.streamMessage(sendRequest("tenant-b").toJson()),
                route(A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE));

        assertThat(result.error()).isPresent();
        assertThat(errorCode(result.error().orElseThrow())).isEqualTo("tenant_not_supported");
    }

    @Test
    void validatesCapabilityBeforeSendMessagePreflightError() {
        WayangA2aHttpOperationPreflightPolicy policy =
                policy(cardWithCapabilities("Wayang", false, false, false));

        WayangA2aHttpOperationPreflightPolicy.Result result = policy.validate(
                WayangA2aHttpRequest.streamMessage(sendRequest(
                        "tenant-a",
                        message("agent-role", A2aRole.ROLE_AGENT)).toJson()),
                route(A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE));

        assertThat(result.error()).isPresent();
        assertThat(errorCode(result.error().orElseThrow())).isEqualTo("unsupported_operation");
    }

    @Test
    void returnsSendMessagePreflightErrorBeforeExtensionNegotiation() {
        WayangA2aHttpOperationPreflightPolicy policy =
                policy(cardRequiringExtension("Wayang", REQUIRED_EXTENSION));

        WayangA2aHttpOperationPreflightPolicy.Result result = policy.validate(
                WayangA2aHttpRequest.sendMessage(sendRequest(
                        "tenant-a",
                        message("agent-role", A2aRole.ROLE_AGENT)).toJson()),
                route(A2aProtocol.OPERATION_SEND_MESSAGE));

        assertThat(result.error()).isPresent();
        assertThat(errorCode(result.error().orElseThrow())).isEqualTo("invalid_message_role");
    }

    @Test
    void validatesRequiredExtensionsAfterSendMessagePreflight() {
        WayangA2aHttpOperationPreflightPolicy policy =
                policy(cardRequiringExtension("Wayang", REQUIRED_EXTENSION));

        WayangA2aHttpOperationPreflightPolicy.Result result = policy.validate(
                WayangA2aHttpRequest.sendMessage(sendRequest("tenant-a").toJson()),
                route(A2aProtocol.OPERATION_SEND_MESSAGE));

        assertThat(result.error()).isPresent();
        assertThat(errorCode(result.error().orElseThrow())).isEqualTo("extension_support_required");
        assertThat(result.error().orElseThrow().headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS, REQUIRED_EXTENSION);
    }

    @SuppressWarnings("unchecked")
    private static String errorCode(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return String.valueOf(((Map<String, Object>) payload.get("error")).get("code"));
    }

    private static WayangA2aHttpOperationPreflightPolicy policy(A2aAgentCard card) {
        return WayangA2aHttpOperationPreflightPolicy.fromAgentCards(
                card,
                null,
                WayangA2aHttpRouteGuard.strict());
    }

    private static A2aHttpRoute route(String operation) {
        return A2aHttpRouteCatalog.standard().routeForOperation(operation).orElseThrow();
    }

    private static A2aAgentCard cardForTenant(String name, String tenant) {
        return new A2aAgentCard(
                name,
                "A2A endpoint",
                List.of(new A2aAgentInterface(
                        "https://wayang.test/a2a/" + tenant,
                        A2aProtocol.BINDING_HTTP_JSON,
                        tenant,
                        A2aProtocol.VERSION)),
                null,
                "1.0.0",
                null,
                A2aAgentCapabilities.basic(),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }

    private static A2aAgentCard cardWithCapabilities(
            String name,
            boolean streaming,
            boolean pushNotifications,
            boolean extendedAgentCard) {
        return new A2aAgentCard(
                name,
                "A2A endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(streaming, pushNotifications, List.of(), extendedAgentCard),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }

    private static A2aAgentCard cardRequiringExtension(String name, String requiredExtension) {
        return new A2aAgentCard(
                name,
                "A2A endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(
                        false,
                        false,
                        List.of(new A2aAgentExtension(
                                requiredExtension,
                                "Requires explicit client support",
                                true,
                                Map.of())),
                        false),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }

    private static A2aSendMessageRequest sendRequest(String tenant) {
        return sendRequest(tenant, message(tenant, A2aRole.ROLE_USER));
    }

    private static A2aSendMessageRequest sendRequest(String tenant, A2aMessage message) {
        return new A2aSendMessageRequest(tenant, message, null, Map.of());
    }

    private static A2aMessage message(String id, A2aRole role) {
        return new A2aMessage(
                "message-" + id,
                "context-" + id,
                "task-" + id,
                role,
                List.of(A2aPart.text("ping")),
                Map.of(),
                List.of(),
                List.of());
    }
}
