package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.Map;

/**
 * Shared lenient JSON body decoder for HTTP responses that may carry transport envelopes.
 */
public final class HttpResponseBodyDecoder {

    public static Map<String, Object> lenientJsonBody(
            String responseBody,
            String blankMessage,
            String errorMessage) {
        return lenientJsonEnvelope(responseBody, blankMessage, errorMessage).body();
    }

    public static Envelope lenientJsonEnvelope(
            String responseBody,
            String blankMessage,
            String errorMessage) {
        if (responseBody == null || responseBody.isBlank()) {
            return Envelope.empty();
        }
        Map<String, Object> responseMap = jsonMap(responseBody, blankMessage, errorMessage);
        if (!transportEnvelope(responseMap)) {
            return new Envelope(Map.of(), responseMap, "", "", "");
        }
        try {
            WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromMap(responseMap);
            String payload = transport.body().isBlank() ? responseBody : transport.body();
            return new Envelope(
                    transport.metadata(),
                    jsonMap(payload, blankMessage, errorMessage),
                    transport.mimeType(),
                    transport.bodyEncoding(),
                    transport.outcome().name());
        } catch (IllegalArgumentException ignored) {
            return new Envelope(Map.of(), responseMap, "", "", "");
        }
    }

    private static boolean transportEnvelope(Map<String, Object> values) {
        return values.containsKey(WayangA2uiTransportFields.DATA_PARTS)
                || values.containsKey(WayangA2uiTransportFields.HANDLED_COUNT)
                || values.containsKey(WayangA2uiTransportFields.REJECTED_COUNT)
                || values.containsKey(WayangA2uiTransportFields.OUTCOME)
                || values.containsKey(WayangA2uiTransportFields.EMPTY)
                || values.containsKey(WayangA2uiTransportFields.MIME_TYPE)
                || values.containsKey(WayangA2uiTransportFields.BODY_ENCODING);
    }

    private static Map<String, Object> jsonMap(
            String body,
            String blankMessage,
            String errorMessage) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return TransportJson.map(body, blankMessage, errorMessage);
        } catch (IllegalArgumentException ignored) {
            return Map.of();
        }
    }

    public record Envelope(
            Map<String, Object> metadata,
            Map<String, Object> body,
            String mimeType,
            String bodyEncoding,
            String outcome) {

        public Envelope {
            metadata = TransportMaps.copy(metadata);
            body = TransportMaps.copy(body);
            mimeType = DecodeValues.text(mimeType);
            bodyEncoding = DecodeValues.text(bodyEncoding);
            outcome = DecodeValues.text(outcome);
        }

        static Envelope empty() {
            return new Envelope(Map.of(), Map.of(), "", "", "");
        }
    }

    private HttpResponseBodyDecoder() {
    }
}
