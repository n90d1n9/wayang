package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpResponseTest {

    @Test
    void readsHeadersAsRawTextWithEmptyMissingFallback() {
        WayangA2uiHttpResponse response = new WayangA2uiHttpResponse(
                200,
                WayangA2uiTransportContent.MIME_JSON,
                "{}",
                Map.of(
                        "X-Number",
                        42,
                        "X-Spaced",
                        " value "));

        assertThat(response.header("X-Number")).isEqualTo("42");
        assertThat(response.header("X-Spaced")).isEqualTo(" value ");
        assertThat(response.header("missing")).isEmpty();
    }
}
