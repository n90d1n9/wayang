package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP endpoint diagnostic summaries.
 */
public final class WayangA2uiHttpEndpointDiagnosticSummaryDecoder {

    public static WayangA2uiHttpEndpointDiagnosticSummary fromMap(Map<?, ?> values) {
        Map<String, Object> summary = WayangA2uiTransportMaps.copy(values);
        return new WayangA2uiHttpEndpointDiagnosticSummary(
                WayangA2uiDecodeValues.text(summary.get("diagnosticsId")),
                WayangA2uiDecodeValues.bool(summary.get(WayangA2uiTransportFields.PASSED), false),
                WayangA2uiDecodeValues.clampedNonNegativeInt(
                        summary.get(WayangA2uiTransportFields.EXIT_CODE),
                        WayangA2uiHttpSmokeResult.EXIT_FAILURE),
                WayangA2uiDecodeValues.clampedNonNegativeInt(summary.get("exchangeCount"), 0),
                WayangA2uiDecodeValues.clampedNonNegativeInt(summary.get(WayangA2uiTransportFields.ISSUE_COUNT), 0),
                WayangA2uiDecodeValues.nonNegativeLong(summary.get("knownPathCount"), 0L),
                WayangA2uiDecodeValues.nonNegativeLong(summary.get("unknownPathCount"), 0L),
                WayangA2uiDecodeValues.nonNegativeLong(summary.get("matchedCount"), 0L),
                WayangA2uiDecodeValues.nonNegativeLong(summary.get("unmatchedCount"), 0L),
                WayangA2uiDecodeValues.nonNegativeLong(summary.get("successfulCount"), 0L),
                WayangA2uiDecodeValues.nonNegativeLong(summary.get("clientErrorCount"), 0L),
                WayangA2uiDecodeValues.nonNegativeLong(summary.get("serverErrorCount"), 0L),
                WayangA2uiDecodeValues.bool(summary.get("transportErrors"), false),
                WayangA2uiDecodeCollections.integers(summary.get("statusCodes")),
                WayangA2uiDecodeCollections.texts(summary.get("outcomes")),
                WayangA2uiDecodeCollections.texts(summary.get("issueCategories")),
                WayangA2uiDecodeCollections.texts(summary.get("errorCodes")),
                WayangA2uiTransportMaps.copyMap(summary.get("attributes")));
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI HTTP endpoint diagnostic summary JSON must not be blank",
                "Unable to decode A2UI HTTP endpoint diagnostic summary JSON"));
    }

    private WayangA2uiHttpEndpointDiagnosticSummaryDecoder() {
    }
}
