package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentCategorySummariesTest {

    @Test
    void buildsOrderedSummariesFromRequirements() {
        WayangA2aSpecAlignmentCategorySummaries summaries =
                WayangA2aSpecAlignmentCategorySummaries.fromRequirements(List.of(
                        WayangA2aSpecAlignmentRequirement.aligned(
                                "protocol.metadata",
                                "protocol",
                                "Protocol metadata",
                                java.util.Map.of("version", "1.0"),
                                java.util.Map.of("version", "1.0")),
                        WayangA2aSpecAlignmentRequirement.gap(
                                "route.SendMessage",
                                "route",
                                "Send message route",
                                java.util.Map.of("present", true),
                                java.util.Map.of("present", false),
                                "Missing route"),
                        WayangA2aSpecAlignmentRequirement.aligned(
                                "route.GetTask",
                                "route",
                                "Get task route",
                                java.util.Map.of("present", true),
                                java.util.Map.of("present", true))));

        assertThat(summaries.summaries())
                .extracting(WayangA2aSpecAlignmentCategorySummary::category)
                .containsExactly("protocol", "route");
        assertThat(summaries.find(" route "))
                .contains(new WayangA2aSpecAlignmentCategorySummary(
                        "route",
                        2,
                        1,
                        1,
                        List.of("route.SendMessage")));
        assertThat(summaries.gapCategories()).containsExactly("route");
        assertThat(summaries.maps())
                .hasSize(2)
                .last()
                .satisfies(summary -> assertThat(summary)
                        .containsEntry("category", "route")
                        .containsEntry("gapIds", List.of("route.SendMessage")));
    }

    @Test
    void filtersNullSummariesAndIgnoresBlankLookup() {
        WayangA2aSpecAlignmentCategorySummaries summaries =
                WayangA2aSpecAlignmentCategorySummaries.fromSummaries(Arrays.asList(
                        new WayangA2aSpecAlignmentCategorySummary("jsonrpc", 1, 1, 0, List.of()),
                        null));

        assertThat(summaries.summaries()).singleElement()
                .satisfies(summary -> assertThat(summary.category()).isEqualTo("jsonrpc"));
        assertThat(summaries.find(" ")).isEmpty();
        assertThat(summaries.gaps()).isEmpty();
        assertThat(summaries.gapCategories()).isEmpty();
    }
}
