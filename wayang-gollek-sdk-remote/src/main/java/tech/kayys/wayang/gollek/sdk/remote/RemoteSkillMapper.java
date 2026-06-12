package tech.kayys.wayang.gollek.sdk.remote;

import tech.kayys.wayang.gollek.sdk.AgentSkillDescriptor;
import tech.kayys.wayang.gollek.sdk.AgentSkillDiscovery;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.AgentSkillState;
import tech.kayys.wayang.gollek.sdk.RegisteredSkill;
import tech.kayys.wayang.gollek.sdk.SkillRegistry;
import tech.kayys.wayang.gollek.sdk.WayangSkillCatalog;

import java.util.List;
import java.util.Map;

final class RemoteSkillMapper {

    SkillRegistry registry(RemoteResponse response) {
        List<RegisteredSkill> skills = RemoteJson.objectArrayField(response.body(), "skills").stream()
                .map(RemoteJson::object)
                .filter(map -> !map.isEmpty())
                .map(map -> skill(map, ""))
                .toList();
        return skills.isEmpty() ? WayangSkillCatalog.defaultRegistry() : SkillRegistry.of(skills);
    }

    AgentSkillDiscovery discovery(RemoteResponse response, AgentSkillQuery query, String search) {
        List<RegisteredSkill> skills = RemoteJson.objectArrayField(response.body(), "skills").stream()
                .map(RemoteJson::object)
                .filter(map -> !map.isEmpty())
                .map(map -> skill(map, ""))
                .toList();
        int totalSkills = RemoteJson.intField(response.body(), "totalSkills", skills.size());
        return AgentSkillDiscovery.of(query, search, skills, totalSkills);
    }

    RegisteredSkill skill(RemoteResponse response, String fallbackSkillId) {
        Map<String, Object> wrapped = RemoteJson.objectField(response.body(), "skill");
        if (!wrapped.isEmpty()) {
            return skill(wrapped, fallbackSkillId);
        }
        List<Map<String, Object>> listed = RemoteJson.objectArrayField(response.body(), "skills").stream()
                .map(RemoteJson::object)
                .filter(map -> !map.isEmpty())
                .toList();
        if (!listed.isEmpty()) {
            return listed.stream()
                    .map(map -> skill(map, fallbackSkillId))
                    .filter(skill -> skill.matchesIdOrAlias(fallbackSkillId))
                    .findFirst()
                    .orElseGet(() -> skill(listed.get(0), fallbackSkillId));
        }
        Map<String, Object> source = RemoteJson.object(response.body());
        return source.isEmpty() || !hasSkillShape(source)
                ? WayangSkillCatalog.defaultRegistry().require(fallbackSkillId)
                : skill(source, fallbackSkillId);
    }

    private RegisteredSkill skill(Map<String, Object> source, String fallbackSkillId) {
        Map<String, Object> descriptorSource = object(source.get("descriptor"));
        Map<String, Object> descriptor = descriptorSource.isEmpty() ? source : descriptorSource;
        String id = string(descriptor, "id", fallbackSkillId);
        AgentSkillDescriptor skillDescriptor = new AgentSkillDescriptor(
                id,
                string(descriptor, "name", id),
                string(descriptor, "description", ""),
                string(descriptor, "category", "General"),
                string(descriptor, "source", "remote"),
                string(descriptor, "version", "1.0.0"),
                strings(descriptor, "surfaceIds"),
                strings(descriptor, "inputKeys"),
                strings(descriptor, "outputKeys"),
                strings(descriptor, "tags"),
                object(descriptor.get("metadata")));
        return new RegisteredSkill(
                skillDescriptor,
                state(string(source, "state", string(descriptor, "state", "ACTIVE"))),
                strings(source, "aliases"));
    }

    private static AgentSkillState state(String value) {
        try {
            return AgentSkillState.from(value);
        } catch (IllegalArgumentException e) {
            return AgentSkillState.ACTIVE;
        }
    }

    private static String string(Map<String, Object> source, String field, String fallback) {
        Object value = source.get(field);
        String normalized = value == null ? "" : String.valueOf(value).trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static List<String> strings(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (value instanceof Iterable<?> iterable) {
            return values(iterable);
        }
        if (value instanceof String string && !string.isBlank()) {
            return List.of(string.trim());
        }
        return List.of();
    }

    private static List<String> values(Iterable<?> values) {
        java.util.ArrayList<String> normalized = new java.util.ArrayList<>();
        for (Object value : values) {
            String item = value == null ? "" : String.valueOf(value).trim();
            if (!item.isEmpty()) {
                normalized.add(item);
            }
        }
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    private static boolean hasSkillShape(Map<String, Object> source) {
        return source.containsKey("descriptor")
                || source.containsKey("id")
                || source.containsKey("name")
                || source.containsKey("category")
                || source.containsKey("source");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
