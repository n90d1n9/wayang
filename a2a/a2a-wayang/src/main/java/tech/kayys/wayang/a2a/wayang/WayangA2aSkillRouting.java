package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves skill routing hints and skill-scoped A2A mode support.
 */
final class WayangA2aSkillRouting {

    private final List<String> defaultInputModes;
    private final List<String> defaultOutputModes;
    private final List<String> supportedSkillIds;
    private final Map<String, List<String>> skillInputModes;
    private final Map<String, List<String>> skillOutputModes;

    private WayangA2aSkillRouting(A2aAgentCard agentCard) {
        A2aAgentCard resolved = Objects.requireNonNull(agentCard, "agentCard");
        this.defaultInputModes = WayangA2aMediaTypes.copyDistinct(resolved.defaultInputModes());
        this.defaultOutputModes = WayangA2aMediaTypes.copyDistinct(resolved.defaultOutputModes());
        this.supportedSkillIds = copyDistinct(resolved.skills().stream()
                .map(A2aAgentSkill::id)
                .toList());
        this.skillInputModes = skillModes(resolved.skills(), true);
        this.skillOutputModes = skillModes(resolved.skills(), false);
    }

    static WayangA2aSkillRouting fromAgentCard(A2aAgentCard agentCard) {
        return new WayangA2aSkillRouting(agentCard);
    }

    List<String> supportedSkillIds() {
        return supportedSkillIds;
    }

    List<String> requestedSkillIds(A2aSendMessageRequest request) {
        return WayangA2aSkillHints.allowedSkills(request);
    }

    List<String> unsupportedSkillIds(A2aSendMessageRequest request) {
        return requestedSkillIds(request).stream()
                .filter(skillId -> !supportedSkillIds.contains(skillId))
                .distinct()
                .toList();
    }

    List<String> inputModes(A2aSendMessageRequest request) {
        return modesFor(requestedSkillIds(request), skillInputModes, defaultInputModes);
    }

    List<String> outputModes(A2aSendMessageRequest request) {
        return modesFor(requestedSkillIds(request), skillOutputModes, defaultOutputModes);
    }

    private static List<String> modesFor(
            List<String> skillIds,
            Map<String, List<String>> skillModes,
            List<String> defaultModes) {
        if (skillIds.isEmpty()) {
            return defaultModes;
        }
        Set<String> modes = new LinkedHashSet<>();
        for (String skillId : skillIds) {
            modes.addAll(skillModes.getOrDefault(skillId, List.of()));
        }
        return modes.isEmpty() ? defaultModes : List.copyOf(modes);
    }

    private static Map<String, List<String>> skillModes(List<A2aAgentSkill> skills, boolean input) {
        Map<String, List<String>> modes = new LinkedHashMap<>();
        for (A2aAgentSkill skill : skills) {
            modes.put(skill.id(), WayangA2aMediaTypes.copyDistinct(input ? skill.inputModes() : skill.outputModes()));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(modes));
    }

    private static List<String> copyDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> copy = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = WayangA2aMaps.optional(value);
            if (normalized != null) {
                copy.add(normalized);
            }
        }
        return List.copyOf(copy);
    }
}
