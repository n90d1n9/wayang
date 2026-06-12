package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiRecordTextNormalizationTest {

    @Test
    void normalizesActionResultTextFields() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nullable", null);
        metadata.put(null, "ignored");
        WayangA2uiActionResult result = WayangA2uiActionResult.handled(
                " action ",
                " run ",
                " message ",
                List.of(),
                metadata);

        assertThat(result.actionName()).isEqualTo("action");
        assertThat(result.runId()).isEqualTo("run");
        assertThat(result.message()).isEqualTo("message");
        assertThat(result.metadata())
                .containsEntry("nullable", null)
                .doesNotContainKey(null);
    }

    @Test
    void normalizesSmokeProbeTextFields() {
        WayangA2uiHttpSmokeSummary summary = new WayangA2uiHttpSmokeSummary(
                true,
                0,
                " suite ",
                1,
                0,
                2,
                true,
                List.of(),
                Map.of(),
                Map.of());
        WayangA2uiHttpSmokeProbeResult probe = new WayangA2uiHttpSmokeProbeResult(
                200,
                true,
                " smoke ",
                " read-only ",
                " success ",
                summary,
                Map.of());

        assertThat(summary.suiteId()).isEqualTo("suite");
        assertThat(probe.routeOperation()).isEqualTo("smoke");
        assertThat(probe.allow()).isEqualTo("read-only");
        assertThat(probe.outcome()).isEqualTo("success");
    }

    @Test
    void normalizesBindingProbeTextFields() {
        WayangA2uiHttpBindingReportProbeResult probe = new WayangA2uiHttpBindingReportProbeResult(
                200,
                true,
                " binding-report ",
                " read-only ",
                " success ",
                " application/json ",
                " application/a2ui+json ",
                " json ",
                true,
                1,
                1,
                0,
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(probe.routeOperation()).isEqualTo("binding-report");
        assertThat(probe.allow()).isEqualTo("read-only");
        assertThat(probe.outcome()).isEqualTo("success");
        assertThat(probe.contentType()).isEqualTo("application/json");
        assertThat(probe.mimeType()).isEqualTo("application/a2ui+json");
        assertThat(probe.bodyEncoding()).isEqualTo("json");
    }

    @Test
    void normalizesSpecAlignmentRequirementTextFields() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("present", true);
        expected.put("nullable", null);
        WayangA2uiSpecAlignmentRequirement requirement = WayangA2uiSpecAlignmentRequirement.gap(
                " requirement ",
                " category ",
                " title ",
                expected,
                Map.of(),
                " message ");

        assertThat(requirement.id()).isEqualTo("requirement");
        assertThat(requirement.category()).isEqualTo("category");
        assertThat(requirement.title()).isEqualTo("title");
        assertThat(requirement.message()).isEqualTo("message");
        assertThat(requirement.expected())
                .containsEntry("present", true)
                .doesNotContainKey("nullable");
    }
}
