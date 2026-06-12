package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;
import tech.kayys.wayang.a2ui.wayang.transport.TransportExchangeMetrics;

import java.util.List;
import java.util.Map;

/**
 * Shared aggregate metrics for A2UI HTTP exchanges.
 */
public final class HttpExchangeMetrics {

    public static int exchangeCount(List<? extends HttpMetricExchange> exchanges) {
        return exchanges(exchanges).size();
    }

    public static long successfulCount(List<? extends HttpMetricExchange> exchanges) {
        return exchanges(exchanges).stream()
                .filter(HttpMetricExchange::successful)
                .count();
    }

    public static long clientErrorCount(List<? extends HttpMetricExchange> exchanges) {
        return exchanges(exchanges).stream()
                .mapToInt(HttpMetricExchange::statusCode)
                .filter(statusCode -> statusCode >= 400 && statusCode < 500)
                .count();
    }

    public static long serverErrorCount(List<? extends HttpMetricExchange> exchanges) {
        return exchanges(exchanges).stream()
                .mapToInt(HttpMetricExchange::statusCode)
                .filter(statusCode -> statusCode >= 500)
                .count();
    }

    public static long handledCount(List<? extends HttpMetricExchange> exchanges) {
        return TransportExchangeMetrics.handledCount(exchanges);
    }

    public static long rejectedCount(List<? extends HttpMetricExchange> exchanges) {
        return TransportExchangeMetrics.rejectedCount(exchanges);
    }

    public static boolean hasTransportErrors(List<? extends HttpMetricExchange> exchanges) {
        return TransportExchangeMetrics.hasTransportErrors(exchanges);
    }

    public static List<Integer> statusCodes(List<? extends HttpMetricExchange> exchanges) {
        return exchanges(exchanges).stream()
                .map(HttpMetricExchange::statusCode)
                .toList();
    }

    public static List<WayangA2uiTransportOutcome> outcomes(List<? extends HttpMetricExchange> exchanges) {
        return TransportExchangeMetrics.outcomes(exchanges);
    }

    public static List<Map<String, Object>> responseEnvelopes(List<? extends HttpMetricExchange> exchanges) {
        return TransportExchangeMetrics.responseEnvelopes(exchanges);
    }

    private static List<? extends HttpMetricExchange> exchanges(
            List<? extends HttpMetricExchange> exchanges) {
        return RecordCollections.nonNullList(exchanges);
    }

    private HttpExchangeMetrics() {
    }
}
