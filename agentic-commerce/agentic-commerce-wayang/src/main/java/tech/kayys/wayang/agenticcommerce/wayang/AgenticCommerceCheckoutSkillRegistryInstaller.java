package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Installs Agentic Commerce checkout definitions and runtime skills into a registry.
 */
public final class AgenticCommerceCheckoutSkillRegistryInstaller {

    private AgenticCommerceCheckoutSkillRegistryInstaller() {
    }

    public static AgenticCommerceSkillRegistration installAll(
            SkillRegistry registry,
            AgenticCommerceCheckoutService service) {
        return install(registry, service, AgenticCommerceWayang.checkoutSkillIds(), true, true);
    }

    public static AgenticCommerceSkillRegistration installDefinitions(SkillRegistry registry) {
        return install(registry, null, AgenticCommerceWayang.checkoutSkillIds(), true, false);
    }

    public static AgenticCommerceSkillRegistration installRuntimeSkills(
            SkillRegistry registry,
            AgenticCommerceCheckoutService service) {
        return install(registry, service, AgenticCommerceWayang.checkoutSkillIds(), false, true);
    }

    public static AgenticCommerceSkillRegistration install(
            SkillRegistry registry,
            AgenticCommerceCheckoutService service,
            List<String> skillIds,
            boolean includeDefinitions,
            boolean includeRuntimeSkills) {
        Objects.requireNonNull(registry, "registry");
        if (includeRuntimeSkills) {
            Objects.requireNonNull(service, "service");
        }
        AgenticCommerceCheckoutSkillProjector projector = new AgenticCommerceCheckoutSkillProjector();
        List<String> requested = requestedSkillIds(skillIds);
        List<String> registeredDefinitions = new ArrayList<>();
        List<String> registeredRuntimeSkills = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skillId : requested) {
            projector.skillForId(skillId).ifPresentOrElse(
                    definition -> installOne(
                            registry,
                            service,
                            definition,
                            includeDefinitions,
                            includeRuntimeSkills,
                            registeredDefinitions,
                            registeredRuntimeSkills),
                    () -> missing.add(skillId));
        }
        return new AgenticCommerceSkillRegistration(
                requested,
                registeredDefinitions,
                registeredRuntimeSkills,
                missing,
                metadata(includeDefinitions, includeRuntimeSkills));
    }

    private static void installOne(
            SkillRegistry registry,
            AgenticCommerceCheckoutService service,
            SkillDefinition definition,
            boolean includeDefinitions,
            boolean includeRuntimeSkills,
            List<String> registeredDefinitions,
            List<String> registeredRuntimeSkills) {
        if (includeDefinitions) {
            registry.registerSkill(definition);
            registeredDefinitions.add(definition.id());
        }
        if (includeRuntimeSkills) {
            AgentSkill runtimeSkill = new AgenticCommerceCheckoutAgentSkill(service, definition);
            registry.register(runtimeSkill);
            registeredRuntimeSkills.add(runtimeSkill.id());
        }
    }

    private static List<String> requestedSkillIds(List<String> skillIds) {
        List<String> requested = AgenticCommerceWayangMaps.stringList(skillIds);
        return requested.isEmpty() ? AgenticCommerceWayang.checkoutSkillIds() : requested;
    }

    private static Map<String, Object> metadata(boolean includeDefinitions, boolean includeRuntimeSkills) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("definitionsIncluded", includeDefinitions);
        values.put("runtimeSkillsIncluded", includeRuntimeSkills);
        return Map.copyOf(values);
    }
}
