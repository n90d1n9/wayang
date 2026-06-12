package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentReportProjectionTest {

    @Test
    void projectsAlignedReportInStableFieldOrder() {
        WayangA2aSpecAlignmentReport report = WayangA2aSpecAlignmentReport.defaults();
        Map<String, Object> values = WayangA2aSpecAlignmentReportProjection.report(report);
        String json = WayangA2aHttpJson.write(values);

        assertThat(values).isEqualTo(report.toMap());
        assertThat(values)
                .containsEntry("protocol", "a2a")
                .containsEntry("protocolVersion", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 20)
                .containsEntry("gapCount", 0);
        assertThat(json).startsWith("{\"protocol\":");
        assertThat(json.indexOf("\"standard\"")).isGreaterThan(json.indexOf("\"binding\""));
        assertThat(json.indexOf("\"categorySummaries\"")).isGreaterThan(json.indexOf("\"gapCategories\""));
        assertThat(json.indexOf("\"requirements\"")).isGreaterThan(json.indexOf("\"routeCatalog\""));
    }

    @Test
    void projectsGapCategoriesForMissingRoutes() {
        A2aHttpRoute discover = A2aHttpRouteCatalog.standard()
                .routeForOperation(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD)
                .orElseThrow();
        WayangA2aSpecAlignmentReport report =
                WayangA2aSpecAlignmentReport.from(new A2aHttpRouteCatalog(List.of(discover)));

        Map<String, Object> values = WayangA2aSpecAlignmentReportProjection.report(report);

        assertThat(values)
                .containsEntry("aligned", false)
                .containsEntry("gapCount", 11)
                .containsEntry("gapCategories", List.of("route"));
        assertThat(strings(values.get("gapIds")))
                .contains("route." + A2aProtocol.OPERATION_SEND_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }
}
