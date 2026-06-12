package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcDiagnosticsReportAttributes(
        Map<String, Object> values) {

    WayangA2aJsonRpcDiagnosticsReportAttributes {
        values = WayangA2aMaps.copyMap(values);
    }

    static WayangA2aJsonRpcDiagnosticsReportAttributes from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcHttpConfig config,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        WayangA2aJsonRpcReadinessProbeResult resolvedReadiness =
                Objects.requireNonNull(readiness, "readiness");
        WayangA2aSpecAlignmentSnapshot resolvedSpecAlignment = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("config", config == null ? Map.of() : config.toMap());
        attributes.put("protocol", "A2A");
        attributes.put("binding", "JSONRPC");
        attributes.put("specAlignment", resolvedSpecAlignment.toMap());
        WayangA2aJsonRpcReadinessMethodDispatchSnapshot methodDispatch =
                WayangA2aJsonRpcReadinessMethodDispatchSnapshot.from(resolvedReadiness.bindingReportProbe());
        if (methodDispatch.reported()) {
            attributes.put("methodDispatch", methodDispatch.toMap());
        }
        if (resolvedReadiness.methodRegistryReported()) {
            attributes.put("methodRegistry", resolvedReadiness.methodRegistrySnapshot().toMap());
        }
        return new WayangA2aJsonRpcDiagnosticsReportAttributes(attributes);
    }
}
