package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceProfile;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.arrayField;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.name;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsPersistenceProfileInspectJson {

    private SkillsPersistenceProfileInspectJson() {
    }

    static String toJson(SkillsPersistenceProfileInspectReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        profileFields(builder, report.profile());
        name(builder, "status");
        builder.append(SkillsPersistenceStatusJson.toJson(report.status())).append(',');
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }

    private static void profileFields(
            StringBuilder builder,
            SkillManagementAdminPersistenceProfile profile) {
        field(builder, "label", profile.label());
        arrayField(builder, "aliases", profile.aliases());
        field(builder, "description", profile.description());
    }
}
