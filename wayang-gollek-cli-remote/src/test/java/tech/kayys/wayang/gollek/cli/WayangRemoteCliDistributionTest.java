package tech.kayys.wayang.gollek.cli;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangCommandDiscoveryService;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchCatalog;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandDiscovery;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WayangRemoteCliDistributionTest {

    private HttpServer server;

    @AfterEach
    void stopServerAfterTest() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void discoversRemoteProviderFromDistributionClasspath() throws Exception {
        startServer();
        TestConsole console = new TestConsole();

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "status");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("Wayang remote")
                .contains("Remote Wayang API reached");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void submitsRemoteRunFromDistributionClasspath() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        startServer(body);
        TestConsole console = new TestConsole();

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "--default-tenant",
                "tenant-r",
                "--default-model",
                "model-r",
                "run",
                "draft a workflow",
                "--workflow",
                "planner",
                "--surface",
                "assistant-agent",
                "--workspace",
                "/repo",
                "--harness");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("wayang-remote-api")
                .contains("sdkMode=remote")
                .contains("tenant-r")
                .contains("model-r")
                .contains("surface=assistant-agent");
        assertThat(body.get())
                .contains("\"tenantId\":\"tenant-r\"")
                .contains("\"modelId\":\"model-r\"")
                .contains("\"workflowId\":\"planner\"")
                .contains("\"surfaceId\":\"assistant-agent\"")
                .contains("\"workspacePath\":\"/repo\"")
                .contains("\"workspaceEnabled\":true")
                .contains("\"harnessEnabled\":true");
    }

    @Test
    void rendersRemoteSafeCommandDiscoveryFromDistributionClasspath() throws Exception {
        startServer();
        TestConsole console = new TestConsole();
        WorkbenchCommandDiscovery expected = WayangCommandDiscoveryService.create().commandDiscovery(
                WayangWorkbenchCatalog.remoteCommands(),
                WorkbenchCommandQuery.of("assistant-agent", "Runs", null));

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "commands",
                "--surface",
                "assistant-agent",
                "--category",
                "Runs",
                "--index",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("\"product\":\"Wayang\"")
                .contains("\"surfaceId\":\"assistant-agent\"")
                .contains("\"profileId\":null")
                .contains("\"resolvedSurfaceId\":\"assistant-agent\"")
                .contains("\"category\":\"Runs\"")
                .contains("\"totalCommands\":" + expected.totalCommands())
                .contains("\"matchingCommands\":" + expected.matchingCommands())
                .contains("\"categorySummaries\":[{\"name\":\"Runs\",\"count\":" + expected.matchingCommands())
                .contains("\"run-result-json\"")
                .contains("\"run-preflight-json\"")
                .contains("\"run-profile\"")
                .contains("\"run-session-context\"")
                .contains("\"run-inspect-json\"")
                .contains("\"run-events-json\"")
                .contains("\"run-events-filter-json\"")
                .contains("\"run-events-cursor-json\"")
                .contains("\"run-events-follow-json\"")
                .contains("\"run-events-follow-result-json\"")
                .contains("\"run-events-follow-result-only-json\"")
                .contains("\"run-events-follow-result-only-stats-json\"")
                .contains("\"run-events-stats-json\"")
                .contains("\"run-list-page-json\"")
                .contains("\"run-stats-json\"")
                .contains("\"run-list-filter-json\"")
                .contains("\"run-list-profile-json\"")
                .contains("\"run-stats-profile-json\"")
                .contains("\"run-wait-json\"")
                .contains("\"run-cancel-json\"")
                .doesNotContain("\"tui\"");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRemoteSkillDiscoveryFromDistributionClasspath() throws Exception {
        AtomicReference<String> skillsPath = new AtomicReference<>();
        startServer(new AtomicReference<>(), skillsPath);
        TestConsole console = new TestConsole();

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "skills",
                "list",
                "--surface",
                "assistant-agent",
                "--source",
                "remote-rag",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("\"totalSkills\":1")
                .contains("\"matchingSkills\":1")
                .contains("\"categoryCounts\":{\"Retrieval\":1}")
                .contains("\"categorySummaries\":[{\"name\":\"Retrieval\",\"count\":1,\"skillIds\":[\"remote.rag\"]}]")
                .contains("\"sourceCounts\":{\"remote-rag\":1}")
                .contains("\"sourceSummaries\":[{\"name\":\"remote-rag\",\"count\":1,\"skillIds\":[\"remote.rag\"]}]")
                .contains("\"skillIds\":[\"remote.rag\"]")
                .contains("\"id\":\"remote.rag\"")
                .contains("\"source\":\"remote-rag\"")
                .contains("\"aliases\":[\"remote-rag\"]")
                .contains("\"metadata\":{\"endpoint\":\"remote-cli\",\"priority\":3}");
        assertThat(skillsPath.get())
                .startsWith("/skills?")
                .contains("surfaceId=assistant-agent")
                .contains("source=remote-rag");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRemoteSkillInspectFromDistributionClasspath() throws Exception {
        startServer();
        TestConsole console = new TestConsole();

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "skills",
                "inspect",
                "remote-rag",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("\"skillId\":\"remote.rag\"")
                .contains("\"id\":\"remote.rag\"")
                .contains("\"source\":\"remote-rag\"")
                .contains("\"aliases\":[\"remote-rag\"]");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRemoteRunEventsFromDistributionClasspath() throws Exception {
        startServer();
        TestConsole console = new TestConsole();

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "run",
                "events",
                "remote-cli-test",
                "--state",
                "completed",
                "--type",
                "run.completed",
                "--after-sequence",
                "3",
                "--limit",
                "7",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("\"runId\":\"remote-cli-test\"")
                .contains("\"query\":{\"state\":\"COMPLETED\",\"type\":\"run.completed\",\"afterSequence\":3,\"limit\":7,\"filtered\":true}")
                .contains("\"totalEvents\":1")
                .contains("\"returnedEvents\":1")
                .contains("\"lastSequence\":4")
                .contains("\"nextAfterSequence\":4")
                .contains("\"stateCounts\":{\"completed\":1}")
                .contains("\"typeCounts\":{\"run.completed\":1}")
                .contains("\"type\":\"run.completed\"")
                .contains("\"state\":\"COMPLETED\"");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRemoteRunInspectionFromDistributionClasspath() throws Exception {
        startServer();
        TestConsole console = new TestConsole();

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "run",
                "inspect",
                "remote-cli-test",
                "--state",
                "completed",
                "--limit",
                "7",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("\"runId\":\"remote-cli-test\"")
                .contains("\"known\":true")
                .contains("\"status\":{\"handle\":{\"runId\":\"remote-cli-test\",\"state\":\"COMPLETED\"")
                .contains("\"events\":{\"contract\":{\"schema\":\"wayang.run.lifecycle\",\"version\":1,\"envelope\":\"run-events\"},\"runId\":\"remote-cli-test\"")
                .contains("\"query\":{\"state\":\"COMPLETED\",\"type\":null,\"afterSequence\":0,\"limit\":7,\"filtered\":true}")
                .contains("\"nextAfterSequence\":4")
                .contains("\"stateCounts\":{\"completed\":1}");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void waitsRemoteRunFromDistributionClasspath() throws Exception {
        startServer();
        TestConsole console = new TestConsole();

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "run",
                "wait",
                "remote-cli-test",
                "--timeout-seconds",
                "0",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("\"runId\":\"remote-cli-test\"")
                .contains("\"terminal\":true")
                .contains("\"timedOut\":false")
                .contains("\"state\":\"COMPLETED\"");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void cancelsRemoteRunFromDistributionClasspath() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        startServer(body);
        TestConsole console = new TestConsole();

        int exitCode = WayangGollekCli.execute(
                null,
                console.outStream(),
                console.errStream(),
                "--sdk-mode",
                "REMOTE",
                "--endpoint",
                endpoint(),
                "run",
                "cancel",
                "remote-cli-test",
                "--reason",
                "user stop",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(console.out())
                .contains("\"runId\":\"remote-cli-test\"")
                .contains("\"cancelled\":true")
                .contains("\"state\":\"CANCELLED\"");
        assertThat(body.get()).contains("\"reason\":\"user stop\"");
        assertThat(console.err()).isEmpty();
    }

    private void startServer() throws IOException {
        startServer(new AtomicReference<>());
    }

    private void startServer(AtomicReference<String> runBody) throws IOException {
        startServer(runBody, new AtomicReference<>());
    }

    private void startServer(AtomicReference<String> runBody, AtomicReference<String> skillsPath) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/status", exchange -> respond(exchange, 200, "{}"));
        server.createContext("/skills", exchange -> {
            skillsPath.set(exchange.getRequestURI().toString());
            respond(exchange, 200, "{\"totalSkills\":1,\"skills\":[" + remoteSkillJson() + "]}");
        });
        server.createContext("/runs", exchange -> {
            if ("GET".equals(exchange.getRequestMethod()) && exchange.getRequestURI().getPath().endsWith("/status")) {
                respond(exchange, 200, """
                        {"runId":"remote-cli-test","state":"COMPLETED","strategy":"remote-status","known":true,"message":"done"}
                        """);
                return;
            }
            if ("GET".equals(exchange.getRequestMethod()) && exchange.getRequestURI().getPath().endsWith("/events")) {
                respond(exchange, 200, """
                        {"runId":"remote-cli-test","totalEvents":1,"message":"remote events","events":[{"runId":"remote-cli-test","sequence":4,"type":"run.completed","state":"COMPLETED","message":"done"}]}
                        """);
                return;
            }
            if (exchange.getRequestURI().getPath().endsWith("/cancel")) {
                runBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, 200, """
                        {"runId":"remote-cli-test","state":"CANCELLED","strategy":"remote-cancel","cancelled":true}
                        """);
                return;
            }
            runBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"runId\":\"remote-cli-test\"}");
        });
        server.start();
    }

    private static String remoteSkillJson() {
        return """
                {"id":"remote.rag","name":"Remote RAG","description":"Remote RAG capability.","category":"Retrieval","source":"remote-rag","version":"2.0.0","state":"ACTIVE","surfaceIds":["assistant-agent"],"inputKeys":["query"],"outputKeys":["citations"],"tags":["rag","remote"],"aliases":["remote-rag"],"metadata":{"endpoint":"remote-cli","priority":3}}
                """;
    }

    private String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static final class TestConsole {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();
        private final PrintStream outStream = new PrintStream(out);
        private final PrintStream errStream = new PrintStream(err);

        PrintStream outStream() {
            return outStream;
        }

        PrintStream errStream() {
            return errStream;
        }

        String out() {
            return out.toString();
        }

        String err() {
            return err.toString();
        }
    }
}
