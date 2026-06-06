package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical decoder for stored, remote, and transport-backed A2UI HTTP smoke summaries.
 */
public final class WayangA2uiHttpSmokeSummaryDecoder {

    public static WayangA2uiHttpSmokeSummary from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        return from(WayangA2uiTransportResponse.fromJson(resolved.body()));
    }

    public static WayangA2uiHttpSmokeSummary from(WayangA2uiTransportResponse response) {
        WayangA2uiTransportResponse resolved = Objects.requireNonNull(response, "response");
        return fromMaps(resolved.metadata(), bodyMap(resolved.body()));
    }

    public static WayangA2uiHttpSmokeSummary fromResultJson(String resultJson) {
        return fromMaps(Map.of(), bodyMap(resultJson));
    }

    public static WayangA2uiHttpSmokeSummary fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2uiTransportMaps.copy(values);
        return new WayangA2uiHttpSmokeSummary(
                WayangA2uiDecodeValues.bool(copy.get(WayangA2uiTransportFields.PASSED), false),
                WayangA2uiDecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.EXIT_CODE),
                        WayangA2uiHttpSmokeResult.EXIT_FAILURE),
                WayangA2uiDecodeValues.text(copy.get(WayangA2uiTransportFields.SUITE_ID), ""),
                WayangA2uiDecodeValues.nonNegativeInt(copy.get(WayangA2uiTransportFields.SCENARIO_COUNT), 0),
                WayangA2uiDecodeValues.nonNegativeInt(copy.get(WayangA2uiTransportFields.ISSUE_COUNT), 0),
                WayangA2uiDecodeValues.nonNegativeInt(copy.get(WayangA2uiTransportFields.ROUTE_COUNT), 0),
                WayangA2uiDecodeValues.bool(copy.get("smokeResult"), false),
                WayangA2uiTransportMaps.copyMapList(copy.get("issues")),
                WayangA2uiTransportMaps.copyMap(copy.get(WayangA2uiTransportFields.METADATA)),
                WayangA2uiTransportMaps.copyMap(copy.get(WayangA2uiTransportFields.BODY)));
    }

    public static WayangA2uiHttpSmokeSummary fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI HTTP smoke summary JSON must not be blank",
                "Unable to decode A2UI HTTP smoke summary JSON"));
    }

    private static WayangA2uiHttpSmokeSummary fromMaps(Map<?, ?> metadataValues, Map<?, ?> bodyValues) {
        Map<String, Object> metadata = WayangA2uiTransportMaps.copy(metadataValues);
        Map<String, Object> body = WayangA2uiTransportMaps.copy(bodyValues);
        Map<String, Object> suiteReport = child(body, "suiteReport");
        Map<String, Object> expectationResult = child(body, "expectationResult");
        Map<String, Object> attributes = child(body, "attributes");
        int suiteIssues = WayangA2uiDecodeValues.nonNegativeInt(
                suiteReport.get(WayangA2uiTransportFields.ISSUE_COUNT),
                0);
        int expectationIssues = WayangA2uiDecodeValues.nonNegativeInt(
                expectationResult.get(WayangA2uiTransportFields.ISSUE_COUNT),
                0);
        List<Map<String, Object>> issues = WayangA2uiHttpSmokeProbeProjection.issues(
                suiteReport,
                expectationResult);
        int bodyIssueCount = Math.max(suiteIssues + expectationIssues, issues.size());
        boolean passed = WayangA2uiDecodeValues.bool(metadata.get(WayangA2uiTransportFields.PASSED),
                WayangA2uiDecodeValues.bool(body.get(WayangA2uiTransportFields.PASSED), false));
        return new WayangA2uiHttpSmokeSummary(
                passed,
                WayangA2uiDecodeValues.nonNegativeInt(
                        metadata.get(WayangA2uiTransportFields.EXIT_CODE),
                        WayangA2uiDecodeValues.nonNegativeInt(body.get(WayangA2uiTransportFields.EXIT_CODE),
                                passed
                                        ? WayangA2uiHttpSmokeResult.EXIT_SUCCESS
                                        : WayangA2uiHttpSmokeResult.EXIT_FAILURE)),
                WayangA2uiDecodeValues.text(
                        metadata.get(WayangA2uiTransportFields.SUITE_ID),
                        WayangA2uiDecodeValues.text(suiteReport.get(WayangA2uiTransportFields.SUITE_ID),
                                WayangA2uiDecodeValues.text(
                                        attributes.get(WayangA2uiTransportFields.SUITE_ID),
                                        ""))),
                WayangA2uiDecodeValues.nonNegativeInt(
                        metadata.get(WayangA2uiTransportFields.SCENARIO_COUNT),
                        WayangA2uiDecodeValues.nonNegativeInt(
                                suiteReport.get(WayangA2uiTransportFields.SCENARIO_COUNT),
                                0)),
                Math.max(WayangA2uiDecodeValues.nonNegativeInt(
                        metadata.get(WayangA2uiTransportFields.ISSUE_COUNT),
                        0), bodyIssueCount),
                WayangA2uiDecodeValues.nonNegativeInt(
                        metadata.get(WayangA2uiTransportFields.ROUTE_COUNT),
                        WayangA2uiDecodeValues.nonNegativeInt(
                                attributes.get(WayangA2uiTransportFields.ROUTE_COUNT),
                                0)),
                WayangA2uiTransportFields.RESPONSE_KIND_HTTP_SMOKE_RESULT.equals(
                        WayangA2uiDecodeValues.text(metadata.get(WayangA2uiTransportFields.RESPONSE_KIND), "")),
                issues,
                metadata,
                body);
    }

    private static Map<String, Object> bodyMap(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        return WayangA2uiTransportJson.map(
                body,
                "A2UI HTTP smoke result JSON must not be blank",
                "Unable to decode A2UI HTTP smoke result JSON");
    }

    private static Map<String, Object> child(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof Map<?, ?> map) {
            return WayangA2uiTransportMaps.copy(map);
        }
        return Map.of();
    }

    private WayangA2uiHttpSmokeSummaryDecoder() {
    }
}
