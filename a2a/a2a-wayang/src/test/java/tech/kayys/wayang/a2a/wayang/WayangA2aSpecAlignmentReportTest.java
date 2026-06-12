package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentReportTest {

    @Test
    void standardCatalogIsAlignedWithPinnedA2aSnapshot() {
        WayangA2aSpecAlignmentReport report = WayangA2aSpecAlignmentReport.defaults();
        Map<String, Object> values = report.toMap();
        String reportJson = WayangA2aHttpJson.write(values);

        assertThat(report.aligned()).isTrue();
        assertThat(report.requirementCount()).isEqualTo(20);
        assertThat(report.alignedCount()).isEqualTo(20);
        assertThat(report.gapCount()).isZero();
        assertThat(report.gapIds()).isEmpty();
        assertThat(report.requirementIds())
                .contains(
                        "protocol.metadata",
                        "binding.metadata",
                        "agent_card.top_level_fields",
                        "agent_card.component_fields",
                        "agent_card.binding_defaults",
                        "route." + A2aProtocol.OPERATION_DISCOVER_AGENT_CARD,
                        "route." + A2aProtocol.OPERATION_SEND_MESSAGE,
                        "route." + A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD,
                        "jsonrpc.method_registry",
                        "jsonrpc.response_media",
                        "jsonrpc.capability_gates");
        assertThat(values)
                .containsEntry("protocol", "a2a")
                .containsEntry("protocolVersion", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 20)
                .containsEntry("gapCount", 0)
                .containsEntry("gapCategories", List.of());
        assertThat(map(values.get("standard")))
                .containsEntry("standardId", "a2a")
                .containsEntry("name", WayangA2aSpecAlignmentReport.STANDARD_NAME)
                .containsEntry("version", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("specUrl", WayangA2aSpecAlignmentReport.SPEC_URL);
        assertThat(summary(report, "protocol").toMap())
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 1)
                .containsEntry("alignedCount", 1)
                .containsEntry("gapCount", 0);
        assertThat(summary(report, "binding").toMap()).containsEntry("requirementCount", 1);
        assertThat(summary(report, "agent_card").toMap()).containsEntry("requirementCount", 3);
        assertThat(summary(report, "route").toMap()).containsEntry("requirementCount", 12);
        assertThat(summary(report, "jsonrpc").toMap()).containsEntry("requirementCount", 3);
        assertThat(maps(values.get("categorySummaries")))
                .extracting(summary -> summary.get("category"))
                .containsExactly("protocol", "binding", "agent_card", "route", "jsonrpc");
        assertThat(map(values.get("routeCatalog"))).containsEntry("routeCount", 12);
        assertThat(strings(map(requirement(report, "agent_card.top_level_fields").actual()).get("fields")))
                .containsExactly(
                        "name",
                        "description",
                        "supportedInterfaces",
                        "provider",
                        "version",
                        "documentationUrl",
                        "capabilities",
                        "securitySchemes",
                        "securityRequirements",
                        "defaultInputModes",
                        "defaultOutputModes",
                        "skills",
                        "signatures",
                        "iconUrl");
        assertThat(map(requirement(report, "agent_card.component_fields").actual()))
                .containsEntry("interfaceFields", List.of("url", "protocolBinding", "tenant", "protocolVersion"))
                .containsEntry("capabilityFields", List.of(
                        "streaming",
                        "pushNotifications",
                        "extensions",
                        "extendedAgentCard"))
                .containsEntry("skillFields", List.of(
                        "id",
                        "name",
                        "description",
                        "tags",
                        "examples",
                        "inputModes",
                        "outputModes",
                        "securityRequirements"));
        assertThat(map(requirement(report, "agent_card.binding_defaults").actual()))
                .containsEntry("preferredProtocolBinding", A2aProtocol.BINDING_HTTP_JSON)
                .containsEntry("preferredProtocolVersion", A2aProtocol.VERSION)
                .containsEntry("defaultInputModes", List.of("text/plain"))
                .containsEntry("defaultOutputModes", List.of("text/plain"));
        assertThat(map(requirement(report, "route." + A2aProtocol.OPERATION_DISCOVER_AGENT_CARD).actual()))
                .containsEntry("path", A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH)
                .containsEntry("httpMethod", "GET")
                .containsEntry("streaming", false);
        assertThat(map(requirement(report, "jsonrpc.method_registry").actual()))
                .containsEntry("methodCount", 11);
        assertThat(strings(map(requirement(report, "jsonrpc.method_registry").actual()).get("methods")))
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(strings(map(requirement(report, "jsonrpc.response_media").actual()).get("streamingMethods")))
                .containsExactly(
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK);
        assertThat(map(requirement(report, "jsonrpc.response_media").actual()))
                .containsEntry("jsonMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry("streamingMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(strings(map(requirement(report, "jsonrpc.capability_gates").actual())
                        .get("pushNotificationMethods")))
                .containsExactly(
                        WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
        assertThat(strings(map(requirement(report, "jsonrpc.capability_gates").actual())
                        .get("extendedAgentCardMethods")))
                .containsExactly(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD);
        assertThat(reportJson).startsWith("{\"protocol\":");
        assertThat(reportJson.indexOf("\"standard\""))
                .isGreaterThan(reportJson.indexOf("\"binding\""));
        assertThat(reportJson.indexOf("\"categorySummaries\""))
                .isGreaterThan(reportJson.indexOf("\"gapCategories\""));
        assertThat(reportJson.indexOf("\"requirements\""))
                .isGreaterThan(reportJson.indexOf("\"routeCatalog\""));
    }

    @Test
    void missingRoutesAreReportedAsGaps() {
        A2aHttpRoute discover = A2aHttpRouteCatalog.standard()
                .routeForOperation(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD)
                .orElseThrow();
        A2aHttpRouteCatalog catalog = new A2aHttpRouteCatalog(List.of(discover));

        WayangA2aSpecAlignmentReport report = WayangA2aSpecAlignmentReport.from(catalog);
        WayangA2aSpecAlignmentRequirement sendMessage =
                requirement(report, "route." + A2aProtocol.OPERATION_SEND_MESSAGE);

        assertThat(report.aligned()).isFalse();
        assertThat(report.requirementCount()).isEqualTo(20);
        assertThat(report.alignedCount()).isEqualTo(9);
        assertThat(report.gapCount()).isEqualTo(11);
        assertThat(report.gapIds()).containsExactlyElementsOf(A2aHttpRouteCatalog.standard().routes().stream()
                .filter(route -> !A2aProtocol.OPERATION_DISCOVER_AGENT_CARD.equals(route.operation()))
                .map(route -> "route." + route.operation())
                .toList());
        assertThat(summary(report, "route").toMap())
                .containsEntry("requirementCount", 12)
                .containsEntry("alignedCount", 1)
                .containsEntry("gapCount", 11);
        assertThat(summary(report, "route").gapIds()).containsExactlyElementsOf(report.gapIds());
        assertThat(summary(report, "route").hasGaps()).isTrue();
        assertThat(report.categorySummary("route")).contains(summary(report, "route"));
        assertThat(report.categorySummary("missing")).isEmpty();
        assertThat(report.gapCategorySummaries()).containsExactly(summary(report, "route"));
        assertThat(report.gapCategories()).containsExactly("route");
        assertThat(strings(report.toMap().get("gapCategories"))).containsExactly("route");
        assertThat(sendMessage.aligned()).isFalse();
        assertThat(map(sendMessage.actual())).containsEntry("present", false);
        assertThat(sendMessage.message()).contains("missing");
    }

    @Test
    void jsonRpcMethodRegistryExposesAlignmentReport() {
        WayangA2aSpecAlignmentReport report = WayangA2aJsonRpcMethods.specAlignmentReport();

        assertThat(report.aligned()).isTrue();
        assertThat(report.toMap())
                .containsEntry("protocol", "a2a")
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC);
    }

    @Test
    void snapshotProvidesCompactDiagnosticsView() {
        WayangA2aSpecAlignmentSnapshot snapshot =
                WayangA2aSpecAlignmentSnapshot.from(WayangA2aSpecAlignmentReport.defaults());
        String snapshotJson = WayangA2aHttpJson.write(snapshot.toMap());

        assertThat(snapshot.toMap())
                .containsEntry("protocol", "a2a")
                .containsEntry("protocolVersion", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 20)
                .containsEntry("alignedCount", 20)
                .containsEntry("gapCount", 0)
                .containsEntry("gapIds", List.of())
                .containsEntry("gapCategories", List.of());
        assertThat(map(snapshot.toMap().get("standard")))
                .containsEntry("standardId", "a2a")
                .containsEntry("name", WayangA2aSpecAlignmentReport.STANDARD_NAME)
                .containsEntry("version", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("specUrl", WayangA2aSpecAlignmentReport.SPEC_URL);
        assertThat(maps(snapshot.toMap().get("categorySummaries")))
                .extracting(summary -> summary.get("category"))
                .containsExactly("protocol", "binding", "agent_card", "route", "jsonrpc");
        assertThat(snapshot.categorySummary("route")).contains(summary(WayangA2aSpecAlignmentReport.defaults(), "route"));
        assertThat(snapshot.categorySummary("missing")).isEmpty();
        assertThat(snapshot.gapCategorySummaries()).isEmpty();
        assertThat(snapshot.gapCategories()).isEmpty();
        assertThat(WayangA2aSpecAlignmentSnapshot.fromMap(snapshot.toMap()).toMap())
                .isEqualTo(snapshot.toMap());
        assertThat(snapshotJson).startsWith("{\"protocol\":");
        assertThat(snapshotJson.indexOf("\"standard\""))
                .isGreaterThan(snapshotJson.indexOf("\"binding\""));
        assertThat(snapshotJson.indexOf("\"categorySummaries\""))
                .isGreaterThan(snapshotJson.indexOf("\"gapCategories\""));
    }

    private static WayangA2aSpecAlignmentRequirement requirement(
            WayangA2aSpecAlignmentReport report,
            String id) {
        return report.requirements().stream()
                .filter(requirement -> requirement.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static WayangA2aSpecAlignmentCategorySummary summary(
            WayangA2aSpecAlignmentReport report,
            String category) {
        return report.categorySummaries().stream()
                .filter(summary -> summary.category().equals(category))
                .findFirst()
                .orElseThrow();
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

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }
}
