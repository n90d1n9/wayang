package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Maps operation trace event pages to stable admin DTOs.
 */
final class SkillManagementAdminOperationTraceViews {

    private SkillManagementAdminOperationTraceViews() {
    }

    static SkillManagementAdminOperationTrace operationTrace(
            String operationId,
            SkillManagementEventPage page) {
        Objects.requireNonNull(page, "page");
        return operationTrace(operationId, page, page);
    }

    static SkillManagementAdminOperationTrace operationTrace(
            String operationId,
            SkillManagementEventPage rootPage,
            SkillManagementEventPage childPage) {
        Objects.requireNonNull(rootPage, "rootPage");
        Objects.requireNonNull(childPage, "childPage");
        String resolvedOperationId = SkillManagementAdminValueSupport.identifier(operationId);
        SkillManagementAdminEvent rootEvent = rootPage.events().stream()
                .map(SkillManagementAdminEventViews::event)
                .filter(event -> resolvedOperationId.equals(event.operationId()))
                .findFirst()
                .orElse(null);
        List<SkillManagementAdminEvent> childEvents = childPage.events().stream()
                .map(SkillManagementAdminEventViews::event)
                .toList();
        return new SkillManagementAdminOperationTrace(resolvedOperationId, rootEvent, childEvents);
    }
}
