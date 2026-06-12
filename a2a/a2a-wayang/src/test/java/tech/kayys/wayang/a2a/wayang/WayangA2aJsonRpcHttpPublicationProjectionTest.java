package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpPublicationProjectionTest {

    @Test
    void keepsOrderedPublicationEnvelope() {
        WayangA2aJsonRpcHttpPublication publication = publication();

        Map<String, Object> values = WayangA2aJsonRpcHttpPublicationProjection.publication(publication);

        assertThat(values.keySet()).containsExactly(
                "routeCount",
                "enabledRouteCount",
                "publishedRouteCount",
                "operations",
                "routes");
        assertThat(values)
                .containsEntry("routeCount", 8)
                .containsEntry("enabledRouteCount", 6)
                .containsEntry("publishedRouteCount", 6);
        assertThat(WayangA2aMaps.stringList(values.get("operations")))
                .containsExactly(
                        WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC,
                        WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
                        WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                        WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY);
        assertThat(WayangA2aMaps.objectList(values.get("routes")))
                .hasSize(6)
                .allSatisfy(route -> assertThat(route).containsEntry("published", true));
    }

    @Test
    void keepsOrderedPublishedRouteBindingEnvelope() {
        WayangA2aJsonRpcHttpRouteBinding binding = publication()
                .bindingForOperation(WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS)
                .orElseThrow();

        Map<String, Object> values = WayangA2aJsonRpcHttpPublicationProjection.binding(binding);

        assertThat(values.keySet()).containsExactly(
                "operation",
                "enabled",
                "path",
                "httpMethod",
                "allowedMethods",
                "allow",
                "requestMediaType",
                "responseMediaTypes",
                "requestBodyRequired",
                "published");
        assertThat(values)
                .containsEntry("operation", WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS)
                .containsEntry("enabled", true)
                .containsEntry("path", "/ops/a2a/diagnostics")
                .containsEntry("httpMethod", "GET")
                .containsEntry("allow", "GET, OPTIONS")
                .containsEntry("published", true);
        assertThat(WayangA2aMaps.stringList(values.get("allowedMethods")))
                .containsExactly("GET", "OPTIONS");
        assertThat(binding.toMap()).isEqualTo(values);
    }

    @Test
    void publicationDelegatesToProjectionForMap() {
        WayangA2aJsonRpcHttpPublication publication = publication();

        assertThat(publication.toMap()).isEqualTo(WayangA2aJsonRpcHttpPublicationProjection.publication(publication));
    }

    private static WayangA2aJsonRpcHttpPublication publication() {
        return configuredAdapter(WayangA2aJsonRpcHttpConfig.builder()
                        .endpointPath("/a2a/rpc")
                        .smokeEnabled(false)
                        .diagnosticsReportPath("/ops/a2a/diagnostics")
                        .readinessEnabled(false)
                        .build())
                .routePublication();
    }

    private static WayangA2aJsonRpcHttpAdapter configuredAdapter(WayangA2aJsonRpcHttpConfig config) {
        return WayangA2aJsonRpcHttpAdapter.configured(
                WayangA2aJsonRpcDispatcher.forExecution(
                        card(),
                        new InMemoryWayangA2aTaskStore(),
                        request -> AgentResponse.builder()
                                .runId("run-jsonrpc-publication-projection")
                                .requestId(request.requestId())
                                .answer("pong")
                                .strategy("react")
                                .build()),
                config);
    }

    private static A2aAgentCard card() {
        return A2aAgentCard.minimal(
                "Wayang",
                "A2A JSON-RPC publication projection endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }
}
