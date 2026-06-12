package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpPublicationTest {

    @Test
    void publishesEnabledRoutesForFrameworkRegistration() {
        WayangA2aJsonRpcHttpAdapter adapter = configuredAdapter(WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokeEnabled(false)
                .diagnosticsReportPath("/ops/a2a/diagnostics")
                .readinessEnabled(false)
                .build());

        WayangA2aJsonRpcHttpPublication publication = adapter.routePublication();
        Map<String, Object> publicationMap = publication.toMap();
        String publicationJson = WayangA2aHttpJson.write(publicationMap);
        List<Map<String, Object>> routes = maps(publicationMap.get("routes"));

        assertThat(publication.routeCount()).isEqualTo(8);
        assertThat(publication.enabledRouteCount()).isEqualTo(6);
        assertThat(publication.publishedRouteCount()).isEqualTo(6);
        assertThat(publicationJson).startsWith("{\"routeCount\":");
        assertThat(publicationJson.indexOf("\"routes\""))
                .isGreaterThan(publicationJson.indexOf("\"operations\""));
        assertThat(publication.operations())
                .containsExactly(
                        WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC,
                        WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
                        WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                        WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY);
        assertThat(publication.bindingForOperation(WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS))
                .get()
                .satisfies(binding -> assertThat(binding.toMap())
                        .containsEntry("path", "/ops/a2a/diagnostics")
                        .containsEntry("httpMethod", "GET")
                        .containsEntry("published", true));
        assertThat(publication.binding("GET", "/ops/a2a/diagnostics")).isPresent();
        assertThat(publication.binding("OPTIONS", "/ops/a2a/diagnostics")).isPresent();
        assertThat(publication.binding("GET", "/a2a/rpc")).isEmpty();
        assertThat(publication.bindingForPath(WayangA2aJsonRpcHttpAdapter.DEFAULT_SMOKE_PATH)).isEmpty();
        assertThat(publication.bindingForOperation(WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS))
                .isEmpty();
        assertThat(routes)
                .extracting(route -> route.get("operation"))
                .doesNotContain(
                        WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE,
                        WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS);
    }

    @Test
    void dispatchesPublishedRoutesThroughAdapter() {
        WayangA2aJsonRpcHttpAdapter adapter = configuredAdapter(WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokeEnabled(false)
                .build());
        WayangA2aJsonRpcHttpPublication publication = adapter.routePublication();

        WayangA2aHttpResponse diagnostics = publication.dispatch(getJson(
                WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH));
        WayangA2aHttpResponse diagnosticsOptions = publication.dispatch(new WayangA2aHttpRequest(
                "OPTIONS",
                WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH,
                "",
                Map.of(),
                Map.of()));
        WayangA2aHttpResponse wrongMethod = publication.dispatch(WayangA2aHttpRequest.get("/a2a/rpc"));
        WayangA2aHttpResponse missing = publication.dispatch(WayangA2aHttpRequest.get("/missing"));

        assertThat(diagnostics.statusCode()).isEqualTo(200);
        assertThat(diagnostics.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2aJsonRpcDiagnosticsReport.fromJson(diagnostics.body()).passed()).isTrue();
        assertThat(diagnosticsOptions.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(diagnosticsOptions.body()))
                .containsEntry("operation", WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS);
        assertThat(wrongMethod.statusCode()).isEqualTo(405);
        assertThat(errorCode(wrongMethod)).isEqualTo("method_not_allowed");
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(errorCode(missing)).isEqualTo("jsonrpc_route_not_published");
    }

    private static WayangA2aHttpRequest getJson(String path) {
        return new WayangA2aHttpRequest(
                "GET",
                path,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of());
    }

    private static WayangA2aJsonRpcHttpAdapter configuredAdapter(WayangA2aJsonRpcHttpConfig config) {
        return WayangA2aJsonRpcHttpAdapter.configured(
                WayangA2aJsonRpcDispatcher.forExecution(
                        card(),
                        new InMemoryWayangA2aTaskStore(),
                        request -> AgentResponse.builder()
                                .runId("run-jsonrpc-publication")
                                .requestId(request.requestId())
                                .answer("pong")
                                .strategy("react")
                                .build()),
                config);
    }

    @SuppressWarnings("unchecked")
    private static String errorCode(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return String.valueOf(((Map<String, Object>) payload.get("error")).get("code"));
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
        return A2aAgentCard.minimal(
                "Wayang",
                "A2A JSON-RPC publication endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }
}
