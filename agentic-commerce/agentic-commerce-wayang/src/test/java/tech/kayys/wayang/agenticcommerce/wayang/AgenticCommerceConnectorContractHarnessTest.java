package tech.kayys.wayang.agenticcommerce.wayang;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmoke;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutStatus;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceConnectorContractHarnessTest {

    private final AgenticCommerceConnectorContractHarness harness =
            AgenticCommerceConnectorContractHarness.checkoutLifecycle();

    @Test
    void inMemoryConnectorPassesCheckoutLifecycleContract() {
        AgenticCommerceConnectorContractReport report = harness.run(new InMemoryAgenticCommerceConnector());
        Map<String, Object> values = report.toMap();

        assertThat(report.passed()).isTrue();
        assertThat(report.exchangeCount()).isEqualTo(5);
        assertThat(report.issueCount()).isZero();
        assertThat(report.scenarioResult().exchanges())
                .extracting(exchange -> exchange.result().checkoutSession().status())
                .containsExactly(
                        AgenticCommerceCheckoutStatus.OPEN,
                        AgenticCommerceCheckoutStatus.OPEN,
                        AgenticCommerceCheckoutStatus.READY_FOR_PAYMENT,
                        AgenticCommerceCheckoutStatus.COMPLETED,
                        AgenticCommerceCheckoutStatus.CANCELED);
        assertThat(values)
                .containsEntry("passed", true)
                .containsEntry("connectorName", "InMemoryAgenticCommerceConnector")
                .containsEntry("scenarioId", "agentic-commerce-checkout-smoke")
                .containsEntry("expectationId", "agentic-commerce-checkout-smoke-expectation")
                .containsKeys("runtimeConfig", "scenarioResult", "expectationResult", "attributes");
    }

    @Test
    void runtimeConvenienceRunsContractAgainstActiveConnector() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();

        AgenticCommerceConnectorContractReport report = runtime.connectorContract();

        assertThat(report.passed()).isTrue();
        assertThat(report.connectorName()).isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY);
        assertThat(map(report.toMap().get("attributes")))
                .containsEntry("contractId", AgenticCommerceConnectorContractHarness.CONTRACT_CHECKOUT_LIFECYCLE)
                .containsEntry("connectorFactoryKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY);
    }

    @Test
    void httpConnectorPassesLifecycleContractAgainstLocalFakeSeller() throws Exception {
        HttpServer server = server(exchange -> respond(exchange, AgenticCommerceCheckoutHttpSmoke.responder()
                .respond(request(exchange))));
        try {
            AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.configured(
                    new HttpAgenticCommerceConnector(AgenticCommerceConnectorConfig.defaults().withBaseUrl(baseUrl(server))),
                    AgenticCommerceWayangRuntimeConfig.builder()
                            .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                                    "mode",
                                    "seller-http")))
                            .connectorConfig(AgenticCommerceConnectorConfig.defaults().withBaseUrl(baseUrl(server)))
                            .build());

            AgenticCommerceConnectorContractReport report = harness.run(runtime);

            assertThat(report.passed()).isTrue();
            assertThat(report.connectorName()).isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
            assertThat(report.scenarioResult().exchanges())
                    .extracting(exchange -> exchange.result().response().statusCode())
                    .containsExactly(201, 200, 200, 200, 200);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsContractFailuresWithoutThrowing() {
        AgenticCommerceConnector failing = request -> AgenticCommerceHttpResponse.json(500, "{}");

        AgenticCommerceConnectorContractReport report = harness.run(failing);

        assertThat(report.passed()).isFalse();
        assertThat(report.issueCount()).isGreaterThan(0);
        assertThat(report.expectationResult().valid()).isFalse();
    }

    private static HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.setExecutor(command -> {
            Thread thread = new Thread(command, "agentic-commerce-contract-http-test");
            thread.setDaemon(true);
            thread.start();
        });
        server.start();
        return server;
    }

    private static String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static AgenticCommerceHttpRequest request(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("protocol", "agentic-commerce");
        attributes.put("operation", operation(exchange.getRequestMethod(), path));
        sessionId(path).ifPresent(sessionId -> attributes.put("checkoutSessionId", sessionId));
        return new AgenticCommerceHttpRequest(
                exchange.getRequestMethod(),
                path,
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                headers(exchange),
                attributes);
    }

    private static Map<String, Object> headers(HttpExchange exchange) {
        Map<String, Object> values = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((key, value) -> values.put(key, String.join(", ", value)));
        return Map.copyOf(values);
    }

    private static String operation(String method, String path) {
        if ("POST".equals(method) && AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS.equals(path)) {
            return AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION;
        }
        if ("GET".equals(method) && path.startsWith(AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS + "/")) {
            return AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION;
        }
        if ("POST".equals(method) && path.endsWith("/complete")) {
            return AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION;
        }
        if ("POST".equals(method) && path.endsWith("/cancel")) {
            return AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION;
        }
        return AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION;
    }

    private static java.util.Optional<String> sessionId(String path) {
        String prefix = AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS + "/";
        if (!path.startsWith(prefix)) {
            return java.util.Optional.empty();
        }
        String id = path.substring(prefix.length())
                .replace("/complete", "")
                .replace("/cancel", "");
        return id.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(id);
    }

    private static void respond(HttpExchange exchange, AgenticCommerceHttpResponse response) throws IOException {
        response.headers().forEach((key, value) -> exchange.getResponseHeaders().add(key, String.valueOf(value)));
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.statusCode(), bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
