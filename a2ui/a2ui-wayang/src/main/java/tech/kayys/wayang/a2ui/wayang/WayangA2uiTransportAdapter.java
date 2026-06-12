package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.util.Map;
import java.util.Objects;

/**
 * Transport-neutral facade over a Wayang A2UI session.
 */
public final class WayangA2uiTransportAdapter {

    private final WayangA2uiSession session;

    public WayangA2uiTransportAdapter(WayangGollekSdk sdk) {
        this(new WayangA2uiSession(sdk));
    }

    public WayangA2uiTransportAdapter(WayangGollekSdk sdk, WayangA2uiSessionConfig config) {
        this(new WayangA2uiSession(sdk, config));
    }

    public WayangA2uiTransportAdapter(
            WayangGollekSdk sdk,
            WayangA2uiSessionConfig config,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        this(new WayangA2uiSession(sdk, config, surfaceRegistry));
    }

    public WayangA2uiTransportAdapter(WayangA2uiSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    public WayangA2uiTransportResponse exchange(WayangA2uiTransportRequest request) {
        WayangA2uiTransportRequest resolved = Objects.requireNonNull(request, "request");
        WayangA2uiTransportResponse response = switch (resolved.kind()) {
            case JSON_LINE -> WayangA2uiTransportResponse.from(session.handleJsonLine(resolved.body()));
            case JSONL -> WayangA2uiTransportResponse.from(session.handleJsonl(resolved.body()));
            case DATA_PART_JSON -> WayangA2uiTransportResponse.from(session.handleDataPart(resolved.body()));
            case DATA_PART_MAP -> WayangA2uiTransportResponse.from(session.handleDataPart(resolved.dataPart()));
            case SURFACE_CATALOG -> WayangA2uiTransportResponse.from(session.surfaceCatalog());
            case ACTION_BINDING_REPORT -> WayangA2uiTransportResponse.from(session.actionBindingReport());
        };
        return response.withMetadata(WayangA2uiTransportMetadata.request(resolved.kind()));
    }

    public WayangA2uiTransportResponse exchangeEnvelope(Map<?, ?> requestEnvelope) {
        return exchange(WayangA2uiTransportRequest.fromMap(requestEnvelope));
    }

    public WayangA2uiTransportResponse exchangeEnvelopeOrError(Map<?, ?> requestEnvelope) {
        try {
            return exchangeEnvelope(requestEnvelope);
        } catch (RuntimeException e) {
            return WayangA2uiTransportResponse.error("invalid_request_envelope", e.getMessage());
        }
    }

    public WayangA2uiTransportResponse exchangeEnvelopeJson(String requestEnvelopeJson) {
        return exchange(WayangA2uiTransportRequest.fromJson(requestEnvelopeJson));
    }

    public WayangA2uiTransportResponse exchangeEnvelopeJsonOrError(String requestEnvelopeJson) {
        try {
            return exchangeEnvelopeJson(requestEnvelopeJson);
        } catch (RuntimeException e) {
            return WayangA2uiTransportResponse.error("invalid_request_json", e.getMessage());
        }
    }

    public Map<String, Object> exchangeEnvelopeAsMap(Map<?, ?> requestEnvelope) {
        return exchangeEnvelope(requestEnvelope).toMap();
    }

    public Map<String, Object> exchangeEnvelopeAsMapOrError(Map<?, ?> requestEnvelope) {
        return exchangeEnvelopeOrError(requestEnvelope).toMap();
    }

    public String exchangeEnvelopeAsJson(String requestEnvelopeJson) {
        return exchangeEnvelopeJson(requestEnvelopeJson).toJson();
    }

    public String exchangeEnvelopeAsJsonOrError(String requestEnvelopeJson) {
        return exchangeEnvelopeJsonOrError(requestEnvelopeJson).toJson();
    }

    public WayangA2uiTransportResponse exchangeJsonLine(String line) {
        return exchange(WayangA2uiTransportRequest.jsonLine(line));
    }

    public WayangA2uiTransportResponse exchangeJsonl(String jsonl) {
        return exchange(WayangA2uiTransportRequest.jsonl(jsonl));
    }

    public WayangA2uiTransportResponse exchangeDataPart(String dataPart) {
        return exchange(WayangA2uiTransportRequest.dataPart(dataPart));
    }

    public WayangA2uiTransportResponse exchangeDataPart(Map<?, ?> dataPart) {
        return exchange(WayangA2uiTransportRequest.dataPart(dataPart));
    }

    public WayangA2uiTransportResponse exchangeSurfaceCatalog() {
        return exchange(WayangA2uiTransportRequest.surfaceCatalog());
    }

    public WayangA2uiTransportResponse exchangeActionBindingReport() {
        return exchange(WayangA2uiTransportRequest.actionBindingReport());
    }

    public WayangA2uiSurfaceCatalog surfaceCatalog() {
        return session.surfaceCatalog();
    }

    public WayangA2uiActionBindingReport actionBindingReport() {
        return session.actionBindingReport();
    }
}
