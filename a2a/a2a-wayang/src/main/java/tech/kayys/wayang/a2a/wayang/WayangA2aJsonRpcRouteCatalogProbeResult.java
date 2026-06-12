package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.lenientBodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.allowHeader;
import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.protocolVersionHeader;
import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.routeOperationHeader;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcProbeResponseChecks.countMissingIssue;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcProbeResponseChecks.responseIssues;

/**
 * HTTP-aware result for probing the A2A JSON-RPC route catalog surface.
 */
public record WayangA2aJsonRpcRouteCatalogProbeResult(
        int statusCode,
        boolean httpSuccessful,
        String routeOperation,
        String protocolVersion,
        String contentType,
        String allow,
        int routeCount,
        int enabledRouteCount,
        boolean endpointDescriptor,
        boolean smokeDescriptor,
        boolean routeCatalogDescriptor,
        boolean diagnosticsReportDescriptor,
        boolean specComplianceReportDescriptor,
        boolean bindingReportDescriptor,
        boolean readinessDescriptor,
        boolean readinessIssueSummaryDescriptor,
        int issueCount,
        List<Map<String, Object>> issues,
        Map<String, Object> body,
        Map<String, Object> headers) {

    public WayangA2aJsonRpcRouteCatalogProbeResult {
        statusCode = Math.max(0, statusCode);
        routeOperation = routeOperation == null ? "" : routeOperation.trim();
        protocolVersion = protocolVersion == null ? "" : protocolVersion.trim();
        contentType = contentType == null ? "" : contentType.trim();
        allow = allow == null ? "" : allow.trim();
        routeCount = Math.max(0, routeCount);
        enabledRouteCount = Math.max(0, enabledRouteCount);
        issues = copyObjects(issues);
        issueCount = Math.max(Math.max(0, issueCount), issues.size());
        body = WayangA2aMaps.copyMap(body);
        headers = WayangA2aMaps.copyMap(headers);
    }

    public static WayangA2aJsonRpcRouteCatalogProbeResult run(WayangA2aJsonRpcHttpAdapter adapter) {
        return from(Objects.requireNonNull(adapter, "adapter").routeCatalogResponse());
    }

    public static WayangA2aJsonRpcRouteCatalogProbeResult from(WayangA2aHttpResponse response) {
        WayangA2aHttpResponse resolved = Objects.requireNonNull(response, "response");
        Map<String, Object> body = lenientBodyMap(resolved.body());
        List<Map<String, Object>> routes = WayangA2aMaps.objectList(body.get("routes"));
        int routeCount = number(body.get("routeCount"), routes.size());
        int enabledRouteCount = number(body.get("enabledRouteCount"), enabledRouteCount(routes));
        WayangA2aJsonRpcRouteCatalogDescriptorCoverage descriptorCoverage =
                WayangA2aJsonRpcRouteCatalogDescriptorCoverage.fromRoutes(routes);
        List<Map<String, Object>> issues = issues(resolved, routeCount, descriptorCoverage);
        return new WayangA2aJsonRpcRouteCatalogProbeResult(
                resolved.statusCode(),
                resolved.successful(),
                routeOperationHeader(resolved),
                protocolVersionHeader(resolved),
                resolved.contentType(),
                allowHeader(resolved),
                routeCount,
                enabledRouteCount,
                descriptorCoverage.endpointDescriptor(),
                descriptorCoverage.smokeDescriptor(),
                descriptorCoverage.routeCatalogDescriptor(),
                descriptorCoverage.diagnosticsReportDescriptor(),
                descriptorCoverage.specComplianceReportDescriptor(),
                descriptorCoverage.bindingReportDescriptor(),
                descriptorCoverage.readinessDescriptor(),
                descriptorCoverage.readinessIssueSummaryDescriptor(),
                issues.size(),
                issues,
                body,
                resolved.headers());
    }

    public static WayangA2aJsonRpcRouteCatalogProbeResult fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcRouteCatalogProbeProjection.fromMap(values);
    }

    public static WayangA2aJsonRpcRouteCatalogProbeResult fromJson(String json) {
        return fromMap(lenientBodyMap(json));
    }

    public boolean routeCatalogRoute() {
        return WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG.equals(routeOperation);
    }

    public boolean jsonContent() {
        return WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON.equals(contentType);
    }

    public boolean complete() {
        return routeCount > 0
                && endpointDescriptor
                && smokeDescriptor
                && routeCatalogDescriptor
                && diagnosticsReportDescriptor
                && specComplianceReportDescriptor
                && bindingReportDescriptor
                && readinessDescriptor
                && readinessIssueSummaryDescriptor;
    }

    public boolean passed() {
        return httpSuccessful && routeCatalogRoute() && jsonContent() && complete();
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcRouteCatalogProbeProjection.probe(this);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    private static List<Map<String, Object>> issues(
            WayangA2aHttpResponse response,
            int routeCount,
            WayangA2aJsonRpcRouteCatalogDescriptorCoverage descriptorCoverage) {
        List<Map<String, Object>> values = new ArrayList<>(responseIssues(
                response,
                "routeCatalog",
                WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG,
                "route_catalog_route_mismatch",
                "route catalog response"));
        if (routeCount <= 0) {
            values.add(countMissingIssue(
                    "routeCatalog",
                    "route_count_missing",
                    "routeCount",
                    routeCount,
                    "route catalog",
                    "routes"));
        }
        values.addAll(descriptorCoverage.missingIssues());
        return List.copyOf(values);
    }

    private static int enabledRouteCount(List<Map<String, Object>> routes) {
        return (int) routes.stream()
                .filter(route -> bool(route.get("enabled"), false))
                .count();
    }

}
