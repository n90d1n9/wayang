package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical decoder for stored or remote A2UI HTTP endpoint diagnostic reports.
 */
public final class WayangA2uiHttpEndpointDiagnosticReportDecoder {

    public static WayangA2uiHttpEndpointDiagnosticReport fromMap(Map<?, ?> values) {
        Map<String, Object> report = WayangA2uiTransportMaps.copy(values);
        return new WayangA2uiHttpEndpointDiagnosticReport(
                WayangA2uiDecodeValues.rawText(report.get("diagnosticsId")),
                intCount(report.get("exchangeCount"), "exchangeCount"),
                count(report.get("knownPathCount"), "knownPathCount"),
                count(report.get("unknownPathCount"), "unknownPathCount"),
                count(report.get("matchedCount"), "matchedCount"),
                count(report.get("unmatchedCount"), "unmatchedCount"),
                count(report.get("successfulCount"), "successfulCount"),
                count(report.get("clientErrorCount"), "clientErrorCount"),
                count(report.get("serverErrorCount"), "serverErrorCount"),
                count(report.get("handledCount"), "handledCount"),
                count(report.get("rejectedCount"), "rejectedCount"),
                WayangA2uiDecodeValues.bool(report.get("transportErrors"), false),
                integers(report.get("statusCodes"), "statusCodes"),
                WayangA2uiDecodeCollections.rawTexts(report.get("outcomes")),
                WayangA2uiDecodeCollections.maps(report.get("exchanges")),
                WayangA2uiDecodeCollections.maps(report.get("issues")),
                WayangA2uiTransportMaps.copyMap(report.get("attributes")));
    }

    public static WayangA2uiHttpEndpointDiagnosticReport fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI HTTP endpoint diagnostic report JSON must not be blank",
                "Unable to decode A2UI HTTP endpoint diagnostic report JSON"));
    }

    private static List<Integer> integers(Object value, String fieldName) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> integer(item, fieldName))
                    .filter(Objects::nonNull)
                    .toList();
        }
        Integer integer = integer(value, fieldName);
        return integer == null ? List.of() : List.of(integer);
    }

    private static Integer integer(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = WayangA2uiDecodeValues.rawText(value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "A2UI HTTP endpoint diagnostic report integer list must be numeric: " + fieldName,
                    e);
        }
    }

    private static int intCount(Object value, String fieldName) {
        long count = count(value, fieldName);
        try {
            return Math.toIntExact(count);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "A2UI HTTP endpoint diagnostic report count must fit int: " + fieldName,
                    e);
        }
    }

    private static long count(Object value, String fieldName) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = WayangA2uiDecodeValues.rawText(value).trim();
        if (text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "A2UI HTTP endpoint diagnostic report count must be numeric: " + fieldName,
                    e);
        }
    }

    private WayangA2uiHttpEndpointDiagnosticReportDecoder() {
    }
}
