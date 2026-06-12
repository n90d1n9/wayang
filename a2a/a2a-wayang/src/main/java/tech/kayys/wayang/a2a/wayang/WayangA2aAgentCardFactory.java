package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.List;

/**
 * Builds A2A Agent Cards from Wayang runtime profile and skill catalogs.
 */
public final class WayangA2aAgentCardFactory {

    private final WayangA2aSkillProjector skillProjector;

    public WayangA2aAgentCardFactory() {
        this(new WayangA2aSkillProjector());
    }

    public WayangA2aAgentCardFactory(WayangA2aSkillProjector skillProjector) {
        this.skillProjector = skillProjector == null ? new WayangA2aSkillProjector() : skillProjector;
    }

    public A2aAgentCard fromSkillDefinitions(WayangA2aAgentProfile profile, List<SkillDefinition> skills) {
        return fromA2aSkills(profile, skillProjector.fromSkillDefinitions(skills));
    }

    public A2aAgentCard fromAgentSkills(WayangA2aAgentProfile profile, List<AgentSkill> skills) {
        return fromA2aSkills(profile, skillProjector.fromAgentSkills(skills));
    }

    public A2aAgentCard fromRegistry(WayangA2aAgentProfile profile, SkillRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        List<SkillDefinition> definitions = registry.listSkills();
        if (definitions != null && !definitions.isEmpty()) {
            return fromSkillDefinitions(profile, definitions);
        }
        return fromAgentSkills(profile, registry.listAll());
    }

    public A2aAgentCard fromA2aSkills(WayangA2aAgentProfile profile, List<A2aAgentSkill> skills) {
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        if (skills == null || skills.isEmpty()) {
            throw new IllegalArgumentException("A2A Agent Card requires at least one projected skill");
        }
        return new A2aAgentCard(
                profile.name(),
                profile.description(),
                profile.supportedInterfaces(),
                profile.provider(),
                profile.version(),
                profile.documentationUrl(),
                profile.capabilities(),
                profile.securitySchemes(),
                profile.securityRequirements(),
                profile.defaultInputModes(),
                profile.defaultOutputModes(),
                skills,
                List.of(),
                profile.iconUrl());
    }
}
