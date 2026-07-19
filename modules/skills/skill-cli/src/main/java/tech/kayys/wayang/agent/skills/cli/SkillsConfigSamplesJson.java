package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSampleDescriptor;

import java.util.List;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.arrayField;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.name;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsConfigSamplesJson {

    private SkillsConfigSamplesJson() {
    }

    static String toJson(SkillsConfigSampleCatalogReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "sampleCount", report.samples().size());
        samplesField(builder, report.samples());
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }

    private static void samplesField(
            StringBuilder builder,
            List<SkillManagementRuntimeConfigSampleDescriptor> samples) {
        name(builder, "samples");
        builder.append('[');
        for (SkillManagementRuntimeConfigSampleDescriptor sample : samples) {
            sample(builder, sample);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    private static void sample(
            StringBuilder builder,
            SkillManagementRuntimeConfigSampleDescriptor sample) {
        builder.append('{');
        field(builder, "name", sample.name());
        field(builder, "profile", sample.profile());
        field(builder, "objectStorageProvider", sample.objectStorageProvider());
        field(builder, "description", sample.description());
        arrayField(builder, "aliases", sample.aliases());
        trimComma(builder);
        builder.append('}');
    }
}
