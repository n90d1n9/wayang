package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminPersistenceViewsTest {

    @Test
    void mapsDefaultPersistenceStrategyToAdminProjection() {
        SkillManagementAdminPersistenceStrategy view =
                SkillManagementAdminPersistenceViews.persistenceStrategy(SkillManagementServiceConfig.defaults());

        assertThat(view.strategy()).isEqualTo("ephemeral");
        assertThat(view.fullyDurable()).isFalse();
        assertThat(view.hasEphemeralRole()).isTrue();
        assertThat(view.hasDurableFallback()).isFalse();
        assertThat(view.roleCount()).isEqualTo(4);
        assertThat(view.durableRoleCount()).isZero();
        assertThat(view.ephemeralRoleCount()).isEqualTo(3);
        assertThat(view.disabledRoleCount()).isEqualTo(1);
        assertThat(view.warningCount()).isEqualTo(2);
        assertThat(view.warnings()).containsExactly(
                "Disabled skill persistence roles: event-history",
                "Ephemeral skill persistence roles: definition, lifecycle-state, artifact");
    }

    @Test
    void mapsHybridFallbackStrategyWithChildRoles() {
        SkillManagementServiceConfig config = SkillManagementServiceProfiles.config(
                SkillManagementServiceProfile.HYBRID_OBJECT_FILE,
                SkillManagementServiceProfileOptions.defaults()
                        .withObjectPrefix("prod/skills")
                        .withBaseDirectory(Path.of("/var/lib/wayang/skills")));

        SkillManagementAdminPersistenceStrategy view =
                SkillManagementAdminPersistenceViews.persistenceStrategy(config.persistenceStrategy());

        assertThat(view.strategy()).isEqualTo("hybrid-fallback");
        assertThat(view.fullyDurable()).isTrue();
        assertThat(view.hasExternalProvider()).isTrue();
        assertThat(view.hasCompositeProvider()).isTrue();
        assertThat(view.hasDurableFallback()).isTrue();
        assertThat(view.warnings()).isEmpty();

        SkillManagementAdminPersistenceRole definition = view.roles().stream()
                .filter(role -> role.role().equals("definition"))
                .findFirst()
                .orElseThrow();
        assertThat(definition.strategy()).isEqualTo("hybrid-fallback");
        assertThat(definition.persistenceClass()).isEqualTo("composed");
        assertThat(definition.durable()).isTrue();
        assertThat(definition.durableFallback()).isTrue();
        assertThat(definition.capabilities()).contains("read", "write", "delete", "list", "primary-fallback");
        assertThat(definition.children())
                .extracting(SkillManagementAdminPersistenceRole::provider)
                .containsExactly("object-storage", "filesystem");
    }

    @Test
    void mapsPersistenceProfileCatalogToAdminProjection() {
        SkillManagementAdminPersistenceProfileCatalog catalog =
                SkillManagementAdminPersistenceViews.persistenceProfiles();

        assertThat(catalog.profileCount()).isEqualTo(6);
        assertThat(catalog.durableProfileCount()).isEqualTo(5);
        assertThat(catalog.compositeProfileCount()).isEqualTo(2);
        assertThat(catalog.mirroredProfileCount()).isEqualTo(1);
        assertThat(catalog.profiles())
                .extracting(SkillManagementAdminPersistenceProfile::label)
                .containsExactly(
                        "default",
                        "local-filesystem",
                        "object-storage",
                        "jdbc",
                        "hybrid-object-file",
                        "mirrored-object-file");

        SkillManagementAdminPersistenceProfile objectStorage = catalog.profiles().stream()
                .filter(profile -> profile.label().equals("object-storage"))
                .findFirst()
                .orElseThrow();
        assertThat(objectStorage.aliases()).contains("s3", "rustfs", "cloud-storage");
        assertThat(objectStorage.description()).contains("S3/RustFS-compatible");
        assertThat(objectStorage.persistence().strategy()).isEqualTo("object-storage");
        assertThat(objectStorage.persistence().fullyDurable()).isTrue();
        assertThat(objectStorage.persistence().hasExternalProvider()).isTrue();
    }

    @Test
    void mapsSinglePersistenceProfileToAdminProjection() {
        SkillManagementAdminPersistenceProfile profile =
                SkillManagementAdminPersistenceViews.persistenceProfile("mirror");

        assertThat(profile.label()).isEqualTo("mirrored-object-file");
        assertThat(profile.aliases()).contains("mirrored", "mirror", "replicated");
        assertThat(profile.persistence().strategy()).isEqualTo("mirrored");
        assertThat(profile.persistence().hasMirroredProvider()).isTrue();
        assertThat(profile.persistence().hasCompositeProvider()).isTrue();
    }

    @Test
    void adminPersistenceRecordsDeriveCountsAndNormalizeLists() {
        SkillManagementAdminPersistenceRole durable = new SkillManagementAdminPersistenceRole(
                " definition ",
                "definition",
                "filesystem",
                "filesystem",
                "local-filesystem",
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                java.util.Arrays.asList("read", "", null, "write"),
                List.of());
        SkillManagementAdminPersistenceRole disabled = new SkillManagementAdminPersistenceRole(
                "event-history",
                "event-history",
                "none",
                "disabled",
                "disabled",
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                List.of(),
                List.of());

        SkillManagementAdminPersistenceStrategy view = new SkillManagementAdminPersistenceStrategy(
                "mixed",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                99,
                99,
                99,
                99,
                99,
                99,
                java.util.Arrays.asList("warning", "", null),
                List.of(durable, disabled));

        assertThat(view.roleCount()).isEqualTo(2);
        assertThat(view.durableRoleCount()).isEqualTo(1);
        assertThat(view.disabledRoleCount()).isEqualTo(1);
        assertThat(view.warningCount()).isEqualTo(1);
        assertThat(view.warnings()).containsExactly("warning");
        assertThat(durable.role()).isEqualTo("definition");
        assertThat(durable.capabilities()).containsExactly("read", "write");

        SkillManagementAdminPersistenceProfile profile = new SkillManagementAdminPersistenceProfile(
                " custom-profile ",
                java.util.Arrays.asList("custom", "", null),
                null,
                view);
        SkillManagementAdminPersistenceProfileCatalog catalog =
                new SkillManagementAdminPersistenceProfileCatalog(List.of(profile));

        assertThat(profile.label()).isEqualTo("custom-profile");
        assertThat(profile.aliases()).containsExactly("custom");
        assertThat(profile.description()).isEmpty();
        assertThat(catalog.profileCount()).isEqualTo(1);
        assertThat(catalog.durableProfileCount()).isZero();
    }
}
