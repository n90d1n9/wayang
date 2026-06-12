package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointResponseTest {

    @Test
    void projectsHttpResponsesIntoFrameworkFriendlyHeaderLists() {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("X-Trace", List.of("trace-1", "trace-2"));
        headers.put("X-Array", new String[] {"one", "two"});
        headers.put("X-Optional", Optional.of("present"));
        headers.put("X-Blank", " ");
        WayangA2uiHttpResponse response = new WayangA2uiHttpResponse(
                201,
                WayangA2uiTransportContent.MIME_JSON,
                "{\"ok\":true}",
                headers);

        WayangA2uiHttpEndpointResponse projected = WayangA2uiHttpEndpointResponse.from(response);

        assertThat(projected.statusCode()).isEqualTo(201);
        assertThat(projected.contentType()).isEqualTo(WayangA2uiTransportContent.MIME_JSON);
        assertThat(projected.body()).isEqualTo("{\"ok\":true}");
        assertThat(projected.successful()).isTrue();
        assertThat(projected.headers())
                .containsEntry("X-Trace", List.of("trace-1", "trace-2"))
                .containsEntry("X-Array", List.of("one", "two"))
                .containsEntry("X-Optional", List.of("present"))
                .containsEntry(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE,
                        List.of(WayangA2uiTransportContent.MIME_JSON))
                .doesNotContainKey("X-Blank");
        assertThat(projected.header("x-trace")).contains(List.of("trace-1", "trace-2"));
        assertThat(projected.firstHeader("x-array")).contains("one");
        assertThat(projected.firstHeader("missing")).isEmpty();
    }

    @Test
    void preservesExplicitContentTypeHeaderAndSerializesProjection() {
        WayangA2uiHttpResponse response = new WayangA2uiHttpResponse(
                404,
                WayangA2uiTransportContent.MIME_JSON,
                "{}",
                Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, "application/problem+json"));

        WayangA2uiHttpEndpointResponse projected = WayangA2uiHttpEndpointResponse.from(response);
        Map<String, Object> values = projected.toMap();

        assertThat(projected.successful()).isFalse();
        assertThat(projected.firstHeader(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE))
                .contains("application/problem+json");
        assertThat(values)
                .containsEntry("statusCode", 404)
                .containsEntry("contentType", WayangA2uiTransportContent.MIME_JSON)
                .containsEntry("body", "{}")
                .containsEntry("headerCount", 1);
        assertThat((Map<String, List<String>>) values.get("headers"))
                .containsEntry(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, List.of("application/problem+json"));
    }

    @Test
    void endpointBindingCanReturnProjectedResponsesDirectly() {
        WayangA2uiHttpEndpointBinding endpoint = new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");

        WayangA2uiHttpEndpointResponse response = endpoint.respond(
                "GET",
                "/api/a2ui/route-catalog",
                "",
                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT, List.of(WayangA2uiTransportContent.MIME_JSON)));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.firstHeader(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION))
                .contains(WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG);
        assertThat(response.firstHeader(WayangA2uiHttpResponse.HEADER_ALLOW))
                .contains("GET, OPTIONS");
        assertThat(WayangA2uiTransportResponse.fromJson(response.body()).body())
                .contains("\"path\":\"/api/a2ui/exchange\"");
    }
}
