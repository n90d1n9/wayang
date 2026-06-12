package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentRequirementSummaryTest {

    @Test
    void summarizesRequirementCountsAndIds() {
        WayangA2aSpecAlignmentRequirementSummary summary =
                new WayangA2aSpecAlignmentRequirementSummary(List.of(
                        WayangA2aSpecAlignmentRequirement.aligned(
                                "protocol.metadata",
                                "protocol",
                                "Protocol metadata",
                                Map.of("version", "1.0"),
                                Map.of("version", "1.0")),
                        WayangA2aSpecAlignmentRequirement.gap(
                                "route.SendMessage",
                                "route",
                                "Send message route",
                                Map.of("present", true),
                                Map.of("present", false),
                                "Missing route")));

        assertThat(summary.aligned()).isFalse();
        assertThat(summary.requirementCount()).isEqualTo(2);
        assertThat(summary.alignedCount()).isEqualTo(1);
        assertThat(summary.gapCount()).isEqualTo(1);
        assertThat(summary.requirementIds()).containsExactly("protocol.metadata", "route.SendMessage");
        assertThat(summary.gapIds()).containsExactly("route.SendMessage");
        assertThat(summary.gaps()).singleElement()
                .satisfies(requirement -> assertThat(requirement.id()).isEqualTo("route.SendMessage"));
    }

    @Test
    void filtersNullRequirementsAndTreatsEmptySummaryAsAligned() {
        WayangA2aSpecAlignmentRequirementSummary summary =
                new WayangA2aSpecAlignmentRequirementSummary(Arrays.asList(null, null));

        assertThat(summary.aligned()).isTrue();
        assertThat(summary.requirementCount()).isZero();
        assertThat(summary.alignedCount()).isZero();
        assertThat(summary.gapCount()).isZero();
        assertThat(summary.requirementIds()).isEmpty();
        assertThat(summary.gapIds()).isEmpty();
        assertThat(summary.gaps()).isEmpty();
    }
}
