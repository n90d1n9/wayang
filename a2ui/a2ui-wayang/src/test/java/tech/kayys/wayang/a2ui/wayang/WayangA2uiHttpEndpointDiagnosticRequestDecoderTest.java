package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpEndpointDiagnosticRequestDecoderTest {

    @Test
    void decodesNullAndEmptyMapsAsCanonicalDefaultRequest() {
        assertThat(WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromMap(null))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticRequest.defaultRequest());
        assertThat(WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromMap(Map.of()))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticRequest.defaultRequest());
    }

    @Test
    void decodesExternalRequestAliasesAndContext() {
        WayangA2uiHttpEndpointDiagnosticRequest request =
                WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromMap(Map.of(
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
    void prefersRawPathAliasOverPath() {
        WayangA2uiHttpEndpointDiagnosticRequest request =
                WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromMap(Map.of(
                        "method",
                        "GET",
                        "rawPath",
                        "/api/a2ui/route-catalog",
                        "path",
                        "/api/a2ui/missing"));

        assertThat(request.rawPath()).isEqualTo("/api/a2ui/route-catalog");
    }

    @Test
    void preservesRawRequestBodyText() {
        WayangA2uiHttpEndpointDiagnosticRequest request =
                WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromMap(Map.of(
                        "method",
                        "POST",
                        "path",
                        "/api/a2ui/exchange",
                        "body",
                        "  {\"kind\":\"json\"}\n"));

        assertThat(request.body()).isEqualTo("  {\"kind\":\"json\"}\n");
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = Map.of(
                "method",
                "GET",
                "path",
                "/api/a2ui/route-catalog");

        assertThat(WayangA2uiHttpEndpointDiagnosticRequest.fromMap(values))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromMap(values));
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiHttpEndpointDiagnosticRequest decoded =
                WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromJson("""
                        {
                          "method": "GET",
                          "path": "/api/a2ui/route-catalog"
                        }
                        """);

        assertThat(decoded.method()).isEqualTo("GET");
        assertThat(decoded.rawPath()).isEqualTo("/api/a2ui/route-catalog");
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP endpoint diagnostic request JSON must not be blank");
    }
}
