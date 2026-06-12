package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSampleDescriptor;

import java.io.PrintStream;

final class SkillsConfigSamplesText {

    private SkillsConfigSamplesText() {
    }

    static void render(SkillsConfigSampleCatalogReport report, PrintStream out) {
        out.printf("runtime config samples: %d%n", report.samples().size());
        report.samples().forEach(sample -> render(sample, out));
    }

    private static void render(
            SkillManagementRuntimeConfigSampleDescriptor sample,
            PrintStream out) {
        String provider = sample.objectStorageProvider().isBlank()
                ? ""
                : " provider=" + sample.objectStorageProvider();
        out.printf("- %s: profile=%s%s%n", sample.name(), sample.profile(), provider);
        if (!sample.aliases().isEmpty()) {
            out.printf("  aliases: %s%n", String.join(",", sample.aliases()));
        }
        if (!sample.description().isBlank()) {
            out.printf("  description: %s%n", sample.description());
        }
    }
}
