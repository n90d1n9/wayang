package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpHarnessTest {

    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    void runsHttpScenariosAndSummarizesResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(adapter);
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));
        WayangA2uiHttpScenario scenario = WayangA2uiHttpScenario.of(
                        "inspect-route-diagnostics",
                        WayangA2uiHttpRequest.exchange(request.toJson()),
                        WayangA2uiHttpRequest.routeCatalog(),
                        WayangA2uiHttpRequest.bindingReport(),
                        new WayangA2uiHttpRequest(
                                "OPTIONS",
                                WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                                "",
                                Map.of(),
                                Map.of()))
                .withAttributes(Map.of("tenant", "demo"));

        WayangA2uiHttpScenarioResult result = harness.run(scenario);

        assertThat(scenario.size()).isEqualTo(4);
        assertThat(scenario.empty()).isFalse();
        assertThat(result.scenarioId()).isEqualTo("inspect-route-diagnostics");
        assertThat(result.attributes()).containsEntry("tenant", "demo");
        assertThat(result.exchangeCount()).isEqualTo(4);
        assertThat(result.successfulCount()).isEqualTo(4L);
        assertThat(result.clientErrorCount()).isZero();
        assertThat(result.serverErrorCount()).isZero();
        assertThat(result.handledCount()).isEqualTo(1L);
        assertThat(result.rejectedCount()).isZero();
        assertThat(result.hasTransportErrors()).isFalse();
        assertThat(result.statusCodes()).containsExactly(200, 200, 200, 200);
        assertThat(result.outcomes()).containsExactly(
                WayangA2uiTransportOutcome.SUCCESS,
                WayangA2uiTransportOutcome.SUCCESS,
                WayangA2uiTransportOutcome.SUCCESS,
                WayangA2uiTransportOutcome.SUCCESS);
        assertThat(result.exchanges())
                .extracting(WayangA2uiHttpScenarioExchange::index)
                .containsExactly(1, 2, 3, 4);
        assertThat(result.responseEnvelopes().get(0))
                .containsEntry(WayangA2uiTransportFields.HANDLED_COUNT, 1L);
        assertThat(result.exchanges().get(0).transportResponse().body()).contains(WayangA2uiActions.RUN_INSPECT);
        assertThat(result.exchanges().get(1).transportResponse().body())
                .contains(WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG);
        assertThat(result.exchanges().get(2).transportResponse().metadata())
                .containsEntry(WayangA2uiTransportFields.COMPLETE, true)
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_BINDING_REPORT);
        assertThat(result.exchanges().get(3).response().headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void runsSmokeRunnerForOperationalEntrypoints() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpSmokeRunner runner = WayangA2uiHttpSmokeRunner.of(adapter);

        WayangA2uiHttpSmokeResult result = runner.run();
        Map<String, Object> resultMap = result.toMap();

        assertThat(result.passed()).isTrue();
        assertThat(result.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_SUCCESS);
        assertThat(result.suiteResult().scenarioCount()).isEqualTo(3);
        assertThat(result.suiteResult().exchangeCount()).isEqualTo(17L);
        assertThat(result.expectationResult().passed()).isTrue();
        assertThat(result.expectationResult().issueCount()).isZero();
        assertThat(result.attributes())
                .containsEntry("suiteId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry("routeCount", 6);
        assertThat(resultMap)
                .containsEntry("passed", true)
                .containsEntry("exitCode", 0);
        assertThat((Map<String, Object>) resultMap.get("suiteReport"))
                .containsEntry("suiteId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry("passed", true);
        assertThat((Map<String, Object>) resultMap.get("expectationResult"))
                .containsEntry("targetId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry("passed", true);
        assertThat(result.toJson())
                .contains("\"exitCode\":0")
                .contains(WayangA2uiHttpScenarios.SMOKE_SUITE_ID);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void mapsSmokeValidationFailuresToFailureExitCode() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(adapter);
        WayangA2uiHttpScenarioSuiteResult suiteResult = harness.run(WayangA2uiHttpScenarios.smokeSuite(
                adapter.routeCatalog()));
        WayangA2uiHttpExpectationResult validation = suiteResult.validate(
                WayangA2uiHttpScenarioSuiteExpectation.pass().withExpectedScenarioCount(99));

        WayangA2uiHttpSmokeResult result = new WayangA2uiHttpSmokeResult(
                suiteResult,
                validation,
                Map.of("suiteId", "bad-expectation"));

        assertThat(validation.passed()).isFalse();
        assertThat(result.passed()).isFalse();
        assertThat(result.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(result.toMap())
                .containsEntry("passed", false)
                .containsEntry("exitCode", 1);
        assertThat(result.toJson()).contains("\"field\":\"scenarioCount\"");
        assertThat(sdk.inspected).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void runsBuiltInSmokeSuitesAndAggregatesReports() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(adapter);
        WayangA2uiHttpScenarioSuite suite = WayangA2uiHttpScenarios.smokeSuite(adapter.routeCatalog());

        WayangA2uiHttpScenarioSuiteResult result = harness.run(suite);
        WayangA2uiHttpScenarioSuiteReport report = result.report();
        Map<String, Object> reportMap = result.toMap();

        assertThat(suite.id()).isEqualTo(WayangA2uiHttpScenarios.SMOKE_SUITE_ID);
        assertThat(suite.size()).isEqualTo(3);
        assertThat(suite.empty()).isFalse();
        assertThat(suite.attributes())
                .containsEntry("suiteKind", "smoke")
                .containsEntry("routeCount", 6);
        assertThat(result.suiteId()).isEqualTo(WayangA2uiHttpScenarios.SMOKE_SUITE_ID);
        assertThat(result.scenarioCount()).isEqualTo(3);
        assertThat(result.passedScenarioCount()).isEqualTo(3L);
        assertThat(result.failedScenarioCount()).isZero();
        assertThat(result.exchangeCount()).isEqualTo(17L);
        assertThat(result.successfulCount()).isEqualTo(17L);
        assertThat(result.clientErrorCount()).isZero();
        assertThat(result.serverErrorCount()).isZero();
        assertThat(result.handledCount()).isZero();
        assertThat(result.rejectedCount()).isZero();
        assertThat(result.hasTransportErrors()).isFalse();
        assertThat(result.issueCount()).isZero();
        assertThat(result.scenarioIds()).containsExactly(
                WayangA2uiHttpScenarios.DISCOVERY_ID,
                WayangA2uiHttpScenarios.ROUTE_OPTIONS_ID,
                WayangA2uiHttpScenarios.DIAGNOSTICS_ID);
        assertThat(report.passed()).isTrue();
        assertThat(reportMap)
                .containsEntry("suiteId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry("passed", true)
                .containsEntry("scenarioCount", 3)
                .containsEntry("exchangeCount", 17L)
                .containsEntry("issueCount", 0L);
        assertThat((Iterable<String>) reportMap.get("scenarioIds")).containsExactly(
                WayangA2uiHttpScenarios.DISCOVERY_ID,
                WayangA2uiHttpScenarios.ROUTE_OPTIONS_ID,
                WayangA2uiHttpScenarios.DIAGNOSTICS_ID);
        assertThat((Map<String, Object>) reportMap.get("attributes"))
                .containsEntry("suiteKind", "smoke")
                .containsEntry("routeCount", 6);
        List<Map<String, Object>> scenarioReports = (List<Map<String, Object>>) reportMap.get("scenarios");
        assertThat(scenarioReports)
                .extracting(scenario -> scenario.get("scenarioId"))
                .containsExactly(
                        WayangA2uiHttpScenarios.DISCOVERY_ID,
                        WayangA2uiHttpScenarios.ROUTE_OPTIONS_ID,
                        WayangA2uiHttpScenarios.DIAGNOSTICS_ID);
        assertThat(result.toJson())
                .contains("\"suiteId\":\"" + WayangA2uiHttpScenarios.SMOKE_SUITE_ID + "\"")
                .contains(WayangA2uiHttpScenarios.DIAGNOSTICS_ID);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsFailedScenarioSuites() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(
                WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk)));
        WayangA2uiHttpScenarioSuite suite = WayangA2uiHttpScenarioSuite.of(
                        "mixed-suite",
                        WayangA2uiHttpScenarios.discovery(),
                        WayangA2uiHttpScenario.of("broken-exchange", WayangA2uiHttpRequest.exchange("{not-json")))
                .withAttributes(Map.of("suiteKind", "mixed"));

        WayangA2uiHttpScenarioSuiteResult result = harness.run(suite);
        WayangA2uiHttpScenarioSuiteReport report = result.report();
        Map<String, Object> reportMap = report.toMap();

        assertThat(suite.size()).isEqualTo(2);
        assertThat(result.scenarioCount()).isEqualTo(2);
        assertThat(result.passedScenarioCount()).isEqualTo(1L);
        assertThat(result.failedScenarioCount()).isEqualTo(1L);
        assertThat(result.exchangeCount()).isEqualTo(3L);
        assertThat(result.successfulCount()).isEqualTo(2L);
        assertThat(result.clientErrorCount()).isEqualTo(1L);
        assertThat(result.rejectedCount()).isEqualTo(1L);
        assertThat(result.hasTransportErrors()).isTrue();
        assertThat(result.issueCount()).isEqualTo(1L);
        assertThat(report.passed()).isFalse();
        assertThat(reportMap)
                .containsEntry("suiteId", "mixed-suite")
                .containsEntry("passed", false)
                .containsEntry("failedScenarioCount", 1L)
                .containsEntry("transportErrors", true)
                .containsEntry("issueCount", 1L);
        assertThat((List<Map<String, Object>>) reportMap.get("issues"))
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("scenarioId", "broken-exchange")
                        .containsEntry("exchangeIndex", 1)
                        .containsEntry("statusCode", 400)
                        .containsEntry("errorCode", "invalid_request_json"));
        assertThat(report.toJson())
                .contains("\"passed\":false")
                .contains("broken-exchange")
                .contains("invalid_request_json");
        assertThat(sdk.inspected).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void validatesScenarioAndSuiteExpectations() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(adapter);

        WayangA2uiHttpScenarioResult scenarioResult = harness.run(WayangA2uiHttpScenario.of(
                "route-catalog-check",
                WayangA2uiHttpRequest.routeCatalog(),
                WayangA2uiHttpRequest.bindingReport()));
        WayangA2uiHttpScenarioExpectation scenarioExpectation = WayangA2uiHttpScenarioExpectation.pass()
                .withExpectedExchangeCount(2)
                .withExpectedStatusCodes(List.of(200, 200))
                .withExpectedOutcomes(List.of(
                        WayangA2uiTransportOutcome.SUCCESS.name(),
                        WayangA2uiTransportOutcome.SUCCESS.name()))
                .withExpectedRouteOperations(List.of(
                        WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT));

        WayangA2uiHttpExpectationResult scenarioValidation = scenarioResult.validate(scenarioExpectation);

        assertThat(scenarioValidation.passed()).isTrue();
        assertThat(scenarioValidation.issueCount()).isZero();
        assertThat(scenarioValidation.toMap())
                .containsEntry("targetId", "route-catalog-check")
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);

        WayangA2uiHttpScenarioSuiteResult suiteResult = harness.run(WayangA2uiHttpScenarios.smokeSuite(
                adapter.routeCatalog()));
        WayangA2uiHttpExpectationResult suiteValidation = suiteResult.validate(
                WayangA2uiHttpScenarios.smokeSuiteExpectation(adapter.routeCatalog()));

        assertThat(suiteValidation.passed()).isTrue();
        assertThat(suiteValidation.issueCount()).isZero();
        assertThat(suiteValidation.toJson())
                .contains("\"targetId\":\"" + WayangA2uiHttpScenarios.SMOKE_SUITE_ID + "\"")
                .contains("\"passed\":true");
        assertThat(sdk.inspected).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsExpectationValidationIssues() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(
                WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk)));
        WayangA2uiHttpScenarioResult result = harness.run(WayangA2uiHttpScenario.of(
                "broken-expected-pass",
                WayangA2uiHttpRequest.exchange("{not-json")));
        WayangA2uiHttpScenarioExpectation expectation = WayangA2uiHttpScenarioExpectation.pass()
                .withExpectedExchangeCount(2)
                .withExpectedStatusCodes(List.of(200))
                .withExpectedRouteOperations(List.of(WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG));

        WayangA2uiHttpExpectationResult validation = result.validate(expectation);
        Map<String, Object> validationMap = validation.toMap();

        assertThat(validation.passed()).isFalse();
        assertThat(validation.issueCount()).isGreaterThanOrEqualTo(5);
        assertThat(validationMap)
                .containsEntry("targetId", "broken-expected-pass")
                .containsEntry("passed", false);
        assertThat((List<Map<String, Object>>) validationMap.get("validationIssues"))
                .extracting(issue -> issue.get("field"))
                .contains(
                        "passed",
                        "exchangeCount",
                        "issueCount",
                        "transportErrors",
                        "statusCodes",
                        "routeOperations");
        assertThat(validation.toJson())
                .contains("\"field\":\"statusCodes\"")
                .contains("broken-expected-pass");
        assertThat(sdk.inspected).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void runsBuiltInDiagnosticScenarios() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(adapter);
        WayangA2uiHttpScenario scenario = WayangA2uiHttpScenarios.diagnostics(adapter.routeCatalog());

        WayangA2uiHttpScenarioResult result = harness.run(scenario);
        Map<String, Object> report = result.toMap();

        assertThat(scenario.id()).isEqualTo(WayangA2uiHttpScenarios.DIAGNOSTICS_ID);
        assertThat(scenario.size()).isEqualTo(9);
        assertThat(scenario.attributes())
                .containsEntry("scenarioKind", "diagnostics")
                .containsEntry("routeCount", 6);
        assertThat(scenario.requests())
                .extracting(WayangA2uiHttpRequest::method)
                .containsExactly(
                        "GET", "GET", "GET",
                        "OPTIONS", "OPTIONS", "OPTIONS", "OPTIONS", "OPTIONS", "OPTIONS");
        assertThat(result.exchangeCount()).isEqualTo(9);
        assertThat(result.successfulCount()).isEqualTo(9L);
        assertThat(result.hasTransportErrors()).isFalse();
        assertThat(result.statusCodes()).containsExactly(200, 200, 200, 200, 200, 200, 200, 200, 200);
        assertThat(report)
                .containsEntry("scenarioId", WayangA2uiHttpScenarios.DIAGNOSTICS_ID)
                .containsEntry("passed", true)
                .containsEntry("exchangeCount", 9);
        assertThat((Map<String, Object>) report.get("attributes"))
                .containsEntry("scenarioKind", "diagnostics")
                .containsEntry("routeCount", 6);
        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) report.get("exchanges");
        Map<String, Object> optionRequest = (Map<String, Object>) exchanges.get(3).get("request");
        assertThat(optionRequest)
                .containsEntry("method", "OPTIONS")
                .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE);
        assertThat((Map<String, Object>) optionRequest.get("attributes"))
                .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry("routeMethod", "POST");
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void buildsBuiltInDiscoveryAndRouteOptionsScenarios() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(adapter);
        WayangA2uiHttpScenario discovery = WayangA2uiHttpScenarios.discovery();
        WayangA2uiHttpScenario routeOptions = WayangA2uiHttpScenarios.routeOptions(adapter.routeCatalog());

        WayangA2uiHttpScenarioResult discoveryResult = harness.run(discovery);
        WayangA2uiHttpScenarioResult routeOptionsResult = harness.run(routeOptions);

        assertThat(discovery.id()).isEqualTo(WayangA2uiHttpScenarios.DISCOVERY_ID);
        assertThat(discovery.size()).isEqualTo(2);
        assertThat(discovery.requests())
                .extracting(WayangA2uiHttpRequest::path)
                .containsExactly(
                        WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG,
                        WayangA2uiHttpBridgeAdapter.PATH_BINDING_REPORT);
        assertThat(discoveryResult.report().passed()).isTrue();
        assertThat(discoveryResult.statusCodes()).containsExactly(200, 200);

        assertThat(routeOptions.id()).isEqualTo(WayangA2uiHttpScenarios.ROUTE_OPTIONS_ID);
        assertThat(routeOptions.size()).isEqualTo(6);
        assertThat(routeOptions.requests())
                .extracting(WayangA2uiHttpRequest::method)
                .containsExactly("OPTIONS", "OPTIONS", "OPTIONS", "OPTIONS", "OPTIONS", "OPTIONS");
        assertThat(routeOptions.requests())
                .extracting(WayangA2uiHttpRequest::path)
                .containsExactly(
                        WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                        WayangA2uiHttpBridgeAdapter.PATH_SURFACE_CATALOG,
                        WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG,
                        WayangA2uiHttpBridgeAdapter.PATH_BINDING_REPORT,
                        WayangA2uiHttpBridgeAdapter.PATH_SMOKE,
                        WayangA2uiHttpBridgeAdapter.PATH_READINESS);
        assertThat(routeOptionsResult.report().passed()).isTrue();
        assertThat(routeOptionsResult.statusCodes()).containsExactly(200, 200, 200, 200, 200, 200);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void buildsExchangeJsonScenarios() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpHarness harness = new WayangA2uiHttpHarness(
                WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk)));
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));
        WayangA2uiHttpScenario scenario = WayangA2uiHttpScenario.exchangeJson(
                "exchange-json",
                List.of(request.toJson()));

        WayangA2uiHttpScenarioResult result = harness.run(scenario);

        assertThat(scenario.size()).isEqualTo(1);
        assertThat(result.exchangeCount()).isEqualTo(1);
        assertThat(result.statusCodes()).containsExactly(200);
        assertThat(result.handledCount()).isEqualTo(1L);
        assertThat(result.hasTransportErrors()).isFalse();
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportsScenarioReportsForAutomation() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(
                WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk)));
        WayangA2uiHttpScenario scenario = WayangA2uiHttpScenario.of(
                        "route-report",
                        WayangA2uiHttpRequest.routeCatalog(),
                        WayangA2uiHttpRequest.bindingReport())
                .withAttributes(Map.of("traceId", "trace-1"));

        WayangA2uiHttpScenarioResult result = harness.run(scenario);
        WayangA2uiHttpScenarioReport report = result.report();
        Map<String, Object> reportMap = result.toMap();

        assertThat(report.passed()).isTrue();
        assertThat(reportMap)
                .containsEntry("scenarioId", "route-report")
                .containsEntry("passed", true)
                .containsEntry("exchangeCount", 2)
                .containsEntry("successfulCount", 2L)
                .containsEntry("transportErrors", false)
                .containsEntry("issueCount", 0);
        assertThat((Iterable<Integer>) reportMap.get("statusCodes")).containsExactly(200, 200);
        assertThat((Iterable<String>) reportMap.get("outcomes")).containsExactly(
                WayangA2uiTransportOutcome.SUCCESS.name(),
                WayangA2uiTransportOutcome.SUCCESS.name());
        assertThat((Map<String, Object>) reportMap.get("attributes"))
                .containsEntry("traceId", "trace-1");

        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) reportMap.get("exchanges");
        Map<String, Object> firstExchange = exchanges.get(0);
        Map<String, Object> firstRequest = (Map<String, Object>) firstExchange.get("request");
        Map<String, Object> firstResponse = (Map<String, Object>) firstExchange.get("response");
        assertThat(firstExchange).containsEntry("index", 1);
        assertThat(firstRequest)
                .containsEntry("method", "GET")
                .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_ROUTE_CATALOG)
                .containsEntry("bodyPresent", false)
                .containsEntry("bodyLength", 0);
        assertThat(firstResponse)
                .containsEntry("statusCode", 200)
                .containsEntry("successful", true)
                .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG)
                .containsEntry("allow", "GET, OPTIONS")
                .containsEntry("transportError", false);
        assertThat((Map<String, Object>) firstResponse.get("responseEnvelope"))
                .containsEntry(WayangA2uiTransportFields.OUTCOME, WayangA2uiTransportOutcome.SUCCESS.name());
        assertThat(report.toJson())
                .contains("\"scenarioId\":\"route-report\"")
                .contains(WayangA2uiHttpRoute.OPERATION_BINDING_REPORT);
        assertThat(result.toJson()).contains("\"scenarioId\":\"route-report\"");
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void capturesHttpProblemResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));
        WayangA2uiHttpHarness harness = WayangA2uiHttpHarness.of(adapter);
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));
        WayangA2uiHttpScenario scenario = WayangA2uiHttpScenario.of(
                "http-problems",
                WayangA2uiHttpRequest.exchange("{not-json"),
                new WayangA2uiHttpRequest(
                        "POST",
                        WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                        request.toJson(),
                        Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, "text/plain"),
                        Map.of()),
                new WayangA2uiHttpRequest(
                        "POST",
                        WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE,
                        request.toJson(),
                        Map.of(
                                WayangA2uiHttpResponse.HEADER_CONTENT_TYPE,
                                WayangA2uiTransportContent.MIME_JSON,
                                WayangA2uiHttpResponse.HEADER_ACCEPT,
                                "text/plain"),
                        Map.of()),
                WayangA2uiHttpRequest.get("/future"));

        WayangA2uiHttpScenarioResult result = harness.run(scenario);

        assertThat(result.exchangeCount()).isEqualTo(4);
        assertThat(result.successfulCount()).isZero();
        assertThat(result.clientErrorCount()).isEqualTo(4L);
        assertThat(result.serverErrorCount()).isZero();
        assertThat(result.handledCount()).isZero();
        assertThat(result.rejectedCount()).isEqualTo(4L);
        assertThat(result.hasTransportErrors()).isTrue();
        assertThat(result.statusCodes()).containsExactly(400, 415, 406, 404);
        assertThat(result.outcomes()).containsExactly(
                WayangA2uiTransportOutcome.TRANSPORT_ERROR,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR,
                WayangA2uiTransportOutcome.TRANSPORT_ERROR);
        assertThat(result.exchanges())
                .extracting(exchange -> exchange.transportResponse().transportError().orElseThrow().code())
                .containsExactly(
                        "invalid_request_json",
                        "unsupported_media_type",
                        "not_acceptable",
                        "not_found");
        WayangA2uiHttpScenarioReport report = result.report();
        Map<String, Object> reportMap = report.toMap();
        assertThat(report.passed()).isFalse();
        assertThat(report.issueCount()).isEqualTo(4);
        assertThat(reportMap)
                .containsEntry("passed", false)
                .containsEntry("transportErrors", true)
                .containsEntry("clientErrorCount", 4L)
                .containsEntry("issueCount", 4);
        assertThat((List<Map<String, Object>>) reportMap.get("issues"))
                .extracting(issue -> issue.get("errorCode"))
                .containsExactly(
                        "invalid_request_json",
                        "unsupported_media_type",
                        "not_acceptable",
                        "not_found");
        assertThat((List<Map<String, Object>>) reportMap.get("issues"))
                .first()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("scenarioId", "http-problems")
                        .containsEntry("exchangeIndex", 1)
                        .containsEntry("method", "POST")
                        .containsEntry("path", WayangA2uiHttpBridgeAdapter.PATH_EXCHANGE)
                        .containsEntry("statusCode", 400)
                        .containsEntry("outcome", WayangA2uiTransportOutcome.TRANSPORT_ERROR.name()));
        assertThat(report.toJson())
                .contains("\"passed\":false")
                .contains("unsupported_media_type")
                .contains("not_found");
        assertThat(sdk.inspected).isZero();
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
