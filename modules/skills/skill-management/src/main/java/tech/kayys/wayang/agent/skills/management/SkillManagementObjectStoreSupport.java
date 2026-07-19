package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Shared object-store operations for cloud-backed skill-management stores.
 */
final class SkillManagementObjectStoreSupport {

    private SkillManagementObjectStoreSupport() {
    }

    static List<String> keys(SkillManagementObjectStore objectStore, String prefix, Predicate<String> include) {
        Objects.requireNonNull(objectStore, "objectStore");
        String resolvedPrefix = prefix == null ? "" : prefix;
        Predicate<String> resolvedInclude = include == null ? key -> true : include;
        List<String> keys = objectStore.list(resolvedPrefix);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .filter(Objects::nonNull)
                .filter(key -> key.startsWith(resolvedPrefix))
                .filter(resolvedInclude)
                .sorted()
                .toList();
    }

    static List<String> keysWithExtension(
            SkillManagementObjectStore objectStore,
            String prefix,
            String extension) {
        return keys(
                objectStore,
                prefix,
                key -> extension == null || extension.isBlank() || key.endsWith(extension));
    }

    static <T> Optional<T> read(
            SkillManagementObjectStore objectStore,
            String key,
            Function<byte[], T> decode) {
        Objects.requireNonNull(objectStore, "objectStore");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(decode, "decode");
        Optional<byte[]> content = objectStore.get(key);
        return content == null ? Optional.empty() : content.map(decode);
    }

    static void put(SkillManagementObjectStore objectStore, String key, byte[] content) {
        Objects.requireNonNull(objectStore, "objectStore")
                .put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(content, "content"));
    }

    static boolean delete(SkillManagementObjectStore objectStore, String key) {
        return Objects.requireNonNull(objectStore, "objectStore")
                .delete(Objects.requireNonNull(key, "key"));
    }
}
