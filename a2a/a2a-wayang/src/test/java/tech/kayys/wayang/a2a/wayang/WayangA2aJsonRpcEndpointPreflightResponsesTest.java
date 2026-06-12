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

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcEndpointPreflightResponsesTest {

    private static final String REQUIRED_EXTENSION = "https://wayang.test/extensions/human-approval/v1";

    @Test
    void versionResponseCarriesJsonRpcIdAndStableRouteHeaders() {
        WayangA2aHttpRequest request = request(WayangA2aJsonRpcRequest.of(
                "bad-version",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                Map.of()));

        WayangA2aHttpResponse response = WayangA2aJsonRpcEndpointPreflightResponses.versionNotSupported(
                WayangA2aJsonRpcHttpRequestContext.from(request),
                "0.5",
                route());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(WayangA2aHttpJson.read(response.body()).keySet()).containsExactly("jsonrpc", "id", "error");
        assertThat(WayangA2aHttpJson.read(response.body())).containsEntry("id", "bad-version");
        assertThat(error(response)).containsEntry("code", WayangA2aJsonRpcError.VERSION_NOT_SUPPORTED);
        assertThat(errorMetadata(response).keySet()).containsExactly(
                "requestedVersion",
                "supportedVersions",
                "timestamp");
        assertThat(response.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aHttpResponse.HEADER_ALLOW);
    }

    @Test
    void extensionResponseAppendsRequiredExtensionHeaderAfterRouteHeaders() {
        WayangA2aHttpRequest request = request(WayangA2aJsonRpcRequest.of(
                "missing-extension",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                Map.of()));
        WayangA2aExtensionNegotiator negotiator = WayangA2aExtensionNegotiator.fromAgentCard(cardRequiringExtension());

        WayangA2aHttpResponse response = WayangA2aJsonRpcEndpointPreflightResponses.extensionSupportRequired(
                request,
                WayangA2aJsonRpcHttpRequestContext.from(request),
                List.of(REQUIRED_EXTENSION),
                route(),
                negotiator);

        assertThat(response.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aHttpResponse.HEADER_ALLOW,
                WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS, REQUIRED_EXTENSION);
        assertThat(errorMetadata(response).keySet()).containsExactly(
                "missingExtensions",
                "requiredExtensions",
                "providedExtensions",
                "timestamp");
        assertThat(errorMetadata(response))
                .containsEntry("missingExtensions", List.of(REQUIRED_EXTENSION))
                .containsEntry("requiredExtensions", List.of(REQUIRED_EXTENSION))
                .containsEntry("providedExtensions", List.of());
    }

    @Test
    void unauthorizedExtendedCardResponseAppendsAuthenticationHeaderAfterRouteHeaders() {
        WayangA2aHttpRequest request = request(WayangA2aJsonRpcRequest.of(
                "extended-card",
                WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                Map.of()));

        WayangA2aHttpResponse response = WayangA2aJsonRpcEndpointPreflightResponses.extendedAgentCardUnauthorized(
                WayangA2aJsonRpcHttpRequestContext.from(request),
                route());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aHttpResponse.HEADER_ALLOW,
                WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD)
                .containsEntry(WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE,
                        "Bearer realm=\"" + WayangA2aExtendedAgentCardAuthorizer.BEARER_REALM + "\"");
        assertThat(errorMetadata(response).keySet()).containsExactly("scheme", "timestamp");
    }

    private static WayangA2aJsonRpcHttpRouteDescriptor route() {
        return WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(WayangA2aJsonRpcHttpConfig.defaults()).getFirst();
    }

    private static WayangA2aHttpRequest request(WayangA2aJsonRpcRequest rpcRequest) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        headers.put(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        return new WayangA2aHttpRequest("POST", "/", rpcRequest.toJson(), headers, Map.of());
    }

    private static A2aAgentCard cardRequiringExtension() {
        return new A2aAgentCard(
                "Wayang",
                "A2A JSON-RPC endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(
                        false,
                        false,
                        List.of(new A2aAgentExtension(
                                REQUIRED_EXTENSION,
                                "Requires explicit client support",
                                true,
                                Map.of())),
                        true),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Map<String, Object> body = WayangA2aHttpJson.read(response.body());
        return WayangA2aMaps.copyMap((Map<?, ?>) body.get("error"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> errorMetadata(WayangA2aHttpResponse response) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) error(response).get("data");
        return WayangA2aMaps.copyMap((Map<?, ?>) data.getFirst().get("metadata"));
    }
}
