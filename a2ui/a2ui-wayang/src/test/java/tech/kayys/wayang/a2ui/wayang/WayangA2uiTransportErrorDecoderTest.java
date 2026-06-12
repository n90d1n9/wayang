package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiTransportErrorDecoderTest {

    @Test
    void defaultsEmptyErrorDetails() {
        WayangA2uiTransportError error = WayangA2uiTransportError.defaultError();

        assertThat(error.code()).isEqualTo(WayangA2uiTransportError.DEFAULT_CODE);
        assertThat(error.message()).isEqualTo(WayangA2uiTransportError.DEFAULT_MESSAGE);
        assertThat(WayangA2uiTransportError.fromMap(Map.of())).isEqualTo(error);
        assertThat(WayangA2uiTransportErrorDecoder.fromMap(Map.of())).isEqualTo(error);
        assertThat(WayangA2uiTransportErrorDecoder.fromMap(null)).isEqualTo(error);
    }

    @Test
    void decodesMapAndLetsRecordNormalizeText() {
        WayangA2uiTransportError error = WayangA2uiTransportErrorDecoder.fromMap(Map.of(
                WayangA2uiTransportFields.CODE,
                " bad_request ",
                WayangA2uiTransportFields.MESSAGE,
                " Bad request. "));

        assertThat(error.code()).isEqualTo("bad_request");
        assertThat(error.message()).isEqualTo("Bad request.");
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = Map.of(
                WayangA2uiTransportFields.CODE,
                "malformed_json",
                WayangA2uiTransportFields.MESSAGE,
                "Malformed JSON.");

        assertThat(WayangA2uiTransportError.fromMap(values))
                .isEqualTo(WayangA2uiTransportErrorDecoder.fromMap(values));
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiTransportError decoded = WayangA2uiTransportErrorDecoder.fromJson("""
                {
                  "code": "unsupported_media_type",
                  "message": "Unsupported media type."
                }
                """);

        assertThat(decoded).isEqualTo(
                WayangA2uiTransportError.of("unsupported_media_type", "Unsupported media type."));
        assertThat(WayangA2uiTransportError.fromJson(decoded.toJson())).isEqualTo(decoded);
        assertThatThrownBy(() -> WayangA2uiTransportErrorDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI transport error JSON must not be blank");
    }
}
