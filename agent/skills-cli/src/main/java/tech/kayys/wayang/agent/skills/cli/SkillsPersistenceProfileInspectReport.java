package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceProfile;

import java.util.Objects;

record SkillsPersistenceProfileInspectReport(
        SkillManagementAdminPersistenceProfile profile,
        SkillsPersistenceStatusReport status) {

    SkillsPersistenceProfileInspectReport {
        profile = Objects.requireNonNull(profile, "profile");
        status = Objects.requireNonNull(status, "status");
    }
}
