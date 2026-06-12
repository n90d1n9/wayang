package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutAgentSkillTest {

    @Test
    void factoryCreatesExecutableCheckoutSkills() {
        AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector());

        List<AgentSkill> skills = AgenticCommerceCheckoutAgentSkills.skills(service);

        assertThat(skills).hasSize(5);
        assertThat(skills).extracting(AgentSkill::id)
                .containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());
        assertThat(skills).allSatisfy(skill -> {
            assertThat(skill.category()).isEqualTo(SkillCategory.EXECUTION.name());
            assertThat(skill.aliases()).hasSize(1);
            assertThat(skill.canHandle(Map.of("skillId", skill.id()))).isTrue();
        });
    }

    @Test
    void createAndRetrieveSkillsExecuteThroughCheckoutService() {
        AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector());
        AgentSkill create = AgenticCommerceCheckoutAgentSkills
                .skillForId(service, AgenticCommerceWayang.SKILL_CREATE_CHECKOUT)
                .orElseThrow();
        AgentSkill retrieve = AgenticCommerceCheckoutAgentSkills
                .skillForId(service, AgenticCommerceWayang.SKILL_RETRIEVE_CHECKOUT)
                .orElseThrow();

        Map<String, Object> created = create.execute(Map.of(
                "line_items",
                List.of(Map.of("id", "sku_1", "name", "Wayang Tee", "quantity", 2, "unit_amount", 3000)),
                "currency",
                "usd"))
                .await().indefinitely();
        Map<String, Object> retrieved = retrieve.execute(Map.of("checkout_session_id", "cs_1"))
                .await().indefinitely();

        assertThat(created)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS")
                .containsEntry("status_code", 201)
                .containsEntry("checkout_session_id", "cs_1")
                .containsEntry("checkout_status", "open");
        assertThat(map(created.get("checkout_session")))
                .containsEntry("currency", "USD");
        assertThat(retrieved)
                .containsEntry("success", true)
                .containsEntry("status_code", 200)
                .containsEntry("checkout_session_id", "cs_1");
    }

    @Test
    void updateCompleteAndCancelSkillsShareServiceState() {
        AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector());
        AgentSkill create = AgenticCommerceCheckoutAgentSkill.of(service, AgenticCommerceWayang.SKILL_CREATE_CHECKOUT);
        AgentSkill update = AgenticCommerceCheckoutAgentSkill.of(service, AgenticCommerceWayang.SKILL_UPDATE_CHECKOUT);
        AgentSkill complete = AgenticCommerceCheckoutAgentSkill.of(service, AgenticCommerceWayang.SKILL_COMPLETE_CHECKOUT);
        AgentSkill cancel = AgenticCommerceCheckoutAgentSkill.of(service, AgenticCommerceWayang.SKILL_CANCEL_CHECKOUT);

        create.execute(Map.of(
                "payload",
                Map.of(
                        "items",
                        List.of(Map.of("id", "sku_1", "quantity", 1, "unit_amount", 1000)),
                        "currency",
                        "usd")))
                .await().indefinitely();
        Map<String, Object> updated = update.execute(Map.of(
                "checkout_session_id",
                "cs_1",
                "payload",
                Map.of("items", List.of(Map.of("id", "sku_2", "quantity", 1, "unit_amount", 1200)))))
                .await().indefinitely();
        Map<String, Object> completed = complete.execute(Map.of(
                "checkout_session_id",
                "cs_1",
                "payload",
                Map.of("payment_data", Map.of("handler_id", "test"))))
                .await().indefinitely();
        Map<String, Object> canceled = cancel.execute(Map.of("checkout_session_id", "cs_1"))
                .await().indefinitely();

        assertThat(updated).containsEntry("checkout_status", "ready_for_payment");
        assertThat(completed).containsEntry("checkout_status", "completed");
        assertThat(canceled).containsEntry("checkout_status", "canceled");
    }

    @Test
    void missingSessionIdReturnsRuntimeSkillError() {
        AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector());
        AgentSkill retrieve = AgenticCommerceCheckoutAgentSkill.of(service, AgenticCommerceWayang.SKILL_RETRIEVE_CHECKOUT);

        Map<String, Object> result = retrieve.execute(Map.of()).await().indefinitely();

        assertThat(result)
                .containsEntry("success", false)
                .containsEntry("status", "ERROR")
                .containsEntry("skill_id", AgenticCommerceWayang.SKILL_RETRIEVE_CHECKOUT)
                .containsEntry("error", IllegalArgumentException.class.getName());
        assertThat(String.valueOf(result.get("observation"))).contains("checkout_session_id");
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
