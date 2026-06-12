package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpRouteGuardTest {

    private final WayangA2uiHttpRouteGuard guard = WayangA2uiHttpRouteGuard.strict();
    private final WayangA2uiHttpRoute exchange = WayangA2uiHttpRoute.exchange();

    @Test
    void acceptsValidRouteRequests() {
        Optional<WayangA2uiHttpResponse> response = guard.validate(
                WayangA2uiHttpRequest.exchange("{}"),
                exchange);

        assertThat(response).isEmpty();
    }

    @Test
    void rejectsMethodAcceptAndContentTypeViolationsAsRouteResponses() {
        WayangA2uiHttpResponse method = guard.validate(WayangA2uiHttpRequest.get(
                        WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE),
                exchange).orElseThrow();
        WayangA2uiHttpResponse accept = guard.validate(new WayangA2uiHttpRequest(
                        "POST",
                        WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                        "{}",
                        Map.of(
                                WayangA2uiHttpResponse.HEADER_CONTENT_TYPE,
                                WayangA2uiTransportContent.MIME_JSON,
                                WayangA2uiHttpResponse.HEADER_ACCEPT,
                                "text/plain"),
                        Map.of()),
                exchange).orElseThrow();
        WayangA2uiHttpResponse contentType = guard.validate(new WayangA2uiHttpRequest(
                        "POST",
                        WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                        "{}",
                        Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, "text/plain"),
                        Map.of()),
                exchange).orElseThrow();

        assertThat(method.statusCode()).isEqualTo(405);
        assertThat(transportError(method)).contains(WayangA2uiTransportError.of(
                "method_not_allowed",
                "A2UI HTTP route /a2ui/exchange only supports POST."));
        assertThat(accept.statusCode()).isEqualTo(406);
        assertThat(transportError(accept)).contains(WayangA2uiTransportError.of(
                "not_acceptable",
                "A2UI HTTP route /a2ui/exchange produces application/json, but Accept was text/plain."));
        assertThat(contentType.statusCode()).isEqualTo(415);
        assertThat(transportError(contentType)).contains(WayangA2uiTransportError.of(
                "unsupported_media_type",
                "A2UI HTTP route /a2ui/exchange requires Content-Type application/json, received text/plain."));
        assertThat(contentType.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
    }

    @Test
    void validatesOptionsWithResponseNegotiationOnly() {
        Optional<WayangA2uiHttpResponse> valid = guard.validateOptions(
                WayangA2uiHttpRequest.get(WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE),
                exchange);
        Optional<WayangA2uiHttpResponse> unacceptable = guard.validateOptions(
                new WayangA2uiHttpRequest(
                        "OPTIONS",
                        WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                        "",
                        Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT, "text/plain"),
                        Map.of()),
                exchange);

        assertThat(valid).isEmpty();
        assertThat(unacceptable).isPresent();
        assertThat(unacceptable.orElseThrow().statusCode()).isEqualTo(406);
    }

    private static Optional<WayangA2uiTransportError> transportError(WayangA2uiHttpResponse response) {
        return WayangA2uiTransportResponse.fromJson(response.body()).transportError();
    }
}
