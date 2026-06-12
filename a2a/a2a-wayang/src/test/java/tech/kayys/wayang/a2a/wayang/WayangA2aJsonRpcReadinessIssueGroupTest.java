package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessIssueGroupTest {

    @Test
    void wrapsIssuesWithProbeName() {
        WayangA2aJsonRpcReadinessIssueGroup group =
                new WayangA2aJsonRpcReadinessIssueGroup(
                        "smoke",
                        List.of(Map.of("code", "smoke_failed")));

        assertThat(group.issueCount()).isEqualTo(1);
        assertThat(group.wrappedIssues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("probe", "smoke")
                        .containsEntry("code", "smoke_failed"));
    }

    @Test
    void normalizesProbeNameAndMissingIssues() {
        WayangA2aJsonRpcReadinessIssueGroup group =
                new WayangA2aJsonRpcReadinessIssueGroup(" methodDispatch ", null);

        assertThat(group.probe()).isEqualTo("methodDispatch");
        assertThat(group.issueCount()).isZero();
        assertThat(group.wrappedIssues()).isEmpty();
    }
}
