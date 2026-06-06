package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;

/**
 * Canonical map builders for Wayang A2UI transport envelopes.
 */
public final class WayangA2uiTransportEnvelope {

    public static Map<String, Object> request(
            WayangA2uiTransportPayloadKind kind,
            String body,
            Map<?, ?> dataPart) {
        return WayangA2uiTransportProjection.request(kind, body, dataPart);
    }

    public static Map<String, Object> response(
            String mimeType,
            String bodyEncoding,
            String body,
            List<? extends Map<?, ?>> dataParts,
            long handledCount,
            long rejectedCount,
            Map<?, ?> metadata,
            WayangA2uiTransportOutcome outcome,
            boolean empty) {
        return WayangA2uiTransportProjection.response(
                mimeType,
                bodyEncoding,
                body,
                dataParts,
                handledCount,
                rejectedCount,
                metadata,
                outcome,
                empty);
    }

    private WayangA2uiTransportEnvelope() {
    }
}
