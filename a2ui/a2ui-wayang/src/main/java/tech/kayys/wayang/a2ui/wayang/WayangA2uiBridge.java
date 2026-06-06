package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Bridge SPI for concrete A2UI transports such as HTTP, MCP, A2A, and harnesses.
 */
public interface WayangA2uiBridge {

    WayangA2uiBridgeResponse exchange(WayangA2uiBridgeRequest request);

    default WayangA2uiBridgeResponse exchange(WayangA2uiTransportRequest request) {
        return exchange(WayangA2uiBridgeRequest.of(request));
    }

    default WayangA2uiBridgeResponse exchangeEnvelope(Map<?, ?> requestEnvelope) {
        return exchange(WayangA2uiBridgeRequest.envelope(requestEnvelope));
    }

    default WayangA2uiBridgeResponse exchangeEnvelopeJson(String requestEnvelopeJson) {
        return exchange(WayangA2uiBridgeRequest.envelopeJson(requestEnvelopeJson));
    }

    default WayangA2uiBridgeResponse exchangeEnvelopeOrError(Map<?, ?> requestEnvelope) {
        try {
            return exchangeEnvelope(requestEnvelope);
        } catch (RuntimeException e) {
            return WayangA2uiBridgeResponse.of(
                    WayangA2uiTransportResponse.error("invalid_request_envelope", e.getMessage()));
        }
    }

    default WayangA2uiBridgeResponse exchangeEnvelopeJsonOrError(String requestEnvelopeJson) {
        try {
            return exchangeEnvelopeJson(requestEnvelopeJson);
        } catch (RuntimeException e) {
            return WayangA2uiBridgeResponse.of(
                    WayangA2uiTransportResponse.error("invalid_request_json", e.getMessage()));
        }
    }

    static WayangA2uiBridge from(WayangA2uiTransportAdapter adapter) {
        return new WayangA2uiTransportBridge(adapter);
    }
}
