package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMetadataProjection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransportMetadataProjectionTest {

    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    void projectsOrderedRequestMetadataAndPublicFacadeDelegates() {
        Map<String, Object> values = TransportMetadataProjection.request(
                WayangA2uiTransportPayloadKind.JSON_LINE);

        assertThat(WayangA2uiTransportMetadata.request(WayangA2uiTransportPayloadKind.JSON_LINE))
                .isEqualTo(values);
        assertThat(values.keySet()).containsExactly("requestKind");
        assertThat(values).containsEntry("requestKind", WayangA2uiTransportPayloadKind.JSON_LINE.name());
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void projectsOrderedSessionAndCatalogMetadata() {
        WayangA2uiActionResult actionResult = WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_INSPECT,
                "run-1",
                "Run inspected.",
                List.of(),
                Map.of());
        WayangA2uiSessionResult sessionResult = WayangA2uiSessionResult.of(List.of(actionResult), codec);
        WayangA2uiSurfaceCatalog surfaceCatalog = WayangA2uiSurfaceRegistry.readOnly().surfaceCatalog();
        WayangA2uiActionBindingReport actionBindingReport = new WayangA2uiActionBindingReport(
                List.of(WayangA2uiActions.RUN_INSPECT),
                List.of(WayangA2uiActions.RUN_INSPECT),
                List.of(),
                List.of());
        WayangA2uiHttpRouteCatalog routeCatalog = WayangA2uiHttpRouteCatalog.defaultCatalog();

        Map<String, Object> sessionValues = TransportMetadataProjection.sessionResult(sessionResult);
        Map<String, Object> surfaceValues = TransportMetadataProjection.surfaceCatalog(surfaceCatalog);
        Map<String, Object> actionBindingValues =
                TransportMetadataProjection.actionBindingReport(actionBindingReport);
        Map<String, Object> routeValues = TransportMetadataProjection.httpRouteCatalog(routeCatalog);

        assertThat(WayangA2uiTransportMetadata.sessionResult(sessionResult)).isEqualTo(sessionValues);
        assertThat(sessionValues.keySet()).containsExactly(
                "responseKind",
                "actionCount",
                "messageCount",
                "dataPartCount",
                "handledCount",
                "rejectedCount",
                "empty");
        assertThat(sessionValues)
                .containsEntry("responseKind", WayangA2uiTransportFields.RESPONSE_KIND_SESSION_RESULT)
                .containsEntry("actionCount", 1)
                .containsEntry("handledCount", 1L)
                .containsEntry("rejectedCount", 0L)
                .containsEntry("empty", false);
        assertThat((Integer) sessionValues.get("messageCount")).isPositive();
        assertThat((Integer) sessionValues.get("dataPartCount")).isPositive();

        assertThat(WayangA2uiTransportMetadata.surfaceCatalog(surfaceCatalog)).isEqualTo(surfaceValues);
        assertThat(surfaceValues.keySet()).containsExactly("responseKind", "surfaceKindCount", "descriptorCount");
        assertThat(surfaceValues)
                .containsEntry("responseKind", WayangA2uiTransportFields.RESPONSE_KIND_SURFACE_CATALOG)
                .containsEntry("surfaceKindCount", surfaceCatalog.surfaceKinds().size())
                .containsEntry("descriptorCount", surfaceCatalog.descriptorCount());

        assertThat(WayangA2uiTransportMetadata.actionBindingReport(actionBindingReport)).isEqualTo(actionBindingValues);
        assertThat(actionBindingValues.keySet()).containsExactly(
                "responseKind",
                "complete",
                "policyActionCount",
                "handlerActionCount",
                "missingHandlerCount",
                "orphanHandlerCount");
        assertThat(actionBindingValues)
                .containsEntry("responseKind", WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT)
                .containsEntry("complete", true)
                .containsEntry("policyActionCount", 1)
                .containsEntry("handlerActionCount", 1);

        assertThat(WayangA2uiTransportMetadata.httpRouteCatalog(routeCatalog)).isEqualTo(routeValues);
        assertThat(routeValues.keySet()).containsExactly("responseKind", "routeCount");
        assertThat(routeValues)
                .containsEntry("responseKind", WayangA2uiTransportFields.RESPONSE_KIND_HTTP_ROUTE_CATALOG)
                .containsEntry("routeCount", routeCatalog.routeCount());
    }

    @Test
    void projectsOrderedHttpDiagnosticMetadata() {
        WayangA2uiHttpBindingReport bindingReport = WayangA2uiContractFixtures.contractBindingReport();
        WayangA2uiHttpSmokeResult smokeResult = WayangA2uiContractFixtures.contractSmokeResult();
        WayangA2uiHttpReadinessProbeResult readiness = WayangA2uiContractFixtures.contractReadinessProbeResult();

        Map<String, Object> bindingValues = TransportMetadataProjection.httpBindingReport(bindingReport);
        Map<String, Object> smokeValues = TransportMetadataProjection.httpSmokeResult(smokeResult);
        Map<String, Object> readinessValues = TransportMetadataProjection.httpReadinessProbe(readiness);

        assertThat(WayangA2uiTransportMetadata.httpBindingReport(bindingReport)).isEqualTo(bindingValues);
        assertThat(bindingValues.keySet()).containsExactly(
                "responseKind",
                "complete",
                "routeOperationCount",
                "handlerOperationCount",
                "missingHandlerCount",
                "orphanHandlerCount");
        assertThat(bindingValues)
                .containsEntry("responseKind", WayangA2uiTransportFields.RESPONSE_KIND_HTTP_BINDING_REPORT)
                .containsEntry("complete", true)
                .containsEntry("routeOperationCount", bindingReport.routeOperationCount())
                .containsEntry("handlerOperationCount", bindingReport.handlerOperationCount());

        assertThat(WayangA2uiTransportMetadata.httpSmokeResult(smokeResult)).isEqualTo(smokeValues);
        assertThat(smokeValues.keySet()).containsExactly(
                "responseKind",
                "passed",
                "exitCode",
                "suiteId",
                "scenarioCount",
                "issueCount",
                "routeCount");
        assertThat(smokeValues)
                .containsEntry("responseKind", WayangA2uiTransportFields.RESPONSE_KIND_HTTP_SMOKE_RESULT)
                .containsEntry("passed", true)
                .containsEntry("exitCode", 0)
                .containsEntry("suiteId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry("routeCount", 6);

        assertThat(WayangA2uiTransportMetadata.httpReadinessProbe(readiness)).isEqualTo(readinessValues);
        assertThat(readinessValues.keySet()).containsExactly(
                "responseKind",
                "passed",
                "exitCode",
                "issueCount",
                "bindingReportPassed",
                "actionBindingPassed",
                "smokeRequired",
                "smokePassed");
        assertThat(readinessValues)
                .containsEntry("responseKind", WayangA2uiTransportFields.RESPONSE_KIND_HTTP_READINESS_PROBE)
                .containsEntry("passed", true)
                .containsEntry("bindingReportPassed", true)
                .containsEntry("actionBindingPassed", true)
                .containsEntry("smokeRequired", true)
                .containsEntry("smokePassed", true);
    }

    @Test
    void projectsOrderedErrorMetadataAndMergeDefensivelyCopies() {
        WayangA2uiTransportError error = WayangA2uiTransportError.of("bad_request", "Bad request.");
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("nested", nested);
        extra.put("ignored", null);

        Map<String, Object> errorValues = TransportMetadataProjection.error(error);
        Map<String, Object> merged = TransportMetadataProjection.merge(Map.of("base", "value"), extra);
        nested.put("value", "changed");

        assertThat(WayangA2uiTransportMetadata.error(error)).isEqualTo(errorValues);
        assertThat(errorValues.keySet()).containsExactly(
                "responseKind",
                "errorCode",
                "error",
                "handledCount",
                "rejectedCount");
        assertThat(errorValues)
                .containsEntry("responseKind", WayangA2uiTransportFields.RESPONSE_KIND_TRANSPORT_ERROR)
                .containsEntry("errorCode", "bad_request")
                .containsEntry("handledCount", 0L)
                .containsEntry("rejectedCount", 1L);
        assertThat((Map<String, Object>) errorValues.get("error"))
                .containsEntry("code", "bad_request")
                .containsEntry("message", "Bad request.");

        assertThat(WayangA2uiTransportMetadata.merge(Map.of("base", "value"), extra))
                .containsEntry("base", "value")
                .doesNotContainKey("ignored");
        assertThat(merged)
                .containsEntry("base", "value")
                .doesNotContainKey("ignored");
        assertThat((Map<String, Object>) merged.get("nested"))
                .containsEntry("value", "original");
    }
}
