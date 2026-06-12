package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;

/**
 * Small facade for A2A JSON-RPC spec-health views shared by diagnostics and compliance endpoints.
 */
public final class WayangA2aJsonRpcSpecHealth {

    private final WayangA2aJsonRpcHttpAdapter adapter;

    private WayangA2aJsonRpcSpecHealth(WayangA2aJsonRpcHttpAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public static WayangA2aJsonRpcSpecHealth from(WayangA2aJsonRpcHttpAdapter adapter) {
        return new WayangA2aJsonRpcSpecHealth(adapter);
    }

    public WayangA2aSpecAlignmentReport specAlignmentReport() {
        return WayangA2aJsonRpcMethods.specAlignmentReport();
    }

    public WayangA2aSpecAlignmentSnapshot specAlignmentSnapshot() {
        return WayangA2aSpecAlignmentSnapshot.from(specAlignmentReport());
    }

    public WayangA2aJsonRpcDiagnosticsReport diagnosticsReport() {
        return diagnosticsReport(specAlignmentSnapshot());
    }

    public WayangA2aJsonRpcDiagnosticsReport diagnosticsReport(WayangA2aSpecAlignmentSnapshot specAlignment) {
        return WayangA2aJsonRpcDiagnosticsReport.from(
                adapter.readinessProbe(),
                adapter.config(),
                resolve(specAlignment));
    }

    public WayangA2aJsonRpcSpecComplianceReport specComplianceReport() {
        return specComplianceReport(specAlignmentSnapshot());
    }

    public WayangA2aJsonRpcSpecComplianceReport specComplianceReport(WayangA2aSpecAlignmentSnapshot specAlignment) {
        return WayangA2aJsonRpcSpecComplianceReport.from(
                adapter.routePublication(),
                resolve(specAlignment));
    }

    private static WayangA2aSpecAlignmentSnapshot resolve(WayangA2aSpecAlignmentSnapshot specAlignment) {
        return specAlignment == null ? WayangA2aSpecAlignmentSnapshot.defaults() : specAlignment;
    }
}
