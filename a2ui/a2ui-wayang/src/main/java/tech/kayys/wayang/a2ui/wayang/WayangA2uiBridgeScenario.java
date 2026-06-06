package tech.kayys.wayang.a2ui.wayang;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Named bridge scenario for replaying a deterministic sequence of A2UI requests.
 */
public record WayangA2uiBridgeScenario(
        String id,
        List<WayangA2uiBridgeRequest> requests,
        Map<String, Object> attributes) {

    public WayangA2uiBridgeScenario {
        id = id == null || id.isBlank() ? "a2ui-bridge-scenario" : id.trim();
        requests = requests == null
                ? List.of()
                : requests.stream()
                        .filter(Objects::nonNull)
                        .toList();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiBridgeScenario of(String id, WayangA2uiBridgeRequest... requests) {
        return of(id, requests == null ? List.of() : Arrays.asList(requests));
    }

    public static WayangA2uiBridgeScenario of(String id, List<WayangA2uiBridgeRequest> requests) {
        return new WayangA2uiBridgeScenario(id, requests, Map.of());
    }

    public static WayangA2uiBridgeScenario envelope(String id, List<? extends Map<?, ?>> requestEnvelopes) {
        List<WayangA2uiBridgeRequest> requests = requestEnvelopes == null
                ? List.of()
                : requestEnvelopes.stream()
                        .filter(Objects::nonNull)
                        .map(WayangA2uiBridgeRequest::envelope)
                        .toList();
        return of(id, requests);
    }

    public static WayangA2uiBridgeScenario envelopeJson(String id, List<String> requestEnvelopeJsons) {
        List<WayangA2uiBridgeRequest> requests = requestEnvelopeJsons == null
                ? List.of()
                : requestEnvelopeJsons.stream()
                        .filter(json -> json != null && !json.isBlank())
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
                WayangA2uiTransportMetadata.merge(attributes, WayangA2uiTransportMaps.copy(extraAttributes)));
    }

    public boolean empty() {
        return requests.isEmpty();
    }

    public int size() {
        return requests.size();
    }
}
