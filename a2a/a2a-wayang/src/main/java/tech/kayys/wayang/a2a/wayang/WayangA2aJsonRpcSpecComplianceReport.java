package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcDiagnosticIssues.issue;

/**
 * JSON-ready A2A v1.0 JSON-RPC method-mapping and publication compliance view.
 */
public record WayangA2aJsonRpcSpecComplianceReport(
        String complianceId,
        String specUrl,
        String protocolVersion,
        String binding,
        boolean passed,
        int operationCount,
        int supportedOperationCount,
        int streamingOperationCount,
        boolean endpointPublished,
        String endpointPath,
        int issueCount,
        List<Map<String, Object>> issues,
        List<Map<String, Object>> operations,
        Map<String, Object> publication,
        Map<String, Object> attributes) {

    public static final String COMPLIANCE_ID = "a2a.jsonrpc.spec-compliance";
    public static final String OPERATION_JSON_RPC_SPEC_COMPLIANCE = "JsonRpcSpecCompliance";
    public static final String SPEC_URL = WayangA2aSpecAlignmentReport.SPEC_URL;

    public WayangA2aJsonRpcSpecComplianceReport {
        complianceId = complianceId == null || complianceId.isBlank() ? COMPLIANCE_ID : complianceId.trim();
        specUrl = specUrl == null || specUrl.isBlank() ? SPEC_URL : specUrl.trim();
        protocolVersion = protocolVersion == null || protocolVersion.isBlank()
                ? A2aProtocol.VERSION
                : protocolVersion.trim();
        binding = binding == null || binding.isBlank() ? A2aProtocol.BINDING_JSONRPC : binding.trim();
        operationCount = Math.max(0, operationCount);
        supportedOperationCount = Math.max(0, supportedOperationCount);
        streamingOperationCount = Math.max(0, streamingOperationCount);
        endpointPath = endpointPath == null ? "" : endpointPath.trim();
        issues = copyObjects(issues);
        issueCount = Math.max(Math.max(0, issueCount), issues.size());
        operations = copyObjects(operations);
        publication = WayangA2aMaps.copyMap(publication);
        attributes = WayangA2aMaps.copyMap(attributes);
    }

    public static WayangA2aJsonRpcSpecComplianceReport from(WayangA2aJsonRpcHttpAdapter adapter) {
        WayangA2aJsonRpcHttpAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return WayangA2aJsonRpcSpecHealth.from(resolved).specComplianceReport();
    }

    public static WayangA2aJsonRpcSpecComplianceReport from(WayangA2aJsonRpcHttpPublication publication) {
        return from(publication, WayangA2aSpecAlignmentSnapshot.defaults());
    }

    public static WayangA2aJsonRpcSpecComplianceReport from(
            WayangA2aJsonRpcHttpPublication publication,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        WayangA2aJsonRpcHttpPublication resolved = Objects.requireNonNull(publication, "publication");
        WayangA2aSpecAlignmentSnapshot resolvedSpecAlignment = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
        List<WayangA2aJsonRpcSpecOperation> specOperations = WayangA2aJsonRpcSpecOperation.standardOperations();
        List<Map<String, Object>> operations = specOperations.stream()
                .map(WayangA2aJsonRpcSpecOperation::toMap)
                .toList();
        int supportedOperationCount = (int) specOperations.stream()
                .filter(WayangA2aJsonRpcSpecOperation::supported)
                .count();
        int streamingOperationCount = (int) specOperations.stream()
                .filter(WayangA2aJsonRpcSpecOperation::streaming)
                .count();
        boolean endpointPublished =
                resolved.bindingForOperation(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC).isPresent();
        String endpointPath = resolved.adapter().endpointPath();
        List<Map<String, Object>> issues = issues(
                specOperations,
                endpointPublished,
                endpointPath,
                resolvedSpecAlignment);
        return new WayangA2aJsonRpcSpecComplianceReport(
                COMPLIANCE_ID,
                SPEC_URL,
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                issues.isEmpty(),
                specOperations.size(),
                supportedOperationCount,
                streamingOperationCount,
                endpointPublished,
                endpointPath,
                issues.size(),
                issues,
                operations,
                resolved.toMap(),
                WayangA2aJsonRpcSpecComplianceReportProjection.attributes(resolvedSpecAlignment));
    }

    public static WayangA2aJsonRpcSpecComplianceReport fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcSpecComplianceReportProjection.fromMap(values);
    }

    public static WayangA2aJsonRpcSpecComplianceReport fromJson(String json) {
        return fromMap(bodyMap(json));
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcSpecComplianceReportProjection.report(this);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    public WayangA2aHttpResponse response() {
        return WayangA2aJsonRpcSpecComplianceReportProjection.response(this);
    }

    private static List<Map<String, Object>> issues(
            List<WayangA2aJsonRpcSpecOperation> operations,
            boolean endpointPublished,
            String endpointPath,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        List<Map<String, Object>> values = new ArrayList<>();
        if (!endpointPublished) {
            values.add(endpointNotPublishedIssue(endpointPath));
        }
        if (specAlignment != null && !specAlignment.aligned()) {
            values.add(WayangA2aSpecAlignmentDiagnosticIssues.gapIssue(specAlignment));
        }
        operations.stream()
                .filter(operation -> !operation.supported())
                .map(WayangA2aJsonRpcSpecComplianceReport::methodMappingMissingIssue)
                .forEach(values::add);
        return List.copyOf(values);
    }

    static Map<String, Object> endpointNotPublishedIssue(String endpointPath) {
        return issue(
                "publication",
                "jsonrpc_endpoint_not_published",
                "endpointPublished",
                "true",
                String.valueOf(false),
                "A2A JSON-RPC endpoint " + (endpointPath == null ? "" : endpointPath) + " is not published.");
    }

    static Map<String, Object> methodMappingMissingIssue(WayangA2aJsonRpcSpecOperation operation) {
        WayangA2aJsonRpcSpecOperation resolved = Objects.requireNonNull(operation, "operation");
        return issue(
                "spec",
                "jsonrpc_method_mapping_missing",
                "jsonRpcMethod",
                resolved.jsonRpcMethod(),
                "missing",
                "A2A JSON-RPC method " + resolved.jsonRpcMethod()
                        + " is missing from the local method mapping.");
    }
}
