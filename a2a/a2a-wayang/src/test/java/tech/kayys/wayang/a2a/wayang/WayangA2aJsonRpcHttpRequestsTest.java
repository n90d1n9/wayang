package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpRequestsTest {

    @Test
    void getJsonBuildsJsonAcceptDiagnosticRequest() {
        WayangA2aHttpRequest request = WayangA2aJsonRpcHttpRequests.getJson("internal/a2a/diagnostics");

        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.path()).isEqualTo("/internal/a2a/diagnostics");
        assertThat(request.body()).isEmpty();
        assertThat(request.accept()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(request.attributes()).isEmpty();
    }

    @Test
    void routeProbeBuildsEmptyRequestForRouteMatching() {
        WayangA2aHttpRequest request = WayangA2aJsonRpcHttpRequests.routeProbe("options", "a2a/rpc");

        assertThat(request.method()).isEqualTo("OPTIONS");
        assertThat(request.path()).isEqualTo("/a2a/rpc");
        assertThat(request.body()).isEmpty();
        assertThat(request.headers()).isEmpty();
        assertThat(request.attributes()).isEmpty();
    }
}
