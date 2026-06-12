package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;
import java.util.Objects;

/**
 * Bridge-facing request wrapper with adapter-specific attributes.
 */
public record WayangA2uiBridgeRequest(
        WayangA2uiTransportRequest transportRequest,
        Map<String, Object> attributes) {

    public WayangA2uiBridgeRequest {
        transportRequest = Objects.requireNonNull(transportRequest, "transportRequest");
        attributes = TransportMaps.copy(attributes);
    }

    public static WayangA2uiBridgeRequest of(WayangA2uiTransportRequest request) {
        return new WayangA2uiBridgeRequest(request, Map.of());
    }

    public static WayangA2uiBridgeRequest of(WayangA2uiTransportRequest request, Map<?, ?> attributes) {
        return new WayangA2uiBridgeRequest(request, TransportMaps.copy(attributes));
    }

    public static WayangA2uiBridgeRequest envelope(Map<?, ?> requestEnvelope) {
        return of(WayangA2uiTransportRequest.fromMap(requestEnvelope));
    }

    public static WayangA2uiBridgeRequest envelopeJson(String requestEnvelopeJson) {
        return of(WayangA2uiTransportRequest.fromJson(requestEnvelopeJson));
    }

    public Map<String, Object> transportEnvelope() {
        return transportRequest.toMap();
    }

    public String transportEnvelopeJson() {
        return transportRequest.toJson();
    }

    public WayangA2uiBridgeRequest withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        return new WayangA2uiBridgeRequest(
                transportRequest,
                WayangA2uiTransportMetadata.merge(attributes, TransportMaps.copy(extraAttributes)));
    }
}
