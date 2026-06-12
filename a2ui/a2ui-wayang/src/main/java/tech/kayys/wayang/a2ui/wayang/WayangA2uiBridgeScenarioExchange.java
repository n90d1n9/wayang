package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMetricExchange;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;

import java.util.Map;
import java.util.Objects;

/**
 * One request/response exchange captured during a bridge scenario run.
 */
public record WayangA2uiBridgeScenarioExchange(
        int index,
        WayangA2uiBridgeRequest request,
        WayangA2uiBridgeResponse response) implements TransportMetricExchange {

    public WayangA2uiBridgeScenarioExchange {
        index = RecordNumbers.oneBased(index);
        request = Objects.requireNonNull(request, "request");
        response = Objects.requireNonNull(response, "response");
    }

    public WayangA2uiTransportResponse transportResponse() {
        return response.transportResponse();
    }

    public Map<String, Object> requestEnvelope() {
        return request.transportEnvelope();
    }
}
