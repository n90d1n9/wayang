package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSample;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSampleEntry;

import java.util.List;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.name;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsConfigSampleJson {

    private SkillsConfigSampleJson() {
    }

    static String toJson(SkillManagementRuntimeConfigSample sample) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "profile", sample.profile());
        field(builder, "description", sample.description());
        field(builder, "propertyCount", sample.properties().size());
        field(builder, "environmentCount", sample.environment().size());
        entriesField(builder, "properties", sample.properties());
        entriesField(builder, "environment", sample.environment());
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }

    private static void entriesField(
            StringBuilder builder,
            String name,
            List<SkillManagementRuntimeConfigSampleEntry> entries) {
        name(builder, name);
        builder.append('[');
        for (SkillManagementRuntimeConfigSampleEntry entry : entries) {
            entry(builder, entry);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    private static void entry(
            StringBuilder builder,
            SkillManagementRuntimeConfigSampleEntry entry) {
        builder.append('{');
        field(builder, "key", entry.key());
        field(builder, "value", entry.value());
        field(builder, "description", entry.description());
        trimComma(builder);
        builder.append('}');
    }
}
