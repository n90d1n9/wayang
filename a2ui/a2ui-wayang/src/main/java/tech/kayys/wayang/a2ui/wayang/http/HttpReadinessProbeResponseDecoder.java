package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpReadinessProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpReadinessProbeResultDecoder;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;

import java.util.Objects;

/**
 * Decodes HTTP readiness probe responses into canonical readiness results.
 */
public final class HttpReadinessProbeResponseDecoder {

    public static WayangA2uiHttpReadinessProbeResult from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        return WayangA2uiHttpReadinessProbeResultDecoder.fromMap(
                HttpResponseBodyDecoder.lenientJsonBody(
                        resolved.body(),
                        "A2UI HTTP readiness probe result JSON must not be blank",
                        "Unable to decode A2UI HTTP readiness probe result JSON"));
    }

    private HttpReadinessProbeResponseDecoder() {
    }
}
