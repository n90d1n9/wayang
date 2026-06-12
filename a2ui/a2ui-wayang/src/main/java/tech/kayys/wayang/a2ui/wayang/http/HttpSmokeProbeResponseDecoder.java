package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeSummary;

import java.util.Objects;

/**
 * Decodes HTTP smoke probe responses into canonical probe results.
 */
public final class HttpSmokeProbeResponseDecoder {

    public static WayangA2uiHttpSmokeProbeResult from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        return new WayangA2uiHttpSmokeProbeResult(
                resolved.statusCode(),
                resolved.successful(),
                resolved.header(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION),
                resolved.header(WayangA2uiHttpResponse.HEADER_ALLOW),
                resolved.header(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME),
                WayangA2uiHttpSmokeSummary.from(resolved),
                resolved.headers());
    }

    private HttpSmokeProbeResponseDecoder() {
    }
}
