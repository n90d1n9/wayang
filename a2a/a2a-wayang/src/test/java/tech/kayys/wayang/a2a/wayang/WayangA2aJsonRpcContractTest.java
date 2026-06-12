package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcContractTest {

    private final A2aContractAssert contracts = new A2aContractAssert();

    @Test
    void jsonRpcSmokeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-smoke-result.json",
                contractSmokeResult().toJson());
    }

    @Test
    void jsonRpcSmokeResponseBodyMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-smoke-result.json",
                contractSmokeHttpResponse().body());
    }

    @Test
    void jsonRpcSmokeSummaryMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-smoke-summary.json",
                WayangA2aJsonRpcSmokeSummary.from(contractSmokeResult()).toJson());
    }

    @Test
    void jsonRpcSmokeProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-smoke-probe-result.json",
                WayangA2aJsonRpcSmokeProbeResult.from(contractSmokeHttpResponse()).toJson());
    }

    @Test
    void jsonRpcPassingSmokeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-smoke-result-passed.json",
                passingContractSmokeResult().toJson());
    }

    @Test
    void jsonRpcPassingSmokeResponseBodyMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-smoke-result-passed.json",
                passingContractSmokeHttpResponse().body());
    }

    @Test
    void jsonRpcPassingSmokeSummaryMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-smoke-summary-passed.json",
                WayangA2aJsonRpcSmokeSummary.from(passingContractSmokeResult()).toJson());
    }

    @Test
    void jsonRpcPassingSmokeProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-smoke-probe-result-passed.json",
                WayangA2aJsonRpcSmokeProbeResult.from(passingContractSmokeHttpResponse()).toJson());
    }

    @Test
    void jsonRpcBindingReportMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-binding-report.json",
                contractBindingReport().toJson());
    }

    @Test
    void jsonRpcRouteCatalogMatchesContractFixture() throws IOException {
        String routeCatalogJson = contractRouteCatalog().toJson();
        String responseBody = contractRouteCatalogResponse().body();

        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-route-catalog.json",
                routeCatalogJson);
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-route-catalog.json",
                responseBody);
        assertThat(routeCatalogJson).startsWith("{\"binding\":");
        assertThat(routeCatalogJson.indexOf("\"routes\""))
                .isGreaterThan(routeCatalogJson.indexOf("\"enabledRouteCount\""));
        assertThat(responseBody).isEqualTo(routeCatalogJson);
    }

    @Test
    void jsonRpcRouteCatalogProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-route-catalog-probe-result.json",
                WayangA2aJsonRpcRouteCatalogProbeResult.from(contractRouteCatalogResponse()).toJson());
    }

    @Test
    void jsonRpcDiagnosticsReportMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-diagnostics-report.json",
                contractDiagnosticsReport().toJson());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-diagnostics-report.json",
                WayangA2aJsonRpcDiagnosticsReport.fromJson(contractDiagnosticsReport().toJson()).toJson());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-diagnostics-report.json",
                contractDiagnosticsReportResponse().body());
    }

    @Test
    void jsonRpcDiagnosticsReportKeepsWireFieldOrder() {
        String reportJson = contractDiagnosticsReport().toJson();

        assertJsonKeyOrder(
                reportJson,
                "diagnosticsId",
                "passed",
                "exitCode",
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "issueCount",
                "checks",
                "issues",
                "attributes");
        assertThat(reportJson).contains("\"checks\":[{\"probe\":\"bindingReport\"");
    }

    @Test
    void jsonRpcSpecComplianceReportMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-spec-compliance-report.json",
                contractSpecComplianceReport().toJson());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-spec-compliance-report.json",
                WayangA2aJsonRpcSpecComplianceReport.fromJson(contractSpecComplianceReport().toJson()).toJson());
    }

    @Test
    void jsonRpcBindingReportProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-binding-report-probe-result.json",
                WayangA2aJsonRpcBindingReportProbeResult.from(contractBindingReportHttpResponse()).toJson());
    }

    @Test
    void jsonRpcMethodDispatchCoverageMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-method-dispatch-coverage-incomplete.json",
                WayangA2aHttpJson.write(contractMethodDispatchCoverage().toMap()));
    }

    @Test
    void jsonRpcMethodDispatchIssuesMatchContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-method-dispatch-issues-incomplete.json",
                WayangA2aHttpJson.write(Map.of(
                        "issues",
                        WayangA2aJsonRpcMethodDispatchIssues.from(contractMethodDispatchCoverage()))));
    }

    @Test
    void incompleteJsonRpcBindingReportProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-binding-report-probe-result-incomplete.json",
                WayangA2aJsonRpcBindingReportProbeResult.from(incompleteContractBindingReportHttpResponse()).toJson());
    }

    @Test
    void jsonRpcReadinessProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-probe-result.json",
                contractReadinessProbeResult().toJson());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-probe-result.json",
                WayangA2aJsonRpcReadinessProbeResult.fromJson(contractReadinessProbeResult().toJson()).toJson());
    }

    @Test
    void jsonRpcReadinessProbeResultKeepsWireFieldOrder() {
        String readinessJson = contractReadinessProbeResult().toJson();

        assertJsonKeyOrder(
                readinessJson,
                "passed",
                "exitCode",
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "issueCount",
                "issues",
                "bindingReportProbe",
                "routeCatalogProbe",
                "smokeProbe");
        assertThat(readinessJson).contains("\"issues\":[],\"bindingReportProbe\"");
    }

    @Test
    void jsonRpcReadinessEndpointDecodeMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-probe-result.json",
                WayangA2aJsonRpcReadinessProbeResult.from(contractReadinessHttpResponse()).toJson());
    }

    @Test
    void failedJsonRpcReadinessProbeResultMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-probe-result-failed.json",
                failedContractReadinessProbeResult().toJson());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-probe-result-failed.json",
                WayangA2aJsonRpcReadinessProbeResult.fromJson(
                        failedContractReadinessProbeResult().toJson()).toJson());
    }

    @Test
    void failedJsonRpcReadinessEndpointDecodeMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-probe-result-failed.json",
                WayangA2aJsonRpcReadinessProbeResult.from(failedContractReadinessHttpResponse()).toJson());
    }

    @Test
    void failedJsonRpcReadinessIssueSummaryMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-issue-summary-failed.json",
                WayangA2aJsonRpcReadinessIssueSummary.from(failedContractReadinessProbeResult()).toJson());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-issue-summary-failed.json",
                WayangA2aJsonRpcReadinessIssueSummary.fromJson(WayangA2aJsonRpcReadinessIssueSummary.from(
                        failedContractReadinessProbeResult()).toJson()).toJson());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-issue-summary-failed.json",
                WayangA2aJsonRpcReadinessIssueSummary.from(failedContractReadinessHttpResponse()).toJson());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-issue-summary-failed.json",
                failedContractReadinessIssueSummaryResponse().body());
        contracts.matchesJsonFixture(
                "contracts/a2a/wayang-jsonrpc-readiness-issue-summary-failed.json",
                WayangA2aJsonRpcReadinessIssueSummary.from(
                        failedContractReadinessIssueSummaryResponse()).toJson());
    }

    private static WayangA2aJsonRpcSmokeResult contractSmokeResult() {
        WayangA2aJsonRpcScenario scenario = WayangA2aJsonRpcScenarios.methodError(
                "a2a.jsonrpc.contract",
                "UnknownMethod");
        WayangA2aJsonRpcScenarioExchange exchange = scenario.exchanges().getFirst();
        WayangA2aHttpResponse response = new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aJsonRpcResponse.error(
                        "error",
                        WayangA2aJsonRpcError.methodNotFound("UnknownMethod")).toJson(),
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON));
        WayangA2aJsonRpcScenarioExchangeResult exchangeResult = new WayangA2aJsonRpcScenarioExchangeResult(
                0,
                exchange,
                response,
                WayangA2aHttpJson.read(response.body()),
                List.of());
        WayangA2aJsonRpcScenarioResult scenarioResult = new WayangA2aJsonRpcScenarioResult(
                scenario,
                List.of(exchangeResult),
                List.of(WayangA2aJsonRpcScenarioIssue.from(scenario.id(), exchangeResult)));
        return new WayangA2aJsonRpcSmokeResult(
                scenarioResult,
                Map.of(
                        "source", "contract",
                        "scenarioId", scenario.id(),
                        "exchangeCount", 1));
    }

    private static WayangA2aHttpResponse contractSmokeHttpResponse() {
        return WayangA2aJsonRpcSmokeProbeResult.response(contractSmokeResult());
    }

    private static WayangA2aJsonRpcSmokeResult passingContractSmokeResult() {
        WayangA2aJsonRpcScenario scenario = new WayangA2aJsonRpcScenario(
                "a2a.jsonrpc.contract.pass",
                "A2A JSON-RPC success contract scenario",
                List.of(WayangA2aJsonRpcScenarioExchange.of(WayangA2aJsonRpcRequest.of(
                        "ok",
                        WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                        Map.of()))),
                Map.of());
        WayangA2aJsonRpcScenarioExchange exchange = scenario.exchanges().getFirst();
        WayangA2aHttpResponse response = new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aJsonRpcResponse.result(
                        "ok",
                        Map.of(
                                "kind", "contract-pass",
                                "name", "Wayang")).toJson(),
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON));
        WayangA2aJsonRpcScenarioExchangeResult exchangeResult = new WayangA2aJsonRpcScenarioExchangeResult(
                0,
                exchange,
                response,
                WayangA2aHttpJson.read(response.body()),
                List.of());
        WayangA2aJsonRpcScenarioResult scenarioResult = new WayangA2aJsonRpcScenarioResult(
                scenario,
                List.of(exchangeResult),
                List.of());
        return new WayangA2aJsonRpcSmokeResult(
                scenarioResult,
                Map.of(
                        "source", "contract",
                        "scenarioId", scenario.id(),
                        "exchangeCount", 1));
    }

    private static WayangA2aHttpResponse passingContractSmokeHttpResponse() {
        return WayangA2aJsonRpcSmokeProbeResult.response(passingContractSmokeResult());
    }

    private static WayangA2aJsonRpcBindingReport contractBindingReport() {
        return WayangA2aJsonRpcBindingReport.fromConfig(contractHttpConfig());
    }

    private static WayangA2aJsonRpcHttpRouteCatalog contractRouteCatalog() {
        return WayangA2aJsonRpcHttpRouteCatalog.fromConfig(contractHttpConfig());
    }

    private static WayangA2aHttpResponse contractRouteCatalogResponse() {
        return contractRouteCatalog().response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG));
    }

    private static WayangA2aJsonRpcHttpConfig contractHttpConfig() {
        return WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokePath("/internal/a2a/smoke")
                .smokeEnabled(false)
                .build();
    }

    private static WayangA2aJsonRpcDiagnosticsReport contractDiagnosticsReport() {
        return WayangA2aJsonRpcDiagnosticsReport.from(contractReadinessProbeResult(), contractHttpConfig());
    }

    private static WayangA2aJsonRpcSpecComplianceReport contractSpecComplianceReport() {
        return WayangA2aJsonRpcHttpAdapter.configured(contractDispatcher(), contractHttpConfig())
                .specComplianceReport();
    }

    private static WayangA2aJsonRpcDispatcher contractDispatcher() {
        return WayangA2aJsonRpcDispatcher.forExecution(
                contractAgentCard(),
                new InMemoryWayangA2aTaskStore(),
                request -> AgentResponse.builder()
                        .runId("run-jsonrpc-contract")
                        .requestId(request.requestId())
                        .answer("contract")
                        .strategy("contract")
                        .build());
    }

    private static A2aAgentCard contractAgentCard() {
        return A2aAgentCard.minimal(
                "Wayang",
                "A2A JSON-RPC contract endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }

    private static WayangA2aHttpResponse contractDiagnosticsReportResponse() {
        return contractDiagnosticsReport().response()
                .withHeaders(Map.of(
                        WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_DIAGNOSTICS_REPORT));
    }

    private static WayangA2aHttpResponse contractBindingReportHttpResponse() {
        return contractBindingReport().response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT));
    }

    private static WayangA2aJsonRpcMethodDispatchCoverage contractMethodDispatchCoverage() {
        return WayangA2aJsonRpcMethodDispatchCoverage.from(
                List.of(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                        WayangA2aJsonRpcMethods.GET_TASK),
                List.of(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                        "CustomMethod"));
    }

    private static WayangA2aHttpResponse incompleteContractBindingReportHttpResponse() {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("binding", "JSONRPC");
        body.put("protocolVersion", "1.0");
        body.put("endpoint", Map.of());
        body.put("smoke", Map.of("enabled", true));
        body.put("routeCatalog", Map.of("enabled", true));
        body.put("diagnosticsReport", Map.of("enabled", true));
        body.put("specComplianceReport", Map.of("enabled", true));
        body.put("bindingReport", Map.of("enabled", true));
        body.put("readiness", Map.of("enabled", true));
        body.put("readinessIssueSummary", Map.of("enabled", true));
        body.put("methodCount", 0);
        body.put("methods", List.of());
        body.put("streamingMethods", List.of());
        body.put("config", Map.of());
        return new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aHttpJson.write(Map.copyOf(body)),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                        WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                        "1.0",
                        WayangA2aHttpResponse.HEADER_A2A_VERSION,
                        "1.0",
                        WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT));
    }

    private static WayangA2aJsonRpcReadinessProbeResult contractReadinessProbeResult() {
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(contractBindingReportHttpResponse()),
                WayangA2aJsonRpcRouteCatalogProbeResult.from(contractRouteCatalogResponse()),
                true,
                null,
                false);
    }

    private static WayangA2aHttpResponse contractReadinessHttpResponse() {
        return contractReadinessProbeResult().response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_READINESS));
    }

    private static WayangA2aJsonRpcReadinessProbeResult failedContractReadinessProbeResult() {
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(failedContractBindingReportHttpResponse()),
                WayangA2aJsonRpcRouteCatalogProbeResult.from(failedContractRouteCatalogResponse()),
                true,
                null,
                false);
    }

    private static WayangA2aHttpResponse failedContractReadinessHttpResponse() {
        return failedContractReadinessProbeResult().response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_READINESS));
    }

    private static WayangA2aHttpResponse failedContractReadinessIssueSummaryResponse() {
        return WayangA2aJsonRpcReadinessIssueSummary.from(failedContractReadinessProbeResult()).response();
    }

    private static WayangA2aHttpResponse failedContractRouteCatalogResponse() {
        return WayangA2aJsonRpcHttpRouteCatalog.fromConfig(WayangA2aJsonRpcHttpConfig.builder()
                        .bindingReportEnabled(false)
                        .build())
                .response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG));
    }

    private static WayangA2aHttpResponse failedContractBindingReportHttpResponse() {
        return new WayangA2aHttpResponse(
                404,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aHttpJson.write(Map.of(
                        "error",
                        Map.of(
                                "code",
                                "jsonrpc_path_not_found",
                                "message",
                                "No A2A JSON-RPC path matches /a2a/jsonrpc/binding-report."))),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC,
                        WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                        "1.0",
                        WayangA2aHttpResponse.HEADER_A2A_VERSION,
                        "1.0"));
    }

    private static void assertJsonKeyOrder(String json, String... keys) {
        int cursor = -1;
        for (String key : keys) {
            int index = json.indexOf("\"" + key + "\"", cursor + 1);
            assertThat(index)
                    .as("JSON key '%s' should appear after position %s in %s", key, cursor, json)
                    .isGreaterThan(cursor);
            cursor = index;
        }
    }
}
