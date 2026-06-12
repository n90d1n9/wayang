package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Shared summary derivation for admin-facing sync projections.
 */
record SkillManagementAdminSyncSummary(
        long copied,
        long updated,
        long unchanged,
        long conflicts,
        long deleted,
        long changed) {

    static SkillManagementAdminSyncSummary from(
            List<? extends SkillManagementAdminSyncChangeView> changes) {
        return new SkillManagementAdminSyncSummary(
                count(changes, "COPIED"),
                count(changes, "UPDATED"),
                count(changes, "UNCHANGED"),
                count(changes, "CONFLICT"),
                count(changes, "DELETED"),
                SkillManagementAdminValueSupport.countMatching(
                        changes,
                        SkillManagementAdminSyncChangeView::changed));
    }

    private static long count(
            List<? extends SkillManagementAdminSyncChangeView> changes,
            String action) {
        return SkillManagementAdminValueSupport.countAction(
                changes,
                SkillManagementAdminSyncChangeView::action,
                action);
    }
}

interface SkillManagementAdminSyncChangeView {

    String action();

    boolean changed();
}
