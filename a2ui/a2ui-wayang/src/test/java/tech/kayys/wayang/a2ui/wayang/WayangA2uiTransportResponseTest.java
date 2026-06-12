package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiTransportResponseTest {

    @Test
    void errorResponseJsonUsesCanonicalMapOrder() {
        WayangA2uiTransportResponse response = WayangA2uiTransportResponse.error("not_found", "Missing");

        assertThat(response.body()).isEqualTo("{\"code\":\"not_found\",\"message\":\"Missing\"}");
        assertThat(response.toJson()).isEqualTo(
                "{\"mimeType\":\"application/problem+json\",\"bodyEncoding\":\"json\","
                        + "\"body\":\"{\\\"code\\\":\\\"not_found\\\",\\\"message\\\":\\\"Missing\\\"}\","
                        + "\"dataParts\":[],\"handledCount\":0,\"rejectedCount\":1,"
                        + "\"metadata\":{\"responseKind\":\"transport-error\",\"errorCode\":\"not_found\","
                        + "\"error\":{\"code\":\"not_found\",\"message\":\"Missing\"},"
                        + "\"handledCount\":0,\"rejectedCount\":1},"
                        + "\"outcome\":\"TRANSPORT_ERROR\",\"empty\":false}");
    }
}
