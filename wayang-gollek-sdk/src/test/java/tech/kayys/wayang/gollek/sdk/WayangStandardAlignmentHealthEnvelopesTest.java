package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangStandardAlignmentHealthEnvelopesTest {

    @Test
    void healthEnvelopeOwnsPublishedAlignmentHealthShape() {
        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.from(
                WayangStandardAlignmentPortfolio.builder().build());

        Map<String, Object> values = WayangStandardAlignmentHealthEnvelopes.health(" Wayang ", health);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsKey("health");
        assertThat(objectMap(values.get("health")))
                .containsEntry("reportId", WayangStandardAlignmentHealthReport.DEFAULT_REPORT_ID)
                .containsEntry("status", "ready")
                .containsEntry("ready", true)
                .containsEntry("standardCount", 0)
                .containsEntry("gapCount", 0)
                .containsEntry("standardIds", List.of())
                .containsEntry("gapStandardIds", List.of())
                .containsKeys(
                        "portfolio",
                        "policyAssessment",
                        "providerPolicyAssessment",
                        "registryDrift",
                        "providerDiagnostics",
                        "recommendations");
    }

    @Test
    void nullHealthNormalizesToEmptyReadyReport() {
        Map<String, Object> values = WayangStandardAlignmentHealthEnvelopes.health(null, null);
        Map<String, Object> health = objectMap(values.get("health"));

        assertThat(values)
                .containsEntry("product", "")
                .containsKey("health");
        assertThat(health)
                .containsEntry("status", "ready")
                .containsEntry("ready", true)
                .containsEntry("standardCount", 0)
                .containsEntry("providerCount", 0)
                .containsEntry("providerIssueCount", 0);
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> health.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }
}
