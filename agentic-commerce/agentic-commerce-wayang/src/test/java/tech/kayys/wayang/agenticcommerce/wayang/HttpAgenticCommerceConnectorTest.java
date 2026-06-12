package tech.kayys.wayang.agenticcommerce.wayang;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutItem;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCreateCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;

import static org.assertj.core.api.Assertions.assertThat;

class HttpAgenticCommerceConnectorTest {

    @Test
    void serviceCreatesCheckoutSessionThroughHttpConnector() throws Exception {
        AtomicReference<RecordedRequest> recorded = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            recorded.set(record(exchange));
            respond(exchange, 201, """
                    {"id":"cs_http","status":"open","currency":"USD","line_items":[]}
                    """);
        });
        try {
            AgenticCommerceConnectorConfig config = AgenticCommerceConnectorConfig.bearer("seller-token")
                    .withBaseUrl(baseUrl(server))
                    .withHeaders(Map.of("X-Seller", "demo"));
            AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(
                    new HttpAgenticCommerceConnector(config),
                    config);

            AgenticCommerceCheckoutHttpResult result = service.create(new AgenticCommerceCreateCheckoutSessionRequest(
                    null,
                    List.of(new AgenticCommerceCheckoutItem("sku_1", "Wayang Hoodie", "", 1, 2500, Map.of())),
                    "usd",
                    null,
                    Map.of(),
                    List.of(),
                    Map.of(),
                    List.of(),
                    Map.of(),
                    "en-US",
                    "UTC",
                    "",
                    Map.of("source", "http-test")));

            assertThat(result.successful()).isTrue();
            assertThat(result.checkoutSession().id()).isEqualTo("cs_http");
            assertThat(result.response().attributes())
                    .containsEntry(AgenticCommerceWayang.METADATA_CONNECTOR, HttpAgenticCommerceConnector.CONNECTOR_NAME)
                    .containsEntry("connectorBaseUrl", baseUrl(server));
            assertThat(recorded.get()).isNotNull();
            assertThat(recorded.get().method()).isEqualTo("POST");
            assertThat(recorded.get().path()).isEqualTo(AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS);
            assertThat(recorded.get().headers().get(AgenticCommerceProtocol.HEADER_AUTHORIZATION))
                    .isEqualTo("Bearer seller-token");
            assertThat(recorded.get().headers().get(AgenticCommerceProtocol.HEADER_API_VERSION))
                    .isEqualTo(AgenticCommerceProtocol.SPEC_VERSION);
            assertThat(recorded.get().headers().get("X-Seller")).isEqualTo("demo");
            assertThat(AgenticCommerceJson.readObject(recorded.get().body()))
                    .containsEntry("currency", "USD");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void transportErrorsReturnProtocolJsonErrorResponse() {
        String baseUrl = "http://seller.example";
        HttpAgenticCommerceConnector connector = new HttpAgenticCommerceConnector(
                AgenticCommerceConnectorConfig.defaults()
                        .withBaseUrl(baseUrl)
                        .withAttributes(Map.of("timeoutMillis", 250)),
                new FailingHttpClient());

        AgenticCommerceHttpResponse response = connector.exchange(new AgenticCommerceHttpRequest(
                "GET",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSION.replace("{checkout_session_id}", "cs_missing"),
                "",
                Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req_transport"),
                Map.of("operation", AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION)));

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.contentType(AgenticCommerceProtocol.MIME_JSON)).isTrue();
        assertThat(response.requestId()).isEqualTo("req_transport");
        assertThat(response.attributes())
                .containsEntry(AgenticCommerceWayang.METADATA_CONNECTOR, HttpAgenticCommerceConnector.CONNECTOR_NAME)
                .containsEntry("connectorBaseUrl", baseUrl)
                .containsKey("transportError");
        assertThat(map(AgenticCommerceJson.readObject(response.body()).get("error")))
                .containsEntry("code", "seller_connector_transport_error");
    }

    private static HttpServer server(HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.setExecutor(command -> {
            Thread thread = new Thread(command, "agentic-commerce-http-test");
            thread.setDaemon(true);
            thread.start();
        });
        server.start();
        return server;
    }

    private static String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static RecordedRequest record(HttpExchange exchange) throws IOException {
        return new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                Map.of(
                        AgenticCommerceProtocol.HEADER_AUTHORIZATION,
                        value(exchange, AgenticCommerceProtocol.HEADER_AUTHORIZATION),
                        AgenticCommerceProtocol.HEADER_API_VERSION,
                        value(exchange, AgenticCommerceProtocol.HEADER_API_VERSION),
                        "X-Seller",
                        value(exchange, "X-Seller")));
    }

    private static String value(HttpExchange exchange, String header) {
        String value = exchange.getRequestHeaders().getFirst(header);
        return value == null ? "" : value;
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON);
        exchange.getResponseHeaders().add(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req_http");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private record RecordedRequest(
            String method,
            String path,
            String body,
            Map<String, String> headers) {
    }

    private static final class FailingHttpClient extends HttpClient {
        private final HttpClient delegate = HttpClient.newHttpClient();

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return delegate.cookieHandler();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return delegate.connectTimeout();
        }

        @Override
        public Redirect followRedirects() {
            return delegate.followRedirects();
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return delegate.proxy();
        }

        @Override
        public SSLContext sslContext() {
            return delegate.sslContext();
        }

        @Override
        public SSLParameters sslParameters() {
            return delegate.sslParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return delegate.authenticator();
        }

        @Override
        public Version version() {
            return delegate.version();
        }

        @Override
        public Optional<Executor> executor() {
            return delegate.executor();
        }

        @Override
        public <T> java.net.http.HttpResponse<T> send(
                java.net.http.HttpRequest request,
                BodyHandler<T> responseBodyHandler) throws IOException {
            throw new IOException("transport unavailable");
        }

        @Override
        public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
                java.net.http.HttpRequest request,
                BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.failedFuture(new IOException("transport unavailable"));
        }

        @Override
        public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
                java.net.http.HttpRequest request,
                BodyHandler<T> responseBodyHandler,
                PushPromiseHandler<T> pushPromiseHandler) {
            return CompletableFuture.failedFuture(new IOException("transport unavailable"));
        }
    }
}
