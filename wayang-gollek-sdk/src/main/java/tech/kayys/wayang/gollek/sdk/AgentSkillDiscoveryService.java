package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Locale;

public final class AgentSkillDiscoveryService {

    private static final AgentSkillDiscoveryService INSTANCE = new AgentSkillDiscoveryService();

    private AgentSkillDiscoveryService() {
    }

    public static AgentSkillDiscoveryService create() {
        return INSTANCE;
    }

    public AgentSkillDiscovery discover(SkillRegistry registry, AgentSkillQuery query) {
        return discover(registry, query, "");
    }

    public AgentSkillDiscovery discover(SkillRegistry registry, AgentSkillQuery query, String search) {
        SkillRegistry source = registry == null ? SkillRegistry.create() : registry;
        AgentSkillQuery normalized = query == null ? AgentSkillQuery.all() : query;
        List<RegisteredSkill> filtered = filterSearch(source.discover(normalized), search);
        return AgentSkillDiscovery.of(normalized, search, filtered, source.size());
    }

    public List<RegisteredSkill> filterSearch(List<RegisteredSkill> skills, String search) {
        String term = SdkText.trimToEmpty(search).toLowerCase(Locale.ROOT);
        if (term.isEmpty()) {
            return SdkLists.copy(skills);
        }
        return SdkLists.copy(skills).stream()
                .filter(skill -> matchesSearch(skill, term))
                .toList();
    }

    private static boolean matchesSearch(RegisteredSkill skill, String term) {
        AgentSkillDescriptor descriptor = skill.descriptor();
        return contains(skill.id(), term)
                || contains(descriptor.name(), term)
                || contains(descriptor.description(), term)
                || contains(descriptor.category(), term)
                || contains(descriptor.source(), term)
                || descriptor.tags().stream().anyMatch(tag -> contains(tag, term))
                || skill.aliases().stream().anyMatch(alias -> contains(alias, term));
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }
}
