package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceCheckoutSkillRegistryInstallerTest {

    @Test
    void installsDefinitionsAndRuntimeSkillsTogether() {
        AgenticCommerceTestSkillRegistry registry = new AgenticCommerceTestSkillRegistry();
        AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector());

        AgenticCommerceSkillRegistration registration =
                AgenticCommerceCheckoutSkillRegistryInstaller.installAll(registry, service);

        assertThat(registration.successful()).isTrue();
        assertThat(registration.definitionCount()).isEqualTo(5);
        assertThat(registration.runtimeSkillCount()).isEqualTo(5);
        assertThat(registration.toMap())
                .containsEntry("definitionCount", 5)
                .containsEntry("runtimeSkillCount", 5)
                .containsEntry("missingCount", 0);
        assertThat(registry.listSkills()).extracting(SkillDefinition::id)
                .containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());
        assertThat(registry.listAll()).extracting(AgentSkill::id)
                .containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());

        Map<String, Object> created = registry.findOrThrow(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT)
                .execute(Map.of(
                        "items",
                        List.of(Map.of("id", "sku_1", "quantity", 1, "unit_amount", 1000)),
                        "currency",
                        "usd"))
                .await().indefinitely();
        assertThat(created)
                .containsEntry("success", true)
                .containsEntry("checkout_session_id", "cs_1");
    }

    @Test
    void installsDefinitionsOnlyWithoutService() {
        AgenticCommerceTestSkillRegistry registry = new AgenticCommerceTestSkillRegistry();

        AgenticCommerceSkillRegistration registration =
                AgenticCommerceCheckoutSkillRegistryInstaller.installDefinitions(registry);

        assertThat(registration.definitionCount()).isEqualTo(5);
        assertThat(registration.runtimeSkillCount()).isZero();
        assertThat(registry.listSkills()).hasSize(5);
        assertThat(registry.listAll()).isEmpty();
    }

    @Test
    void installsSelectedRuntimeSkillsAndReportsMissingIds() {
        AgenticCommerceTestSkillRegistry registry = new AgenticCommerceTestSkillRegistry();
        AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector());

        AgenticCommerceSkillRegistration registration = AgenticCommerceCheckoutSkillRegistryInstaller.install(
                registry,
                service,
                List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT, "missing-skill"),
                false,
                true);

        assertThat(registration.successful()).isFalse();
        assertThat(registration.requestedSkillIds())
                .containsExactly(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT, "missing-skill");
        assertThat(registration.registeredDefinitionIds()).isEmpty();
        assertThat(registration.registeredRuntimeSkillIds())
                .containsExactly(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT);
        assertThat(registration.missingSkillIds()).containsExactly("missing-skill");
        assertThat(registry.find(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT)).isPresent();
        assertThat(registry.getSkill(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT)).isEmpty();
    }

    @Test
    void runtimeInstallationRequiresService() {
        assertThatThrownBy(() -> AgenticCommerceCheckoutSkillRegistryInstaller.install(
                new AgenticCommerceTestSkillRegistry(),
                null,
                AgenticCommerceWayang.checkoutSkillIds(),
                false,
                true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("service");
    }
}
