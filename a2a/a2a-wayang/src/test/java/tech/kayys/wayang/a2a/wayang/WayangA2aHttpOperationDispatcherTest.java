package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentExtension;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageConfiguration;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aSendMessageResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpOperationDispatcherTest {

    private static final String REQUIRED_EXTENSION = "https://wayang.test/extensions/human-approval/v1";

    @Test
    void servesPublicAndExtendedAgentCards() {
        A2aAgentCard publicCard = cardWithCapabilities("Wayang Public", false, false, true);
        A2aAgentCard extendedCard = card("Wayang Extended");
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                publicCard,
                extendedCard,
                Map.of(),
                null,
                null);

        WayangA2aHttpResponse publicResponse = dispatcher.dispatch(WayangA2aHttpRequest.get(
                A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH));
        WayangA2aHttpResponse extendedResponse = dispatcher.dispatch(WayangA2aHttpRequest.get("/extendedAgentCard"));
        String publicEtag = String.valueOf(publicResponse.headers().get(WayangA2aHttpResponse.HEADER_ETAG));
        WayangA2aHttpResponse notModified = dispatcher.dispatch(new WayangA2aHttpRequest(
                "GET",
                A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_IF_NONE_MATCH, publicEtag),
                Map.of()));

        assertThat(publicResponse.statusCode()).isEqualTo(200);
        assertThat(A2aAgentCard.fromJson(publicResponse.body()).name()).isEqualTo("Wayang Public");
        assertThat(publicResponse.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_DISCOVER_AGENT_CARD)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_CACHE_CONTROL,
                        WayangA2aHttpResponse.AGENT_CARD_CACHE_CONTROL)
                .containsEntry(WayangA2aHttpResponse.HEADER_ETAG, publicEtag);
        assertThat(publicEtag).startsWith("\"").endsWith("\"");
        assertThat(A2aAgentCard.fromJson(extendedResponse.body()).name()).isEqualTo("Wayang Extended");
        assertThat(extendedResponse.headers())
                .containsKey(WayangA2aHttpResponse.HEADER_ETAG)
                .containsEntry(WayangA2aHttpResponse.HEADER_CACHE_CONTROL,
                        WayangA2aHttpResponse.AGENT_CARD_CACHE_CONTROL);
        assertThat(notModified.statusCode()).isEqualTo(304);
        assertThat(notModified.body()).isBlank();
        assertThat(notModified.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ETAG, publicEtag)
                .containsEntry(WayangA2aHttpResponse.HEADER_CACHE_CONTROL,
                        WayangA2aHttpResponse.AGENT_CARD_CACHE_CONTROL)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_DISCOVER_AGENT_CARD)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
    }

    @Test
    void canProtectExtendedAgentCardWithBearerAuthorizer() {
        A2aAgentCard publicCard = cardWithCapabilities("Wayang Public", false, false, true);
        A2aAgentCard extendedCard = card("Wayang Extended");
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                publicCard,
                extendedCard,
                Map.of(),
                null,
                null,
                WayangA2aExtendedAgentCardAuthorizer.requireBearerToken("secret-token"));
        WayangA2aHttpRequest authorizedRequest = new WayangA2aHttpRequest(
                "GET",
                "/extendedAgentCard",
                "",
                Map.of("authorization", "Bearer secret-token"),
                Map.of());

        WayangA2aHttpResponse publicResponse = dispatcher.dispatch(WayangA2aHttpRequest.get(
                A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH));
        WayangA2aHttpResponse unauthorized = dispatcher.dispatch(WayangA2aHttpRequest.get("/extendedAgentCard"));
        WayangA2aHttpResponse authorized = dispatcher.dispatch(authorizedRequest);

        assertThat(publicResponse.statusCode()).isEqualTo(200);
        assertThat(unauthorized.statusCode()).isEqualTo(401);
        assertThat(unauthorized.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE,
                        "Bearer realm=\"a2a-extended-agent-card\"")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(errorCode(unauthorized)).isEqualTo("extended_agent_card_unauthorized");
        assertThat(authorized.statusCode()).isEqualTo(200);
        assertThat(A2aAgentCard.fromJson(authorized.body()).name()).isEqualTo("Wayang Extended");
    }

    @Test
    void validatesOptionalCapabilitiesForHttpOperations() {
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                card("Wayang"),
                card("Wayang Extended"),
                Map.of(
                        A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE,
                        (request, routeMatch) -> WayangA2aHttpResponse.eventStream(200, ""),
                        A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        (request, routeMatch) -> WayangA2aHttpResponse.object(200, Map.of("ok", true))),
                null,
                null);
        WayangA2aHttpOperationDispatcher declaresExtendedButNotConfigured = new WayangA2aHttpOperationDispatcher(
                cardWithCapabilities("Wayang", false, false, true),
                null,
                Map.of(),
                null,
                null);

        WayangA2aHttpResponse streaming = dispatcher.dispatch(new WayangA2aHttpRequest(
                "POST",
                "/message:stream",
                "{}",
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE,
                        WayangA2aHttpResponse.HEADER_ACCEPT, A2aProtocol.EVENT_STREAM_MEDIA_TYPE),
                Map.of()));
        WayangA2aHttpResponse push = dispatcher.dispatch(new WayangA2aHttpRequest(
                "POST",
                "/tasks/task-1/pushNotificationConfigs",
                "{}",
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of()));
        WayangA2aHttpResponse extendedUnsupported = dispatcher.dispatch(WayangA2aHttpRequest.get(
                "/extendedAgentCard"));
        WayangA2aHttpResponse extendedNotConfigured = declaresExtendedButNotConfigured.dispatch(
                WayangA2aHttpRequest.get("/extendedAgentCard"));

        assertThat(streaming.statusCode()).isEqualTo(400);
        assertThat(errorCode(streaming)).isEqualTo("unsupported_operation");
        assertThat(push.statusCode()).isEqualTo(400);
        assertThat(errorCode(push)).isEqualTo("push_notification_not_supported");
        assertThat(errorMetadata(push).keySet()).containsExactly("operation", "capability");
        assertThat(extendedUnsupported.statusCode()).isEqualTo(400);
        assertThat(errorCode(extendedUnsupported)).isEqualTo("unsupported_operation");
        assertThat(extendedNotConfigured.statusCode()).isEqualTo(400);
        assertThat(errorCode(extendedNotConfigured)).isEqualTo("extended_agent_card_not_configured");
    }

    @Test
    void dispatchesRegisteredSendMessageHandler() {
        AtomicReference<A2aSendMessageRequest> handledRequest = new AtomicReference<>();
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                card("Wayang"),
                Map.of(A2aProtocol.OPERATION_SEND_MESSAGE, (request, routeMatch) -> {
                    handledRequest.set(request.sendMessageRequest());
                    A2aMessage response = new A2aMessage(
                            "message-2",
                            handledRequest.get().message().contextId(),
                            null,
                            A2aRole.ROLE_AGENT,
                            List.of(A2aPart.text("pong")),
                            Map.of(),
                            List.of(),
                            List.of());
                    return WayangA2aHttpResponse.object(200, A2aSendMessageResponse.message(response).toMap());
                }));
        A2aSendMessageRequest request = A2aSendMessageRequest.of(new A2aMessage(
                "message-1",
                "context-1",
                null,
                A2aRole.ROLE_USER,
                List.of(A2aPart.text("ping")),
                Map.of(),
                List.of(),
                List.of()));

        WayangA2aHttpResponse response = dispatcher.dispatch(WayangA2aHttpRequest.sendMessage(request.toJson()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(handledRequest.get().message().messageId()).isEqualTo("message-1");
        assertThat(WayangA2aHttpJson.read(response.body())).containsKey("message");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_MESSAGE);
    }

    @Test
    void validatesSendMessageRequestPayloadForHttpOperations() {
        AtomicReference<Boolean> handled = new AtomicReference<>(false);
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                cardWithCapabilities("Wayang", false, false, false),
                Map.of(A2aProtocol.OPERATION_SEND_MESSAGE, (request, routeMatch) -> {
                    handled.set(true);
                    return WayangA2aHttpResponse.object(200, Map.of("ok", true));
                }));
        WayangA2aHttpRequest agentRole = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-agent-role",
                new A2aMessage(
                        "message-agent-role",
                        "context-agent-role",
                        "task-agent-role",
                        A2aRole.ROLE_AGENT,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                null).toJson());
        WayangA2aHttpRequest jsonPart = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-json-part",
                new A2aMessage(
                        "message-json-part",
                        "context-json-part",
                        "task-json-part",
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.data(Map.of("prompt", "ping"))),
                        Map.of(),
                        List.of(),
                        List.of()),
                null).toJson());
        WayangA2aHttpRequest unknownSkill = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-unknown-skill",
                new A2aMessage(
                        "message-unknown-skill",
                        "context-unknown-skill",
                        "task-unknown-skill",
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                null,
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("missing-skill"))).toJson());

        WayangA2aHttpResponse rejectedRole = dispatcher.dispatch(agentRole);
        WayangA2aHttpResponse rejectedInput = dispatcher.dispatch(jsonPart);
        WayangA2aHttpResponse rejectedSkill = dispatcher.dispatch(unknownSkill);

        assertThat(rejectedRole.statusCode()).isEqualTo(400);
        assertThat(errorCode(rejectedRole)).isEqualTo("invalid_message_role");
        assertThat(rejectedInput.statusCode()).isEqualTo(400);
        assertThat(errorCode(rejectedInput)).isEqualTo("unsupported_input_mode");
        assertThat(rejectedSkill.statusCode()).isEqualTo(400);
        assertThat(errorCode(rejectedSkill)).isEqualTo("skill_not_supported");
        assertThat(handled.get()).isFalse();
    }

    @Test
    void usesSelectedSkillInputModesForHttpOperations() {
        AtomicReference<Boolean> handled = new AtomicReference<>(false);
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                cardWithModeSkills(),
                Map.of(A2aProtocol.OPERATION_SEND_MESSAGE, (request, routeMatch) -> {
                    handled.set(true);
                    return WayangA2aHttpResponse.object(200, Map.of("ok", true));
                }));
        A2aMessage jsonMessage = new A2aMessage(
                "message-json-mode",
                "context-json-mode",
                "task-json-mode",
                A2aRole.ROLE_USER,
                List.of(A2aPart.data(Map.of("prompt", "ping"))),
                Map.of(),
                List.of(),
                List.of());
        WayangA2aHttpRequest defaultMode = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-default-json",
                jsonMessage,
                null).toJson());
        WayangA2aHttpRequest jsonSkill = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-json-skill",
                jsonMessage,
                null,
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("json"))).toJson());
        WayangA2aHttpRequest textSkill = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-text-skill",
                jsonMessage,
                null,
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("chat"))).toJson());

        WayangA2aHttpResponse rejectedDefault = dispatcher.dispatch(defaultMode);
        assertThat(rejectedDefault.statusCode()).isEqualTo(400);
        assertThat(errorCode(rejectedDefault)).isEqualTo("unsupported_input_mode");
        assertThat(handled.get()).isFalse();

        WayangA2aHttpResponse accepted = dispatcher.dispatch(jsonSkill);
        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(handled.get()).isTrue();

        handled.set(false);
        WayangA2aHttpResponse rejectedTextSkill = dispatcher.dispatch(textSkill);
        assertThat(rejectedTextSkill.statusCode()).isEqualTo(400);
        assertThat(errorCode(rejectedTextSkill)).isEqualTo("unsupported_input_mode");
        assertThat(handled.get()).isFalse();
    }

    @Test
    void validatesSendMessageConfigurationForHttpOperations() {
        AtomicReference<Boolean> handled = new AtomicReference<>(false);
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                cardWithCapabilities("Wayang", false, false, false),
                Map.of(A2aProtocol.OPERATION_SEND_MESSAGE, (request, routeMatch) -> {
                    handled.set(true);
                    return WayangA2aHttpResponse.object(200, Map.of("ok", true));
                }));
        WayangA2aHttpRequest unsupportedMode = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-a",
                new A2aSendMessageConfiguration(
                        List.of("application/json"),
                        Map.of(),
                        null,
                        null)).toJson());
        WayangA2aHttpRequest compatibleModes = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-b",
                new A2aSendMessageConfiguration(
                        List.of("application/json", "text/*"),
                        Map.of(),
                        null,
                        null)).toJson());
        WayangA2aHttpRequest pushConfig = WayangA2aHttpRequest.sendMessage(sendRequest(
                "tenant-c",
                new A2aSendMessageConfiguration(
                        List.of("text/plain"),
                        Map.of(
                                "configId", "primary",
                                "url", "https://hooks.test/a2a"),
                        null,
                        null)).toJson());

        WayangA2aHttpResponse rejectedMode = dispatcher.dispatch(unsupportedMode);

        assertThat(rejectedMode.statusCode()).isEqualTo(400);
        assertThat(errorCode(rejectedMode)).isEqualTo("unsupported_output_mode");
        assertThat(handled.get()).isFalse();

        WayangA2aHttpResponse accepted = dispatcher.dispatch(compatibleModes);

        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(handled.get()).isTrue();

        handled.set(false);
        WayangA2aHttpResponse rejectedPush = dispatcher.dispatch(pushConfig);

        assertThat(rejectedPush.statusCode()).isEqualTo(400);
        assertThat(errorCode(rejectedPush)).isEqualTo("push_notification_not_supported");
        assertThat(handled.get()).isFalse();
    }

    @Test
    void requiresDeclaredExtensionsForOperationalRoutes() {
        AtomicReference<Boolean> handled = new AtomicReference<>(false);
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                cardRequiringExtension("Wayang", REQUIRED_EXTENSION),
                Map.of(A2aProtocol.OPERATION_SEND_MESSAGE, (request, routeMatch) -> {
                    handled.set(true);
                    return WayangA2aHttpResponse.object(200, Map.of("ok", true));
                }));
        WayangA2aHttpRequest missingExtension = new WayangA2aHttpRequest(
                "POST",
                "/message:send",
                "{}",
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of());
        WayangA2aHttpRequest supportedExtension = new WayangA2aHttpRequest(
                "POST",
                "/message:send",
                "{}",
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE,
                        "a2a-extensions", "https://wayang.test/extensions/trace/v1, " + REQUIRED_EXTENSION),
                Map.of());

        WayangA2aHttpResponse discovery = dispatcher.dispatch(WayangA2aHttpRequest.get(
                A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH));
        WayangA2aHttpResponse rejected = dispatcher.dispatch(missingExtension);

        assertThat(discovery.statusCode()).isEqualTo(200);
        assertThat(rejected.statusCode()).isEqualTo(400);
        assertThat(rejected.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS, REQUIRED_EXTENSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_MESSAGE)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(errorCode(rejected)).isEqualTo("extension_support_required");
        assertThat(handled.get()).isFalse();

        WayangA2aHttpResponse accepted = dispatcher.dispatch(supportedExtension);

        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(handled.get()).isTrue();
    }

    @Test
    void validatesExplicitTenantAgainstAdvertisedInterfacesForHttpOperations() {
        AtomicReference<Boolean> handled = new AtomicReference<>(false);
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                cardForTenant("Wayang", "tenant-a"),
                Map.of(A2aProtocol.OPERATION_SEND_MESSAGE, (request, routeMatch) -> {
                    handled.set(true);
                    return WayangA2aHttpResponse.object(200, Map.of("ok", true));
                }));
        WayangA2aHttpRequest wrongTenant = WayangA2aHttpRequest.sendMessage(sendRequest("tenant-b").toJson());
        WayangA2aHttpRequest matchingTenant = WayangA2aHttpRequest.sendMessage(sendRequest("tenant-a").toJson());

        WayangA2aHttpResponse rejected = dispatcher.dispatch(wrongTenant);

        assertThat(rejected.statusCode()).isEqualTo(400);
        assertThat(rejected.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_MESSAGE)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(errorCode(rejected)).isEqualTo("tenant_not_supported");
        assertThat(handled.get()).isFalse();

        WayangA2aHttpResponse accepted = dispatcher.dispatch(matchingTenant);

        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(handled.get()).isTrue();
    }

    @Test
    void returnsRouteErrorsForMethodPathAndUnsupportedOperations() {
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(card("Wayang"));

        WayangA2aHttpResponse method = dispatcher.dispatch(WayangA2aHttpRequest.get("/message:send"));
        WayangA2aHttpResponse missing = dispatcher.dispatch(WayangA2aHttpRequest.get("/not-a-route"));
        WayangA2aHttpResponse unsupported = dispatcher.dispatch(new WayangA2aHttpRequest(
                "GET",
                "/tasks/task-1",
                "",
                Map.of(),
                Map.of()));

        assertThat(method.statusCode()).isEqualTo(405);
        assertThat(method.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(errorCode(method)).isEqualTo("method_not_allowed");
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(errorCode(missing)).isEqualTo("route_not_found");
        assertThat(unsupported.statusCode()).isEqualTo(501);
        assertThat(errorCode(unsupported)).isEqualTo("unsupported_route_operation");
    }

    @Test
    void servesOptionsWithoutInvokingHandlers() {
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                card("Wayang"),
                Map.of(A2aProtocol.OPERATION_SEND_MESSAGE,
                        (request, routeMatch) -> WayangA2aHttpResponse.error(500, "unexpected", "Unexpected")));

        WayangA2aHttpResponse response = dispatcher.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                "/message:send",
                "",
                Map.of(),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_MESSAGE);
        assertThat(WayangA2aHttpJson.read(response.body())).containsEntry("operation", A2aProtocol.OPERATION_SEND_MESSAGE);
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

    private static A2aAgentCard card(String name) {
        return A2aAgentCard.minimal(
                name,
                "A2A endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
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

    private static A2aAgentCard cardWithModeSkills() {
        return new A2aAgentCard(
                "Wayang Modes",
                "A2A endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(false, false, List.of(), false),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(
                        new A2aAgentSkill(
                                "chat",
                                "Chat",
                                "Plain text chat",
                                List.of("chat"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("text/plain"),
                                List.of()),
                        new A2aAgentSkill(
                                "json",
                                "JSON",
                                "Structured JSON exchange",
                                List.of("json"),
                                List.of(),
                                List.of("application/json"),
                                List.of("application/json"),
                                List.of())),
                List.of(),
                null);
    }

    private static A2aSendMessageRequest sendRequest(String tenant) {
        return sendRequest(tenant, null);
    }

    private static A2aSendMessageRequest sendRequest(String tenant, A2aSendMessageConfiguration configuration) {
        return sendRequest(
                tenant,
                new A2aMessage(
                        "message-" + tenant,
                        "context-" + tenant,
                        "task-" + tenant,
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                configuration);
    }

    private static A2aSendMessageRequest sendRequest(
            String tenant,
            A2aMessage message,
            A2aSendMessageConfiguration configuration) {
        return sendRequest(tenant, message, configuration, Map.of());
    }

    private static A2aSendMessageRequest sendRequest(
            String tenant,
            A2aMessage message,
            A2aSendMessageConfiguration configuration,
            Map<String, Object> metadata) {
        return new A2aSendMessageRequest(
                tenant,
                message,
                configuration,
                metadata);
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
}
