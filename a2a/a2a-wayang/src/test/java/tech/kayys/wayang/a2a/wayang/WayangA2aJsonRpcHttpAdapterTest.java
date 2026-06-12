package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentExtension;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpAdapterTest {

    private static final String REQUIRED_EXTENSION = "https://wayang.test/extensions/human-approval/v1";

    @Test
    void dispatchesJsonRpcPostThroughHttpAdapter() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcHttpAdapter adapter = adapter(store);
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-http-jsonrpc",
                "context-http-jsonrpc",
                "task-http-jsonrpc",
                "ping");
        WayangA2aJsonRpcRequest rpcRequest = WayangA2aJsonRpcRequest.of(
                "send",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                sendRequest.toMap());

        WayangA2aHttpResponse response = adapter.dispatch(post("/", rpcRequest.toJson(), "application/json"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION, A2aProtocol.OPERATION_SEND_MESSAGE)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(status(resultTask(response)))
                .containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());
        assertThat(store.get("task-http-jsonrpc").orElseThrow().status().state())
                .isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
    }

    @Test
    void preservesEventStreamContentTypeForStreamingJsonRpcMethods() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcHttpAdapter adapter = adapter(store);
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-http-jsonrpc-stream",
                "context-http-jsonrpc-stream",
                "task-http-jsonrpc-stream",
                "ping");
        WayangA2aJsonRpcRequest rpcRequest = WayangA2aJsonRpcRequest.of(
                "stream",
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                sendRequest.toMap());

        WayangA2aHttpResponse response = adapter.dispatch(post(
                "/",
                rpcRequest.toJson(),
                A2aProtocol.EVENT_STREAM_MEDIA_TYPE));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.EVENT_STREAM_MEDIA_TYPE)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(response.body())
                .contains("\"jsonrpc\":\"2.0\"")
                .contains("\"task\"");
        assertThat(store.get("task-http-jsonrpc-stream").orElseThrow().status().state())
                .isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
    }

    @Test
    void validatesA2aVersionHeaderBeforeJsonRpcExecution() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcHttpAdapter adapter = adapter(store);
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-http-jsonrpc-version",
                "context-http-jsonrpc-version",
                "task-http-jsonrpc-version",
                "ping");
        WayangA2aJsonRpcRequest supported = WayangA2aJsonRpcRequest.of(
                "supported-version",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                sendRequest.toMap());
        WayangA2aJsonRpcRequest unsupported = WayangA2aJsonRpcRequest.of(
                "unsupported-version",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                sendRequest.toMap());

        WayangA2aHttpResponse supportedResponse = adapter.dispatch(post(
                "/",
                supported.toJson(),
                "application/json",
                A2aProtocol.VERSION));
        WayangA2aHttpResponse unsupportedResponse = adapter.dispatch(post(
                "/",
                unsupported.toJson(),
                "application/json",
                "0.5"));

        assertThat(supportedResponse.statusCode()).isEqualTo(200);
        assertThat(status(resultTask(supportedResponse)))
                .containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());
        assertThat(unsupportedResponse.statusCode()).isEqualTo(400);
        assertThat(unsupportedResponse.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(WayangA2aHttpJson.read(unsupportedResponse.body()))
                .containsEntry("id", "unsupported-version");
        assertThat(error(unsupportedResponse))
                .containsEntry("code", WayangA2aJsonRpcError.VERSION_NOT_SUPPORTED)
                .containsEntry("message", "A2A protocol version is not supported: 0.5");
    }

    @Test
    void validatesRequiredA2aExtensionsBeforeJsonRpcExecution() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.of(
                dispatcher(store, cardRequiringExtension()));
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-http-jsonrpc-extension",
                "context-http-jsonrpc-extension",
                "task-http-jsonrpc-extension",
                "ping");
        WayangA2aJsonRpcRequest rpcRequest = WayangA2aJsonRpcRequest.of(
                "required-extension",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                sendRequest.toMap());
        WayangA2aHttpRequest missingExtension = post("/", rpcRequest.toJson(), "application/json");
        WayangA2aHttpRequest supportedExtension = new WayangA2aHttpRequest(
                "POST",
                "/",
                rpcRequest.toJson(),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_ACCEPT, "application/json",
                        "a2a-extensions", "https://wayang.test/extensions/trace/v1, " + REQUIRED_EXTENSION),
                Map.of());

        WayangA2aHttpResponse rejected = adapter.dispatch(missingExtension);
        WayangA2aHttpResponse accepted = adapter.dispatch(supportedExtension);

        assertThat(rejected.statusCode()).isEqualTo(400);
        assertThat(rejected.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS, REQUIRED_EXTENSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(WayangA2aHttpJson.read(rejected.body()))
                .containsEntry("id", "required-extension");
        assertThat(error(rejected))
                .containsEntry("code", WayangA2aJsonRpcError.EXTENSION_SUPPORT_REQUIRED)
                .containsEntry("message", "A2A request requires extension support: " + REQUIRED_EXTENSION + ".");
        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(status(resultTask(accepted)))
                .containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());
    }

    @Test
    void canProtectJsonRpcExtendedAgentCardWithBearerAuthorizer() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.withExtendedAgentCardAuthorizer(
                dispatcher(store, cardRequiringExtension()),
                WayangA2aExtendedAgentCardAuthorizer.requireBearerToken("secret-token"));
        WayangA2aJsonRpcRequest rpcRequest = WayangA2aJsonRpcRequest.of(
                "extended-card",
                WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                Map.of());
        WayangA2aHttpRequest unauthorizedRequest = post("/", rpcRequest.toJson(), "application/json");
        WayangA2aHttpRequest authorizedRequest = new WayangA2aHttpRequest(
                "POST",
                "/",
                rpcRequest.toJson(),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_ACCEPT, "application/json",
                        "authorization", "Bearer secret-token"),
                Map.of());

        WayangA2aHttpResponse unauthorized = adapter.dispatch(unauthorizedRequest);
        WayangA2aHttpResponse authorized = adapter.dispatch(authorizedRequest);

        assertThat(unauthorized.statusCode()).isEqualTo(401);
        assertThat(unauthorized.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE,
                        "Bearer realm=\"a2a-extended-agent-card\"")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(WayangA2aHttpJson.read(unauthorized.body()))
                .containsEntry("id", "extended-card");
        assertThat(error(unauthorized))
                .containsEntry("code", WayangA2aJsonRpcError.AUTHENTICATION_REQUIRED)
                .containsEntry("message", "A2A extended Agent Card requires authorization.");
        assertThat(authorized.statusCode()).isEqualTo(200);
        assertThat(map(WayangA2aHttpJson.read(authorized.body()).get("result")))
                .containsEntry("name", "Wayang");
    }

    @Test
    void servesSmokeProbeThroughHttpAdapter() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher dispatcher = dispatcher(store);
        WayangA2aJsonRpcSmokeRunner runner = WayangA2aJsonRpcSmokeRunner.of(
                dispatcher,
                WayangA2aSendMessageServiceTest.request(
                        "message-http-jsonrpc-smoke",
                        "context-http-jsonrpc-smoke",
                        "task-http-jsonrpc-smoke",
                        "ping"));
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.withSmoke(dispatcher, runner);
        WayangA2aJsonRpcHttpAdapter directAdapter = smokeAdapter("direct");

        WayangA2aHttpResponse response = adapter.dispatch(new WayangA2aHttpRequest(
                "GET",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_SMOKE_PATH,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of()));
        WayangA2aJsonRpcSmokeProbeResult probe = WayangA2aJsonRpcSmokeProbeResult.from(response);
        WayangA2aJsonRpcSmokeProbeResult directProbe = directAdapter.smokeProbe();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(smokeAdapter("response").smokeResponse().statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(probe.passed()).isTrue();
        assertThat(probe.summary().scenarioId()).isEqualTo("a2a.jsonrpc.smoke");
        assertThat(directProbe.passed()).isTrue();
        assertThat(directProbe.summary().scenarioId()).isEqualTo("a2a.jsonrpc.smoke");
    }

    @Test
    void servesBindingReportThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = adapter(new InMemoryWayangA2aTaskStore());

        WayangA2aHttpResponse response = adapter.dispatch(new WayangA2aHttpRequest(
                "GET",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_BINDING_REPORT_PATH,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of()));
        Map<String, Object> body = WayangA2aHttpJson.read(response.body());
        WayangA2aHttpResponse options = adapter.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_BINDING_REPORT_PATH,
                "",
                Map.of(),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(body)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("methodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(map(body.get("methodDispatch")))
                .containsEntry("complete", true)
                .containsEntry("registeredMethodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("dispatchMethodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(map(body.get("methodRegistry")))
                .containsEntry("reported", true)
                .containsEntry("groupCount", 3)
                .containsEntry("overridePolicy", "ALLOW_REPLACE")
                .containsEntry("overrideCount", 0);
        assertThat(map(body.get("bindingReport")))
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_BINDING_REPORT_PATH)
                .containsEntry("enabled", true);
        assertThat(map(body.get("routeCatalog")))
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH)
                .containsEntry("enabled", true);
        assertThat(map(body.get("diagnosticsReport")))
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH)
                .containsEntry("enabled", true);
        assertThat(map(body.get("specComplianceReport")))
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH)
                .containsEntry("enabled", true);
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(options.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2aHttpJson.read(options.body()))
                .containsEntry("operation", WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_BINDING_REPORT_PATH);
    }

    @Test
    void probesBindingReportThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = adapter(new InMemoryWayangA2aTaskStore());

        WayangA2aJsonRpcBindingReportProbeResult probe = adapter.bindingReportProbe();

        assertThat(probe.statusCode()).isEqualTo(200);
        assertThat(adapter.bindingReportResponse().statusCode()).isEqualTo(200);
        assertThat(probe.httpSuccessful()).isTrue();
        assertThat(probe.bindingReportRoute()).isTrue();
        assertThat(probe.jsonContent()).isTrue();
        assertThat(probe.complete()).isTrue();
        assertThat(probe.passed()).isTrue();
        assertThat(probe.issueCount()).isZero();
        assertThat(probe.issues()).isEmpty();
        assertThat(probe.methodCount()).isEqualTo(WayangA2aJsonRpcMethods.methods().size());
        assertThat(probe.streamingMethodCount()).isEqualTo(2);
        assertThat(probe.endpointPath()).isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_ENDPOINT_PATH);
        assertThat(probe.smokePath()).isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_SMOKE_PATH);
        assertThat(probe.routeCatalogPath()).isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH);
        assertThat(probe.diagnosticsReportPath())
                .isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH);
        assertThat(probe.specComplianceReportPath())
                .isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH);
        assertThat(probe.bindingReportPath()).isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_BINDING_REPORT_PATH);
        assertThat(probe.readinessPath()).isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_PATH);
        assertThat(probe.readinessIssueSummaryPath())
                .isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_ISSUE_SUMMARY_PATH);
        assertThat(probe.diagnosticHandlersComplete()).isTrue();
        assertThat(probe.diagnosticRouteKeyCount()).isEqualTo(7);
        assertThat(probe.diagnosticHandlerKeyCount()).isEqualTo(7);
        assertThat(probe.missingDiagnosticHandlerKeys()).isEmpty();
        assertThat(probe.orphanDiagnosticHandlerKeys()).isEmpty();
        assertThat(probe.methodDispatchReported()).isTrue();
        assertThat(probe.methodDispatchComplete()).isTrue();
        assertThat(probe.registeredMethodCount()).isEqualTo(WayangA2aJsonRpcMethods.methods().size());
        assertThat(probe.dispatchMethodCount()).isEqualTo(WayangA2aJsonRpcMethods.methods().size());
        assertThat(probe.registeredMethods()).containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(probe.dispatchMethods()).containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(probe.missingDispatchMethods()).isEmpty();
        assertThat(probe.orphanDispatchMethods()).isEmpty();
        assertThat(probe.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(probe.toMap())
                .containsEntry("passed", true)
                .containsEntry("methodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("methodDispatchComplete", true);
    }

    @Test
    void exposesMethodDispatchCoverageThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = adapter(new InMemoryWayangA2aTaskStore());

        Map<String, Object> coverage = adapter.dispatchCoverage();

        assertThat(adapter.methodDispatchCoverage().complete()).isTrue();
        assertThat(coverage)
                .containsEntry("complete", true)
                .containsEntry("registeredMethodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("dispatchMethodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(list(coverage.get("registeredMethods")))
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(list(coverage.get("dispatchMethods")))
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(list(coverage.get("missingDispatchMethods"))).isEmpty();
        assertThat(list(coverage.get("orphanDispatchMethods"))).isEmpty();
    }

    @Test
    void servesRouteCatalogThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = adapter(new InMemoryWayangA2aTaskStore());

        WayangA2aHttpResponse response = adapter.dispatch(new WayangA2aHttpRequest(
                "GET",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of()));
        Map<String, Object> body = WayangA2aHttpJson.read(response.body());
        List<Map<String, Object>> routes = maps(body.get("routes"));
        WayangA2aHttpResponse options = adapter.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH,
                "",
                Map.of(),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(adapter.routeCatalogResponse().statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(body)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("routeCount", 8)
                .containsEntry("enabledRouteCount", 8);
        assertThat(routes)
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC)
                        .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ENDPOINT_PATH)
                        .containsEntry("httpMethod", "POST")
                        .containsEntry("requestBodyRequired", true))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation",
                                WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                        .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH)
                        .containsEntry("allow", "GET, OPTIONS"))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation",
                                WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS)
                        .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH)
                        .containsEntry("allow", "GET, OPTIONS"))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation",
                                WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE)
                        .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH)
                        .containsEntry("allow", "GET, OPTIONS"));
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(options.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2aHttpJson.read(options.body()))
                .containsEntry("operation", WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH);

        WayangA2aJsonRpcRouteCatalogProbeResult probe = adapter.routeCatalogProbe();
        assertThat(probe.statusCode()).isEqualTo(200);
        assertThat(probe.passed()).isTrue();
        assertThat(probe.complete()).isTrue();
        assertThat(probe.issueCount()).isZero();
        assertThat(probe.routeCount()).isEqualTo(8);
        assertThat(probe.routeCatalogDescriptor()).isTrue();
        assertThat(probe.diagnosticsReportDescriptor()).isTrue();
        assertThat(probe.specComplianceReportDescriptor()).isTrue();
    }

    @Test
    void probesReadinessThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = smokeAdapter("readiness");

        WayangA2aJsonRpcReadinessProbeResult readiness = adapter.readinessProbe();

        assertThat(readiness.bindingReportPassed()).isTrue();
        assertThat(readiness.routeCatalogRequired()).isTrue();
        assertThat(readiness.routeCatalogPassed()).isTrue();
        assertThat(readiness.smokeRequired()).isTrue();
        assertThat(readiness.smokePassed()).isTrue();
        assertThat(readiness.passed()).isTrue();
        assertThat(readiness.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        assertThat(readiness.issueCount()).isZero();
        assertThat(readiness.issues()).isEmpty();
        assertThat(readiness.bindingReportProbe().methodCount())
                .isEqualTo(WayangA2aJsonRpcMethods.methods().size());
        assertThat(readiness.routeCatalogProbe().passed()).isTrue();
        assertThat(readiness.smokeProbe().summary().scenarioId()).isEqualTo("a2a.jsonrpc.smoke");
        assertThat(readiness.toMap())
                .containsEntry("passed", true)
                .containsEntry("routeCatalogRequired", true)
                .containsEntry("routeCatalogPassed", true)
                .containsEntry("smokeRequired", true)
                .containsEntry("smokePassed", true);
    }

    @Test
    void servesReadinessThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = smokeAdapter("readiness-endpoint");

        WayangA2aHttpResponse response = adapter.dispatch(new WayangA2aHttpRequest(
                "GET",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_PATH,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of()));
        Map<String, Object> body = WayangA2aHttpJson.read(response.body());
        WayangA2aJsonRpcReadinessProbeResult decoded = WayangA2aJsonRpcReadinessProbeResult.from(response);
        WayangA2aHttpResponse options = adapter.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_PATH,
                "",
                Map.of(),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(smokeAdapter("readiness-response").readinessResponse().statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(body)
                .containsEntry("passed", true)
                .containsEntry("routeCatalogRequired", true)
                .containsEntry("routeCatalogPassed", true)
                .containsEntry("smokeRequired", true)
                .containsEntry("smokePassed", true);
        assertThat(map(body.get("methodDispatch")))
                .containsEntry("reported", true)
                .containsEntry("complete", true)
                .containsEntry("passed", true)
                .containsEntry("registeredMethodCount", adapter.methodDispatchCoverage().registeredMethodCount())
                .containsEntry("dispatchMethodCount", adapter.methodDispatchCoverage().dispatchMethodCount());
        assertThat(map(body.get("bindingReportProbe")))
                .containsEntry("readinessPath", WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_PATH);
        assertThat(decoded.passed()).isTrue();
        assertThat(decoded.routeCatalogPassed()).isTrue();
        assertThat(decoded.smokeRequired()).isTrue();
        assertThat(decoded.smokeProbe().summary().scenarioId()).isEqualTo("a2a.jsonrpc.smoke");
        assertThat(decoded.smokeProbe().summary().successfulExit()).isTrue();
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(options.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2aHttpJson.read(options.body()))
                .containsEntry("operation", WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_PATH);
    }

    @Test
    void servesReadinessIssueSummaryThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = smokeAdapter("readiness-issue-summary");

        WayangA2aHttpResponse response = adapter.dispatch(new WayangA2aHttpRequest(
                "GET",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_ISSUE_SUMMARY_PATH,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of()));
        WayangA2aJsonRpcReadinessIssueSummary summary =
                WayangA2aJsonRpcReadinessIssueSummary.from(response);
        WayangA2aHttpResponse options = adapter.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_ISSUE_SUMMARY_PATH,
                "",
                Map.of(),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(summary.passed()).isTrue();
        assertThat(summary.issueCount()).isZero();
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(options.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2aHttpJson.read(options.body()))
                .containsEntry("operation",
                        WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_ISSUE_SUMMARY_PATH);
    }

    @Test
    void treatsDisabledSmokeAsReadyWhenBindingReportPasses() {
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.configured(
                dispatcher(new InMemoryWayangA2aTaskStore()),
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokeEnabled(false)
                        .build());

        WayangA2aJsonRpcReadinessProbeResult readiness = adapter.readinessProbe();
        WayangA2aJsonRpcReadinessIssueSummary summary = adapter.readinessIssueSummary();
        WayangA2aHttpResponse summaryResponse = adapter.readinessIssueSummaryResponse();

        assertThat(readiness.bindingReportPassed()).isTrue();
        assertThat(readiness.routeCatalogRequired()).isTrue();
        assertThat(readiness.routeCatalogPassed()).isTrue();
        assertThat(readiness.smokeRequired()).isFalse();
        assertThat(readiness.smokePassed()).isTrue();
        assertThat(readiness.passed()).isTrue();
        assertThat(readiness.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        assertThat(readiness.issueCount()).isZero();
        assertThat(readiness.smokeProbe().statusCode()).isZero();
        assertThat(summary.passed()).isTrue();
        assertThat(summary.issueCount()).isZero();
        assertThat(summaryResponse.statusCode()).isEqualTo(200);
        assertThat(summaryResponse.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION);
        assertThat(WayangA2aJsonRpcReadinessIssueSummary.from(summaryResponse).issueCount()).isZero();
    }

    @Test
    void reportsAggregateDiagnosticsWhenReady() {
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.configured(
                dispatcher(new InMemoryWayangA2aTaskStore()),
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokeEnabled(false)
                        .build());

        WayangA2aJsonRpcDiagnosticsReport report = adapter.diagnosticsReport();
        List<Map<String, Object>> checks = maps(report.toMap().get("checks"));
        Map<String, Object> attributes = map(report.toMap().get("attributes"));

        assertThat(report.passed()).isTrue();
        assertThat(report.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        assertThat(report.issueCount()).isZero();
        assertThat(report.routeCatalogPassed()).isTrue();
        assertThat(report.bindingReportPassed()).isTrue();
        assertThat(report.smokeRequired()).isFalse();
        assertThat(map(attributes.get("methodDispatch")))
                .containsEntry("reported", true)
                .containsEntry("complete", true)
                .containsEntry("passed", true);
        assertThat(map(attributes.get("methodRegistry")))
                .containsEntry("reported", true)
                .containsEntry("groupCount", 3)
                .containsEntry("overridePolicy", WayangA2aJsonRpcMethodHandlerOverridePolicy.ALLOW_REPLACE.name());
        assertThat(map(attributes.get("specAlignment")))
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 20)
                .containsEntry("gapCount", 0)
                .containsEntry("gapCategories", List.of());
        assertThat(map(map(attributes.get("specAlignment")).get("standard")))
                .containsEntry("standardId", "a2a")
                .containsEntry("version", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC);
        assertThat(maps(map(attributes.get("specAlignment")).get("categorySummaries")))
                .extracting(summary -> summary.get("category"))
                .containsExactly("protocol", "binding", "agent_card", "route", "jsonrpc");
        assertThat(checks)
                .extracting(check -> check.get("probe"))
                .containsExactly(
                        "bindingReport",
                        "routeCatalog",
                        "smoke",
                        "methodDispatch",
                        "methodRegistry",
                        "specAlignment",
                        "specAlignment:protocol",
                        "specAlignment:binding",
                        "specAlignment:agent_card",
                        "specAlignment:route",
                        "specAlignment:jsonrpc",
                        "readiness");
        assertThat(checks)
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "smoke")
                        .containsEntry("required", false)
                        .containsEntry("passed", true))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "methodDispatch")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry("issueCount", 0))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "methodRegistry")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry("issueCount", 0))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry("issueCount", 0))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment:route")
                        .containsEntry("category", "route")
                        .containsEntry("passed", true)
                        .containsEntry("gapIds", List.of()));
        assertThat(WayangA2aJsonRpcDiagnosticsReport.fromJson(report.toJson()).toMap())
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
    }

    @Test
    void servesDiagnosticsReportThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.configured(
                dispatcher(new InMemoryWayangA2aTaskStore()),
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokeEnabled(false)
                        .build());

        WayangA2aHttpResponse response = adapter.dispatch(new WayangA2aHttpRequest(
                "GET",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of()));
        WayangA2aJsonRpcDiagnosticsReport report = WayangA2aJsonRpcDiagnosticsReport.fromJson(response.body());
        WayangA2aHttpResponse options = adapter.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH,
                "",
                Map.of(),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(adapter.diagnosticsReportResponse().statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(report.passed()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.routeCatalogPassed()).isTrue();
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(options.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2aHttpJson.read(options.body()))
                .containsEntry("operation", WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH);
    }

    @Test
    void servesSpecComplianceReportThroughHttpAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.configured(
                dispatcher(new InMemoryWayangA2aTaskStore()),
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokeEnabled(false)
                        .build());

        WayangA2aHttpResponse response = adapter.dispatch(new WayangA2aHttpRequest(
                "GET",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of()));
        WayangA2aJsonRpcSpecComplianceReport report =
                WayangA2aJsonRpcSpecComplianceReport.fromJson(response.body());
        WayangA2aHttpResponse options = adapter.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH,
                "",
                Map.of(),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(adapter.specComplianceReportResponse().statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(report.passed()).isTrue();
        assertThat(report.operationCount()).isEqualTo(11);
        assertThat(report.endpointPublished()).isTrue();
        assertThat(map(report.attributes().get("specAlignment")))
                .containsEntry("aligned", true)
                .containsEntry("gapCount", 0)
                .containsEntry("gapCategories", List.of());
        assertThat(map(map(report.attributes().get("specAlignment")).get("standard")))
                .containsEntry("standardId", "a2a")
                .containsEntry("version", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC);
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(options.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2aHttpJson.read(options.body()))
                .containsEntry("operation", WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH);
    }

    @Test
    void failsReadinessWhenBindingReportProbeFails() {
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.configured(
                dispatcher(new InMemoryWayangA2aTaskStore()),
                WayangA2aJsonRpcHttpConfig.builder()
                        .bindingReportEnabled(false)
                        .build());

        WayangA2aJsonRpcReadinessProbeResult readiness = adapter.readinessProbe();
        WayangA2aJsonRpcReadinessIssueSummary summary = adapter.readinessIssueSummary();
        WayangA2aHttpResponse summaryResponse = adapter.readinessIssueSummaryResponse();

        assertThat(readiness.bindingReportPassed()).isFalse();
        assertThat(readiness.routeCatalogRequired()).isTrue();
        assertThat(readiness.routeCatalogPassed()).isTrue();
        assertThat(readiness.passed()).isFalse();
        assertThat(readiness.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(readiness.issues()).singleElement().satisfies(issue ->
                assertThat(issue)
                        .containsEntry("code", "binding_report_probe_failed")
                        .containsEntry("statusCode", 404));
        assertThat(summary.passed()).isFalse();
        assertThat(summary.issueCount()).isEqualTo(13);
        assertThat(summary.readinessIssueCount()).isEqualTo(1);
        assertThat(summary.bindingReportIssueCount()).isEqualTo(12);
        assertThat(summary.routeCatalogIssueCount()).isZero();
        assertThat(summary.issues())
                .extracting(issue -> issue.get("probe"))
                .contains("readiness", "bindingReport");
        assertThat(summaryResponse.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY);
        assertThat(WayangA2aJsonRpcReadinessIssueSummary.from(summaryResponse).issueCount()).isEqualTo(13);

        WayangA2aJsonRpcDiagnosticsReport report = adapter.diagnosticsReport();
        assertThat(report.passed()).isFalse();
        assertThat(report.issueCount()).isEqualTo(13);
        assertThat(report.bindingReportPassed()).isFalse();
        assertThat(report.routeCatalogPassed()).isTrue();
        assertThat(report.issues())
                .extracting(issue -> issue.get("probe"))
                .contains("readiness", "bindingReport");
        assertThat(maps(report.toMap().get("checks")))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "bindingReport")
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 12))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "routeCatalog")
                        .containsEntry("passed", true)
                        .containsEntry("issueCount", 0));
    }

    @Test
    void handlesOptionsAndHttpValidationErrors() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcHttpAdapter adapter = adapter(store);
        WayangA2aJsonRpcRequest streamingRequest = WayangA2aJsonRpcRequest.of(
                "stream-rejected",
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                WayangA2aSendMessageServiceTest.request(
                        "message-http-jsonrpc-rejected",
                        "context-http-jsonrpc-rejected",
                        "task-http-jsonrpc-rejected",
                        "ping").toMap());

        WayangA2aHttpResponse options = adapter.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                "/",
                "",
                Map.of(),
                Map.of()));
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(options.body()))
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("operation", WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC);
        assertThat(options.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");

        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/"))))
                .containsEntry("code", "method_not_allowed");
        assertThat(error(adapter.dispatch(post("/", "", "application/json"))))
                .containsEntry("code", "missing_request_body");
        assertThat(error(adapter.dispatch(new WayangA2aHttpRequest(
                "POST",
                "/",
                streamingRequest.toJson(),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, "text/plain",
                        WayangA2aHttpResponse.HEADER_ACCEPT, A2aProtocol.EVENT_STREAM_MEDIA_TYPE),
                Map.of()))))
                .containsEntry("code", "unsupported_media_type");
        assertThat(error(adapter.dispatch(post("/", streamingRequest.toJson(), "application/json"))))
                .containsEntry("code", "not_acceptable");
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/missing"))))
                .containsEntry("code", "jsonrpc_path_not_found");
        assertThat(store.get("task-http-jsonrpc-rejected")).isEmpty();
    }

    @Test
    void honorsConfiguredPathsAndDisabledSmokeExposure() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher dispatcher = dispatcher(store);
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.fromMap(Map.ofEntries(
                Map.entry("endpointPath", "a2a/rpc"),
                Map.entry("smokePath", "internal/a2a/smoke"),
                Map.entry("smokeEnabled", "false"),
                Map.entry("routeCatalogPath", "internal/a2a/routes"),
                Map.entry("routeCatalogEnabled", "false"),
                Map.entry("diagnosticsReportPath", "internal/a2a/diagnostics"),
                Map.entry("diagnosticsReportEnabled", "false"),
                Map.entry("specComplianceReportPath", "internal/a2a/spec-compliance"),
                Map.entry("specComplianceReportEnabled", "false"),
                Map.entry("bindingReportPath", "internal/a2a/binding-report"),
                Map.entry("bindingReportEnabled", "false"),
                Map.entry("readinessPath", "internal/a2a/readiness"),
                Map.entry("readinessEnabled", "false"),
                Map.entry("readinessIssueSummaryPath", "internal/a2a/readiness/issues"),
                Map.entry("readinessIssueSummaryEnabled", "false")));
        WayangA2aJsonRpcSmokeRunner runner = WayangA2aJsonRpcSmokeRunner.of(
                dispatcher,
                WayangA2aSendMessageServiceTest.request(
                        "message-http-jsonrpc-disabled-smoke",
                        "context-http-jsonrpc-disabled-smoke",
                        "task-http-jsonrpc-disabled-smoke",
                        "ping"));
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.withSmoke(dispatcher, runner, config);
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-http-jsonrpc-custom-path",
                "context-http-jsonrpc-custom-path",
                "task-http-jsonrpc-custom-path",
                "ping");
        WayangA2aJsonRpcRequest rpcRequest = WayangA2aJsonRpcRequest.of(
                "custom-path",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                sendRequest.toMap());

        WayangA2aHttpResponse response = adapter.dispatch(post("/a2a/rpc", rpcRequest.toJson(), "application/json"));

        assertThat(adapter.endpointPath()).isEqualTo("/a2a/rpc");
        assertThat(adapter.smokePath()).isEqualTo("/internal/a2a/smoke");
        assertThat(adapter.routeCatalogPath()).isEqualTo("/internal/a2a/routes");
        assertThat(adapter.diagnosticsReportPath()).isEqualTo("/internal/a2a/diagnostics");
        assertThat(adapter.specComplianceReportPath()).isEqualTo("/internal/a2a/spec-compliance");
        assertThat(adapter.bindingReportPath()).isEqualTo("/internal/a2a/binding-report");
        assertThat(adapter.readinessPath()).isEqualTo("/internal/a2a/readiness");
        assertThat(adapter.readinessIssueSummaryPath()).isEqualTo("/internal/a2a/readiness/issues");
        assertThat(adapter.config().toMap())
                .containsEntry("endpointPath", "/a2a/rpc")
                .containsEntry("smokePath", "/internal/a2a/smoke")
                .containsEntry("smokeEnabled", false)
                .containsEntry("routeCatalogPath", "/internal/a2a/routes")
                .containsEntry("routeCatalogEnabled", false)
                .containsEntry("diagnosticsReportPath", "/internal/a2a/diagnostics")
                .containsEntry("diagnosticsReportEnabled", false)
                .containsEntry("specComplianceReportPath", "/internal/a2a/spec-compliance")
                .containsEntry("specComplianceReportEnabled", false)
                .containsEntry("bindingReportPath", "/internal/a2a/binding-report")
                .containsEntry("bindingReportEnabled", false)
                .containsEntry("readinessPath", "/internal/a2a/readiness")
                .containsEntry("readinessEnabled", false)
                .containsEntry("readinessIssueSummaryPath", "/internal/a2a/readiness/issues")
                .containsEntry("readinessIssueSummaryEnabled", false);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(status(resultTask(response)))
                .containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/internal/a2a/smoke"))))
                .containsEntry("code", "jsonrpc_path_not_found");
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/internal/a2a/routes"))))
                .containsEntry("code", "jsonrpc_path_not_found");
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/internal/a2a/diagnostics"))))
                .containsEntry("code", "jsonrpc_path_not_found");
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/internal/a2a/spec-compliance"))))
                .containsEntry("code", "jsonrpc_path_not_found");
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/internal/a2a/binding-report"))))
                .containsEntry("code", "jsonrpc_path_not_found");
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/internal/a2a/readiness"))))
                .containsEntry("code", "jsonrpc_path_not_found");
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/internal/a2a/readiness/issues"))))
                .containsEntry("code", "jsonrpc_path_not_found");
        assertThat(error(adapter.dispatch(WayangA2aHttpRequest.get("/"))))
                .containsEntry("code", "jsonrpc_path_not_found");
    }

    private static WayangA2aJsonRpcHttpAdapter adapter(InMemoryWayangA2aTaskStore store) {
        return WayangA2aJsonRpcHttpAdapter.of(dispatcher(store));
    }

    private static WayangA2aJsonRpcHttpAdapter smokeAdapter(String suffix) {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher dispatcher = dispatcher(store);
        WayangA2aJsonRpcSmokeRunner runner = WayangA2aJsonRpcSmokeRunner.of(
                dispatcher,
                WayangA2aSendMessageServiceTest.request(
                        "message-http-jsonrpc-smoke-" + suffix,
                        "context-http-jsonrpc-smoke-" + suffix,
                        "task-http-jsonrpc-smoke-" + suffix,
                        "ping"));
        return WayangA2aJsonRpcHttpAdapter.withSmoke(dispatcher, runner);
    }

    private static WayangA2aJsonRpcDispatcher dispatcher(InMemoryWayangA2aTaskStore store) {
        return dispatcher(store, card());
    }

    private static WayangA2aJsonRpcDispatcher dispatcher(
            InMemoryWayangA2aTaskStore store,
            A2aAgentCard card) {
        return WayangA2aJsonRpcDispatcher.forExecution(
                card,
                store,
                request -> AgentResponse.builder()
                        .runId("run-http-jsonrpc")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
    }

    private static WayangA2aHttpRequest post(String path, String body, String accept) {
        return new WayangA2aHttpRequest(
                "POST",
                path,
                body,
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_ACCEPT, accept),
                Map.of());
    }

    private static WayangA2aHttpRequest post(String path, String body, String accept, String a2aVersion) {
        return new WayangA2aHttpRequest(
                "POST",
                path,
                body,
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_ACCEPT, accept,
                        A2aProtocol.HEADER_VERSION, a2aVersion),
                Map.of());
    }

    private static Map<String, Object> resultTask(WayangA2aHttpResponse response) {
        return map(map(WayangA2aHttpJson.read(response.body()).get("result")).get("task"));
    }

    private static Map<String, Object> status(Map<String, Object> task) {
        return map(task.get("status"));
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        return map(WayangA2aHttpJson.read(response.body()).get("error"));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }

    private static List<String> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return WayangA2aMaps.stringList(value);
    }

    private static List<Map<String, Object>> maps(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return ((List<?>) value).stream()
                .map(entry -> {
                    assertThat(entry).isInstanceOf(Map.class);
                    return WayangA2aMaps.copyMap((Map<?, ?>) entry);
                })
                .toList();
    }

    private static A2aAgentCard card() {
        return new A2aAgentCard(
                "Wayang",
                "A2A JSON-RPC endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(true, true, List.of(), true),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }

    private static A2aAgentCard cardRequiringExtension() {
        return new A2aAgentCard(
                "Wayang",
                "A2A JSON-RPC endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(
                        false,
                        false,
                        List.of(new A2aAgentExtension(
                                REQUIRED_EXTENSION,
                                "Requires explicit client support",
                                true,
                                Map.of())),
                        true),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }
}
