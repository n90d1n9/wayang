package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceProfile;

import java.io.PrintStream;

final class SkillsPersistenceProfileInspectText {

    private SkillsPersistenceProfileInspectText() {
    }

    static void render(SkillsPersistenceProfileInspectReport report, PrintStream out) {
        SkillManagementAdminPersistenceProfile profile = report.profile();
        out.printf("profile: %s%n", profile.label());
        out.printf("aliases: %s%n", profile.aliases().isEmpty() ? "-" : String.join(",", profile.aliases()));
        out.printf("description: %s%n", profile.description().isBlank() ? "-" : profile.description());
        SkillsPersistenceStatusText.render(report.status(), out);
    }
}
