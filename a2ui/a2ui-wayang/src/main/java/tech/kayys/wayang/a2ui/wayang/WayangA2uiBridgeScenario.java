package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * Named bridge scenario for replaying a deterministic sequence of A2UI requests.
 */
public record WayangA2uiBridgeScenario(
        String id,
        List<WayangA2uiBridgeRequest> requests,
        Map<String, Object> attributes) {

    public WayangA2uiBridgeScenario {
        id = RecordValues.textOrDefault(id, "a2ui-bridge-scenario");
        requests = RecordCollections.nonNullList(requests);
        attributes = TransportMaps.copy(attributes);
    }

    public static WayangA2uiBridgeScenario of(String id, WayangA2uiBridgeRequest... requests) {
        return of(id, RecordCollections.nonNullVarargs(requests));
    }

    public static WayangA2uiBridgeScenario of(String id, List<WayangA2uiBridgeRequest> requests) {
        return new WayangA2uiBridgeScenario(id, requests, Map.of());
    }

    public static WayangA2uiBridgeScenario envelope(String id, List<? extends Map<?, ?>> requestEnvelopes) {
        List<WayangA2uiBridgeRequest> requests = RecordCollections.nonNullList(requestEnvelopes)
                .stream()
                .map(WayangA2uiBridgeRequest::envelope)
                .toList();
        return of(id, requests);
    }

    public static WayangA2uiBridgeScenario envelopeJson(String id, List<String> requestEnvelopeJsons) {
        List<WayangA2uiBridgeRequest> requests = RecordCollections.nonBlankStrings(requestEnvelopeJsons)
                .stream()
                .map(WayangA2uiBridgeRequest::envelopeJson)
                .toList();
        return of(id, requests);
    }

    public WayangA2uiBridgeScenario withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        return new WayangA2uiBridgeScenario(
                id,
                requests,
                WayangA2uiTransportMetadata.merge(attributes, TransportMaps.copy(extraAttributes)));
    }

    public boolean empty() {
        return requests.isEmpty();
    }

    public int size() {
        return requests.size();
    }
}
