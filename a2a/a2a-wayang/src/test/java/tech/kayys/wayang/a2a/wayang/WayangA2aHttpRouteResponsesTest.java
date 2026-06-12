package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpRouteResponsesTest {

    @Test
    void optionsResponseCarriesStablePayloadAndHeaders() {
        A2aHttpRoute route = route(A2aProtocol.OPERATION_SEND_MESSAGE);

        WayangA2aHttpResponse response = WayangA2aHttpRouteResponses.options(
                route,
                WayangA2aHttpRouteGuard.strict());

        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        assertThat(payload.keySet()).containsExactly("operation", "httpMethod", "path", "streaming");
        assertThat(payload)
                .containsEntry("operation", A2aProtocol.OPERATION_SEND_MESSAGE)
                .containsEntry("httpMethod", "POST")
                .containsEntry("path", "/message:send")
                .containsEntry("streaming", false);
        assertThat(response.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_STREAMING,
                WayangA2aHttpResponse.HEADER_ALLOW);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_MESSAGE);
    }

    @Test
    void allowHeaderPreservesDistinctRouteMethodOrder() {
        A2aHttpRoute discover = route(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD);
        A2aHttpRoute send = route(A2aProtocol.OPERATION_SEND_MESSAGE);
        A2aHttpRoute stream = route(A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE);

        assertThat(WayangA2aHttpRouteResponses.allowHeader(List.of(discover, send, stream)))
                .isEqualTo("GET, POST, OPTIONS");
    }

    @Test
    void withAllowAppendsAllowHeaderWithoutReorderingExistingHeaders() {
        A2aHttpRoute route = route(A2aProtocol.OPERATION_GET_TASK);
        WayangA2aHttpResponse response = WayangA2aHttpResponse.object(
                200,
                Map.of("ok", true)).withRoute(route);

        WayangA2aHttpResponse withAllow = WayangA2aHttpRouteResponses.withAllow(response, route);

        assertThat(withAllow.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_STREAMING,
                WayangA2aHttpResponse.HEADER_ALLOW);
        assertThat(withAllow.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
    }

    private static A2aHttpRoute route(String operation) {
        return A2aHttpRouteCatalog.standard().routeForOperation(operation).orElseThrow();
    }
}
