package tech.kayys.wayang.storage.spi;

import java.util.Objects;

/**
 * Shared path and URI helpers for storage providers.
 */
public final class StoragePaths {

    public static final String DEFAULT_MODEL_PREFIX = "models/";

    private StoragePaths() {
    }

    public static String normalizePrefix(String pathPrefix) {
        if (pathPrefix == null || pathPrefix.isBlank()) {
            return DEFAULT_MODEL_PREFIX;
        }
        String normalized = trimLeadingSlashes(pathPrefix.trim());
        if (normalized.isBlank()) {
            return DEFAULT_MODEL_PREFIX;
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    public static String modelObjectName(String pathPrefix, String namespace, String modelId, String version) {
        return normalizePrefix(pathPrefix)
                + requireSegment(namespace, "namespace") + "/"
                + requireSegment(modelId, "modelId") + "/"
                + requireSegment(version, "version");
    }

    public static String storageUri(String scheme, String containerName, String objectName) {
        return requireSegment(scheme, "scheme") + "://"
                + requireSegment(containerName, "containerName") + "/"
                + requireObjectName(objectName);
    }

    public static String objectNameFromUri(String storageUri, String scheme, String containerName) {
        String prefix = requireSegment(scheme, "scheme") + "://"
                + requireSegment(containerName, "containerName") + "/";
        String normalizedUri = Objects.requireNonNull(storageUri, "storageUri").trim();
        if (!normalizedUri.startsWith(prefix)) {
            throw new IllegalArgumentException("Storage URI must start with " + prefix);
        }
        return normalizedUri.substring(prefix.length());
    }

    private static String requireSegment(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static String requireObjectName(String objectName) {
        String normalized = trimLeadingSlashes(requireSegment(objectName, "objectName"));
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("objectName must not be blank");
        }
        return normalized;
    }

    private static String trimLeadingSlashes(String value) {
        String normalized = value;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
