package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceProfileCatalog;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;

import java.io.PrintStream;

final class SkillsPersistenceProfileCatalogText {

    private SkillsPersistenceProfileCatalogText() {
    }

    static void render(SkillManagementAdminPersistenceProfileCatalog catalog, PrintStream out) {
        out.printf(
                "persistence profiles: %d durable=%d external=%d composite=%d mirrored=%d durable-fallback=%d%n",
                catalog.profileCount(),
                catalog.durableProfileCount(),
                catalog.externalProfileCount(),
                catalog.compositeProfileCount(),
                catalog.mirroredProfileCount(),
                catalog.durableFallbackProfileCount());
        catalog.profiles().forEach(profile -> {
            SkillManagementAdminPersistenceStrategy persistence = profile.persistence();
            out.printf(
                    "- %s: strategy=%s durable=%s roles=%d warnings=%d%n",
                    profile.label(),
                    persistence.strategy(),
                    persistence.fullyDurable(),
                    persistence.roleCount(),
                    persistence.warningCount());
            out.printf(
                    "  providers: external=%s composite=%s mirrored=%s durable-fallback=%s%n",
                    persistence.hasExternalProvider(),
                    persistence.hasCompositeProvider(),
                    persistence.hasMirroredProvider(),
                    persistence.hasDurableFallback());
            if (!profile.aliases().isEmpty()) {
                out.printf("  aliases: %s%n", String.join(",", profile.aliases()));
            }
            if (!profile.description().isBlank()) {
                out.printf("  %s%n", profile.description());
            }
        });
    }
}
