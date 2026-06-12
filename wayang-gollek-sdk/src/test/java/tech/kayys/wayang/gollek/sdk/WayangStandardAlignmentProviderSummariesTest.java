package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangStandardAlignmentProviderSummariesTest {

    @Test
    void copyDropsNullsAndReturnsImmutableList() {
        WayangStandardAlignmentProviderSummary summary = summary("a2a-provider", "a2a");

        List<WayangStandardAlignmentProviderSummary> copied =
                WayangStandardAlignmentProviderSummaries.copy(Arrays.asList(null, summary));

        assertThat(copied).containsExactly(summary);
        assertThatThrownBy(() -> copied.add(summary))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resolveProviderIdsKeepsExplicitIdsBeforeSummaryIds() {
        assertThat(WayangStandardAlignmentProviderSummaries.resolveProviderIds(
                List.of("explicit-provider"),
                List.of(summary("a2a-provider", "a2a"))))
                .containsExactly("explicit-provider");
    }

    @Test
    void resolveProviderIdsFallsBackToSummaryIds() {
        assertThat(WayangStandardAlignmentProviderSummaries.resolveProviderIds(
                List.of(),
                List.of(summary("a2a-provider", "a2a"), summary("a2ui-provider", "a2ui"))))
                .containsExactly("a2a-provider", "a2ui-provider");
    }

    @Test
    void toMapsRendersProviderSummaryPayloads() {
        assertThat(WayangStandardAlignmentProviderSummaries.toMaps(List.of(summary("a2a-provider", "a2a"))))
                .singleElement()
                .satisfies(provider -> assertThat(provider)
                        .containsEntry("providerId", "a2a-provider")
                        .containsEntry("providerClass", "example.Provider")
                        .containsEntry("priority", 10)
                        .containsEntry("standardCount", 1)
                        .containsEntry("standardIds", List.of("a2a"))
                        .containsEntry("aligned", true)
                        .containsEntry("gapCount", 0));
    }

    private static WayangStandardAlignmentProviderSummary summary(String providerId, String standardId) {
        return WayangStandardAlignmentProviderSummary.from(
                providerId,
                "example.Provider",
                10,
                WayangStandardAlignmentPortfolio.fromReportMaps(report(standardId)));
    }

    private static Map<String, Object> report(String standardId) {
        WayangStandardDefinition definition = WayangStandardRegistry.find(standardId).orElseThrow();
        return Map.of(
                "standard",
                definition.toDescriptor().toMap(),
                "aligned",
                true,
                "requirementCount",
                1,
                "alignedCount",
                1,
                "gapCount",
                0);
    }
}
