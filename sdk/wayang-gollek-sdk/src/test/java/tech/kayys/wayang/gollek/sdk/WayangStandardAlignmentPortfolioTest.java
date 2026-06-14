package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentPortfolioTest {

    @Test
    void parsesNestedStandardDescriptorFromAlignmentReportMap() {
        WayangStandardAlignmentSummary summary = WayangStandardAlignmentSummary.fromReportMap(Map.of(
                "standard", Map.of(
                        "standardId", "a2a",
                        "name", "Agent2Agent Protocol",
                        "version", "1.0",
                        "binding", "JSONRPC",
                        "specUrl", "https://a2a-protocol.org/latest/specification/"),
                "aligned", true,
                "requirementCount", 20,
                "alignedCount", 20,
                "gapCount", 0,
                "gapIds", List.of(),
                "gapCategories", List.of()));

        assertThat(summary.standardId()).isEqualTo("a2a");
        assertThat(summary.aligned()).isTrue();
        assertThat(summary.hasGaps()).isFalse();
        assertThat(summary.toMap())
                .containsEntry("requirementCount", 20)
                .containsEntry("gapCount", 0);
        assertThat(map(summary.toMap().get("standard")))
                .containsEntry("standardId", "a2a")
                .containsEntry("name", "Agent2Agent Protocol")
                .containsEntry("version", "1.0")
                .containsEntry("binding", "JSONRPC");
    }

    @Test
    void fallsBackToLegacyTopLevelReportFields() {
        WayangStandardAlignmentDescriptor descriptor = WayangStandardAlignmentDescriptor.fromReportMap(Map.of(
                "protocol", "a2ui",
                "specVersion", "v0.8",
                "specHome", "https://a2ui.org/specification/v0_8/standard_catalog_definition.json",
                "aligned", true));

        assertThat(descriptor.toMap())
                .containsEntry("standardId", "a2ui")
                .containsEntry("name", "Agent-to-User Interface")
                .containsEntry("version", "v0.8")
                .containsEntry("binding", "HTTP")
                .containsEntry("specUrl", "https://a2ui.org/specification/v0_8/standard_catalog_definition.json");
    }

    @Test
    void rollsUpMultipleStandardAlignmentReports() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", "Agent2Agent Protocol", "1.0", "JSONRPC", true, 20, 20, 0, List.of()),
                report("a2ui", "Agent-to-User Interface", "v0.8", "HTTP", true, 11, 11, 0, List.of()),
                report(
                        "agentic-commerce",
                        "Agentic Commerce Protocol",
                        "2026-01-30",
                        "HTTP+JSON",
                        false,
                        13,
                        12,
                        1,
                        List.of("route.agenticCommerce.checkoutSession.retrieve")));

        assertThat(portfolio.aligned()).isFalse();
        assertThat(portfolio.standardCount()).isEqualTo(3);
        assertThat(portfolio.alignedCount()).isEqualTo(2);
        assertThat(portfolio.gapCount()).isEqualTo(1);
        assertThat(portfolio.standardIds()).containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(portfolio.gapStandardIds()).containsExactly("agentic-commerce");
        assertThat(portfolio.toMap())
                .containsEntry("aligned", false)
                .containsEntry("standardCount", 3)
                .containsEntry("gapStandardIds", List.of("agentic-commerce"));
    }

    @Test
    void treatsReportedGapsAsPortfolioMisalignmentEvenWhenFlagIsTrue() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", "Agent2Agent Protocol", "1.0", "JSONRPC", true, 20, 20, 1, List.of("route.SendMessage")));

        assertThat(portfolio.aligned()).isFalse();
        assertThat(portfolio.alignedCount()).isZero();
        assertThat(portfolio.gapStandardIds()).containsExactly("a2a");
    }

    @Test
    void parsesSpecAlignmentFromDiagnosticsCarrierMap() {
        Map<String, Object> carrier = Map.of(
                "diagnosticsId", "a2a.jsonrpc.diagnostics",
                "attributes", Map.of(
                        "specAlignment", report(
                                "a2a",
                                "Agent2Agent Protocol",
                                "1.0",
                                "JSONRPC",
                                false,
                                20,
                                19,
                                1,
                                List.of("route.tasks.get"))));

        WayangStandardAlignmentSummary summary = WayangStandardAlignmentSummary.fromReportMap(carrier);
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(carrier);

        assertThat(summary.standardId()).isEqualTo("a2a");
        assertThat(summary.requirementCount()).isEqualTo(20);
        assertThat(summary.gapIds()).containsExactly("route.tasks.get");
        assertThat(summary.sources()).singleElement()
                .satisfies(source -> {
                    assertThat(source.sourceType()).isEqualTo("diagnostics");
                    assertThat(source.sourceId()).isEqualTo("a2a.jsonrpc.diagnostics");
                    assertThat(source.reportId()).isEqualTo("a2a.jsonrpc.diagnostics");
                });
        assertThat(summary.toMap()).containsEntry("sourceCount", 1);
        assertThat(portfolio.standardIds()).containsExactly("a2a");
        assertThat(portfolio.gapStandardIds()).containsExactly("a2a");
    }

    @Test
    void parsesTopLevelSpecAlignmentCarrierMap() {
        Map<String, Object> carrier = Map.of(
                "reportId", "a2ui.route.spec-compliance",
                "specAlignment", report(
                        "a2ui",
                        "Agent-to-User Interface",
                        "v0.8",
                        "HTTP",
                        true,
                        11,
                        11,
                        0,
                        List.of()));

        WayangStandardAlignmentDescriptor descriptor = WayangStandardAlignmentDescriptor.fromReportMap(carrier);
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(carrier);

        assertThat(descriptor.standardId()).isEqualTo("a2ui");
        assertThat(descriptor.version()).isEqualTo("v0.8");
        assertThat(portfolio.aligned()).isTrue();
        assertThat(portfolio.standardIds()).containsExactly("a2ui");
    }

    @Test
    void rollsUpStandardsListFromAggregateMap() {
        Map<String, Object> aggregate = Map.of(
                "portfolioId", "wayang.core.standards",
                "standards", List.of(
                        report("a2a", "Agent2Agent Protocol", "1.0", "JSONRPC", true, 20, 20, 0, List.of()),
                        report(
                                "agentic-commerce",
                                "Agentic Commerce Protocol",
                                "2026-01-30",
                                "HTTP+JSON",
                                false,
                                13,
                                12,
                                1,
                                List.of("route.checkout"))));

        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(aggregate);

        assertThat(portfolio.standardCount()).isEqualTo(2);
        assertThat(portfolio.aligned()).isFalse();
        assertThat(portfolio.standardIds()).containsExactly("a2a", "agentic-commerce");
        assertThat(portfolio.gapStandardIds()).containsExactly("agentic-commerce");
    }

    @Test
    void rollsUpNestedCarrierListFromAggregateAttributes() {
        Map<String, Object> aggregate = Map.of(
                "portfolioId", "wayang.gateway.diagnostics",
                "attributes", Map.of(
                        "alignmentReports", List.of(
                                Map.of("attributes", Map.of("specAlignment", report(
                                        "a2a",
                                        "Agent2Agent Protocol",
                                        "1.0",
                                        "JSONRPC",
                                        true,
                                        20,
                                        20,
                                        0,
                                        List.of()))),
                                Map.of("specAlignment", report(
                                        "a2ui",
                                        "Agent-to-User Interface",
                                        "v0.8",
                                        "HTTP",
                                        true,
                                        11,
                                        11,
                                        0,
                                        List.of())))));

        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(aggregate);

        assertThat(portfolio.aligned()).isTrue();
        assertThat(portfolio.standardIds()).containsExactly("a2a", "a2ui");
        assertThat(portfolio.gapCount()).isZero();
    }

    @Test
    void roundTripsPortfolioMapAsAggregateInput() {
        WayangStandardAlignmentPortfolio original = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", "Agent2Agent Protocol", "1.0", "JSONRPC", true, 20, 20, 0, List.of()),
                report("a2ui", "Agent-to-User Interface", "v0.8", "HTTP", true, 11, 11, 0, List.of()));

        WayangStandardAlignmentPortfolio parsed = WayangStandardAlignmentPortfolio.fromReportMaps(original.toMap());

        assertThat(parsed.aligned()).isTrue();
        assertThat(parsed.standardCount()).isEqualTo(2);
        assertThat(parsed.standardIds()).containsExactly("a2a", "a2ui");
    }

    @Test
    void mergesDuplicateStandardsWithoutDoubleCounting() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", "Agent2Agent Protocol", "1.0", "JSONRPC", true, 20, 20, 0, List.of("route.one")),
                Map.of(
                        "standard", Map.of(
                                "standardId", "a2a",
                                "name", "Agent2Agent Protocol",
                                "version", "1.0",
                                "binding", "JSONRPC",
                                "specUrl", "https://example.test/a2a"),
                        "aligned", false,
                        "requirementCount", 20,
                        "alignedCount", 19,
                        "gapCount", 1,
                        "gapIds", List.of("route.two"),
                        "gapCategories", List.of("route")));

        assertThat(portfolio.standardCount()).isEqualTo(1);
        assertThat(portfolio.aligned()).isFalse();
        assertThat(portfolio.alignedCount()).isZero();
        assertThat(portfolio.gapCount()).isEqualTo(2);
        assertThat(portfolio.standardIds()).containsExactly("a2a");
        assertThat(portfolio.gapStandardIds()).containsExactly("a2a");
        assertThat(portfolio.standards().get(0).alignedCount()).isEqualTo(19);
        assertThat(portfolio.standards().get(0).gapIds()).containsExactly("route.one", "route.two");
        assertThat(portfolio.standards().get(0).gapCategories()).containsExactly("route");
    }

    @Test
    void mergesDuplicateStandardSources() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                Map.of(
                        "diagnosticsId", "a2a.diagnostics",
                        "attributes", Map.of("specAlignment", report(
                                "a2a",
                                "Agent2Agent Protocol",
                                "1.0",
                                "JSONRPC",
                                true,
                                20,
                                20,
                                0,
                                List.of()))),
                Map.of(
                        "reportId", "a2a.spec-compliance",
                        "sourceType", "specCompliance",
                        "specAlignment", report(
                                "a2a",
                                "Agent2Agent Protocol",
                                "1.0",
                                "JSONRPC",
                                false,
                                20,
                                19,
                                1,
                                List.of("route.tasks.get"))));

        WayangStandardAlignmentSummary summary = portfolio.standards().get(0);

        assertThat(portfolio.standardCount()).isEqualTo(1);
        assertThat(summary.sourceCount()).isEqualTo(2);
        assertThat(summary.sources())
                .extracting(WayangStandardAlignmentSource::sourceType)
                .containsExactly("diagnostics", "specCompliance");
        assertThat(summary.sources())
                .extracting(WayangStandardAlignmentSource::reportId)
                .containsExactly("a2a.diagnostics", "a2a.spec-compliance");
    }

    @Test
    void keepsRicherDescriptorWhenMergingDuplicateStandards() {
        WayangStandardAlignmentSummary sparse = new WayangStandardAlignmentSummary(
                new WayangStandardAlignmentDescriptor("a2a", "a2a", "", "", "", Map.of()),
                true,
                0,
                0,
                0,
                List.of(),
                List.of());
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.builder()
                .summary(sparse)
                .reportMap(report("a2a", "Agent2Agent Protocol", "1.0", "JSONRPC", true, 20, 20, 0, List.of()))
                .build();

        WayangStandardAlignmentDescriptor descriptor = portfolio.standards().get(0).standard();

        assertThat(portfolio.standardCount()).isEqualTo(1);
        assertThat(descriptor.toMap())
                .containsEntry("standardId", "a2a")
                .containsEntry("name", "Agent2Agent Protocol")
                .containsEntry("version", "1.0")
                .containsEntry("binding", "JSONRPC")
                .containsEntry("specUrl", "https://example.test/a2a");
    }

    @Test
    void builderComposesReportsSummariesAndExistingPortfolio() {
        WayangStandardAlignmentPortfolio base = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", "Agent2Agent Protocol", "1.0", "JSONRPC", true, 20, 20, 0, List.of()));
        WayangStandardAlignmentSummary commerce = WayangStandardAlignmentSummary.fromReportMap(report(
                "agentic-commerce",
                "Agentic Commerce Protocol",
                "2026-01-30",
                "HTTP+JSON",
                false,
                13,
                12,
                1,
                List.of("route.checkout")));

        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.builder(base)
                .reportMap(Map.of("attributes", Map.of("specAlignment", report(
                        "a2ui",
                        "Agent-to-User Interface",
                        "v0.8",
                        "HTTP",
                        true,
                        11,
                        11,
                        0,
                        List.of()))))
                .summary(commerce)
                .build();

        assertThat(portfolio.standardCount()).isEqualTo(3);
        assertThat(portfolio.aligned()).isFalse();
        assertThat(portfolio.standardIds()).containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(portfolio.gapStandardIds()).containsExactly("agentic-commerce");
    }

    @Test
    void builderIgnoresNullInputs() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.builder()
                .portfolio(null)
                .summary(null)
                .summaries(null)
                .reportMap(null)
                .reportMaps((Map<?, ?>[]) null)
                .reportMaps((List<Map<?, ?>>) null)
                .build();

        assertThat(portfolio.aligned()).isTrue();
        assertThat(portfolio.standardCount()).isZero();
        assertThat(portfolio.standardIds()).isEmpty();
    }

    private static Map<String, Object> report(
            String standardId,
            String name,
            String version,
            String binding,
            boolean aligned,
            int requirementCount,
            int alignedCount,
            int gapCount,
            List<String> gapIds) {
        return Map.of(
                "standard", Map.of(
                        "standardId", standardId,
                        "name", name,
                        "version", version,
                        "binding", binding,
                        "specUrl", "https://example.test/" + standardId),
                "aligned", aligned,
                "requirementCount", requirementCount,
                "alignedCount", alignedCount,
                "gapCount", gapCount,
                "gapIds", gapIds);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangStandardAlignmentMaps.copy((Map<?, ?>) value);
    }
}
