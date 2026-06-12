package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminEventPruneViewsTest {

    @Test
    void mapsEventPruneResultToStableAdminProjection() {
        SkillManagementEventPruneResult result = new SkillManagementEventPruneResult(
                false,
                1,
                3,
                2,
                List.of("event-a", "event-b"),
                "",
                List.of());

        SkillManagementAdminEventPruneReport view = SkillManagementAdminEventPruneViews.eventPrune(result);

        assertThat(view.dryRun()).isFalse();
        assertThat(view.skipped()).isFalse();
        assertThat(view.success()).isTrue();
        assertThat(view.changed()).isTrue();
        assertThat(view.keepLatestEvents()).isEqualTo(1);
        assertThat(view.scannedEvents()).isEqualTo(3);
        assertThat(view.prunedEvents()).isEqualTo(2);
        assertThat(view.prunedEventReferences()).containsExactly("event-a", "event-b");
        assertThat(view.failure()).isBlank();
        assertThat(view.children()).isEmpty();
    }

    @Test
    void eventPruneReportDerivesOperationalSummaryFromNormalizedData() {
        SkillManagementAdminEventPruneReport applied = new SkillManagementAdminEventPruneReport(
                false,
                false,
                false,
                false,
                1,
                0,
                99,
                java.util.Arrays.asList("old-a", "", null, "old-b"),
                " ",
                List.of());
        SkillManagementAdminEventPruneReport failedChild = new SkillManagementAdminEventPruneReport(
                false,
                false,
                true,
                true,
                1,
                1,
                1,
                List.of("old-child"),
                "child failure",
                List.of());

        SkillManagementAdminEventPruneReport parent = new SkillManagementAdminEventPruneReport(
                false,
                false,
                true,
                true,
                1,
                1,
                99,
                List.of("parent-old"),
                "",
                java.util.Arrays.asList(applied, failedChild, null));

        assertThat(applied.success()).isTrue();
        assertThat(applied.changed()).isTrue();
        assertThat(applied.scannedEvents()).isEqualTo(2);
        assertThat(applied.prunedEvents()).isEqualTo(2);
        assertThat(applied.prunedEventReferences()).containsExactly("old-a", "old-b");
        assertThat(applied.failure()).isBlank();
        assertThat(failedChild.success()).isFalse();
        assertThat(failedChild.changed()).isFalse();
        assertThat(parent.success()).isFalse();
        assertThat(parent.changed()).isFalse();
        assertThat(parent.children()).containsExactly(applied, failedChild);
    }

    @Test
    void mapsCompositeEventPruneResultWithChildren() {
        SkillManagementEventPruneOptions options = SkillManagementEventPruneOptions.keepLatest(1);
        SkillManagementEventPruneResult applied = SkillManagementEventPruneResult.success(
                options,
                3,
                List.of("old-a", "old-b"));
        SkillManagementEventPruneResult failed = SkillManagementEventPruneResult.failure(
                options,
                "write-only event sink does not support pruning");
        SkillManagementEventPruneResult result = SkillManagementEventPruneResult.composite(
                options,
                List.of(applied, failed));

        SkillManagementAdminEventPruneReport view = SkillManagementAdminEventPruneViews.eventPrune(result);

        assertThat(view.success()).isFalse();
        assertThat(view.changed()).isFalse();
        assertThat(view.scannedEvents()).isEqualTo(3);
        assertThat(view.prunedEvents()).isEqualTo(2);
        assertThat(view.prunedEventReferences()).containsExactly("old-a", "old-b");
        assertThat(view.failure()).isEqualTo("write-only event sink does not support pruning");
        assertThat(view.children()).hasSize(2);
        assertThat(view.children().get(0).success()).isTrue();
        assertThat(view.children().get(0).changed()).isTrue();
        assertThat(view.children().get(1).success()).isFalse();
        assertThat(view.children().get(1).failure())
                .isEqualTo("write-only event sink does not support pruning");
    }
}
