package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpResponseBodyDecoderTest {

    @Test
    void decodesTransportEnvelopeBodyAndMetadata() {
        String responseBody = new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                TransportJson.json(Map.of("complete", true), "Unable to encode test body"),
                List.of(),
                1,
                0,
                Map.of("source", "transport"))
                .toJson();

        HttpResponseBodyDecoder.Envelope envelope =
                HttpResponseBodyDecoder.lenientJsonEnvelope(
                        responseBody,
                        "test JSON must not be blank",
                        "Unable to decode test JSON");

        assertThat(envelope.metadata()).containsEntry("source", "transport");
        assertThat(envelope.body()).containsEntry("complete", true);
        assertThat(envelope.mimeType()).isEqualTo(WayangA2uiTransportContent.MIME_JSON);
        assertThat(envelope.bodyEncoding()).isEqualTo(WayangA2uiTransportContent.ENCODING_JSON);
        assertThat(envelope.outcome()).isEqualTo(WayangA2uiTransportOutcome.SUCCESS.name());
    }

    @Test
    void decodesRawJsonBodyWhenTransportEnvelopeBodyIsEmpty() {
        HttpResponseBodyDecoder.Envelope envelope =
                HttpResponseBodyDecoder.lenientJsonEnvelope(
                        "{\"complete\":true}",
                        "test JSON must not be blank",
                        "Unable to decode test JSON");

        assertThat(envelope.metadata()).isEmpty();
        assertThat(envelope.body()).containsEntry("complete", true);
        assertThat(envelope.mimeType()).isEmpty();
        assertThat(envelope.bodyEncoding()).isEmpty();
        assertThat(envelope.outcome()).isEmpty();
    }

    @Test
    void treatsRawPayloadBodyAndMetadataFieldsAsPayloadFields() {
        HttpResponseBodyDecoder.Envelope envelope =
                HttpResponseBodyDecoder.lenientJsonEnvelope(
                        """
                        {
                          "body": {"complete": true},
                          "metadata": {"source": "raw-payload"}
                        }
                        """,
                        "test JSON must not be blank",
                        "Unable to decode test JSON");

        assertThat(envelope.metadata()).isEmpty();
        assertThat(envelope.body())
                .containsKey(WayangA2uiTransportFields.BODY)
                .containsKey(WayangA2uiTransportFields.METADATA);
        assertThat(envelope.mimeType()).isEmpty();
        assertThat(envelope.bodyEncoding()).isEmpty();
        assertThat(envelope.outcome()).isEmpty();
    }

    @Test
    void blankOrMalformedBodiesDecodeToEmptyMaps() {
        assertThat(HttpResponseBodyDecoder.lenientJsonBody(
                " ",
                "test JSON must not be blank",
                "Unable to decode test JSON")).isEmpty();
        assertThat(HttpResponseBodyDecoder.lenientJsonBody(
                "not-json",
                "test JSON must not be blank",
                "Unable to decode test JSON")).isEmpty();
    }
}
