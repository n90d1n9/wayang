package tech.kayys.wayang.a2ui.wayang.transport;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransportExchangeMetricsTest {

    @Test
    void aggregatesTransportMetricsAcrossExchangeViews() {
        List<MetricExchange> exchanges = List.of(
                new MetricExchange(response(2, 0)),
                new MetricExchange(response(0, 3)),
                new MetricExchange(WayangA2uiTransportResponse.error("transport_failed", "Failed")));

        assertThat(TransportExchangeMetrics.handledCount(exchanges)).isEqualTo(2L);
        assertThat(TransportExchangeMetrics.rejectedCount(exchanges)).isEqualTo(4L);
        assertThat(TransportExchangeMetrics.hasTransportErrors(exchanges)).isTrue();
        assertThat(TransportExchangeMetrics.outcomes(exchanges)).containsExactly(
                WayangA2uiTransportOutcome.SUCCESS,
                WayangA2uiTransportOutcome.ACTION_REJECTED,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR);
        assertThat(TransportExchangeMetrics.responseEnvelopes(exchanges))
                .extracting(envelope -> envelope.get(WayangA2uiTransportFields.OUTCOME))
                .containsExactly("SUCCESS", "ACTION_REJECTED", "TRANSPORT_ERROR");
    }

    @Test
    void treatsNullTransportExchangeListsAsEmpty() {
        assertThat(TransportExchangeMetrics.handledCount(null)).isZero();
        assertThat(TransportExchangeMetrics.rejectedCount(null)).isZero();
        assertThat(TransportExchangeMetrics.hasTransportErrors(null)).isFalse();
        assertThat(TransportExchangeMetrics.outcomes(null)).isEmpty();
        assertThat(TransportExchangeMetrics.responseEnvelopes(null)).isEmpty();
    }

    @Test
    void ignoresNullTransportExchangeEntries() {
        List<MetricExchange> exchanges = new ArrayList<>();
        exchanges.add(new MetricExchange(response(1, 0)));
        exchanges.add(null);
        exchanges.add(new MetricExchange(WayangA2uiTransportResponse.error("transport_failed", "Failed")));

        assertThat(TransportExchangeMetrics.handledCount(exchanges)).isEqualTo(1L);
        assertThat(TransportExchangeMetrics.rejectedCount(exchanges)).isEqualTo(1L);
        assertThat(TransportExchangeMetrics.hasTransportErrors(exchanges)).isTrue();
        assertThat(TransportExchangeMetrics.outcomes(exchanges)).containsExactly(
                WayangA2uiTransportOutcome.SUCCESS,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR);
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
            WayangA2uiTransportResponse transportResponse) implements TransportMetricExchange {
    }
}
