package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP endpoint diagnostic summaries.
 */
public final class WayangA2uiHttpEndpointDiagnosticSummaryDecoder {

    public static WayangA2uiHttpEndpointDiagnosticSummary fromMap(Map<?, ?> values) {
        Map<String, Object> summary = TransportMaps.copy(values);
        if (summary.isEmpty()) {
            return WayangA2uiHttpEndpointDiagnosticSummary.empty();
        }
        return new WayangA2uiHttpEndpointDiagnosticSummary(
                DecodeValues.text(summary.get("diagnosticsId")),
                DecodeValues.bool(summary.get(WayangA2uiTransportFields.PASSED), false),
                DecodeValues.clampedNonNegativeInt(
                        summary.get(WayangA2uiTransportFields.EXIT_CODE),
                        WayangA2uiHttpSmokeResult.EXIT_FAILURE),
                DecodeValues.clampedNonNegativeInt(summary.get("exchangeCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get(WayangA2uiTransportFields.ISSUE_COUNT), 0),
                DecodeValues.nonNegativeLong(summary.get("knownPathCount"), 0L),
                DecodeValues.nonNegativeLong(summary.get("unknownPathCount"), 0L),
                DecodeValues.nonNegativeLong(summary.get("matchedCount"), 0L),
                DecodeValues.nonNegativeLong(summary.get("unmatchedCount"), 0L),
                DecodeValues.nonNegativeLong(summary.get("successfulCount"), 0L),
                DecodeValues.nonNegativeLong(summary.get("clientErrorCount"), 0L),
                DecodeValues.nonNegativeLong(summary.get("serverErrorCount"), 0L),
                DecodeValues.bool(summary.get("transportErrors"), false),
                DecodeCollections.integers(summary.get("statusCodes")),
                DecodeCollections.texts(summary.get("outcomes")),
                DecodeCollections.texts(summary.get("issueCategories")),
                DecodeCollections.texts(summary.get("errorCodes")),
                TransportMaps.copyMap(summary.get("attributes")));
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP endpoint diagnostic summary JSON must not be blank",
                "Unable to decode A2UI HTTP endpoint diagnostic summary JSON"));
    }

    private WayangA2uiHttpEndpointDiagnosticSummaryDecoder() {
    }
}
