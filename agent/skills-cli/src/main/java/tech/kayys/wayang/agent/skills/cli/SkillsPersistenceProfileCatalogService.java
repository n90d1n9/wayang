package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceProfileCatalog;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminViews;

final class SkillsPersistenceProfileCatalogService {

    SkillManagementAdminPersistenceProfileCatalog catalog() {
        return SkillManagementAdminViews.persistenceProfiles();
    }
}
