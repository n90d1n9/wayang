package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigCatalog;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigGroup;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigHint;

import java.util.List;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.arrayField;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.name;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsConfigExplainJson {

    private SkillsConfigExplainJson() {
    }

    static String toJson(SkillManagementRuntimeConfigCatalog catalog) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "groupCount", catalog.groups().size());
        field(builder, "hintCount", catalog.hintCount());
        groupsField(builder, catalog.groups());
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }

    private static void groupsField(
            StringBuilder builder,
            List<SkillManagementRuntimeConfigGroup> groups) {
        name(builder, "groups");
        builder.append('[');
        for (SkillManagementRuntimeConfigGroup group : groups) {
            group(builder, group);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    private static void group(
            StringBuilder builder,
            SkillManagementRuntimeConfigGroup group) {
        builder.append('{');
        field(builder, "name", group.name());
        field(builder, "label", group.label());
        hintsField(builder, group.hints());
        trimComma(builder);
        builder.append('}');
    }

    private static void hintsField(
            StringBuilder builder,
            List<SkillManagementRuntimeConfigHint> hints) {
        name(builder, "hints");
        builder.append('[');
        for (SkillManagementRuntimeConfigHint hint : hints) {
            hint(builder, hint);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    private static void hint(
            StringBuilder builder,
            SkillManagementRuntimeConfigHint hint) {
        builder.append('{');
        field(builder, "name", hint.name());
        field(builder, "description", hint.description());
        arrayField(builder, "properties", hint.properties());
        arrayField(builder, "environment", hint.environment());
        field(builder, "defaultValue", hint.defaultValue());
        arrayField(builder, "notes", hint.notes());
        trimComma(builder);
        builder.append('}');
    }
}
