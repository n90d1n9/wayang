package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * One request/response exchange captured during a bridge scenario run.
 */
public record WayangA2uiBridgeScenarioExchange(
        int index,
        WayangA2uiBridgeRequest request,
        WayangA2uiBridgeResponse response) {

    public WayangA2uiBridgeScenarioExchange {
        index = Math.max(1, index);
        request = Objects.requireNonNull(request, "request");
        response = Objects.requireNonNull(response, "response");
    }

    public WayangA2uiTransportOutcome outcome() {
        return response.outcome();
    }

    public boolean transportError() {
        return response.transportError().isPresent();
    }

    public Map<String, Object> requestEnvelope() {
        return request.transportEnvelope();
    }

    public Map<String, Object> responseEnvelope() {
        return response.transportEnvelope();
    }
}
