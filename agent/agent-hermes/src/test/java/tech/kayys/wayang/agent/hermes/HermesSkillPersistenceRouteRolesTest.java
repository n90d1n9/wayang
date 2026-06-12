package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceRouteRolesTest {

    @Test
    void normalizesRoleNamesForAdapterLookups() {
        assertThat(HermesSkillPersistenceRouteRoles.normalize("Definitions"))
                .isEqualTo(HermesSkillPersistenceRouteRoles.DEFINITIONS);
        assertThat(HermesSkillPersistenceRouteRoles.normalize("cloud storage"))
                .isEqualTo("cloud-storage");
        assertThat(HermesSkillPersistenceRouteRoles.normalize(null))
                .isEqualTo(HermesSkillPersistenceRouteRoles.CUSTOM);
    }

    @Test
    void routeRoleComparisonUsesCanonicalNames() {
        HermesSkillPersistenceRoute route = new HermesSkillPersistenceRoute(
                "Definitions",
                "database",
                "database",
                10,
                false);

        assertThat(route.role()).isEqualTo(HermesSkillPersistenceRouteRoles.DEFINITIONS);
        assertThat(route.roleIs("definitions")).isTrue();
        assertThat(route.roleIs("Definitions")).isTrue();
        assertThat(route.roleIs("artifacts")).isFalse();
    }
}
