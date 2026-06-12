package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;

/**
 * Shared fixture factory for A2UI contract tests.
 */
public final class WayangA2uiContractFixtures {

    private WayangA2uiContractFixtures() {
    }

    public static WayangA2uiHttpSmokeResult contractSmokeResult() {
        return contractSmokeResult(contractSmokeExpectationResult());
    }

    public static WayangA2uiHttpSmokeResult failedContractSmokeResult() {
        return contractSmokeResult(failedContractSmokeExpectationResult());
    }

    public static WayangA2uiTransportResponse contractSmokeTransportResponse() {
        return WayangA2uiTransportResponse.from(contractSmokeResult());
    }

    public static WayangA2uiTransportResponse failedContractSmokeTransportResponse() {
        return WayangA2uiTransportResponse.from(failedContractSmokeResult());
    }

    public static WayangA2uiHttpResponse contractSmokeHttpResponse() {
        return contractSmokeHttpResponse(contractSmokeTransportResponse());
    }

    public static WayangA2uiHttpResponse failedContractSmokeHttpResponse() {
        return contractSmokeHttpResponse(failedContractSmokeTransportResponse());
    }

    public static WayangA2uiHttpReadinessProbeResult contractReadinessProbeResult() {
        return new WayangA2uiHttpReadinessProbeResult(
                WayangA2uiHttpBindingReportProbeResult.from(contractBindingReportHttpResponse()),
                contractActionBindingProbeResult(),
                WayangA2uiHttpSmokeProbeResult.from(contractSmokeHttpResponse()),
                true);
    }

    public static WayangA2uiHttpResponse contractReadinessHttpResponse() {
        return WayangA2uiHttpResponse.fromBridge(WayangA2uiBridgeResponse.of(
                WayangA2uiTransportResponse.from(contractReadinessProbeResult()))).withRoute(
                        WayangA2uiHttpRoute.readiness());
    }

    public static WayangA2uiHttpBindingReport contractBindingReport() {
        WayangA2uiHttpRouteCatalog catalog = WayangA2uiHttpRouteCatalog.defaultCatalog();
        WayangA2uiHttpOperationDispatcher dispatcher = WayangA2uiHttpOperationDispatcher.from(request -> {
            throw new UnsupportedOperationException("not used");
        }, catalog);
        return dispatcher.bindingReport(catalog);
    }

    public static WayangA2uiHttpResponse contractBindingReportHttpResponse() {
        return WayangA2uiHttpResponse.fromBridge(WayangA2uiBridgeResponse.of(
                WayangA2uiTransportResponse.from(contractBindingReport()))).withRoute(WayangA2uiHttpRoute.bindingReport());
    }

    public static WayangA2uiHttpActionBindingProbeResult contractActionBindingProbeResult() {
        return WayangA2uiHttpActionBindingProbeResult.from(
                WayangA2uiHttpResponse.fromBridge(WayangA2uiBridgeResponse.of(
                        WayangA2uiTransportResponse.from(contractActionBindingReport())))
                        .withRoute(WayangA2uiHttpRoute.exchange()));
    }

    public static WayangA2uiActionBindingReport contractActionBindingReport() {
        return WayangA2uiActionBindingReport.of(
                WayangA2uiActionPolicy.runLifecycle(),
                WayangA2uiActionHandlers.standard(
                        new RecordingWayangGollekSdk(),
                        WayangA2uiSurfaceRegistry.readOnly()));
    }

    public static WayangA2uiHttpBindingReport incompleteContractBindingReport() {
        return new WayangA2uiHttpBindingReport(
                List.of(
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                        WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG,
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT),
                List.of(
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                        WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                        "a2ui.customHandler"),
                List.of(
                        WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG,
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT),
                List.of(
                        WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                        "a2ui.customHandler"));
    }

    public static WayangA2uiHttpResponse incompleteContractBindingReportHttpResponse() {
        return WayangA2uiHttpResponse.fromBridge(WayangA2uiBridgeResponse.of(
                WayangA2uiTransportResponse.from(incompleteContractBindingReport())))
                .withRoute(WayangA2uiHttpRoute.bindingReport());
    }

    private static WayangA2uiHttpSmokeResult contractSmokeResult(
            WayangA2uiHttpExpectationResult expectationResult) {
        return new WayangA2uiHttpSmokeResult(
                contractSmokeSuiteResult(),
                expectationResult,
                Map.of(
                        "suiteId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID,
                        "routeCount", 6));
    }

    private static WayangA2uiHttpScenarioSuiteResult contractSmokeSuiteResult() {
        WayangA2uiHttpScenarioResult scenarioResult = new WayangA2uiHttpScenarioResult(
                WayangA2uiHttpScenarios.DISCOVERY_ID,
                List.of(),
                Map.of("scenarioKind", "discovery"));
        return new WayangA2uiHttpScenarioSuiteResult(
                WayangA2uiHttpScenarios.SMOKE_SUITE_ID,
                List.of(scenarioResult),
                Map.of(
                        "suiteKind", "smoke",
                        "routeCount", 6));
    }

    private static WayangA2uiHttpExpectationResult contractSmokeExpectationResult() {
        return WayangA2uiHttpExpectationResult.of(
                WayangA2uiHttpScenarios.SMOKE_SUITE_ID,
                "a2ui-http-smoke-suite-pass",
                List.of(),
                Map.of("source", "contract"));
    }

    private static WayangA2uiHttpExpectationResult failedContractSmokeExpectationResult() {
        return WayangA2uiHttpExpectationResult.of(
                WayangA2uiHttpScenarios.SMOKE_SUITE_ID,
                "a2ui-http-smoke-suite-pass",
                List.of(WayangA2uiHttpExpectationIssue.of(
                        WayangA2uiHttpScenarios.SMOKE_SUITE_ID,
                        "scenarioCount",
                        3,
                        1,
                        "Expected scenarioCount to match exactly.")),
                Map.of("source", "contract"));
    }

    private static WayangA2uiHttpResponse contractSmokeHttpResponse(WayangA2uiTransportResponse response) {
        return WayangA2uiHttpResponse.fromBridge(WayangA2uiBridgeResponse.of(
                response)).withRoute(WayangA2uiHttpRoute.smoke());
    }
}
