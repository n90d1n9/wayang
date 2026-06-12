package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointRequestTest {

    @Test
    void projectsMatchedMountedRequestsForFrameworkDiagnostics() {
        WayangA2uiHttpEndpointBinding endpoint = endpoint();

        WayangA2uiHttpEndpointRequest request = endpoint.project(
                "post",
                "/api/a2ui/exchange?tenant=demo#ignored",
                "{}",
                Map.of(
                        WayangA2uiHttpResponse.HEADER_CONTENT_TYPE,
                        List.of("application/json; charset=utf-8"),
                        WayangA2uiHttpResponse.HEADER_ACCEPT,
                        new String[] {WayangA2uiTransportContent.MIME_JSON}),
                Map.of("traceId", "trace-1"));

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/api/a2ui/exchange");
        assertThat(request.body()).isEqualTo("{}");
        assertThat(request.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, "application/json; charset=utf-8")
                .containsEntry(WayangA2uiHttpResponse.HEADER_ACCEPT, WayangA2uiTransportContent.MIME_JSON);
        assertThat(request.attributes()).containsEntry("traceId", "trace-1");
        assertThat(request.knownPath()).isTrue();
        assertThat(request.matched()).isTrue();
        assertThat(request.operation()).contains(WayangA2uiHttpRoute.OPERATION_EXCHANGE);
        assertThat(request.allow()).contains("POST, OPTIONS");
        assertThat(request.requestBodyRequired()).isTrue();
    }

    @Test
    void reportsKnownPathWhenMethodDoesNotMatch() {
        WayangA2uiHttpEndpointRequest request = endpoint().project(
                "GET",
                "/api/a2ui/exchange",
                "",
                Map.of());

        assertThat(request.knownPath()).isTrue();
        assertThat(request.matched()).isFalse();
        assertThat(request.operation()).contains(WayangA2uiHttpRoute.OPERATION_EXCHANGE);
        assertThat(request.allow()).contains("POST, OPTIONS");
        assertThat(request.toMap())
                .containsEntry("method", "GET")
                .containsEntry("path", "/api/a2ui/exchange")
                .containsEntry("knownPath", true)
                .containsEntry("matched", false)
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry("allow", "POST, OPTIONS");
    }

    @Test
    void reportsUnknownPathsWithoutRouteMetadata() {
        WayangA2uiHttpEndpointRequest request = endpoint().project(
                "GET",
                "/api/a2ui/missing",
                "",
                Map.of());

        assertThat(request.knownPath()).isFalse();
        assertThat(request.matched()).isFalse();
        assertThat(request.operation()).isEmpty();
        assertThat(request.allow()).isEmpty();
        assertThat(request.requestBodyRequired()).isFalse();
        assertThat(request.route()).isEmpty();
        assertThat(request.toMap())
                .containsEntry("knownPath", false)
                .containsEntry("matched", false)
                .containsEntry("requestBodyRequired", false)
                .containsEntry("route", Map.of());
    }

    private static WayangA2uiHttpEndpointBinding endpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
