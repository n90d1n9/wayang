package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSamples;

final class SkillsConfigSampleService {

    SkillsConfigSampleReport report(SkillsConfigSampleRequest request) {
        SkillsConfigSampleRequest resolved = request == null
                ? SkillsConfigSampleRequest.defaults()
                : request;
        return new SkillsConfigSampleReport(
                SkillManagementRuntimeConfigSamples.forProfile(resolved.profileName()),
                resolved.format());
    }
}
