package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpSmokeProbeProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

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
        Map<String, Object> copy = TransportMaps.copy(values);
        return new WayangA2uiHttpSmokeSummary(
                DecodeValues.bool(copy.get(WayangA2uiTransportFields.PASSED), false),
                DecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.EXIT_CODE),
                        WayangA2uiHttpSmokeResult.EXIT_FAILURE),
                DecodeValues.text(copy.get(WayangA2uiTransportFields.SUITE_ID), ""),
                DecodeValues.nonNegativeInt(copy.get(WayangA2uiTransportFields.SCENARIO_COUNT), 0),
                DecodeValues.nonNegativeInt(copy.get(WayangA2uiTransportFields.ISSUE_COUNT), 0),
                DecodeValues.nonNegativeInt(copy.get(WayangA2uiTransportFields.ROUTE_COUNT), 0),
                DecodeValues.bool(copy.get("smokeResult"), false),
                TransportMaps.copyMapList(copy.get("issues")),
                TransportMaps.copyMap(copy.get(WayangA2uiTransportFields.METADATA)),
                TransportMaps.copyMap(copy.get(WayangA2uiTransportFields.BODY)));
    }

    public static WayangA2uiHttpSmokeSummary fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP smoke summary JSON must not be blank",
                "Unable to decode A2UI HTTP smoke summary JSON"));
    }

    private static WayangA2uiHttpSmokeSummary fromMaps(Map<?, ?> metadataValues, Map<?, ?> bodyValues) {
        Map<String, Object> metadata = TransportMaps.copy(metadataValues);
        Map<String, Object> body = TransportMaps.copy(bodyValues);
        Map<String, Object> suiteReport = child(body, "suiteReport");
        Map<String, Object> expectationResult = child(body, "expectationResult");
        Map<String, Object> attributes = child(body, "attributes");
        int suiteIssues = DecodeValues.nonNegativeInt(
                suiteReport.get(WayangA2uiTransportFields.ISSUE_COUNT),
                0);
        int expectationIssues = DecodeValues.nonNegativeInt(
                expectationResult.get(WayangA2uiTransportFields.ISSUE_COUNT),
                0);
        List<Map<String, Object>> issues = HttpSmokeProbeProjection.issues(
                suiteReport,
                expectationResult);
        int bodyIssueCount = Math.max(suiteIssues + expectationIssues, issues.size());
        boolean passed = DecodeValues.bool(metadata.get(WayangA2uiTransportFields.PASSED),
                DecodeValues.bool(body.get(WayangA2uiTransportFields.PASSED), false));
        return new WayangA2uiHttpSmokeSummary(
                passed,
                DecodeValues.nonNegativeInt(
                        metadata.get(WayangA2uiTransportFields.EXIT_CODE),
                        DecodeValues.nonNegativeInt(body.get(WayangA2uiTransportFields.EXIT_CODE),
                                passed
                                        ? WayangA2uiHttpSmokeResult.EXIT_SUCCESS
                                        : WayangA2uiHttpSmokeResult.EXIT_FAILURE)),
                DecodeValues.text(
                        metadata.get(WayangA2uiTransportFields.SUITE_ID),
                        DecodeValues.text(suiteReport.get(WayangA2uiTransportFields.SUITE_ID),
                                DecodeValues.text(
                                        attributes.get(WayangA2uiTransportFields.SUITE_ID),
                                        ""))),
                DecodeValues.nonNegativeInt(
                        metadata.get(WayangA2uiTransportFields.SCENARIO_COUNT),
                        DecodeValues.nonNegativeInt(
                                suiteReport.get(WayangA2uiTransportFields.SCENARIO_COUNT),
                                0)),
                Math.max(DecodeValues.nonNegativeInt(
                        metadata.get(WayangA2uiTransportFields.ISSUE_COUNT),
                        0), bodyIssueCount),
                DecodeValues.nonNegativeInt(
                        metadata.get(WayangA2uiTransportFields.ROUTE_COUNT),
                        DecodeValues.nonNegativeInt(
                                attributes.get(WayangA2uiTransportFields.ROUTE_COUNT),
                                0)),
                WayangA2uiTransportFields.RESPONSE_KIND_HTTP_SMOKE_RESULT.equals(
                        DecodeValues.text(metadata.get(WayangA2uiTransportFields.RESPONSE_KIND), "")),
                issues,
                metadata,
                body);
    }

    private static Map<String, Object> bodyMap(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        return TransportJson.map(
                body,
                "A2UI HTTP smoke result JSON must not be blank",
                "Unable to decode A2UI HTTP smoke result JSON");
    }

    private static Map<String, Object> child(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof Map<?, ?> map) {
            return TransportMaps.copy(map);
        }
        return Map.of();
    }

    private WayangA2uiHttpSmokeSummaryDecoder() {
    }
}
