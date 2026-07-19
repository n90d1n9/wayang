package tech.kayys.wayang.skill;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import tech.kayys.wayang.agent.skill.AgentSkill;
import tech.kayys.wayang.agent.skill.AgentSkillDescriptor;
import tech.kayys.wayang.agent.skill.AgentSkillState;
import tech.kayys.wayang.client.SdkText;

public record RegisteredSkill(
        AgentSkillDescriptor descriptor,
        AgentSkillState state,
        List<String> aliases) implements AgentSkill {

    public RegisteredSkill {
        descriptor = Objects.requireNonNull(descriptor, "Skill descriptor is required.");
        state = state == null ? AgentSkillState.ACTIVE : state;
        aliases = normalizeAliases(aliases);
    }

    public static RegisteredSkill active(AgentSkillDescriptor descriptor) {
        return new RegisteredSkill(descriptor, AgentSkillState.ACTIVE, List.of());
    }

    public static RegisteredSkill preview(AgentSkillDescriptor descriptor) {
        return new RegisteredSkill(descriptor, AgentSkillState.PREVIEW, List.of());
    }

    public static RegisteredSkill from(AgentSkill skill) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill is required.");
        }
        if (skill instanceof RegisteredSkill registered) {
            return registered;
        }
        return new RegisteredSkill(skill.descriptor(), skill.state(), skill.aliases());
    }

    public RegisteredSkill withAliases(List<String> aliases) {
        return new RegisteredSkill(descriptor, state, aliases);
    }

    public String id() {
        return descriptor.id();
    }

    public boolean matchesIdOrAlias(String skillId) {
        String normalized = normalizeAlias(skillId);
        return !normalized.isEmpty() && (id().equals(normalized) || aliases.contains(normalized));
    }

    private static List<String> normalizeAliases(List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String alias : aliases) {
            String item = normalizeAlias(alias);
            if (!item.isEmpty()) {
                normalized.add(item);
            }
        }
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    private static String normalizeAlias(String alias) {
        return SdkText.trimToEmpty(alias).toLowerCase(Locale.ROOT);
    }
}
