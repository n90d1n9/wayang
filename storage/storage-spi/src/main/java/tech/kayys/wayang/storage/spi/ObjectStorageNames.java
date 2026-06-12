package tech.kayys.wayang.storage.spi;

/**
 * Maps logical object-store keys to provider object names under an optional prefix.
 */
public record ObjectStorageNames(String pathPrefix) {

    public ObjectStorageNames {
        pathPrefix = normalizePrefix(pathPrefix);
    }

    public static ObjectStorageNames unprefixed() {
        return new ObjectStorageNames("");
    }

    public static ObjectStorageNames fromPrefix(String pathPrefix) {
        return new ObjectStorageNames(pathPrefix);
    }

    public String objectName(String key) {
        return pathPrefix + normalizeKey(key);
    }

    public String logicalKey(String objectName) {
        String normalized = normalizeKey(objectName);
        return pathPrefix.isEmpty() || !normalized.startsWith(pathPrefix)
                ? normalized
                : normalized.substring(pathPrefix.length());
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = normalizeKey(prefix);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String normalized = key.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
