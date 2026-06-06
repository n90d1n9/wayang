package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter-friendly facade for mounted A2UI endpoint diagnostics.
 */
public final class WayangA2uiHttpEndpointDiagnosticRunner {

    private final WayangA2uiHttpEndpointDiagnostics diagnostics;

    public WayangA2uiHttpEndpointDiagnosticRunner(WayangA2uiHttpEndpointBinding endpoint) {
        this(WayangA2uiHttpEndpointDiagnostics.of(endpoint));
    }

    public WayangA2uiHttpEndpointDiagnosticRunner(
            WayangA2uiHttpEndpointBinding endpoint,
            WayangA2uiHttpEndpointDiagnosticConfig config) {
        this(WayangA2uiHttpEndpointDiagnostics.of(endpoint, config));
    }

    public WayangA2uiHttpEndpointDiagnosticRunner(WayangA2uiHttpEndpointDiagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public static WayangA2uiHttpEndpointDiagnosticRunner of(WayangA2uiHttpEndpointBinding endpoint) {
        return new WayangA2uiHttpEndpointDiagnosticRunner(endpoint);
    }

    public static WayangA2uiHttpEndpointDiagnosticRunner of(
            WayangA2uiHttpEndpointBinding endpoint,
            WayangA2uiHttpEndpointDiagnosticConfig config) {
        return new WayangA2uiHttpEndpointDiagnosticRunner(endpoint, config);
    }

    public static WayangA2uiHttpEndpointDiagnosticRunner from(WayangA2uiHttpEndpointDiagnostics diagnostics) {
        return new WayangA2uiHttpEndpointDiagnosticRunner(diagnostics);
    }

    public WayangA2uiHttpEndpointDiagnosticConfig config() {
        return diagnostics.config();
    }

    public WayangA2uiHttpEndpointDiagnosticRun runDefault() {
        return wrap(diagnostics.runDefault());
    }

    public WayangA2uiHttpEndpointDiagnosticRun run(WayangA2uiHttpEndpointDiagnosticPlan plan) {
        return wrap(diagnostics.run(plan));
    }

    public WayangA2uiHttpEndpointDiagnosticRun runPlanMap(Map<?, ?> plan) {
        return wrap(diagnostics.runPlanMap(plan));
    }

    public WayangA2uiHttpEndpointDiagnosticRun runPlanJson(String planJson) {
        return wrap(diagnostics.runPlanJson(planJson));
    }

    public WayangA2uiHttpEndpointDiagnosticRun runRequests(
            String diagnosticsId,
            List<WayangA2uiHttpEndpointDiagnosticRequest> requests,
            Map<?, ?> attributes) {
        return wrap(diagnostics.run(diagnosticsId, requests, attributes));
    }

    public WayangA2uiHttpEndpointDiagnosticRun runRequestMaps(
            String diagnosticsId,
            List<? extends Map<?, ?>> requests,
            Map<?, ?> attributes) {
        return wrap(diagnostics.runFromMaps(diagnosticsId, requests, attributes));
    }

    private static WayangA2uiHttpEndpointDiagnosticRun wrap(WayangA2uiHttpEndpointDiagnosticResult result) {
        return new WayangA2uiHttpEndpointDiagnosticRun(result);
    }
}
