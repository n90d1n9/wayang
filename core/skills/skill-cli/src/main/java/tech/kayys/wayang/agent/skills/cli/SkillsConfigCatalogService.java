package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigCatalog;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigHints;

final class SkillsConfigCatalogService {

    SkillManagementRuntimeConfigCatalog catalog(SkillsConfigCatalogRequest request) {
        SkillsConfigCatalogRequest resolved = request == null
                ? SkillsConfigCatalogRequest.defaults()
                : request;
        SkillManagementRuntimeConfigCatalog catalog = SkillManagementRuntimeConfigHints.catalog();
        return resolved.hasGroup() ? catalog.selectGroup(resolved.groupName()) : catalog;
    }
}
