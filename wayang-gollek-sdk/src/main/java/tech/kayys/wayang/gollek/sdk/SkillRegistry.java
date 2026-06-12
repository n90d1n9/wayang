package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SkillRegistry {

    private final Map<String, RegisteredSkill> skills = new LinkedHashMap<>();

    private SkillRegistry(List<? extends AgentSkill> initialSkills) {
        SdkLists.copy(initialSkills).forEach(this::register);
    }

    public static SkillRegistry create() {
        return new SkillRegistry(List.of());
    }

    public static SkillRegistry of(List<? extends AgentSkill> skills) {
        return new SkillRegistry(skills);
    }

    public synchronized RegisteredSkill register(AgentSkill skill) {
        RegisteredSkill registered = RegisteredSkill.from(skill);
        ensureUnique(registered);
        skills.put(registered.id(), registered);
        return registered;
    }

    public synchronized boolean unregister(String skillId) {
        Optional<RegisteredSkill> existing = find(skillId);
        existing.ifPresent(skill -> skills.remove(skill.id()));
        return existing.isPresent();
    }

    public synchronized Optional<RegisteredSkill> find(String skillId) {
        String normalized = normalizeLookup(skillId);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        RegisteredSkill exact = skills.get(normalized);
        if (exact != null) {
            return Optional.of(exact);
        }
        return skills.values().stream()
                .filter(skill -> skill.matchesIdOrAlias(normalized))
                .findFirst();
    }

    public synchronized RegisteredSkill require(String skillId) {
        String normalized = normalizeLookup(skillId);
        return find(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Wayang skill id '" + normalized
                        + "'. Known skill ids: " + String.join(", ", skillIds())));
    }

    public synchronized boolean contains(String skillId) {
        return find(skillId).isPresent();
    }

    public synchronized int size() {
        return skills.size();
    }

    public synchronized List<RegisteredSkill> list() {
        return List.copyOf(skills.values());
    }

    public synchronized List<RegisteredSkill> discover(AgentSkillQuery query) {
        AgentSkillQuery normalized = query == null ? AgentSkillQuery.all() : query;
        String resolvedSurfaceId = normalized.resolvedSurfaceId();
        List<String> profileSkills = normalized.hasProfileId()
                ? WayangProductCatalog.profileFor(normalized.profileId()).skills()
                : List.of();
        return skills.values().stream()
                .filter(skill -> matches(skill, normalized, resolvedSurfaceId, profileSkills))
                .toList();
    }

    public synchronized List<String> skillIds() {
        return List.copyOf(skills.keySet());
    }

    public synchronized List<String> categories() {
        return SdkFacets.values(List.copyOf(skills.values()), skill -> skill.descriptor().category());
    }

    public synchronized List<String> sources() {
        return SdkFacets.values(List.copyOf(skills.values()), skill -> skill.descriptor().source());
    }

    private void ensureUnique(RegisteredSkill candidate) {
        if (skills.containsKey(candidate.id())) {
            throw new IllegalArgumentException("Duplicate Wayang skill id '" + candidate.id() + "'.");
        }
        for (RegisteredSkill existing : skills.values()) {
            ensureNoAliasCollision(candidate.id(), existing);
            for (String alias : candidate.aliases()) {
                ensureNoAliasCollision(alias, existing);
            }
        }
    }

    private static void ensureNoAliasCollision(String lookup, RegisteredSkill existing) {
        if (existing.matchesIdOrAlias(lookup)) {
            throw new IllegalArgumentException("Duplicate Wayang skill id or alias '" + lookup + "'.");
        }
    }

    private static boolean matches(
            RegisteredSkill skill,
            AgentSkillQuery query,
            String resolvedSurfaceId,
            List<String> profileSkills) {
        AgentSkillDescriptor descriptor = skill.descriptor();
        return (resolvedSurfaceId.isBlank() || descriptor.supportsSurface(resolvedSurfaceId))
                && (profileSkills.isEmpty() || profileSkills.stream().anyMatch(skill::matchesIdOrAlias))
                && (!query.hasCategory() || descriptor.category().equalsIgnoreCase(query.category()))
                && (!query.hasSource() || descriptor.source().equalsIgnoreCase(query.source()))
                && (query.state() == null || skill.state() == query.state())
                && (!query.hasSkillId() || skill.matchesIdOrAlias(query.skillId()))
                && (!query.hasTag() || descriptor.hasTag(query.tag()))
                && (!query.hasInputKey() || descriptor.hasInputKey(query.inputKey()))
                && (!query.hasOutputKey() || descriptor.hasOutputKey(query.outputKey()));
    }

    private static String normalizeLookup(String value) {
        return SdkText.trimToEmpty(value).toLowerCase(java.util.Locale.ROOT);
    }
}
