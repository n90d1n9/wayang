package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * Named HTTP scenario for replaying a deterministic sequence of A2UI requests.
 */
public record WayangA2uiHttpScenario(
        String id,
        List<WayangA2uiHttpRequest> requests,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenario {
        id = RecordValues.textOrDefault(id, "a2ui-http-scenario");
        requests = RecordCollections.nonNullList(requests);
        attributes = TransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpScenario of(String id, WayangA2uiHttpRequest... requests) {
        return of(id, RecordCollections.nonNullVarargs(requests));
    }

    public static WayangA2uiHttpScenario of(String id, List<WayangA2uiHttpRequest> requests) {
        return new WayangA2uiHttpScenario(id, requests, Map.of());
    }

    public static WayangA2uiHttpScenario exchangeJson(String id, List<String> requestEnvelopeJsons) {
        List<WayangA2uiHttpRequest> requests = RecordCollections.nonBlankStrings(requestEnvelopeJsons)
                .stream()
                .map(WayangA2uiHttpRequest::exchange)
                .toList();
        return of(id, requests);
    }

    public WayangA2uiHttpScenario withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        return new WayangA2uiHttpScenario(
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
