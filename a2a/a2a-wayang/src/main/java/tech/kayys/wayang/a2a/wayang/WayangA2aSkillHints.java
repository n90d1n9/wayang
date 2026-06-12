package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared skill routing hint extraction for A2A SendMessage requests.
 */
final class WayangA2aSkillHints {

    private static final String LEGACY_ALLOWED_SKILLS = "wayang.allowedSkills";

    private WayangA2aSkillHints() {
    }

    static List<String> allowedSkills(A2aSendMessageRequest request) {
        if (request == null) {
            return List.of();
        }
        Set<String> skillIds = new LinkedHashSet<>();
        skillIds.addAll(allowedSkills(request.metadata()));
        skillIds.addAll(allowedSkills(request.message().metadata()));
        return List.copyOf(skillIds);
    }

    static List<String> allowedSkills(Map<String, ?> metadata) {
        return WayangA2aMaps.firstStringList(
                metadata,
                WayangA2a.METADATA_ALLOWED_SKILLS,
                LEGACY_ALLOWED_SKILLS);
    }
}
