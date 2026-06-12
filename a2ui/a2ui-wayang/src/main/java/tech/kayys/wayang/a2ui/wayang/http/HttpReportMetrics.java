package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * Ordered metric fragments shared by A2UI HTTP report projections.
 */
public final class HttpReportMetrics {

    public static void putOutcomeCounts(
            Map<String, Object> values,
            long successfulCount,
            long clientErrorCount,
            long serverErrorCount) {
        values.put("successfulCount", successfulCount);
        values.put("clientErrorCount", clientErrorCount);
        values.put("serverErrorCount", serverErrorCount);
    }

    public static void putTransportCounts(
            Map<String, Object> values,
            long handledCount,
            long rejectedCount) {
        values.put("handledCount", handledCount);
        values.put("rejectedCount", rejectedCount);
    }

    public static void putTransportErrorFlag(Map<String, Object> values, boolean transportErrors) {
        values.put("transportErrors", transportErrors);
    }

    public static void putTransportDigest(
            Map<String, Object> values,
            boolean transportErrors,
            List<Integer> statusCodes,
            List<String> outcomes) {
        putTransportErrorFlag(values, transportErrors);
        values.put("statusCodes", RecordCollections.copyList(statusCodes));
        values.put("outcomes", RecordCollections.copyList(outcomes));
    }

    private HttpReportMetrics() {
    }
}
