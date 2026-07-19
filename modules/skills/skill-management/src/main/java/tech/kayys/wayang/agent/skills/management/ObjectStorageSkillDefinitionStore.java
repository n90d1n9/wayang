package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Object-store-backed skill definition store for cloud and S3-compatible
 * persistence backends.
 */
public final class ObjectStorageSkillDefinitionStore implements SkillDefinitionStore {

    private final SkillManagementObjectStore objectStore;
    private final String prefix;
    private final SkillDefinitionPropertiesCodec codec;

    public ObjectStorageSkillDefinitionStore(SkillManagementObjectStore objectStore, String prefix) {
        this(objectStore, prefix, new SkillDefinitionPropertiesCodec());
    }

    ObjectStorageSkillDefinitionStore(
            SkillManagementObjectStore objectStore,
            String prefix,
            SkillDefinitionPropertiesCodec codec) {
        this.objectStore = Objects.requireNonNull(objectStore, "objectStore");
        this.prefix = SkillManagementObjectKeys.normalizePrefix(prefix, "");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return Optional.empty();
        }
        String key = keyFor(skillId);
        return readKey(key);
    }

    @Override
    public List<SkillDefinition> listSkills() {
        return SkillManagementObjectStoreSupport.keysWithExtension(
                        objectStore,
                        prefix,
                        SkillDefinitionPropertiesCodec.EXTENSION)
                .stream()
                .map(this::readKey)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        Objects.requireNonNull(skill, "skill");
        SkillManagementObjectStoreSupport.put(objectStore, keyFor(skill.id()), codec.toBytes(skill));
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return false;
        }
        return SkillManagementObjectStoreSupport.delete(objectStore, keyFor(skillId));
    }

    private Optional<SkillDefinition> readKey(String key) {
        return SkillManagementObjectStoreSupport.read(
                objectStore,
                key,
                content -> codec.fromBytes(content, key));
    }

    private String keyFor(String skillId) {
        return SkillManagementObjectKeys.skillKey(
                prefix,
                skillId,
                SkillDefinitionPropertiesCodec.EXTENSION,
                "persistence");
    }
}
