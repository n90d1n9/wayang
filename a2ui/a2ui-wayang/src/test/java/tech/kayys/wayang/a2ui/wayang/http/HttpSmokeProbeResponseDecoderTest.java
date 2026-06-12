package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarios;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.kayys.wayang.a2ui.wayang.http.HttpResponseDecoderTestFixtures.jsonResponse;
import static tech.kayys.wayang.a2ui.wayang.http.HttpResponseDecoderTestFixtures.transportEnvelopeJson;

class HttpSmokeProbeResponseDecoderTest {

    @Test
    void decodesTransportEnvelopeResponseAndDelegatingRecordFactory() {
        WayangA2uiHttpResponse response = jsonResponse(
                transportEnvelopeJson(smokeBodyValues(), smokeMetadataValues()),
                WayangA2uiHttpRoute.smoke());

        WayangA2uiHttpSmokeProbeResult decoded = HttpSmokeProbeResponseDecoder.from(response);

        assertThat(decoded.passed()).isTrue();
        assertThat(decoded.smokeRoute()).isTrue();
        assertThat(decoded.summary().smokeResult()).isTrue();
        assertThat(decoded.summary().suiteId()).isEqualTo(WayangA2uiHttpScenarios.SMOKE_SUITE_ID);
        assertThat(decoded.summary().scenarioCount()).isEqualTo(3);
        assertThat(decoded.summary().routeCount()).isEqualTo(6);
        assertThat(decoded.headers()).containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2uiHttpSmokeProbeResult.from(response)).isEqualTo(decoded);
    }

    @Test
    void malformedBodiesKeepStrictTransportDecodeError() {
        assertThatThrownBy(() -> HttpSmokeProbeResponseDecoder
                .from(jsonResponse(" ", WayangA2uiHttpRoute.smoke())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI transport response JSON must not be blank");
        assertThatThrownBy(() -> HttpSmokeProbeResponseDecoder
                .from(jsonResponse("not-json", WayangA2uiHttpRoute.smoke())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to decode A2UI transport response JSON");
    }

    private static Map<String, Object> smokeMetadataValues() {
        return Map.ofEntries(
                Map.entry(
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_SMOKE_RESULT),
                Map.entry(WayangA2uiTransportFields.PASSED, true),
                Map.entry(WayangA2uiTransportFields.EXIT_CODE, 0),
                Map.entry(WayangA2uiTransportFields.SUITE_ID, WayangA2uiHttpScenarios.SMOKE_SUITE_ID),
                Map.entry(WayangA2uiTransportFields.SCENARIO_COUNT, 3),
                Map.entry(WayangA2uiTransportFields.ISSUE_COUNT, 0),
                Map.entry(WayangA2uiTransportFields.ROUTE_COUNT, 6));
    }

    private static Map<String, Object> smokeBodyValues() {
        return Map.of(
                WayangA2uiTransportFields.PASSED,
                true,
                WayangA2uiTransportFields.EXIT_CODE,
                0,
                "suiteReport",
                Map.of(
                        WayangA2uiTransportFields.SUITE_ID,
                        WayangA2uiHttpScenarios.SMOKE_SUITE_ID,
                        WayangA2uiTransportFields.SCENARIO_COUNT,
                        3,
                        WayangA2uiTransportFields.ISSUE_COUNT,
                        0),
                "expectationResult",
                Map.of(WayangA2uiTransportFields.ISSUE_COUNT, 0),
                "attributes",
                Map.of(WayangA2uiTransportFields.ROUTE_COUNT, 6));
    }
}
