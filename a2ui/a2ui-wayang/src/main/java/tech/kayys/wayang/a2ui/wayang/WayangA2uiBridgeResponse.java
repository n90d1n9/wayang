package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bridge-facing response wrapper with adapter-specific attributes.
 */
public record WayangA2uiBridgeResponse(
        WayangA2uiTransportResponse transportResponse,
        Map<String, Object> attributes) {

    public WayangA2uiBridgeResponse {
        transportResponse = Objects.requireNonNull(transportResponse, "transportResponse");
        attributes = TransportMaps.copy(attributes);
    }

    public static WayangA2uiBridgeResponse of(WayangA2uiTransportResponse response) {
        return new WayangA2uiBridgeResponse(response, Map.of());
    }

    public static WayangA2uiBridgeResponse of(WayangA2uiTransportResponse response, Map<?, ?> attributes) {
        return new WayangA2uiBridgeResponse(response, TransportMaps.copy(attributes));
    }

    public Map<String, Object> transportEnvelope() {
        return transportResponse.toMap();
    }

    public String transportEnvelopeJson() {
        return transportResponse.toJson();
    }

    public WayangA2uiTransportOutcome outcome() {
        return transportResponse.outcome();
    }

    public Optional<WayangA2uiTransportError> transportError() {
        return transportResponse.transportError();
    }

    public WayangA2uiBridgeResponse withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        return new WayangA2uiBridgeResponse(
                transportResponse,
                WayangA2uiTransportMetadata.merge(attributes, TransportMaps.copy(extraAttributes)));
    }
}
