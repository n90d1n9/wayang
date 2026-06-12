package tech.kayys.wayang.a2ui.wayang.transport;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportEnvelope;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportError;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportPayloadKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransportProjectionTest {

    @Test
    void projectsOrderedRequestEnvelopeAndPublicFacadeDelegates() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> dataPart = new LinkedHashMap<>();
        dataPart.put("kind", "userAction");
        dataPart.put("nested", nested);
        dataPart.put("ignored", null);

        Map<String, Object> values = TransportProjection.request(
                WayangA2uiTransportPayloadKind.DATA_PART_MAP,
                null,
                dataPart);
        Map<String, Object> facadeValues = WayangA2uiTransportEnvelope.request(
                WayangA2uiTransportPayloadKind.DATA_PART_MAP,
                null,
                dataPart);
        nested.put("value", "changed");

        assertThat(facadeValues).isEqualTo(values);
        assertThat(values.keySet()).containsExactly("kind", "body", "dataPart");
        assertThat(values)
                .containsEntry("kind", WayangA2uiTransportPayloadKind.DATA_PART_MAP.name())
                .containsEntry("body", "");
        assertThat((Map<String, Object>) values.get("dataPart"))
                .doesNotContainKey("ignored");
        assertThat((Map<String, Object>) ((Map<String, Object>) values.get("dataPart")).get("nested"))
                .containsEntry("value", "original");
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void projectsOrderedResponseEnvelopeAndPublicFacadeDelegates() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> dataPart = new LinkedHashMap<>();
        dataPart.put("kind", "data");
        dataPart.put("nested", nested);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nested", nested);

        Map<String, Object> values = TransportProjection.response(
                WayangA2uiTransportContent.MIME_A2UI,
                WayangA2uiTransportContent.ENCODING_JSONL,
                null,
                List.of(dataPart),
                1,
                0,
                metadata,
                WayangA2uiTransportOutcome.SUCCESS,
                false);
        Map<String, Object> facadeValues = WayangA2uiTransportEnvelope.response(
                WayangA2uiTransportContent.MIME_A2UI,
                WayangA2uiTransportContent.ENCODING_JSONL,
                null,
                List.of(dataPart),
                1,
                0,
                metadata,
                WayangA2uiTransportOutcome.SUCCESS,
                false);
        nested.put("value", "changed");

        assertThat(facadeValues).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "mimeType",
                "bodyEncoding",
                "body",
                "dataParts",
                "handledCount",
                "rejectedCount",
                "metadata",
                "outcome",
                "empty");
        assertThat(values)
                .containsEntry("body", "")
                .containsEntry("handledCount", 1L)
                .containsEntry("rejectedCount", 0L)
                .containsEntry("outcome", WayangA2uiTransportOutcome.SUCCESS.name())
                .containsEntry("empty", false);
        assertThat((Map<String, Object>) ((Map<String, Object>) values.get("metadata")).get("nested"))
                .containsEntry("value", "original");
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void projectsOrderedTransportErrorBodyAndRecordDelegates() {
        WayangA2uiTransportError error = WayangA2uiTransportError.of("bad_request", "Bad request.");

        Map<String, Object> values = TransportProjection.error(error);

        assertThat(error.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly("code", "message");
        assertThat(values)
                .containsEntry("code", "bad_request")
                .containsEntry("message", "Bad request.");
    }
}
