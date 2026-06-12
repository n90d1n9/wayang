package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpRouteResponsePolicyTest {

    private final WayangA2aHttpRouteResponsePolicy policy = WayangA2aHttpRouteResponsePolicy.from(
            new WayangA2aHttpRouteMatcher(A2aHttpRouteCatalog.standard()),
            WayangA2aHttpRouteGuard.strict());

    @Test
    void optionsReturnsRouteMetadataForKnownPath() {
        WayangA2aHttpResponse response = policy.options(options("/message:send", Map.of()));

        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(payload)
                .containsEntry("operation", A2aProtocol.OPERATION_SEND_MESSAGE)
                .containsEntry("httpMethod", "POST")
                .containsEntry("path", "/message:send")
                .containsEntry("streaming", false);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_MESSAGE);
    }

    @Test
    void optionsDecoratesRouteGuardErrorsWithAllowHeader() {
        WayangA2aHttpResponse response = policy.options(options(
                "/message:send",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, A2aProtocol.EVENT_STREAM_MEDIA_TYPE)));

        assertThat(response.statusCode()).isEqualTo(406);
        assertThat(errorCode(response)).isEqualTo("not_acceptable");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_MESSAGE);
    }

    @Test
    void unmatchedReturnsMethodNotAllowedForKnownPath() {
        WayangA2aHttpResponse response = policy.unmatched(WayangA2aHttpRequest.get("/message:send"));

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(errorCode(response)).isEqualTo("method_not_allowed");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_MESSAGE);
    }

    @Test
    void unmatchedReturnsRouteNotFoundForUnknownPath() {
        WayangA2aHttpResponse response = policy.unmatched(WayangA2aHttpRequest.get("/not-a-route"));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(errorCode(response)).isEqualTo("route_not_found");
        assertThat(response.headers()).doesNotContainKey(WayangA2aHttpResponse.HEADER_ALLOW);
    }

    @SuppressWarnings("unchecked")
    private static String errorCode(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return String.valueOf(((Map<String, Object>) payload.get("error")).get("code"));
    }

    private static WayangA2aHttpRequest options(String path, Map<String, Object> headers) {
        return new WayangA2aHttpRequest("OPTIONS", path, "", headers, Map.of());
    }
}
