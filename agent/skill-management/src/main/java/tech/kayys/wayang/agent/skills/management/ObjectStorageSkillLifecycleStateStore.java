package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Object-store-backed lifecycle state store for cloud and S3-compatible
 * persistence backends.
 */
public final class ObjectStorageSkillLifecycleStateStore implements SkillLifecycleStateStore {

    public static final String DEFAULT_PREFIX = "skill-management/lifecycle";

    private final SkillManagementObjectStore objectStore;
    private final String prefix;
    private final SkillLifecycleStatePropertiesCodec codec;

    public ObjectStorageSkillLifecycleStateStore(SkillManagementObjectStore objectStore) {
        this(objectStore, DEFAULT_PREFIX);
    }

    public ObjectStorageSkillLifecycleStateStore(SkillManagementObjectStore objectStore, String prefix) {
        this(objectStore, prefix, new SkillLifecycleStatePropertiesCodec());
    }

    ObjectStorageSkillLifecycleStateStore(
            SkillManagementObjectStore objectStore,
            String prefix,
            SkillLifecycleStatePropertiesCodec codec) {
        this.objectStore = Objects.requireNonNull(objectStore, "objectStore");
        this.prefix = SkillManagementObjectKeys.normalizePrefix(prefix, DEFAULT_PREFIX);
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public Optional<SkillLifecycleState> get(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return Optional.empty();
        }
        String key = keyFor(skillId);
        return readKey(key);
    }

    @Override
    public SkillLifecycleState save(SkillLifecycleState state) {
        Objects.requireNonNull(state, "state");
        SkillManagementObjectStoreSupport.put(objectStore, keyFor(state.skillId()), codec.toBytes(state));
        return state;
    }

    @Override
    public boolean remove(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return false;
        }
        return SkillManagementObjectStoreSupport.delete(objectStore, keyFor(skillId));
    }

    @Override
    public Map<String, SkillLifecycleState> snapshot() {
        return SkillManagementObjectStoreSupport.keysWithExtension(
                        objectStore,
                        prefix,
                        SkillLifecycleStatePropertiesCodec.EXTENSION)
                .stream()
                .map(this::readKey)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableMap(SkillLifecycleState::skillId, state -> state));
    }

    private Optional<SkillLifecycleState> readKey(String key) {
        return SkillManagementObjectStoreSupport.read(
                objectStore,
                key,
                content -> codec.fromBytes(content, key));
    }

    private String keyFor(String skillId) {
        return SkillManagementObjectKeys.skillKey(
                prefix,
                skillId,
                SkillLifecycleStatePropertiesCodec.EXTENSION,
                "lifecycle persistence");
    }
}
