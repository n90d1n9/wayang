package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceProfile;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceProfileCatalog;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;

import java.util.List;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.arrayField;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.name;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsPersistenceProfileCatalogJson {

    private SkillsPersistenceProfileCatalogJson() {
    }

    static String toJson(SkillManagementAdminPersistenceProfileCatalog catalog) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "profileCount", catalog.profileCount());
        field(builder, "durableProfileCount", catalog.durableProfileCount());
        field(builder, "externalProfileCount", catalog.externalProfileCount());
        field(builder, "compositeProfileCount", catalog.compositeProfileCount());
        field(builder, "mirroredProfileCount", catalog.mirroredProfileCount());
        field(builder, "durableFallbackProfileCount", catalog.durableFallbackProfileCount());
        profilesField(builder, catalog.profiles());
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }

    private static void profilesField(
            StringBuilder builder,
            List<SkillManagementAdminPersistenceProfile> profiles) {
        name(builder, "profiles");
        builder.append('[');
        for (SkillManagementAdminPersistenceProfile profile : profiles) {
            profile(builder, profile);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    private static void profile(
            StringBuilder builder,
            SkillManagementAdminPersistenceProfile profile) {
        SkillManagementAdminPersistenceStrategy persistence = profile.persistence();
        builder.append('{');
        field(builder, "label", profile.label());
        arrayField(builder, "aliases", profile.aliases());
        field(builder, "description", profile.description());
        field(builder, "strategy", persistence.strategy());
        field(builder, "fullyDurable", persistence.fullyDurable());
        field(builder, "hasExternalProvider", persistence.hasExternalProvider());
        field(builder, "hasCompositeProvider", persistence.hasCompositeProvider());
        field(builder, "hasMirroredProvider", persistence.hasMirroredProvider());
        field(builder, "hasDurableFallback", persistence.hasDurableFallback());
        field(builder, "roleCount", persistence.roleCount());
        field(builder, "warningCount", persistence.warningCount());
        trimComma(builder);
        builder.append('}');
    }
}
