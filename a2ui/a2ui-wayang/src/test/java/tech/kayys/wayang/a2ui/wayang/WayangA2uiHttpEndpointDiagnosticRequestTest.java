package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpEndpointDiagnosticRequestTest {

    @Test
    void exposesCanonicalDefaultRequestFactory() {
        WayangA2uiHttpEndpointDiagnosticRequest request =
                WayangA2uiHttpEndpointDiagnosticRequest.defaultRequest();

        assertThat(request)
                .returns("GET", WayangA2uiHttpEndpointDiagnosticRequest::method)
                .returns("/", WayangA2uiHttpEndpointDiagnosticRequest::rawPath)
                .returns("", WayangA2uiHttpEndpointDiagnosticRequest::body);
        assertThat(WayangA2uiHttpEndpointDiagnosticRequest.fromMap(null)).isEqualTo(request);
    }

    @Test
    void decodesRequestMapsWithPathAliasesAndNestedContext() {
        WayangA2uiHttpEndpointDiagnosticRequest request =
                WayangA2uiHttpEndpointDiagnosticRequest.fromMap(Map.of(
                        "method",
                        "post",
                        "path",
                        "api/a2ui/exchange?tenant=demo",
                        "body",
                        "{}",
                        "headers",
                        Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE,
                                List.of(WayangA2uiTransportContent.MIME_JSON)),
                        "attributes",
                        Map.of("traceId", "trace-1")));

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.rawPath()).isEqualTo("/api/a2ui/exchange?tenant=demo");
        assertThat(request.body()).isEqualTo("{}");
        assertThat(request.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE,
                        List.of(WayangA2uiTransportContent.MIME_JSON));
        assertThat(request.attributes()).containsEntry("traceId", "trace-1");
    }

    @Test
    void roundTripsRequestJsonForExternalHarnesses() {
        WayangA2uiHttpEndpointDiagnosticRequest request =
                WayangA2uiHttpEndpointDiagnosticRequest.postJson("/api/a2ui/exchange", "{\"kind\":\"json\"}")
                        .withHeaders(Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT, "application/json"))
                        .withAttributes(Map.of("tenant", "demo"));

        WayangA2uiHttpEndpointDiagnosticRequest decoded =
                WayangA2uiHttpEndpointDiagnosticRequest.fromJson(request.toJson());

        assertThat(decoded).isEqualTo(request);
        assertThat(decoded.toMap())
                .containsEntry("method", "POST")
                .containsEntry("rawPath", "/api/a2ui/exchange")
                .containsEntry("body", "{\"kind\":\"json\"}")
                .containsEntry("bodyPresent", true)
                .containsEntry("bodyLength", 15);
    }

    @Test
    void rejectsBlankOrInvalidRequestJson() {
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticRequest.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP endpoint diagnostic request JSON must not be blank");
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticRequest.fromJson("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to decode A2UI HTTP endpoint diagnostic request JSON");
    }
}
