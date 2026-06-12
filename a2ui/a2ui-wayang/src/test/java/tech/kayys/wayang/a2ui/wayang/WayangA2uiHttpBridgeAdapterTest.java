package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpBridgeAdapterTest {

    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    void postsExchangeEnvelopesAsHttpResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));

        WayangA2uiHttpResponse response = adapter.exchange(request.toJson());
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.successful()).isTrue();
        assertThat(response.contentType()).isEqualTo(WayangA2uiTransportContent.MIME_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, WayangA2uiTransportContent.MIME_JSON)
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_MIME_TYPE, WayangA2uiTransportContent.MIME_A2UI)
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_BODY_ENCODING,
                        WayangA2uiTransportContent.ENCODING_JSONL)
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME,
                        WayangA2uiTransportOutcome.SUCCESS.name())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(transportResponse.handledCount()).isEqualTo(1L);
        assertThat(transportResponse.rejectedCount()).isZero();
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.REQUEST_KIND, WayangA2uiTransportPayloadKind.JSON_LINE.name());
        assertThat(transportResponse.body()).contains(WayangA2uiActions.RUN_INSPECT);
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void servesSurfaceCatalogAsHttpResponseEnvelope() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = new WayangA2uiHttpBridgeAdapter(WayangA2uiBridge.from(
                new WayangA2uiTransportAdapter(
                        sdk,
                        WayangA2uiSessionConfig.readOnly())));

        WayangA2uiHttpResponse response = adapter.surfaceCatalog();
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_MIME_TYPE, WayangA2uiTransportContent.MIME_JSON)
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_BODY_ENCODING,
                        WayangA2uiTransportContent.ENCODING_JSON)
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME,
                        WayangA2uiTransportOutcome.SUCCESS.name())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(transportResponse.bodyEncoding()).isEqualTo(WayangA2uiTransportContent.ENCODING_JSON);
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.REQUEST_KIND,
                        WayangA2uiTransportPayloadKind.SURFACE_CATALOG.name())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_SURFACE_CATALOG);
        assertThat(transportResponse.body()).contains(WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void postsActionBindingReportEnvelopeAsHttpExchangeResponse() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(
                sdk,
                WayangA2uiSessionConfig.runLifecycle()));

        WayangA2uiHttpResponse response = adapter.exchange(WayangA2uiTransportRequest.actionBindingReport().toJson());
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.fromJson(response.body());
        WayangA2uiActionBindingReport report = WayangA2uiActionBindingReport.from(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.REQUEST_KIND,
                        WayangA2uiTransportPayloadKind.ACTION_BINDING_REPORT.name())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT)
                .containsEntry(WayangA2uiTransportFields.COMPLETE, true)
                .containsEntry(WayangA2uiTransportFields.POLICY_ACTION_COUNT, 5)
                .containsEntry(WayangA2uiTransportFields.HANDLER_ACTION_COUNT, 5);
        assertThat(report.complete()).isTrue();
        assertThat(report.policyActions()).containsExactlyElementsOf(WayangA2uiActions.runLifecycleActionOrder());
        assertThat(transportResponse.body())
                .contains("\"complete\":true")
                .contains(WayangA2uiActions.RUN_INSPECT)
                .contains(WayangA2uiActions.RUN_CANCEL);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void servesRouteCatalogAsHttpResponseEnvelope() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpResponse response = adapter.routeCatalogResponse();
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_MIME_TYPE, WayangA2uiTransportContent.MIME_JSON)
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_BODY_ENCODING,
                        WayangA2uiTransportContent.ENCODING_JSON)
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME,
                        WayangA2uiTransportOutcome.SUCCESS.name())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(transportResponse.bodyEncoding()).isEqualTo(WayangA2uiTransportContent.ENCODING_JSON);
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_ROUTE_CATALOG)
                .containsEntry(WayangA2uiTransportFields.ROUTE_COUNT, 6);
        assertThat(transportResponse.body())
                .contains(WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .contains(WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG)
                .contains(WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG)
                .contains(WayangA2uiHttpRoute.OPERATION_BINDING_REPORT)
                .contains(WayangA2uiHttpRoute.OPERATION_SMOKE)
                .contains(WayangA2uiHttpRoute.OPERATION_READINESS)
                .contains(WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void servesBindingReportAsHttpResponseEnvelope() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpResponse response = adapter.bindingReportResponse();
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_BINDING_REPORT)
                .containsEntry(WayangA2uiTransportFields.COMPLETE, true)
                .containsEntry(WayangA2uiTransportFields.ROUTE_OPERATION_COUNT, 6)
                .containsEntry(WayangA2uiTransportFields.HANDLER_OPERATION_COUNT, 6)
                .containsEntry(WayangA2uiTransportFields.MISSING_HANDLER_COUNT, 0)
                .containsEntry(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT, 0);
        assertThat(transportResponse.body())
                .contains("\"complete\":true")
                .contains(WayangA2uiHttpRoute.OPERATION_BINDING_REPORT);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void probesBindingReportResponseForOperationalDecision() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpBindingReportProbeResult probe = WayangA2uiHttpBindingReportProbeResult.run(adapter);

        assertThat(probe.statusCode()).isEqualTo(200);
        assertThat(probe.httpSuccessful()).isTrue();
        assertThat(probe.bindingReportRoute()).isTrue();
        assertThat(probe.bindingReportResult()).isTrue();
        assertThat(probe.jsonContent()).isTrue();
        assertThat(probe.complete()).isTrue();
        assertThat(probe.passed()).isTrue();
        assertThat(probe.routeOperationCount()).isEqualTo(6);
        assertThat(probe.handlerOperationCount()).isEqualTo(6);
        assertThat(probe.missingHandlerCount()).isZero();
        assertThat(probe.orphanHandlerCount()).isZero();
        assertThat(probe.issueCount()).isZero();
        assertThat(probe.issues()).isEmpty();
        assertThat(probe.routeOperations()).contains(WayangA2uiHttpRoute.OPERATION_BINDING_REPORT);
        assertThat(probe.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(probe.toMap())
                .containsEntry("passed", true)
                .containsEntry("complete", true)
                .containsEntry("routeOperationCount", 6);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void servesSmokeResultAsHttpResponseEnvelope() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpResponse response = adapter.smoke();
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.fromJson(response.body());
        WayangA2uiHttpSmokeSummary summary = WayangA2uiHttpSmokeSummary.from(response);
        WayangA2uiHttpSmokeProbeResult probe = WayangA2uiHttpSmokeProbeResult.from(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_SMOKE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_SMOKE_RESULT)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 0)
                .containsEntry(WayangA2uiTransportFields.SUITE_ID, WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry(WayangA2uiTransportFields.SCENARIO_COUNT, 3)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 0)
                .containsEntry(WayangA2uiTransportFields.ROUTE_COUNT, 6);
        assertThat(transportResponse.body())
                .contains("\"exitCode\":0")
                .contains(WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .contains(WayangA2uiHttpRoute.OPERATION_SMOKE);
        assertThat(summary.smokeResult()).isTrue();
        assertThat(summary.successfulExit()).isTrue();
        assertThat(summary.passed()).isTrue();
        assertThat(summary.exitCode()).isZero();
        assertThat(summary.suiteId()).isEqualTo(WayangA2uiHttpScenarios.SMOKE_SUITE_ID);
        assertThat(summary.scenarioCount()).isEqualTo(3);
        assertThat(summary.issueCount()).isZero();
        assertThat(summary.issues()).isEmpty();
        assertThat(summary.routeCount()).isEqualTo(6);
        assertThat(summary.toMap())
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry("successfulExit", true)
                .containsEntry(WayangA2uiTransportFields.ROUTE_COUNT, 6);
        assertThat(summary.toJson()).contains("\"smokeResult\":true");
        assertThat(probe.httpSuccessful()).isTrue();
        assertThat(probe.smokeRoute()).isTrue();
        assertThat(probe.passed()).isTrue();
        assertThat(probe.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_SUCCESS);
        assertThat(probe.routeOperation()).isEqualTo(WayangA2uiHttpRoute.OPERATION_SMOKE);
        assertThat(probe.allow()).isEqualTo("GET, OPTIONS");
        assertThat(probe.outcome()).isEqualTo(WayangA2uiTransportOutcome.SUCCESS.name());
        assertThat(probe.summary()).isEqualTo(summary);
        assertThat(probe.toMap())
                .containsEntry("statusCode", 200)
                .containsEntry("smokeRoute", true)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 0);
        assertThat(probe.toJson()).contains("\"httpSuccessful\":true");
        assertThat(WayangA2uiHttpSmokeProbeResult.run(adapter).passed()).isTrue();
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void summarizesSmokeValidationFailuresFromTransportResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(adapter);
        WayangA2uiHttpScenarioSuiteResult suiteResult = harness.run(WayangA2uiHttpScenarios.smokeSuite(
                adapter.routeCatalog()));
        WayangA2uiHttpExpectationResult validation = suiteResult.validate(
                WayangA2uiHttpScenarioSuiteExpectation.pass().withExpectedScenarioCount(99));
        WayangA2uiHttpSmokeResult failure = new WayangA2uiHttpSmokeResult(
                suiteResult,
                validation,
                Map.of(
                        "suiteId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID,
                        "routeCount", 6));

        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.from(failure);
        WayangA2uiHttpResponse response = WayangA2uiHttpResponse.fromBridge(WayangA2uiBridgeResponse.of(
                transportResponse)).withRoute(WayangA2uiHttpRoute.smoke());
        WayangA2uiHttpSmokeSummary summary = WayangA2uiHttpSmokeSummary.from(transportResponse);
        WayangA2uiHttpSmokeProbeResult probe = WayangA2uiHttpSmokeProbeResult.from(response);
        WayangA2uiHttpSmokeSummary bodySummary = WayangA2uiHttpSmokeSummary.fromResultJson(failure.toJson());

        assertThat(validation.issueCount()).isEqualTo(1);
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.PASSED, false)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 1)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 1L);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(summary.smokeResult()).isTrue();
        assertThat(summary.successfulExit()).isFalse();
        assertThat(summary.passed()).isFalse();
        assertThat(summary.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(summary.issueCount()).isEqualTo(1);
        assertThat(summary.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "expectation")
                        .containsEntry("targetId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                        .containsEntry("field", "scenarioCount")
                        .containsEntry("actual", "3"));
        assertThat(summary.routeCount()).isEqualTo(6);
        assertThat(probe.httpSuccessful()).isTrue();
        assertThat(probe.smokeRoute()).isTrue();
        assertThat(probe.passed()).isFalse();
        assertThat(probe.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(probe.outcome()).isEqualTo(WayangA2uiTransportOutcome.ACTION_REJECTED.name());
        assertThat(probe.toMap())
                .containsEntry("httpSuccessful", true)
                .containsEntry("smokeRoute", true)
                .containsEntry(WayangA2uiTransportFields.PASSED, false)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 1);
        assertThat(summary.body()).containsKey("expectationResult");
        assertThat(bodySummary.smokeResult()).isFalse();
        assertThat(bodySummary.issueCount()).isEqualTo(1);
        assertThat(bodySummary.issues()).hasSameSizeAs(summary.issues());
        assertThat(bodySummary.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void probesReadinessThroughHttpAdapter() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpReadinessProbeResult readiness = adapter.readinessProbe();

        assertThat(readiness.bindingReportPassed()).isTrue();
        assertThat(readiness.actionBindingPassed()).isTrue();
        assertThat(readiness.smokeRequired()).isTrue();
        assertThat(readiness.smokePassed()).isTrue();
        assertThat(readiness.passed()).isTrue();
        assertThat(readiness.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_SUCCESS);
        assertThat(readiness.issueCount()).isZero();
        assertThat(readiness.issues()).isEmpty();
        assertThat(readiness.bindingReportProbe().routeOperationCount()).isEqualTo(6);
        assertThat(readiness.actionBindingProbe().missingHandlerCount()).isZero();
        assertThat(readiness.actionBindingProbe().orphanHandlerCount()).isGreaterThan(0);
        assertThat(readiness.smokeProbe().summary().routeCount()).isEqualTo(6);
        assertThat(readiness.toMap())
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry("bindingReportPassed", true)
                .containsEntry("actionBindingPassed", true)
                .containsEntry("smokeRequired", true)
                .containsEntry("smokePassed", true);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void exposesSharedReadinessContractView() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpReadinessProbeResult readiness = adapter.readinessProbe();
        Map<String, Object> standard = readiness.standardReadiness().toMap();

        assertThat(standard)
                .containsEntry("readinessId", WayangA2uiHttpReadinessProbeResult.READINESS_ID)
                .containsEntry("ready", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0)
                .containsEntry("issues", List.of());
        assertThat(TransportMaps.copyMapList(standard.get("probes")))
                .hasSize(3)
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "bindingReport")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry(
                                "attributes",
                                Map.of(
                                        "statusCode",
                                        200,
                                        "routeOperation",
                                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT)))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "actionBinding")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry(
                                "attributes",
                                Map.of(
                                        "statusCode",
                                        200,
                                        "routeOperation",
                                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "smoke")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry(
                                "attributes",
                                Map.of(
                                        "statusCode",
                                        200,
                                        "routeOperation",
                                        WayangA2uiHttpRoute.OPERATION_SMOKE)));
        assertThat(TransportMaps.copyMap(standard.get("attributes")))
                .containsEntry("bindingReportPassed", true)
                .containsEntry("actionBindingPassed", true)
                .containsEntry("smokeRequired", true)
                .containsEntry("smokePassed", true);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void standardReadinessIssuesUseSharedFieldsWithoutChangingNativeIssues() {
        WayangA2uiHttpBindingReportProbeResult bindingProbe =
                WayangA2uiHttpBindingReportProbeResult.fromMap(Map.of(
                        "statusCode",
                        500,
                        "routeOperation",
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT,
                        WayangA2uiTransportFields.ISSUE_COUNT,
                        1,
                        "issues",
                        List.of(Map.of("message", "binding failed"))));
        WayangA2uiHttpReadinessProbeResult readiness =
                new WayangA2uiHttpReadinessProbeResult(bindingProbe, null, false);

        Map<String, Object> nativeIssue = readiness.issues().get(0);
        Map<String, Object> standardIssue = TransportMaps
                .copyMapList(readiness.standardReadiness().toMap().get("issues"))
                .get(0);

        assertThat(nativeIssue)
                .containsEntry("code", "binding_report_probe_failed")
                .containsEntry("statusCode", 500)
                .doesNotContainKey("source");
        assertThat(standardIssue)
                .containsEntry("code", "binding_report_probe_failed")
                .containsEntry("source", "http")
                .containsEntry("statusCode", 500)
                .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_BINDING_REPORT);
    }

    @Test
    void servesReadinessResultAsHttpResponseEnvelope() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpResponse response = adapter.readinessResponse();
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.fromJson(response.body());
        WayangA2uiHttpReadinessProbeResult decoded = WayangA2uiHttpReadinessProbeResult.from(response);
        Map<String, Object> body = TransportJson.map(
                transportResponse.body(),
                "A2UI HTTP readiness body must not be blank",
                "Unable to decode A2UI HTTP readiness body");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_READINESS)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_READINESS_PROBE)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry(WayangA2uiTransportFields.EXIT_CODE, 0)
                .containsEntry(WayangA2uiTransportFields.ISSUE_COUNT, 0)
                .containsEntry("bindingReportPassed", true)
                .containsEntry("actionBindingPassed", true)
                .containsEntry("smokeRequired", true)
                .containsEntry("smokePassed", true);
        assertThat(body)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry("bindingReportPassed", true)
                .containsEntry("actionBindingPassed", true)
                .containsEntry("smokePassed", true);
        assertThat(decoded.passed()).isTrue();
        assertThat(decoded.actionBindingProbe().policyActions())
                .contains(WayangA2uiActions.RUN_INSPECT);
        assertThat(decoded.bindingReportProbe().routeOperations())
                .contains(WayangA2uiHttpRoute.OPERATION_READINESS);
        assertThat(decoded.smokeProbe().summary().routeCount()).isEqualTo(6);
        assertThat(adapter.handle(new WayangA2uiHttpRequest(
                "OPTIONS",
                WayangA2uiHttpBridgeAdapter.PATH_READINESS,
                "",
                Map.of(),
                Map.of())).headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS")
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_READINESS);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void servesOptionsRouteDescriptionsWithoutBridgeDispatch() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpResponse response = adapter.handle(new WayangA2uiHttpRequest(
                "OPTIONS",
                WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                "",
                Map.of(),
                Map.of()));
        WayangA2uiTransportResponse transportResponse = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(transportResponse.metadata())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_ROUTE_CATALOG)
                .containsEntry(WayangA2uiTransportFields.ROUTE_COUNT, 1);
        assertThat(transportResponse.body())
                .contains("\"routeCount\":1")
                .contains(WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .contains(WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void acceptsExchangeContentTypeCaseInsensitivelyAndWithParameters() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));

        WayangA2uiHttpResponse response = adapter.handle(new WayangA2uiHttpRequest(
                "POST",
                WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                request.toJson(),
                Map.of("content-type", "APPLICATION/JSON; charset=UTF-8"),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(WayangA2uiTransportResponse.fromJson(response.body()).transportError()).isEmpty();
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void acceptsResponseContentTypeWithWildcardsAndQualityEntries() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpResponse response = adapter.handle(new WayangA2uiHttpRequest(
                "GET",
                WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG,
                "",
                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT, "text/plain;q=0, application/*; charset=UTF-8"),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(WayangA2uiTransportResponse.fromJson(response.body()).transportError()).isEmpty();
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void mapsInvalidHttpInputsToProblemResponseEnvelopes() {
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(
                new RecordingWayangGollekSdk()));

        WayangA2uiHttpResponse invalidJson = adapter.exchange("{not-json");
        WayangA2uiHttpResponse methodNotAllowed = adapter.handle(WayangA2uiHttpRequest.get(
                WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE));
        WayangA2uiHttpResponse notFound = adapter.handle(WayangA2uiHttpRequest.get("/future"));

        assertThat(invalidJson.statusCode()).isEqualTo(400);
        assertThat(invalidJson.successful()).isFalse();
        assertThat(invalidJson.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME,
                        WayangA2uiTransportOutcome.TRANSPORT_ERROR.name());
        assertThat(WayangA2uiTransportResponse.fromJson(invalidJson.body()).transportError())
                .contains(WayangA2uiTransportError.of(
                        "invalid_request_json",
                        "Unable to decode A2UI transport request JSON"));
        assertThat(methodNotAllowed.statusCode()).isEqualTo(405);
        assertThat(WayangA2uiTransportResponse.fromJson(methodNotAllowed.body()).transportError())
                .contains(WayangA2uiTransportError.of(
                        "method_not_allowed",
                        "A2UI HTTP route /a2ui/exchange only supports POST."));
        assertThat(methodNotAllowed.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(notFound.statusCode()).isEqualTo(404);
        assertThat(WayangA2uiTransportResponse.fromJson(notFound.body()).transportError())
                .contains(WayangA2uiTransportError.of(
                        "not_found",
                        "Unknown A2UI HTTP route: GET /future"));
    }

    @Test
    void rejectsUnsupportedExchangeContentTypesBeforeBridgeDispatch() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));

        WayangA2uiHttpResponse response = adapter.handle(new WayangA2uiHttpRequest(
                "POST",
                WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                request.toJson(),
                Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, "text/plain"),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(response.successful()).isFalse();
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME,
                        WayangA2uiTransportOutcome.TRANSPORT_ERROR.name())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(WayangA2uiTransportResponse.fromJson(response.body()).transportError())
                .contains(WayangA2uiTransportError.of(
                        "unsupported_media_type",
                        "A2UI HTTP route /a2ui/exchange requires Content-Type application/json, "
                                + "received text/plain."));
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void rejectsUnacceptableResponseContentTypesBeforeBridgeDispatch() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));

        WayangA2uiHttpResponse response = adapter.handle(new WayangA2uiHttpRequest(
                "POST",
                WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                request.toJson(),
                Map.of(
                        WayangA2uiHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2uiTransportContent.MIME_JSON,
                        WayangA2uiHttpResponse.HEADER_ACCEPT,
                        "text/plain"),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(406);
        assertThat(response.successful()).isFalse();
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME,
                        WayangA2uiTransportOutcome.TRANSPORT_ERROR.name())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(WayangA2uiTransportResponse.fromJson(response.body()).transportError())
                .contains(WayangA2uiTransportError.of(
                        "not_acceptable",
                        "A2UI HTTP route /a2ui/exchange produces application/json, but Accept was text/plain."));
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void normalizesHttpRequestShapeAndAttributes() {
        WayangA2uiHttpRequest request = new WayangA2uiHttpRequest(
                        " post ",
                        "a2ui/exchange",
                        null,
                        Map.of("content-type", WayangA2uiTransportContent.MIME_JSON),
                        Map.of("tenant", "demo"))
                .withAttributes(Map.of("traceId", "trace-1"));

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo(WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE);
        assertThat(request.body()).isEmpty();
        assertThat(request.headers()).containsEntry("content-type", WayangA2uiTransportContent.MIME_JSON);
        assertThat(request.header("Content-Type")).contains(WayangA2uiTransportContent.MIME_JSON);
        assertThat(request.contentType()).isEqualTo(WayangA2uiTransportContent.MIME_JSON);
        assertThat(request.contentType("application/json; ignored=true")).isTrue();
        assertThat(request.accepts(WayangA2uiTransportContent.MIME_JSON)).isTrue();
        WayangA2uiHttpRequest acceptRequest = new WayangA2uiHttpRequest(
                "GET",
                "a2ui/route-catalog",
                "",
                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT, "text/plain;q=0, */*;q=1"),
                Map.of());
        assertThat(acceptRequest.accept()).isEqualTo("text/plain;q=0, */*;q=1");
        assertThat(acceptRequest.accepts(WayangA2uiTransportContent.MIME_JSON)).isTrue();
        assertThat(request.attributes())
                .containsEntry("tenant", "demo")
                .containsEntry("traceId", "trace-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exposesRouteCatalogForFrameworkBindings() {
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(
                new RecordingWayangGollekSdk()));
        WayangA2uiHttpRouteCatalog catalog = adapter.routeCatalog();
        WayangA2uiHttpBindingReport bindingReport = adapter.bindingReport();

        assertThat(catalog).isEqualTo(WayangA2uiHttpBridgeAdapter.defaultRouteCatalog());
        assertThat(bindingReport.complete()).isTrue();
        assertThat(bindingReport.routeOperationCount()).isEqualTo(6);
        assertThat(bindingReport.handlerOperationCount()).isEqualTo(6);
        assertThat(catalog.routeCount()).isEqualTo(6);
        assertThat(catalog.routes()).containsExactly(
                WayangA2uiHttpRoute.exchange(),
                WayangA2uiHttpRoute.surfaceCatalog(),
                WayangA2uiHttpRoute.routeCatalog(),
                WayangA2uiHttpRoute.bindingReport(),
                WayangA2uiHttpRoute.smoke(),
                WayangA2uiHttpRoute.readiness());
        assertThat(WayangA2uiHttpRoute.exchange().allowedMethods()).containsExactly("POST", "OPTIONS");
        assertThat(WayangA2uiHttpRoute.exchange().allowHeader()).isEqualTo("POST, OPTIONS");
        assertThat(catalog.route(" post ", "a2ui/exchange"))
                .contains(WayangA2uiHttpRoute.exchange());
        assertThat(catalog.routeForPath("a2ui/surface-catalog"))
                .contains(WayangA2uiHttpRoute.surfaceCatalog());
        assertThat(catalog.route("GET", "a2ui/route-catalog"))
                .contains(WayangA2uiHttpRoute.routeCatalog());
        assertThat(catalog.route("GET", WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE)).isEmpty();
        assertThat(catalog.routeForOperation(WayangA2uiHttpRoute.OPERATION_EXCHANGE))
                .contains(WayangA2uiHttpRoute.exchange());
        assertThat(catalog.routeForOperation(" " + WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG + " "))
                .contains(WayangA2uiHttpRoute.routeCatalog());
        assertThat(catalog.routeForOperation(WayangA2uiHttpRoute.OPERATION_BINDING_REPORT))
                .contains(WayangA2uiHttpRoute.bindingReport());
        assertThat(catalog.routeForOperation(WayangA2uiHttpRoute.OPERATION_SMOKE))
                .contains(WayangA2uiHttpRoute.smoke());
        assertThat(catalog.routeForOperation(WayangA2uiHttpRoute.OPERATION_READINESS))
                .contains(WayangA2uiHttpRoute.readiness());

        Map<String, Object> manifest = catalog.toMap();
        assertThat(manifest).containsEntry("routeCount", 6);
        assertThat((Iterable<Map<String, Object>>) manifest.get("routes"))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                        .containsEntry("method", "POST")
                        .containsEntry("allowHeader", "POST, OPTIONS")
                        .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE)
                        .containsEntry("requestContentType", WayangA2uiTransportContent.MIME_JSON)
                        .containsEntry("responseContentType", WayangA2uiTransportContent.MIME_JSON)
                        .containsEntry("requestBodyRequired", true)
                        .satisfies(values -> assertThat((Iterable<String>) values.get("allowedMethods"))
                                .containsExactly("POST", "OPTIONS")))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG)
                        .containsEntry("method", "GET")
                        .containsEntry("allowHeader", "GET, OPTIONS")
                        .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_SURFACE_CATALOG)
                        .containsEntry("requestContentType", "")
                        .containsEntry("responseContentType", WayangA2uiTransportContent.MIME_JSON)
                        .containsEntry("requestBodyRequired", false)
                        .satisfies(values -> assertThat((Iterable<String>) values.get("allowedMethods"))
                                .containsExactly("GET", "OPTIONS")))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG)
                        .containsEntry("method", "GET")
                        .containsEntry("allowHeader", "GET, OPTIONS")
                        .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG)
                        .containsEntry("requestContentType", "")
                        .containsEntry("responseContentType", WayangA2uiTransportContent.MIME_JSON)
                        .containsEntry("requestBodyRequired", false)
                        .satisfies(values -> assertThat((Iterable<String>) values.get("allowedMethods"))
                                .containsExactly("GET", "OPTIONS")))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_BINDING_REPORT)
                        .containsEntry("method", "GET")
                        .containsEntry("allowHeader", "GET, OPTIONS")
                        .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_BINDING_REPORT)
                        .containsEntry("requestContentType", "")
                        .containsEntry("responseContentType", WayangA2uiTransportContent.MIME_JSON)
                        .containsEntry("requestBodyRequired", false)
                        .satisfies(values -> assertThat((Iterable<String>) values.get("allowedMethods"))
                                .containsExactly("GET", "OPTIONS")))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_SMOKE)
                        .containsEntry("method", "GET")
                        .containsEntry("allowHeader", "GET, OPTIONS")
                        .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_SMOKE)
                        .containsEntry("requestContentType", "")
                        .containsEntry("responseContentType", WayangA2uiTransportContent.MIME_JSON)
                        .containsEntry("requestBodyRequired", false)
                        .satisfies(values -> assertThat((Iterable<String>) values.get("allowedMethods"))
                                .containsExactly("GET", "OPTIONS")))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_READINESS)
                        .containsEntry("method", "GET")
                        .containsEntry("allowHeader", "GET, OPTIONS")
                        .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_READINESS)
                        .containsEntry("requestContentType", "")
                        .containsEntry("responseContentType", WayangA2uiTransportContent.MIME_JSON)
                        .containsEntry("requestBodyRequired", false)
                        .satisfies(values -> assertThat((Iterable<String>) values.get("allowedMethods"))
                                .containsExactly("GET", "OPTIONS")));
    }

    private static A2uiUserAction action(String name) {
        return new A2uiUserAction(
                name,
                "main",
                "button",
                Instant.parse("2026-05-31T00:00:00Z"),
                Map.of("runId", "run-1"));
    }
}
