package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered metadata projections for Wayang A2UI transport envelopes.
 */
final class WayangA2uiTransportMetadataProjection {

    private WayangA2uiTransportMetadataProjection() {
    }

    static Map<String, Object> request(WayangA2uiTransportPayloadKind kind) {
        WayangA2uiTransportPayloadKind resolved = Objects.requireNonNull(kind, "kind");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiTransportFields.REQUEST_KIND, resolved.name());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> sessionResult(WayangA2uiSessionResult result) {
        WayangA2uiSessionResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                WayangA2uiTransportFields.RESPONSE_KIND,
                WayangA2uiTransportFields.RESPONSE_KIND_SESSION_RESULT);
        values.put(WayangA2uiTransportFields.ACTION_COUNT, resolved.actionResults().size());
        values.put(WayangA2uiTransportFields.MESSAGE_COUNT, resolved.responseMessages().size());
        values.put(WayangA2uiTransportFields.DATA_PART_COUNT, resolved.responseDataParts().size());
        values.put(WayangA2uiTransportFields.HANDLED_COUNT, resolved.handledCount());
        values.put(WayangA2uiTransportFields.REJECTED_COUNT, resolved.rejectedCount());
        values.put(
                WayangA2uiTransportFields.EMPTY,
                resolved.responseJsonl().isBlank() && resolved.responseDataParts().isEmpty());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> surfaceCatalog(WayangA2uiSurfaceCatalog catalog) {
        WayangA2uiSurfaceCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                WayangA2uiTransportFields.RESPONSE_KIND,
                WayangA2uiTransportFields.RESPONSE_KIND_SURFACE_CATALOG);
        values.put(WayangA2uiTransportFields.SURFACE_KIND_COUNT, resolved.surfaceKinds().size());
        values.put(WayangA2uiTransportFields.DESCRIPTOR_COUNT, resolved.descriptorCount());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> httpRouteCatalog(WayangA2uiHttpRouteCatalog catalog) {
        WayangA2uiHttpRouteCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                WayangA2uiTransportFields.RESPONSE_KIND,
                WayangA2uiTransportFields.RESPONSE_KIND_HTTP_ROUTE_CATALOG);
        values.put(WayangA2uiTransportFields.ROUTE_COUNT, resolved.routeCount());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> httpBindingReport(WayangA2uiHttpBindingReport report) {
        WayangA2uiHttpBindingReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                WayangA2uiTransportFields.RESPONSE_KIND,
                WayangA2uiTransportFields.RESPONSE_KIND_HTTP_BINDING_REPORT);
        values.put(WayangA2uiTransportFields.COMPLETE, resolved.complete());
        values.put(WayangA2uiTransportFields.ROUTE_OPERATION_COUNT, resolved.routeOperationCount());
        values.put(WayangA2uiTransportFields.HANDLER_OPERATION_COUNT, resolved.handlerOperationCount());
        values.put(WayangA2uiTransportFields.MISSING_HANDLER_COUNT, resolved.missingHandlerOperations().size());
        values.put(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT, resolved.orphanHandlerOperations().size());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> httpSmokeResult(WayangA2uiHttpSmokeResult result) {
        WayangA2uiHttpSmokeResult resolved = Objects.requireNonNull(result, "result");
        WayangA2uiHttpScenarioSuiteReport report = resolved.suiteResult().report();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                WayangA2uiTransportFields.RESPONSE_KIND,
                WayangA2uiTransportFields.RESPONSE_KIND_HTTP_SMOKE_RESULT);
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put(WayangA2uiTransportFields.SUITE_ID, report.suiteId());
        values.put(WayangA2uiTransportFields.SCENARIO_COUNT, report.scenarioCount());
        values.put(
                WayangA2uiTransportFields.ISSUE_COUNT,
                report.issueCount() + resolved.expectationResult().issueCount());
        Object routeCount = resolved.attributes().get(WayangA2uiTransportFields.ROUTE_COUNT);
        if (routeCount != null) {
            values.put(WayangA2uiTransportFields.ROUTE_COUNT, routeCount);
        }
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> httpReadinessProbe(WayangA2uiHttpReadinessProbeResult result) {
        WayangA2uiHttpReadinessProbeResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                WayangA2uiTransportFields.RESPONSE_KIND,
                WayangA2uiTransportFields.RESPONSE_KIND_HTTP_READINESS_PROBE);
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> error(WayangA2uiTransportError error) {
        WayangA2uiTransportError resolved = Objects.requireNonNull(error, "error");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                WayangA2uiTransportFields.RESPONSE_KIND,
                WayangA2uiTransportFields.RESPONSE_KIND_TRANSPORT_ERROR);
        values.put(WayangA2uiTransportFields.ERROR_CODE, resolved.code());
        values.put(WayangA2uiTransportFields.ERROR, resolved.toMap());
        values.put(WayangA2uiTransportFields.HANDLED_COUNT, 0L);
        values.put(WayangA2uiTransportFields.REJECTED_COUNT, 1L);
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> merge(Map<String, ?> metadata, Map<String, ?> extraMetadata) {
        Map<String, Object> merged = new LinkedHashMap<>(WayangA2uiTransportMaps.copy(metadata));
        merged.putAll(WayangA2uiTransportMaps.copy(extraMetadata));
        return WayangA2uiTransportMaps.freeze(merged);
    }
}
