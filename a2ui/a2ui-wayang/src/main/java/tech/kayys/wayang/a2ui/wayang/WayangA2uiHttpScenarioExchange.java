package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * One HTTP request/response exchange captured during a scenario run.
 */
public record WayangA2uiHttpScenarioExchange(
        int index,
        WayangA2uiHttpRequest request,
        WayangA2uiHttpResponse response) {

    public WayangA2uiHttpScenarioExchange {
        index = Math.max(1, index);
        request = Objects.requireNonNull(request, "request");
        response = Objects.requireNonNull(response, "response");
    }

    public int statusCode() {
        return response.statusCode();
    }

    public boolean successful() {
        return response.successful();
    }

    public WayangA2uiTransportResponse transportResponse() {
        return WayangA2uiTransportResponse.fromJson(response.body());
    }

    public WayangA2uiTransportOutcome outcome() {
        return transportResponse().outcome();
    }

    public boolean transportError() {
        return transportResponse().transportError().isPresent();
    }

    public Map<String, Object> responseEnvelope() {
        return transportResponse().toMap();
    }
}
