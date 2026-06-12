package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpRouteGuardTest {

    private final WayangA2aHttpRouteGuard guard = WayangA2aHttpRouteGuard.strict();

    @Test
    void validatesAcceptContentTypeAndRequiredBodies() {
        A2aHttpRoute sendRoute = A2aHttpRouteCatalog.standard()
                .routeForOperation(A2aProtocol.OPERATION_SEND_MESSAGE)
                .orElseThrow();

        WayangA2aHttpResponse accept = guard.validate(new WayangA2aHttpRequest(
                "POST",
                "/message:send",
                "{}",
                Map.of(
                        WayangA2aHttpResponse.HEADER_ACCEPT, "text/plain",
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of()), sendRoute).orElseThrow();
        WayangA2aHttpResponse contentType = guard.validate(new WayangA2aHttpRequest(
                "POST",
                "/message:send",
                "{}",
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, "text/plain"),
                Map.of()), sendRoute).orElseThrow();
        WayangA2aHttpResponse body = guard.validate(new WayangA2aHttpRequest(
                "POST",
                "/message:send",
                "",
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of()), sendRoute).orElseThrow();

        assertThat(accept.statusCode()).isEqualTo(406);
        assertThat(errorCode(accept)).isEqualTo("not_acceptable");
        assertThat(contentType.statusCode()).isEqualTo(415);
        assertThat(errorCode(contentType)).isEqualTo("unsupported_media_type");
        assertThat(body.statusCode()).isEqualTo(400);
        assertThat(errorCode(body)).isEqualTo("missing_request_body");
    }

    @Test
    void expectsEventStreamForStreamingRoutes() {
        A2aHttpRoute streamRoute = A2aHttpRouteCatalog.standard()
                .routeForOperation(A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE)
                .orElseThrow();

        WayangA2aHttpResponse response = guard.validate(new WayangA2aHttpRequest(
                "POST",
                "/message:stream",
                "{}",
                Map.of(
                        WayangA2aHttpResponse.HEADER_ACCEPT, A2aProtocol.MEDIA_TYPE,
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of()), streamRoute).orElseThrow();

        assertThat(response.statusCode()).isEqualTo(406);
        assertThat(errorCode(response)).isEqualTo("not_acceptable");
    }

    @SuppressWarnings("unchecked")
    private static String errorCode(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return String.valueOf(((Map<String, Object>) payload.get("error")).get("code"));
    }
}
