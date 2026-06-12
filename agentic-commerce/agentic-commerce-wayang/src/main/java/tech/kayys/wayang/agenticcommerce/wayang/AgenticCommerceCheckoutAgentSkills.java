package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Factory helpers for registering Agentic Commerce checkout runtime skills.
 */
public final class AgenticCommerceCheckoutAgentSkills {

    private AgenticCommerceCheckoutAgentSkills() {
    }

    public static List<AgentSkill> skills(AgenticCommerceCheckoutService service) {
        AgenticCommerceCheckoutSkillProjector projector = new AgenticCommerceCheckoutSkillProjector();
        return projector.checkoutSkills().stream()
                .map(definition -> new AgenticCommerceCheckoutAgentSkill(service, definition))
                .map(AgentSkill.class::cast)
                .toList();
    }

    public static Optional<AgentSkill> skillForId(
            AgenticCommerceCheckoutService service,
            String skillId) {
        Objects.requireNonNull(service, "service");
        return new AgenticCommerceCheckoutSkillProjector()
                .skillForId(skillId)
                .map(definition -> new AgenticCommerceCheckoutAgentSkill(service, definition));
    }

    public static Optional<AgentSkill> skillForDefinition(
            AgenticCommerceCheckoutService service,
            SkillDefinition definition) {
        Objects.requireNonNull(service, "service");
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(new AgenticCommerceCheckoutAgentSkill(service, definition));
    }
}
