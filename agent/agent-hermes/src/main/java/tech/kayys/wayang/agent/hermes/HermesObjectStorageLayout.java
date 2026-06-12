package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared object-key conventions for Hermes object-storage backed stores.
 */
final class HermesObjectStorageLayout {

    private HermesObjectStorageLayout() {
    }

    static String normalizePrefix(String prefix, String defaultPrefix) {
        String normalized = prefix == null || prefix.isBlank() ? defaultPrefix : prefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    static List<String> listKeys(
            ObjectStorageService objectStorageService,
            String prefix,
            String... suffixes) {
        List<String> keys = objectStorageService.listObjects(prefix).await().indefinitely();
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<String> acceptedSuffixes = suffixes == null ? List.of() : Arrays.stream(suffixes)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return keys.stream()
                .filter(Objects::nonNull)
                .filter(key -> key.startsWith(prefix))
                .filter(key -> acceptedSuffixes.isEmpty()
                        || acceptedSuffixes.stream().anyMatch(key::endsWith))
                .sorted()
                .toList();
    }

    static Optional<byte[]> read(ObjectStorageService objectStorageService, String key) {
        Optional<byte[]> content = objectStorageService.getObject(key).await().indefinitely();
        return content == null || content.isEmpty() ? Optional.empty() : content;
    }

    static void put(ObjectStorageService objectStorageService, String key, byte[] content) {
        objectStorageService.putObject(key, content == null ? new byte[0] : content)
                .await()
                .indefinitely();
    }

    static void delete(ObjectStorageService objectStorageService, String key) {
        objectStorageService.deleteObject(key).await().indefinitely();
    }

    static String jsonlKey(String prefix, String objectId) {
        return prefix + objectId + ".jsonl";
    }

    static String objectId(String value, String fallback) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized.isBlank() ? fallback : normalized;
    }

    static String hashedObjectId(String value) {
        String raw = value == null ? "" : value;
        String normalized = objectId(raw, "");
        String hash = Integer.toUnsignedString(raw.hashCode(), 36);
        return normalized.isBlank() ? hash : normalized + "-" + hash;
    }
}
