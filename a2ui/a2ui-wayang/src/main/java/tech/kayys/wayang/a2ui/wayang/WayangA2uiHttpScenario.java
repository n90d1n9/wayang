package tech.kayys.wayang.a2ui.wayang;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Named HTTP scenario for replaying a deterministic sequence of A2UI requests.
 */
public record WayangA2uiHttpScenario(
        String id,
        List<WayangA2uiHttpRequest> requests,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenario {
        id = id == null || id.isBlank() ? "a2ui-http-scenario" : id.trim();
        requests = requests == null
                ? List.of()
                : requests.stream()
                        .filter(Objects::nonNull)
                        .toList();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpScenario of(String id, WayangA2uiHttpRequest... requests) {
        return of(id, requests == null ? List.of() : Arrays.asList(requests));
    }

    public static WayangA2uiHttpScenario of(String id, List<WayangA2uiHttpRequest> requests) {
        return new WayangA2uiHttpScenario(id, requests, Map.of());
    }

    public static WayangA2uiHttpScenario exchangeJson(String id, List<String> requestEnvelopeJsons) {
        List<WayangA2uiHttpRequest> requests = requestEnvelopeJsons == null
                ? List.of()
                : requestEnvelopeJsons.stream()
                        .filter(json -> json != null && !json.isBlank())
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
                WayangA2uiTransportMetadata.merge(attributes, WayangA2uiTransportMaps.copy(extraAttributes)));
    }

    public boolean empty() {
        return requests.isEmpty();
    }

    public int size() {
        return requests.size();
    }
}
