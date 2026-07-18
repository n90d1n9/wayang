package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;
import java.util.Properties;

/**
 * Shared properties codec for lightweight lifecycle state persistence backends.
 */
final class SkillLifecycleStatePropertiesCodec {

    static final String EXTENSION = ".state.properties";

    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_REVISION = "revision";
    private static final String KEY_SKILL_ID = "skillId";
    private static final String KEY_STATUS = "status";
    private static final String KEY_UPDATED_AT = "updatedAt";

    Properties toProperties(SkillLifecycleState state) {
        Objects.requireNonNull(state, "state");
        Properties properties = new Properties();
        properties.setProperty(KEY_SKILL_ID, state.skillId());
        properties.setProperty(KEY_STATUS, state.status().name());
        properties.setProperty(KEY_CREATED_AT, state.createdAt().toString());
        properties.setProperty(KEY_UPDATED_AT, state.updatedAt().toString());
        properties.setProperty(KEY_REVISION, Integer.toString(state.revision()));
        return properties;
    }

    byte[] toBytes(SkillLifecycleState state) {
        return SkillManagementPropertiesCodecSupport.storeToBytes(
                toProperties(state),
                "Wayang skill lifecycle state",
                "Failed to encode skill lifecycle state: " + state.skillId());
    }

    SkillLifecycleState fromBytes(byte[] content, String sourceDescription) {
        Properties properties = SkillManagementPropertiesCodecSupport.loadFromBytes(
                content,
                "Failed to decode skill lifecycle state from " + sourceDescription);
        return fromProperties(properties, sourceDescription);
    }

    SkillLifecycleState fromProperties(Properties properties, String sourceDescription) {
        Objects.requireNonNull(properties, "properties");
        return new SkillLifecycleState(
                SkillManagementPropertiesCodecSupport.requiredProperty(properties, KEY_SKILL_ID, sourceDescription),
                SkillLifecycleStatus.valueOf(SkillManagementPropertiesCodecSupport.requiredProperty(
                        properties,
                        KEY_STATUS,
                        sourceDescription)),
                SkillManagementPropertiesCodecSupport.instantOrNull(properties.getProperty(KEY_CREATED_AT)),
                SkillManagementPropertiesCodecSupport.instantOrNull(properties.getProperty(KEY_UPDATED_AT)),
                SkillManagementPropertiesCodecSupport.integerOrDefault(properties.getProperty(KEY_REVISION), 1));
    }
}
