package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigCatalog;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigGroupSummary;

import java.util.List;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.name;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsConfigGroupsJson {

    private SkillsConfigGroupsJson() {
    }

    static String toJson(SkillManagementRuntimeConfigCatalog catalog) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "groupCount", catalog.groups().size());
        field(builder, "hintCount", catalog.hintCount());
        groupsField(builder, catalog.groupSummaries());
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }

    private static void groupsField(
            StringBuilder builder,
            List<SkillManagementRuntimeConfigGroupSummary> groups) {
        name(builder, "groups");
        builder.append('[');
        for (SkillManagementRuntimeConfigGroupSummary group : groups) {
            group(builder, group);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    private static void group(
            StringBuilder builder,
            SkillManagementRuntimeConfigGroupSummary group) {
        builder.append('{');
        field(builder, "name", group.name());
        field(builder, "label", group.label());
        field(builder, "hintCount", group.hintCount());
        trimComma(builder);
        builder.append('}');
    }
}
