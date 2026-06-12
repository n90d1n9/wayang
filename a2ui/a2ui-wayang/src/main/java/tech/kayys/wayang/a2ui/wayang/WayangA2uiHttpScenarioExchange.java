package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpMetricExchange;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;

import java.util.Objects;

/**
 * One HTTP request/response exchange captured during a scenario run.
 */
public record WayangA2uiHttpScenarioExchange(
        int index,
        WayangA2uiHttpRequest request,
        WayangA2uiHttpResponse response) implements HttpMetricExchange {

    public WayangA2uiHttpScenarioExchange {
        index = RecordNumbers.oneBased(index);
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
}
