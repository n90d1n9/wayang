package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardRegistryDriftReportTest {

    @Test
    void matchingRegistryDescriptorIsDriftFree() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(report(
                "a2a",
                "Agent2Agent Protocol",
                "1.0",
                "JSONRPC",
                "https://a2a-protocol.org/latest/specification/",
                Map.of()));

        WayangStandardRegistryDriftReport drift = portfolio.registryDrift();

        assertThat(drift.driftFree()).isTrue();
        assertThat(drift.hasDrift()).isFalse();
        assertThat(drift.checkedStandardIds()).containsExactly("a2a");
        assertThat(drift.unknownStandardIds()).isEmpty();
        assertThat(drift.issues()).isEmpty();
        assertThat(drift.toMap())
                .containsEntry("driftFree", true)
                .containsEntry("checkedStandardIds", List.of("a2a"));
    }

    @Test
    void reportsDescriptorFieldAndAttributeDrift() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(report(
                "a2ui",
                "Agent-to-User Interface",
                "v0.7",
                "SSE",
                "https://example.test/a2ui",
                Map.of("extensionUri", "https://example.test/a2ui-extension")));

        WayangStandardRegistryDriftReport drift = WayangStandardRegistry.driftReport(portfolio);

        assertThat(drift.driftFree()).isFalse();
        assertThat(drift.checkedStandardIds()).containsExactly("a2ui");
        assertThat(drift.unknownStandardIds()).isEmpty();
        assertThat(drift.issues())
                .extracting(WayangStandardRegistryDriftIssue::field)
                .containsExactly("version", "binding", "specUrl", "extensionUri");
        assertThat(drift.issues().get(0).toMap())
                .containsEntry("standardId", "a2ui")
                .containsEntry("field", "version")
                .containsEntry("expected", "v0.8")
                .containsEntry("actual", "v0.7");
    }

    @Test
    void tracksUnknownStandardsWithoutReportingRegistryDrift() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(report(
                "custom-standard",
                "Custom Standard",
                "0.1",
                "HTTP",
                "https://example.test/custom",
                Map.of()));

        WayangStandardRegistryDriftReport drift = portfolio.registryDrift();

        assertThat(drift.driftFree()).isTrue();
        assertThat(drift.hasUnknownStandards()).isTrue();
        assertThat(drift.checkedStandardIds()).isEmpty();
        assertThat(drift.unknownStandardIds()).containsExactly("custom-standard");
        assertThat(drift.issues()).isEmpty();
    }

    private static Map<String, Object> report(
            String standardId,
            String name,
            String version,
            String binding,
            String specUrl,
            Map<String, Object> attributes) {
        java.util.LinkedHashMap<String, Object> standard = new java.util.LinkedHashMap<>();
        standard.put("standardId", standardId);
        standard.put("name", name);
        standard.put("version", version);
        standard.put("binding", binding);
        standard.put("specUrl", specUrl);
        standard.putAll(attributes);
        return Map.of(
                "standard", standard,
                "aligned", true,
                "requirementCount", 1,
                "alignedCount", 1,
                "gapCount", 0,
                "gapIds", List.of());
    }
}
