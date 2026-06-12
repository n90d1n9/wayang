package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceProfile;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminViews;

final class SkillsPersistenceProfileInspectService {

    private final SkillsPersistenceStatusService statusService;

    SkillsPersistenceProfileInspectService(SkillsPersistenceStatusService statusService) {
        this.statusService = java.util.Objects.requireNonNull(statusService, "statusService");
    }

    SkillsPersistenceProfileInspectReport report(SkillsPersistenceProfileInspectRequest request) {
        SkillsPersistenceProfileInspectRequest resolved = request == null
                ? SkillsPersistenceProfileInspectRequest.defaults()
                : request;
        SkillManagementAdminPersistenceProfile profile =
                SkillManagementAdminViews.persistenceProfile(resolved.profileName());
        SkillsPersistenceStatusReport status = statusService.report(SkillsPersistenceStatusRequest.fromOptions(
                resolved.profileName(),
                false,
                resolved.includePreflight(),
                resolved.includeDiagnostics()));
        return new SkillsPersistenceProfileInspectReport(profile, status);
    }
}
