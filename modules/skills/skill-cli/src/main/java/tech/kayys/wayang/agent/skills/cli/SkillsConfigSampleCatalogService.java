package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSamples;

final class SkillsConfigSampleCatalogService {

    SkillsConfigSampleCatalogReport report() {
        return new SkillsConfigSampleCatalogReport(SkillManagementRuntimeConfigSamples.samples());
    }
}
