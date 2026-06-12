package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpBindingReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpBindingReportProjectionTest {

    @Test
    void projectsOrderedCompleteBindingReportAndRecordDelegates() {
        WayangA2uiHttpBindingReport report = WayangA2uiContractFixtures.contractBindingReport();

        Map<String, Object> values = HttpBindingReportProjection.report(report);

        assertThat(report.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "complete",
                "routeOperationCount",
                "handlerOperationCount",
                "routeOperations",
                "handlerOperations",
                "missingHandlerOperations",
                "orphanHandlerOperations");
        assertThat(values)
                .containsEntry("complete", true)
                .containsEntry("routeOperationCount", 6)
                .containsEntry("handlerOperationCount", 6)
                .containsEntry("missingHandlerOperations", List.of())
                .containsEntry("orphanHandlerOperations", List.of());
        assertThat((Iterable<String>) values.get("routeOperations"))
                .containsExactlyElementsOf(report.routeOperations());
        assertThat((Iterable<String>) values.get("handlerOperations"))
                .containsExactlyElementsOf(report.handlerOperations());
    }

    @Test
    void projectsOrderedIncompleteBindingReport() {
        WayangA2uiHttpBindingReport report = WayangA2uiContractFixtures.incompleteContractBindingReport();

        Map<String, Object> values = HttpBindingReportProjection.report(report);

        assertThat(values)
                .containsEntry("complete", false)
                .containsEntry("routeOperationCount", 3)
                .containsEntry("handlerOperationCount", 3);
        assertThat((Iterable<String>) values.get("missingHandlerOperations"))
                .containsExactly(
                        WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG,
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT);
        assertThat((Iterable<String>) values.get("orphanHandlerOperations"))
                .containsExactly(
                        WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                        "a2ui.customHandler");
    }

    @Test
    void nullDispatcherUsesDefaultCatalogWithNoHandlers() {
        WayangA2uiHttpBindingReport report = WayangA2uiHttpBindingReport.of(null, null);

        assertThat(report.routeOperationCount()).isEqualTo(6);
        assertThat(report.handlerOperations()).isEmpty();
        assertThat(report.missingHandlerOperations()).containsExactlyElementsOf(report.routeOperations());
        assertThat(report.orphanHandlerOperations()).isEmpty();
    }

    @Test
    void reportNormalizationKeepsDistinctOrderedOperationsBeforeProjection() {
        WayangA2uiHttpBindingReport report = new WayangA2uiHttpBindingReport(
                List.of(" route.one ", "route.two", "route.one", "", " "),
                List.of("route.two", " handler.extra ", "route.two"),
                List.of(" route.one ", "route.one"),
                List.of(" handler.extra "));

        Map<String, Object> values = HttpBindingReportProjection.report(report);

        assertThat((Iterable<String>) values.get("routeOperations"))
                .containsExactly("route.one", "route.two");
        assertThat((Iterable<String>) values.get("handlerOperations"))
                .containsExactly("route.two", "handler.extra");
        assertThat((Iterable<String>) values.get("missingHandlerOperations"))
                .containsExactly("route.one");
        assertThat((Iterable<String>) values.get("orphanHandlerOperations"))
                .containsExactly("handler.extra");
    }
}
