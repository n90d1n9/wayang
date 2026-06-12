package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcSpecComplianceReportTest {

    @Test
    void standardSpecOperationsReflectA2aMethodMapping() {
        List<WayangA2aJsonRpcSpecOperation> operations =
                WayangA2aJsonRpcSpecOperation.standardOperations();
        String operationJson = WayangA2aHttpJson.write(operations.getFirst().toMap());

        assertThat(operations).hasSize(11);
        assertThat(operations)
                .extracting(WayangA2aJsonRpcSpecOperation::jsonRpcMethod)
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(operations).allSatisfy(operation -> assertThat(operation.supported()).isTrue());
        assertThat(operations)
                .filteredOn(WayangA2aJsonRpcSpecOperation::streaming)
                .extracting(WayangA2aJsonRpcSpecOperation::jsonRpcMethod)
                .containsExactly(
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK);
        assertThat(operations)
                .anySatisfy(operation -> assertThat(operation.toMap())
                        .containsEntry("operation", A2aProtocol.OPERATION_SEND_MESSAGE)
                        .containsEntry("jsonRpcMethod", WayangA2aJsonRpcMethods.SEND_MESSAGE)
                        .containsEntry("restMethod", "POST")
                        .containsEntry("restPath", "/message:send")
                        .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON))
                .anySatisfy(operation -> assertThat(operation.toMap())
                        .containsEntry("operation", A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE)
                        .containsEntry("restPath", "/message:stream")
                        .containsEntry("responseMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE))
                .anySatisfy(operation -> assertThat(operation.toMap())
                        .containsEntry("operation", A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD)
                        .containsEntry("restMethod", "GET")
                        .containsEntry("restPath", "/extendedAgentCard"));
        assertThat(operationJson).startsWith("{\"operation\":");
        assertThat(operationJson.indexOf("\"restPath\""))
                .isGreaterThan(operationJson.indexOf("\"restMethod\""));
        assertThat(operationJson.indexOf("\"description\""))
                .isGreaterThan(operationJson.indexOf("\"supported\""));
    }

    @Test
    void reportsAdapterComplianceAgainstPublishedEndpoint() {
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.configured(
                dispatcher(),
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokeEnabled(false)
                        .build());

        WayangA2aJsonRpcSpecComplianceReport report = adapter.specComplianceReport();
        String reportJson = report.toJson();
        List<Map<String, Object>> operations = maps(report.toMap().get("operations"));
        Map<String, Object> publication = map(report.toMap().get("publication"));
        Map<String, Object> attributes = map(report.toMap().get("attributes"));
        Map<String, Object> specAlignment = map(attributes.get("specAlignment"));

        assertThat(report.complianceId()).isEqualTo(WayangA2aJsonRpcSpecComplianceReport.COMPLIANCE_ID);
        assertThat(report.specUrl()).isEqualTo(WayangA2aJsonRpcSpecComplianceReport.SPEC_URL);
        assertThat(report.passed()).isTrue();
        assertThat(report.operationCount()).isEqualTo(11);
        assertThat(report.supportedOperationCount()).isEqualTo(11);
        assertThat(report.streamingOperationCount()).isEqualTo(2);
        assertThat(report.endpointPublished()).isTrue();
        assertThat(report.endpointPath()).isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_ENDPOINT_PATH);
        assertThat(report.issueCount()).isZero();
        assertThat(report.issues()).isEmpty();
        assertThat(operations).hasSize(11);
        assertThat(operations)
                .extracting(operation -> operation.get("supported"))
                .containsOnly(true);
        assertThat(publication)
                .containsEntry("routeCount", 8)
                .containsEntry("publishedRouteCount", 7);
        assertThat(specAlignment)
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 20)
                .containsEntry("gapCount", 0)
                .containsEntry("gapCategories", List.of());
        assertThat(map(specAlignment.get("standard")))
                .containsEntry("standardId", "a2a")
                .containsEntry("version", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC);
        assertThat(maps(specAlignment.get("categorySummaries")))
                .extracting(summary -> summary.get("category"))
                .containsExactly("protocol", "binding", "agent_card", "route", "jsonrpc");
        assertThat(WayangA2aJsonRpcSpecComplianceReport.fromJson(report.toJson()).toMap())
                .containsEntry("passed", true)
                .containsEntry("operationCount", 11)
                .containsEntry("supportedOperationCount", 11);
        assertThat(reportJson).startsWith("{\"complianceId\":");
        assertThat(reportJson.indexOf("\"issues\""))
                .isGreaterThan(reportJson.indexOf("\"issueCount\""));
        assertThat(reportJson.indexOf("\"attributes\""))
                .isGreaterThan(reportJson.indexOf("\"publication\""));
    }

    @Test
    void specAlignmentGapsFailSpecComplianceReport() {
        WayangA2aJsonRpcHttpAdapter adapter = WayangA2aJsonRpcHttpAdapter.configured(
                dispatcher(),
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokeEnabled(false)
                        .build());
        WayangA2aSpecAlignmentSnapshot specAlignment = new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                false,
                20,
                19,
                1,
                List.of("route.SendMessage"),
                List.of(new WayangA2aSpecAlignmentCategorySummary(
                        "route",
                        12,
                        11,
                        1,
                        List.of("route.SendMessage"))));

        WayangA2aJsonRpcSpecComplianceReport report =
                WayangA2aJsonRpcSpecComplianceReport.from(adapter.routePublication(), specAlignment);
        Map<String, Object> attributes = map(report.toMap().get("attributes"));

        assertThat(report.passed()).isFalse();
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues()).singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "specAlignment")
                        .containsEntry("code", "spec_alignment_gaps")
                        .containsEntry("field", "gapCount")
                        .containsEntry("expected", "0")
                        .containsEntry("actual", "1"));
        assertThat(map(attributes.get("specAlignment")))
                .containsEntry("aligned", false)
                .containsEntry("gapCount", 1)
                .containsEntry("gapIds", List.of("route.SendMessage"))
                .containsEntry("gapCategories", List.of("route"));
    }

    @Test
    void buildsEndpointPublicationIssueWithCanonicalShape() {
        assertThat(WayangA2aJsonRpcSpecComplianceReport.endpointNotPublishedIssue("/a2a/rpc"))
                .containsEntry("source", "publication")
                .containsEntry("code", "jsonrpc_endpoint_not_published")
                .containsEntry("field", "endpointPublished")
                .containsEntry("expected", "true")
                .containsEntry("actual", "false")
                .containsEntry("message", "A2A JSON-RPC endpoint /a2a/rpc is not published.");
    }

    @Test
    void buildsMethodMappingIssueWithCanonicalShape() {
        WayangA2aJsonRpcSpecOperation operation = new WayangA2aJsonRpcSpecOperation(
                "MissingOperation",
                "missing/method",
                "",
                "POST",
                "/missing",
                false,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                false,
                "Missing mapping");

        assertThat(WayangA2aJsonRpcSpecComplianceReport.methodMappingMissingIssue(operation))
                .containsEntry("source", "spec")
                .containsEntry("code", "jsonrpc_method_mapping_missing")
                .containsEntry("field", "jsonRpcMethod")
                .containsEntry("expected", "missing/method")
                .containsEntry("actual", "missing")
                .containsEntry(
                        "message",
                        "A2A JSON-RPC method missing/method is missing from the local method mapping.");
    }

    private static WayangA2aJsonRpcDispatcher dispatcher() {
        return WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                new InMemoryWayangA2aTaskStore(),
                request -> AgentResponse.builder()
                        .runId("run-jsonrpc-compliance")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
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
                "A2A JSON-RPC compliance endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }
}
