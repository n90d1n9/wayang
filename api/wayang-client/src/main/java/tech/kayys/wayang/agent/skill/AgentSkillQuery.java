package tech.kayys.wayang.agent.skill;

import java.util.Locale;

import tech.kayys.wayang.client.ProductProfile;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangProductCatalog;

public record AgentSkillQuery(
        String surfaceId,
        String profileId,
        String category,
        String source,
        AgentSkillState state,
        String skillId,
        String tag,
        String inputKey,
        String outputKey) {

    public AgentSkillQuery {
        surfaceId = normalizeIdentifier(surfaceId);
        profileId = normalizeIdentifier(profileId);
        category = SdkText.trimToEmpty(category);
        source = normalizeIdentifier(source);
        skillId = normalizeIdentifier(skillId);
        tag = normalizeIdentifier(tag);
        inputKey = SdkText.trimToEmpty(inputKey);
        outputKey = SdkText.trimToEmpty(outputKey);
    }

    public AgentSkillQuery(
            String surfaceId,
            String category,
            String source,
            AgentSkillState state,
            String skillId,
            String tag,
            String inputKey,
            String outputKey) {
        this(surfaceId, null, category, source, state, skillId, tag, inputKey, outputKey);
    }

    public static AgentSkillQuery all() {
        return new AgentSkillQuery(null, null, null, null, null, null, null, null, null);
    }

    public static AgentSkillQuery of(String surfaceId, String category, String skillId) {
        return new AgentSkillQuery(surfaceId, null, category, null, null, skillId, null, null, null);
    }

    public static AgentSkillQuery of(String surfaceId, String profileId, String category, String skillId) {
        return new AgentSkillQuery(surfaceId, profileId, category, null, null, skillId, null, null, null);
    }

    public static AgentSkillQuery forProfile(String profileId, String category, String skillId) {
        return new AgentSkillQuery(null, profileId, category, null, null, skillId, null, null, null);
    }

    public boolean filtered() {
        return hasSurfaceId()
                || hasProfileId()
                || hasCategory()
                || hasSource()
                || state != null
                || hasSkillId()
                || hasTag()
                || hasInputKey()
                || hasOutputKey();
    }

    public boolean hasSurfaceId() {
        return !surfaceId.isEmpty();
    }

    public boolean hasProfileId() {
        return !profileId.isEmpty();
    }

    public boolean hasCategory() {
        return !category.isEmpty();
    }

    public boolean hasSource() {
        return !source.isEmpty();
    }

    public boolean hasSkillId() {
        return !skillId.isEmpty();
    }

    public boolean hasTag() {
        return !tag.isEmpty();
    }

    public boolean hasInputKey() {
        return !inputKey.isEmpty();
    }

    public boolean hasOutputKey() {
        return !outputKey.isEmpty();
    }

    public String resolvedSurfaceId() {
        if (!hasProfileId()) {
            return hasSurfaceId() ? WayangProductCatalog.requireKnownSurfaceId(surfaceId) : "";
        }

        ProductProfile profile = WayangProductCatalog.profileFor(profileId);
        if (!hasSurfaceId()) {
            return profile.surfaceId();
        }

        String normalizedSurfaceId = WayangProductCatalog.requireKnownSurfaceId(surfaceId);
        if (!normalizedSurfaceId.equals(profile.surfaceId())) {
            throw new IllegalArgumentException(
                    "Wayang product profile '" + profile.id() + "' belongs to surface '"
                            + profile.surfaceId() + "', not '" + normalizedSurfaceId + "'.");
        }
        return normalizedSurfaceId;
    }

    private static String normalizeIdentifier(String value) {
        return SdkText.trimToEmpty(value).toLowerCase(Locale.ROOT);
    }
}
