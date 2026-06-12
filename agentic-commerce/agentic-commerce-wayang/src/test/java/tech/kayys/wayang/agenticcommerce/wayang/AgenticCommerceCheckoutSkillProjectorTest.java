package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutSkillProjectorTest {

    private final AgenticCommerceCheckoutSkillProjector projector = new AgenticCommerceCheckoutSkillProjector();

    @Test
    void projectsCheckoutOperationsAsWayangSkills() {
        List<SkillDefinition> skills = projector.checkoutSkills();

        assertThat(skills).hasSize(5);
        assertThat(skills).extracting(SkillDefinition::id)
                .containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());
        assertThat(skills).allSatisfy(skill -> {
            assertThat(skill.category()).isEqualTo(SkillCategory.EXECUTION.name());
            assertThat(skill.tools()).containsExactly(AgenticCommerceWayang.PROTOCOL_ID);
            assertThat(skill.metadata()).containsEntry(
                    AgenticCommerceWayang.METADATA_PROTOCOL,
                    AgenticCommerceWayang.PROTOCOL_ID);
            assertThat(skill.metadata()).containsEntry(SkillMetadataKeys.KEY_OUTPUT_FORMAT, "json");
        });
    }

    @Test
    void createSkillCarriesProtocolRouteAndInputSchema() {
        SkillDefinition skill = projector.skillForOperation(AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                .orElseThrow();

        assertThat(skill.id()).isEqualTo(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT);
        assertThat(skill.metadata())
                .containsEntry(AgenticCommerceWayang.METADATA_OPERATION,
                        AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                .containsEntry(AgenticCommerceWayang.METADATA_HTTP_METHOD, "POST")
                .containsEntry(AgenticCommerceWayang.METADATA_PATH_TEMPLATE,
                        AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS);

        assertThat(skill.metadata().get(SkillMetadataKeys.KEY_INPUT_SCHEMA))
                .isInstanceOfSatisfying(Map.class, schema -> assertThat(schema)
                        .containsEntry("type", "object")
                        .containsEntry("additionalProperties", true)
                        .containsEntry("required", List.of("buyer", "line_items", "currency")));
    }

    @Test
    void lookupsReturnEmptyForUnknownKeys() {
        assertThat(projector.skillForId("missing")).isEmpty();
        assertThat(projector.skillForOperation("missing")).isEmpty();
    }
}
