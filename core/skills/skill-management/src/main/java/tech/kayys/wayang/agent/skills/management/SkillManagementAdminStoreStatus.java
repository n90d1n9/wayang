package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;

/**
 * Stable admin-facing projection of a skill persistence store.
 */
public record SkillManagementAdminStoreStatus(
        String name,
        String storeType,
        String status,
        boolean ready,
        int itemCount,
        List<String> itemIds,
        Map<String, Integer> statusCounts,
        String failure,
        List<SkillManagementAdminStoreStatus> children,
        List<String> capabilities) {

    public SkillManagementAdminStoreStatus(
            String name,
            String storeType,
            String status,
            boolean ready,
            int itemCount,
            List<String> itemIds,
            Map<String, Integer> statusCounts,
            String failure,
            List<SkillManagementAdminStoreStatus> children) {
        this(name, storeType, status, ready, itemCount, itemIds, statusCounts, failure, children, List.of());
    }

    public SkillManagementAdminStoreStatus {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (storeType == null || storeType.isBlank()) {
            throw new IllegalArgumentException("storeType must not be blank");
        }
        status = SkillManagementAdminValueSupport.unknownIfBlank(status);
        itemCount = SkillManagementAdminValueSupport.nonNegative(itemCount);
        itemIds = SkillManagementAdminValueSupport.sortedDistinctStrings(itemIds);
        statusCounts = SkillManagementAdminValueSupport.nonNegativeCounts(statusCounts);
        failure = SkillManagementAdminValueSupport.text(failure);
        children = SkillManagementAdminValueSupport.nonNullList(children);
        capabilities = SkillManagementAdminValueSupport.sortedDistinctStrings(capabilities);
    }
}
