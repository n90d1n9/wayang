package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SdkFacetsTest {

    @Test
    void extractsOrderedDistinctValues() {
        List<FacetItem> items = List.of(
                new FacetItem("Runs", List.of("run-json")),
                new FacetItem("Run Specs", List.of("run-spec-json")),
                new FacetItem("Runs", List.of("run-status-json")));

        assertThat(SdkFacets.values(items, FacetItem::name))
                .containsExactly("Runs", "Run Specs");
    }

    @Test
    void extractsNormalizedNonBlankTextValues() {
        List<FacetItem> items = List.of(
                new FacetItem(" Runs ", List.of()),
                new FacetItem(" ", List.of()),
                new FacetItem("Runs", List.of()),
                new FacetItem("Tools", List.of()));

        assertThat(SdkFacets.textValues(items, FacetItem::name))
                .containsExactly("Runs", "Tools");
    }

    @Test
    void extractsFlattenedOrderedDistinctValues() {
        List<FacetItem> items = List.of(
                new FacetItem("Runs", List.of("run-json", "run-status-json")),
                new FacetItem("Run Specs", List.of("run-status-json", "run-spec-json")));

        assertThat(SdkFacets.flatValues(items, FacetItem::values))
                .containsExactly("run-json", "run-status-json", "run-spec-json");
    }

    @Test
    void countsFacetValues() {
        List<FacetItem> items = List.of(
                new FacetItem("Runs", List.of("run-json", "run-status-json")),
                new FacetItem("Runs", List.of("run-json")),
                new FacetItem("Run Specs", List.of("run-status-json", "run-spec-json")));

        assertThat(SdkFacets.counts(items, FacetItem::name))
                .containsExactly(
                        Map.entry("Runs", 2),
                        Map.entry("Run Specs", 1));
        assertThat(SdkFacets.textCounts(items, FacetItem::name))
                .containsExactly(
                        Map.entry("Runs", 2),
                        Map.entry("Run Specs", 1));
        assertThat(SdkFacets.flatCounts(items, FacetItem::values))
                .containsExactly(
                        Map.entry("run-json", 2),
                        Map.entry("run-status-json", 2),
                        Map.entry("run-spec-json", 1));
    }

    private record FacetItem(String name, List<String> values) {
    }
}
