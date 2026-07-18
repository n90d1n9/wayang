package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangJson;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthEnvelopes;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthReports;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProviderIssue;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProviderSummary;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentSummary;
import tech.kayys.wayang.gollek.sdk.WayangStandardDefinition;
import tech.kayys.wayang.gollek.sdk.WayangStandardRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentHealthFormatsTest {

    @Test
    void textRendersBlockingPolicyAndRegistryDrift() {
        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReports.builder()
                .config(WayangStandardAlignmentPolicyConfig.builder()
                        .mode("pinned-registry")
                        .standardIds("a2a", "a2ui")
                        .registryDriftMode("block")
                        .build())
                .portfolio(WayangStandardAlignmentPortfolio.builder()
                        .summary(new WayangStandardAlignmentSummary(
                                new WayangStandardAlignmentDescriptor(
                                        "a2a",
                                        "Agent2Agent Protocol",
                                        "0.9",
                                        "JSONRPC",
                                        "https://a2a-protocol.org/latest/specification/",
                                        Map.of()),
                                true,
                                2,
                                2,
                                0,
                                List.of(),
                                List.of()))
                        .build())
                .build();

        assertThat(WayangStandardAlignmentHealthTextFormat.text("Wayang", health))
                .contains("Wayang standard alignment")
                .contains("status: blocked")
                .contains("ready: no")
                .contains("standards: 1")
                .contains("missing standards:")
                .contains("- a2ui")
                .contains("version mismatches:")
                .contains("- a2a expected=1.0 actual=0.9")
                .contains("registry drift detail:")
                .contains("- a2a version expected=1.0 actual=0.9")
                .contains("recommendations:");
    }

    @Test
    void jsonWrapsHealthPayloadWithProduct() {
        WayangStandardDefinition definition = WayangStandardRegistry.find("a2ui").orElseThrow();
        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReports.builder()
                .config(WayangStandardAlignmentPolicyConfig.strict("a2ui"))
                .reportMap(Map.of(
                        "standard",
                        definition.toDescriptor().toMap(),
                        "aligned",
                        true,
                        "requirementCount",
                        1,
                        "alignedCount",
                        1,
                        "gapCount",
                        0))
                .build();

        assertThat(WayangJson.object(WayangStandardAlignmentHealthEnvelopes.health("Wayang", health)))
                .startsWith("{")
                .contains("\"product\":\"Wayang\"")
                .contains("\"health\":")
                .contains("\"ready\":true")
                .contains("\"providerPolicyAssessment\":")
                .contains("\"providerDiagnostics\":")
                .contains("\"standardIds\":[\"a2ui\"]");
    }

    @Test
    void textRendersProviderIssuesAsWarnings() {
        WayangStandardDefinition definition = WayangStandardRegistry.find("a2a").orElseThrow();
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(Map.of(
                "standard",
                definition.toDescriptor().toMap(),
                "aligned",
                true,
                "requirementCount",
                1,
                "alignedCount",
                1,
                "gapCount",
                0));
        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                portfolio,
                WayangStandardAlignmentPolicyConfig.strict("a2a"),
                List.of("a2a-provider"),
                List.of(WayangStandardAlignmentProviderSummary.from(
                        "a2a-provider",
                        "example.A2aProvider",
                        10,
                        portfolio)),
                List.of(new WayangStandardAlignmentProviderIssue(
                        "broken-provider",
                        "example.BrokenProvider",
                        "boom")));

        assertThat(WayangStandardAlignmentHealthTextFormat.text("Wayang", health))
                .contains("status: warning")
                .contains("ready: yes")
                .contains("providers: 1")
                .contains("provider policy ready: yes")
                .contains("provider issue mode: warn")
                .contains("provider minimum: 0")
                .contains("provider ids:")
                .contains("- a2a-provider")
                .contains("provider detail:")
                .contains("- a2a-provider priority=10 standards=1 aligned=yes gaps=0 ids=a2a")
                .contains("provider issues: 1")
                .contains("provider issue detail:")
                .contains("- broken-provider example.BrokenProvider: boom")
                .contains("Review standard-alignment provider broken-provider: boom");
    }
}
