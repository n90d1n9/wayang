package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Deployment-only slice of a generic skill-management event page.
 */
record SkillManagementAdminDeploymentHistoryEventWindow(
        int matchedDeployments,
        boolean truncated,
        List<SkillManagementEvent> deploymentEvents) {

    SkillManagementAdminDeploymentHistoryEventWindow {
        deploymentEvents = SkillManagementAdminValueSupport.nonNullList(deploymentEvents).stream()
                .filter(SkillManagementAdminDeploymentHistoryEventWindow::isDeployment)
                .toList();
        matchedDeployments = SkillManagementAdminValueSupport.atLeast(
                SkillManagementAdminValueSupport.nonNegative(matchedDeployments),
                deploymentEvents.size());
        truncated = truncated || matchedDeployments > deploymentEvents.size();
    }

    static SkillManagementAdminDeploymentHistoryEventWindow from(SkillManagementEventPage page) {
        Objects.requireNonNull(page, "page");
        List<SkillManagementEvent> deploymentEvents = page.events().stream()
                .filter(SkillManagementAdminDeploymentHistoryEventWindow::isDeployment)
                .toList();
        boolean deploymentOnlyPage = deploymentEvents.size() == page.returnedEvents();
        return new SkillManagementAdminDeploymentHistoryEventWindow(
                deploymentOnlyPage ? page.matchedEvents() : deploymentEvents.size(),
                deploymentOnlyPage && page.truncated(),
                deploymentEvents);
    }

    private static boolean isDeployment(SkillManagementEvent event) {
        return event != null && event.operation() == SkillManagementEventOperation.DEPLOYMENT;
    }
}
