package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangRuntimeTest {

    @Test
    void inMemoryRuntimeWiresServiceHttpAdapterAndDispatcherTogether() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();

        Map<String, Object> created = runtime.executeBySkillId(
                AgenticCommerceWayang.SKILL_CREATE_CHECKOUT,
                Map.of(
                        "items",
                        List.of(Map.of("id", "sku_1", "quantity", 1, "unit_amount", 1500)),
                        "currency",
                        "usd"))
                .await().indefinitely();
        AgenticCommerceHttpResponse retrieved = runtime.dispatch(new AgenticCommerceHttpRequest(
                "GET",
                "/agentic-commerce/checkout_sessions/cs_1",
                "",
                validHeaders(false),
                Map.of()));

        assertThat(created)
                .containsEntry("success", true)
                .containsEntry("checkout_session_id", "cs_1");
        assertThat(retrieved.statusCode()).isEqualTo(200);
        assertThat(AgenticCommerceJson.readObject(retrieved.body()))
                .containsEntry("id", "cs_1")
                .containsEntry("status", "open");
        assertThat(runtime.skillDispatcher().skillIds()).containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());
        assertThat(runtime.smokeProbe().passed()).isTrue();
        assertThat(runtime.bindingReport().routeCount()).isEqualTo(5);
    }

    @Test
    void configuredRuntimeKeepsConnectorAndHttpConfigVisible() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.configured(
                new InMemoryAgenticCommerceConnector(),
                AgenticCommerceConnectorConfig.bearer("seller-token")
                        .withBaseUrl("https://seller.example/")
                        .withAttributes(Map.of("tenant", "demo")),
                AgenticCommerceHttpAdapterConfig.builder()
                        .checkoutBasePath("/commerce/acp")
                        .smokePath("/internal/acp/smoke")
                        .bindingReportPath("/internal/acp/binding")
                        .build());

        Map<String, Object> values = runtime.toMap();
        Map<String, Object> connectorConfig = map(values.get("connectorConfig"));
        Map<String, Object> httpConfig = map(values.get("httpConfig"));
        Map<String, Object> connectorDiagnostics = map(values.get("connectorDiagnostics"));

        assertThat(runtime.connectorConfig().baseUrl()).isEqualTo("https://seller.example");
        assertThat(runtime.httpAdapter().checkoutBasePath()).isEqualTo("/commerce/acp");
        assertThat(connectorConfig)
                .containsEntry("apiVersion", AgenticCommerceProtocol.SPEC_VERSION)
                .containsEntry("bearerTokenConfigured", true)
                .containsEntry("attributeCount", 1);
        assertThat(httpConfig)
                .containsEntry("checkoutBasePath", "/commerce/acp")
                .containsEntry("smokePath", "/internal/acp/smoke")
                .containsEntry("bindingReportPath", "/internal/acp/binding");
        assertThat(map(values.get("checkoutSkillDispatcher"))).containsEntry("skillCount", 5);
        assertThat(connectorDiagnostics)
                .containsEntry("ready", true)
                .containsEntry("contractAvailable", false);
    }

    @Test
    void runtimeInstallsDefinitionsAndRuntimeSkillsIntoRegistry() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();
        AgenticCommerceTestSkillRegistry registry = new AgenticCommerceTestSkillRegistry();

        AgenticCommerceSkillRegistration registration = runtime.installSkills(registry);
        Map<String, Object> result = registry.findOrThrow(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT)
                .execute(Map.of(
                        "items",
                        List.of(Map.of("id", "sku_1", "quantity", 1, "unit_amount", 1000)),
                        "currency",
                        "usd"))
                .await().indefinitely();

        assertThat(registration.successful()).isTrue();
        assertThat(registry.listSkills()).hasSize(5);
        assertThat(registry.listAll()).hasSize(5);
        assertThat(result)
                .containsEntry("success", true)
                .containsEntry("checkout_session_id", "cs_1");
    }

    private static Map<String, Object> validHeaders(boolean bodyPresent) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(AgenticCommerceProtocol.HEADER_AUTHORIZATION, "Bearer token");
        headers.put(AgenticCommerceProtocol.HEADER_API_VERSION, AgenticCommerceProtocol.SPEC_VERSION);
        headers.put(AgenticCommerceProtocol.HEADER_ACCEPT, AgenticCommerceProtocol.MIME_JSON);
        headers.put(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req-runtime");
        if (bodyPresent) {
            headers.put(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON);
        }
        return Map.copyOf(headers);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
