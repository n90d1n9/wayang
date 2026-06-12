package tech.kayys.wayang.a2ui.wayang.projection;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiProtocol;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSpecAlignmentReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSpecAlignmentRequirement;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpecAlignmentProjectionTest {

    @Test
    void projectsOrderedSpecAlignmentReportAndRecordDelegates() {
        WayangA2uiSpecAlignmentReport report = WayangA2uiSpecAlignmentReport.defaultReport();

        Map<String, Object> values = SpecAlignmentProjection.report(report);

        assertThat(report.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "protocol",
                "specVersion",
                "extensionUri",
                "standard",
                "aligned",
                "requirementCount",
                "alignedCount",
                "gapCount",
                "requirementIds",
                "gapIds",
                "routeCatalog",
                "requirements");
        assertThat(values)
                .containsEntry("protocol", WayangA2uiSpecAlignmentReport.STANDARD_ID)
                .containsEntry("specVersion", A2uiProtocol.VERSION)
                .containsEntry("extensionUri", A2uiProtocol.EXTENSION_URI)
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 11)
                .containsEntry("alignedCount", 11)
                .containsEntry("gapCount", 0);
        assertThat((Map<String, Object>) values.get("standard"))
                .containsEntry("standardId", WayangA2uiSpecAlignmentReport.STANDARD_ID)
                .containsEntry("binding", WayangA2uiSpecAlignmentReport.BINDING_HTTP);
        assertThat((Map<String, Object>) values.get("routeCatalog"))
                .containsEntry("routeCount", 6);
    }

    @Test
    void projectsOrderedStandardDescriptorAndReportFacadeDelegates() {
        Map<String, Object> values = SpecAlignmentProjection.standardDescriptor();

        assertThat((Map<String, Object>) WayangA2uiSpecAlignmentReport.defaultReport()
                .toMap()
                .get("standard"))
                .isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "standardId",
                "name",
                "version",
                "binding",
                "specUrl",
                "extensionUri");
        assertThat(values)
                .containsEntry("standardId", WayangA2uiSpecAlignmentReport.STANDARD_ID)
                .containsEntry("name", WayangA2uiSpecAlignmentReport.STANDARD_NAME)
                .containsEntry("version", A2uiProtocol.VERSION)
                .containsEntry("binding", WayangA2uiSpecAlignmentReport.BINDING_HTTP)
                .containsEntry("specUrl", A2uiProtocol.STANDARD_CATALOG_ID)
                .containsEntry("extensionUri", A2uiProtocol.EXTENSION_URI);
    }

    @Test
    void projectsOrderedAlignedRequirementAndRecordDelegates() {
        WayangA2uiSpecAlignmentRequirement requirement = WayangA2uiSpecAlignmentRequirement.aligned(
                "protocol.metadata",
                "protocol",
                "A2UI protocol metadata",
                Map.of("specVersion", "v0.8"),
                Map.of("specVersion", "v0.8"));

        Map<String, Object> values = SpecAlignmentProjection.requirement(requirement);

        assertThat(requirement.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "id",
                "category",
                "title",
                "aligned",
                "expected",
                "actual");
        assertThat(values)
                .containsEntry("id", "protocol.metadata")
                .containsEntry("category", "protocol")
                .containsEntry("title", "A2UI protocol metadata")
                .containsEntry("aligned", true)
                .doesNotContainKey("message");
        assertThat((Map<String, Object>) values.get("expected"))
                .containsEntry("specVersion", "v0.8");
        assertThat((Map<String, Object>) values.get("actual"))
                .containsEntry("specVersion", "v0.8");
    }

    @Test
    void projectsOrderedGapRequirementWithMessage() {
        WayangA2uiSpecAlignmentRequirement requirement = WayangA2uiSpecAlignmentRequirement.gap(
                "route.a2ui.exchange",
                "route",
                "A2UI HTTP route a2ui.exchange",
                Map.of("pathSuffix", "/exchange"),
                Map.of("present", false),
                "A2UI HTTP route is missing from the local route catalog.");

        Map<String, Object> values = SpecAlignmentProjection.requirement(requirement);

        assertThat(values.keySet()).containsExactly(
                "id",
                "category",
                "title",
                "aligned",
                "expected",
                "actual",
                "message");
        assertThat(values)
                .containsEntry("aligned", false)
                .containsEntry("message", "A2UI HTTP route is missing from the local route catalog.");
        assertThat((Map<String, Object>) values.get("actual"))
                .containsEntry("present", false);
    }

    @Test
    void projectsOrderedRouteExpectation() {
        Map<String, Object> values = SpecAlignmentProjection.routeExpectation(WayangA2uiHttpRoute.exchange());

        assertThat(values.keySet()).containsExactly(
                "present",
                "operation",
                "method",
                "pathSuffix",
                "requestContentType",
                "responseContentType",
                "requestBodyRequired",
                "allowsOptions");
        assertThat(values)
                .containsEntry("present", true)
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry("method", "POST")
                .containsEntry("pathSuffix", "/exchange")
                .containsEntry("requestContentType", WayangA2uiTransportContent.MIME_JSON)
                .containsEntry("responseContentType", WayangA2uiTransportContent.MIME_JSON)
                .containsEntry("requestBodyRequired", true)
                .containsEntry("allowsOptions", true);
    }

    @Test
    void projectsOrderedRouteActualAndMountedPathSuffixMatch() {
        WayangA2uiHttpRoute expected = WayangA2uiHttpRoute.exchange();
        WayangA2uiHttpRoute actual = expected.withPath("/api/a2ui/exchange");

        Map<String, Object> values = SpecAlignmentProjection.routeActual(expected, actual);

        assertThat(values.keySet()).containsExactly(
                "present",
                "operation",
                "method",
                "path",
                "pathSuffix",
                "pathSuffixMatched",
                "requestContentType",
                "responseContentType",
                "requestBodyRequired",
                "allowsOptions");
        assertThat(values)
                .containsEntry("present", true)
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry("method", "POST")
                .containsEntry("path", "/api/a2ui/exchange")
                .containsEntry("pathSuffix", "/exchange")
                .containsEntry("pathSuffixMatched", true)
                .containsEntry("allowsOptions", true);
        assertThat(SpecAlignmentProjection.pathSuffixMatches(
                expected.path(),
                actual.path()))
                .isTrue();
    }
}
