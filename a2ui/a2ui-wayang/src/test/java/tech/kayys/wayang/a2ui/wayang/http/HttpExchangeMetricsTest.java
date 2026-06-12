package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExchangeMetricsTest {

    @Test
    void aggregatesSharedExchangeMetrics() {
        List<MetricExchange> exchanges = List.of(
                new MetricExchange(200, true, response(2, 0)),
                new MetricExchange(404, false, WayangA2uiTransportResponse.error("not_found", "Missing")),
                new MetricExchange(503, false, response(0, 3)));

        assertThat(HttpExchangeMetrics.exchangeCount(exchanges)).isEqualTo(3);
        assertThat(HttpExchangeMetrics.successfulCount(exchanges)).isEqualTo(1L);
        assertThat(HttpExchangeMetrics.clientErrorCount(exchanges)).isEqualTo(1L);
        assertThat(HttpExchangeMetrics.serverErrorCount(exchanges)).isEqualTo(1L);
        assertThat(HttpExchangeMetrics.handledCount(exchanges)).isEqualTo(2L);
        assertThat(HttpExchangeMetrics.rejectedCount(exchanges)).isEqualTo(4L);
        assertThat(HttpExchangeMetrics.hasTransportErrors(exchanges)).isTrue();
        assertThat(HttpExchangeMetrics.statusCodes(exchanges)).containsExactly(200, 404, 503);
        assertThat(HttpExchangeMetrics.outcomes(exchanges)).containsExactly(
                WayangA2uiTransportOutcome.SUCCESS,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR,
                WayangA2uiTransportOutcome.ACTION_REJECTED);
        assertThat(HttpExchangeMetrics.responseEnvelopes(exchanges))
                .extracting(envelope -> envelope.get(WayangA2uiTransportFields.OUTCOME))
                .containsExactly("SUCCESS", "TRANSPORT_ERROR", "ACTION_REJECTED");
    }

    @Test
    void treatsNullExchangeListsAsEmpty() {
        assertThat(HttpExchangeMetrics.exchangeCount(null)).isZero();
        assertThat(HttpExchangeMetrics.successfulCount(null)).isZero();
        assertThat(HttpExchangeMetrics.statusCodes(null)).isEmpty();
        assertThat(HttpExchangeMetrics.responseEnvelopes(null)).isEmpty();
    }

    @Test
    void ignoresNullHttpExchangeEntries() {
        List<MetricExchange> exchanges = new ArrayList<>();
        exchanges.add(new MetricExchange(200, true, response(1, 0)));
        exchanges.add(null);
        exchanges.add(new MetricExchange(503, false, response(0, 2)));

        assertThat(HttpExchangeMetrics.exchangeCount(exchanges)).isEqualTo(2);
        assertThat(HttpExchangeMetrics.successfulCount(exchanges)).isEqualTo(1L);
        assertThat(HttpExchangeMetrics.serverErrorCount(exchanges)).isEqualTo(1L);
        assertThat(HttpExchangeMetrics.statusCodes(exchanges)).containsExactly(200, 503);
    }

    private static WayangA2uiTransportResponse response(long handledCount, long rejectedCount) {
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                "{\"ok\":true}",
                List.of(),
                handledCount,
                rejectedCount,
                Map.of());
    }

    private record MetricExchange(
            int statusCode,
            boolean successful,
            WayangA2uiTransportResponse transportResponse) implements HttpMetricExchange {
    }
}
