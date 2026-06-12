package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSample;

/**
 * Resolved runtime config sample plus the selected text rendering format.
 */
record SkillsConfigSampleReport(
        SkillManagementRuntimeConfigSample sample,
        SkillsConfigSampleFormat format) {

    SkillsConfigSampleReport {
        sample = java.util.Objects.requireNonNull(sample, "sample");
        format = java.util.Objects.requireNonNull(format, "format");
    }
}
