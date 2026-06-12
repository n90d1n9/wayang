package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSample;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSampleEntry;

import java.io.PrintStream;
import java.util.List;

final class SkillsConfigSampleText {

    private SkillsConfigSampleText() {
    }

    static void render(
            SkillManagementRuntimeConfigSample sample,
            SkillsConfigSampleFormat format,
            PrintStream out) {
        out.printf("# skill-management runtime config sample: %s%n", sample.profile());
        if (!sample.description().isBlank()) {
            out.printf("# %s%n", sample.description());
        }
        entries(sample, format).forEach(entry -> out.printf("%s=%s%n", entry.key(), entry.value()));
    }

    private static List<SkillManagementRuntimeConfigSampleEntry> entries(
            SkillManagementRuntimeConfigSample sample,
            SkillsConfigSampleFormat format) {
        return switch (format) {
            case PROPERTIES -> sample.properties();
            case ENV -> sample.environment();
        };
    }
}
