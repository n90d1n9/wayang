package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentExtension;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcEndpointPreflightGuardTest {

    private static final String REQUIRED_EXTENSION = "https://wayang.test/extensions/human-approval/v1";

    @Test
    void acceptsSupportedVersionAndNoRequiredExtensions() {
        WayangA2aHttpRequest request = request(
                WayangA2aJsonRpcRequest.of("send", WayangA2aJsonRpcMethods.SEND_MESSAGE, Map.of()),
                Map.of(A2aProtocol.HEADER_VERSION, A2aProtocol.VERSION));

        Optional<WayangA2aHttpResponse> response = validate(
                request,
                card(A2aAgentCapabilities.basic()),
                WayangA2aExtendedAgentCardAuthorizer.allowAll());

        assertThat(response).isEmpty();
    }

    @Test
    void rejectsUnsupportedA2aVersionWithJsonRpcId() {
        WayangA2aHttpRequest request = request(
                WayangA2aJsonRpcRequest.of("bad-version", WayangA2aJsonRpcMethods.SEND_MESSAGE, Map.of()),
                Map.of(A2aProtocol.HEADER_VERSION, "0.5"));

        WayangA2aHttpResponse response = validate(
                request,
                card(A2aAgentCapabilities.basic()),
                WayangA2aExtendedAgentCardAuthorizer.allowAll())
                .orElseThrow();

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(WayangA2aHttpJson.read(response.body())).containsEntry("id", "bad-version");
        assertThat(error(response))
                .containsEntry("code", WayangA2aJsonRpcError.VERSION_NOT_SUPPORTED)
                .containsEntry("message", "A2A protocol version is not supported: 0.5");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC);
    }

    @Test
    void rejectsMissingRequiredExtensionForNormalJsonRpcOperations() {
        WayangA2aHttpRequest request = request(
                WayangA2aJsonRpcRequest.of("missing-extension", WayangA2aJsonRpcMethods.SEND_MESSAGE, Map.of()),
                Map.of());

        WayangA2aHttpResponse response = validate(
                request,
                cardRequiringExtension(),
                WayangA2aExtendedAgentCardAuthorizer.allowAll())
                .orElseThrow();

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS, REQUIRED_EXTENSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT);
        assertThat(WayangA2aHttpJson.read(response.body())).containsEntry("id", "missing-extension");
        assertThat(error(response))
                .containsEntry("code", WayangA2aJsonRpcError.EXTENSION_SUPPORT_REQUIRED)
                .containsEntry("message", "A2A request requires extension support: " + REQUIRED_EXTENSION + ".");
    }

    @Test
    void acceptsProvidedRequiredExtensionForNormalOperations() {
        WayangA2aHttpRequest request = request(
                WayangA2aJsonRpcRequest.of("provided-extension", WayangA2aJsonRpcMethods.SEND_MESSAGE, Map.of()),
                Map.of(A2aProtocol.HEADER_EXTENSIONS, REQUIRED_EXTENSION));

        Optional<WayangA2aHttpResponse> response = validate(
                request,
                cardRequiringExtension(),
                WayangA2aExtendedAgentCardAuthorizer.allowAll());

        assertThat(response).isEmpty();
    }

    @Test
    void requiresAuthorizationOnlyForExtendedAgentCardRequests() {
        WayangA2aHttpRequest unauthorizedRequest = request(
                WayangA2aJsonRpcRequest.of(
                        "extended-card",
                        WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                        Map.of()),
                Map.of());
        WayangA2aHttpRequest authorizedRequest = request(
                WayangA2aJsonRpcRequest.of(
                        "extended-card-authorized",
                        WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                        Map.of()),
                Map.of(WayangA2aHttpResponse.HEADER_AUTHORIZATION, "Bearer secret-token"));
        WayangA2aExtendedAgentCardAuthorizer authorizer =
                WayangA2aExtendedAgentCardAuthorizer.requireBearerToken("secret-token");

        WayangA2aHttpResponse unauthorized = validate(
                unauthorizedRequest,
                cardRequiringExtension(),
                authorizer)
                .orElseThrow();
        Optional<WayangA2aHttpResponse> authorized = validate(
                authorizedRequest,
                cardRequiringExtension(),
                authorizer);

        assertThat(unauthorized.statusCode()).isEqualTo(401);
        assertThat(WayangA2aHttpJson.read(unauthorized.body())).containsEntry("id", "extended-card");
        assertThat(error(unauthorized))
                .containsEntry("code", WayangA2aJsonRpcError.AUTHENTICATION_REQUIRED)
                .containsEntry("message", "A2A extended Agent Card requires authorization.");
        assertThat(unauthorized.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE,
                        "Bearer realm=\"" + WayangA2aExtendedAgentCardAuthorizer.BEARER_REALM + "\"")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD);
        assertThat(authorized).isEmpty();
    }

    private static Optional<WayangA2aHttpResponse> validate(
            WayangA2aHttpRequest request,
            A2aAgentCard card,
            WayangA2aExtendedAgentCardAuthorizer authorizer) {
        return WayangA2aJsonRpcEndpointPreflightGuard.validate(
                request,
                route(),
                WayangA2aJsonRpcHttpRequestContext.from(request),
                WayangA2aExtensionNegotiator.fromAgentCard(card),
                authorizer);
    }

    private static WayangA2aJsonRpcHttpRouteDescriptor route() {
        return WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(WayangA2aJsonRpcHttpConfig.defaults()).getFirst();
    }

    private static WayangA2aHttpRequest request(
            WayangA2aJsonRpcRequest rpcRequest,
            Map<String, Object> extraHeaders) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        headers.put(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        headers.putAll(extraHeaders);
        return new WayangA2aHttpRequest("POST", "/", rpcRequest.toJson(), Map.copyOf(headers), Map.of());
    }

    private static A2aAgentCard cardRequiringExtension() {
        return card(new A2aAgentCapabilities(
                false,
                false,
                List.of(new A2aAgentExtension(
                        REQUIRED_EXTENSION,
                        "Requires explicit client support",
                        true,
                        Map.of())),
                true));
    }

    private static A2aAgentCard card(A2aAgentCapabilities capabilities) {
        return new A2aAgentCard(
                "Wayang",
                "A2A JSON-RPC endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                capabilities,
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Object error = WayangA2aHttpJson.read(response.body()).get("error");
        assertThat(error).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) error);
    }
}
