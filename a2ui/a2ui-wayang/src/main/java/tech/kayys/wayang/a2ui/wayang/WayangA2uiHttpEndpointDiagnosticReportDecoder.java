package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpReportMetricDecoders;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP endpoint diagnostic reports.
 */
public final class WayangA2uiHttpEndpointDiagnosticReportDecoder {

    private static final String OWNER = "A2UI HTTP endpoint diagnostic report";

    public static WayangA2uiHttpEndpointDiagnosticReport fromMap(Map<?, ?> values) {
        Map<String, Object> report = TransportMaps.copy(values);
        if (report.isEmpty()) {
            return WayangA2uiHttpEndpointDiagnosticReport.empty();
        }
        return new WayangA2uiHttpEndpointDiagnosticReport(
                DecodeValues.rawText(report.get("diagnosticsId")),
                HttpReportMetricDecoders.intCount(
                        report.get("exchangeCount"),
                        "exchangeCount",
                        OWNER),
                HttpReportMetricDecoders.count(report.get("knownPathCount"), "knownPathCount", OWNER),
                HttpReportMetricDecoders.count(report.get("unknownPathCount"), "unknownPathCount", OWNER),
                HttpReportMetricDecoders.count(report.get("matchedCount"), "matchedCount", OWNER),
                HttpReportMetricDecoders.count(report.get("unmatchedCount"), "unmatchedCount", OWNER),
                HttpReportMetricDecoders.count(report.get("successfulCount"), "successfulCount", OWNER),
                HttpReportMetricDecoders.count(report.get("clientErrorCount"), "clientErrorCount", OWNER),
                HttpReportMetricDecoders.count(report.get("serverErrorCount"), "serverErrorCount", OWNER),
                HttpReportMetricDecoders.count(report.get("handledCount"), "handledCount", OWNER),
                HttpReportMetricDecoders.count(report.get("rejectedCount"), "rejectedCount", OWNER),
                DecodeValues.bool(report.get("transportErrors"), false),
                HttpReportMetricDecoders.integerList(
                        report.get("statusCodes"),
                        "statusCodes",
                        OWNER),
                DecodeCollections.rawTexts(report.get("outcomes")),
                DecodeCollections.maps(report.get("exchanges")),
                DecodeCollections.maps(report.get("issues")),
                TransportMaps.copyMap(report.get("attributes")));
    }

    public static WayangA2uiHttpEndpointDiagnosticReport fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP endpoint diagnostic report JSON must not be blank",
                "Unable to decode A2UI HTTP endpoint diagnostic report JSON"));
    }

    private WayangA2uiHttpEndpointDiagnosticReportDecoder() {
    }
}
