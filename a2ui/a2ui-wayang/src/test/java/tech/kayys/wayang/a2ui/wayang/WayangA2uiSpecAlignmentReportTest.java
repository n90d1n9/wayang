package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiSpecAlignmentReportTest {

    @Test
    void exposesCanonicalDefaultReportFactory() {
        WayangA2uiSpecAlignmentReport report = WayangA2uiSpecAlignmentReport.defaultReport();

        assertThat(report).isEqualTo(WayangA2uiSpecAlignmentReport.defaults());
        assertThat(report.routeCatalog()).isEqualTo(WayangA2uiHttpRouteCatalog.defaultCatalog());
        assertThat(report.aligned()).isTrue();
    }

    @Test
    void defaultCatalogIsAlignedWithPinnedA2uiSnapshot() {
        WayangA2uiSpecAlignmentReport report = WayangA2uiSpecAlignmentReport.defaultReport();
        Map<String, Object> values = report.toMap();
        String json = TransportJson.json(values, "Unable to encode spec alignment fixture");

        assertThat(report.aligned()).isTrue();
        assertThat(report.requirementCount()).isEqualTo(11);
        assertThat(report.alignedCount()).isEqualTo(11);
        assertThat(report.gapCount()).isZero();
        assertThat(report.gapIds()).isEmpty();
        assertThat(report.requirementIds())
                .contains(
                        "protocol.metadata",
                        "transport.content",
                        "message.server_keys",
                        "message.client_keys",
                        "transport.data_part",
                        "route." + WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                        "route." + WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG,
                        "route." + WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                        "route." + WayangA2uiHttpRoute.OPERATION_BINDING_REPORT,
                        "route." + WayangA2uiHttpRoute.OPERATION_SMOKE,
                        "route." + WayangA2uiHttpRoute.OPERATION_READINESS);
        assertThat(values)
                .containsEntry("protocol", "a2ui")
                .containsEntry("specVersion", A2uiProtocol.VERSION)
                .containsEntry("extensionUri", A2uiProtocol.EXTENSION_URI)
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 11)
                .containsEntry("gapCount", 0);
        assertThat(map(values.get("standard")))
                .containsEntry("standardId", "a2ui")
                .containsEntry("name", WayangA2uiSpecAlignmentReport.STANDARD_NAME)
                .containsEntry("version", A2uiProtocol.VERSION)
                .containsEntry("binding", WayangA2uiSpecAlignmentReport.BINDING_HTTP)
                .containsEntry("specUrl", A2uiProtocol.STANDARD_CATALOG_ID)
                .containsEntry("extensionUri", A2uiProtocol.EXTENSION_URI);
        assertThat(map(values.get("routeCatalog"))).containsEntry("routeCount", 6);
        assertThat(strings(map(requirement(report, "message.server_keys").actual()).get("messageKeys")))
                .containsExactly("dataModelUpdate", "surfaceUpdate", "beginRendering", "deleteSurface");
        assertThat(strings(map(requirement(report, "message.client_keys").actual()).get("messageKeys")))
                .containsExactly("userAction", "error");
        assertThat(map(requirement(report, "transport.data_part").actual()))
                .containsEntry("mimeType", A2uiProtocol.MIME_TYPE)
                .containsEntry("kind", "data");
        assertThat(json).startsWith("{\"protocol\":");
        assertThat(json.indexOf("\"standard\""))
                .isGreaterThan(json.indexOf("\"extensionUri\""));
        assertThat(json.indexOf("\"requirements\""))
                .isGreaterThan(json.indexOf("\"routeCatalog\""));
    }

    @Test
    void mountedCatalogKeepsRouteAlignmentByPathSuffix() {
        WayangA2uiSpecAlignmentReport report = WayangA2uiHttpRouteCatalog.defaultCatalog()
                .mountedAt("/api/a2ui")
                .specAlignmentReport();
        Map<String, Object> exchange = map(requirement(
                report,
                "route." + WayangA2uiHttpRoute.OPERATION_EXCHANGE).actual());

        assertThat(report.aligned()).isTrue();
        assertThat(exchange)
                .containsEntry("path", "/api/a2ui/exchange")
                .containsEntry("pathSuffix", "/exchange")
                .containsEntry("pathSuffixMatched", true);
    }

    @Test
    void missingRoutesAreReportedAsGaps() {
        WayangA2uiHttpRouteCatalog catalog = new WayangA2uiHttpRouteCatalog(List.of(
                WayangA2uiHttpRoute.exchange()));

        WayangA2uiSpecAlignmentReport report = WayangA2uiSpecAlignmentReport.from(catalog);
        WayangA2uiSpecAlignmentRequirement surfaceCatalog =
                requirement(report, "route." + WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG);

        assertThat(report.aligned()).isFalse();
        assertThat(report.requirementCount()).isEqualTo(11);
        assertThat(report.alignedCount()).isEqualTo(6);
        assertThat(report.gapCount()).isEqualTo(5);
        assertThat(report.gapIds())
                .containsExactly(
                        "route." + WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG,
                        "route." + WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                        "route." + WayangA2uiHttpRoute.OPERATION_BINDING_REPORT,
                        "route." + WayangA2uiHttpRoute.OPERATION_SMOKE,
                        "route." + WayangA2uiHttpRoute.OPERATION_READINESS);
        assertThat(surfaceCatalog.aligned()).isFalse();
        assertThat(map(surfaceCatalog.actual())).containsEntry("present", false);
        assertThat(surfaceCatalog.message()).contains("missing");
    }

    private static WayangA2uiSpecAlignmentRequirement requirement(
            WayangA2uiSpecAlignmentReport report,
            String id) {
        return report.requirements().stream()
                .filter(requirement -> requirement.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return copy((Map<?, ?>) value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }

    private static Map<String, Object> copy(Map<?, ?> values) {
        java.util.LinkedHashMap<String, Object> copy = new java.util.LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return Map.copyOf(copy);
    }
}
