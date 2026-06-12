package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP operational diagnostics summaries.
 */
public final class WayangA2uiHttpOperationalDiagnosticsSummaryDecoder {

    public static WayangA2uiHttpOperationalDiagnosticsSummary fromMap(Map<?, ?> values) {
        Map<String, Object> summary = TransportMaps.copy(values);
        if (summary.isEmpty()) {
            return WayangA2uiHttpOperationalDiagnosticsSummary.empty();
        }
        return new WayangA2uiHttpOperationalDiagnosticsSummary(
                DecodeValues.text(summary.get("diagnosticsId")),
                DecodeValues.bool(summary.get(WayangA2uiTransportFields.PASSED), false),
                DecodeValues.clampedNonNegativeInt(
                        summary.get(WayangA2uiTransportFields.EXIT_CODE),
                        WayangA2uiHttpSmokeResult.EXIT_FAILURE),
                DecodeValues.clampedNonNegativeInt(
                        summary.get(WayangA2uiTransportFields.ISSUE_COUNT),
                        0),
                DecodeCollections.texts(summary.get("issueCodes")),
                DecodeValues.bool(summary.get("bindingReportPassed"), false),
                DecodeValues.bool(summary.get("actionBindingPassed"), false),
                DecodeValues.bool(summary.get("smokeRequired"), false),
                DecodeValues.bool(summary.get("smokePassed"), false),
                DecodeValues.clampedNonNegativeInt(summary.get("routeOperationCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("routeHandlerOperationCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("missingRouteHandlerCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("orphanRouteHandlerCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("policyActionCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("actionHandlerCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("missingActionHandlerCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("orphanActionHandlerCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("smokeScenarioCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("smokeIssueCount"), 0),
                DecodeValues.clampedNonNegativeInt(summary.get("smokeRouteCount"), 0),
                TransportMaps.copyMap(summary.get("attributes")));
    }

    public static WayangA2uiHttpOperationalDiagnosticsSummary fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP operational diagnostics summary JSON must not be blank",
                "Unable to decode A2UI HTTP operational diagnostics summary JSON"));
    }

    private WayangA2uiHttpOperationalDiagnosticsSummaryDecoder() {
    }
}
