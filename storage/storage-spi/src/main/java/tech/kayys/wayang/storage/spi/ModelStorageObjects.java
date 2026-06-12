package tech.kayys.wayang.storage.spi;

/**
 * Maps model coordinates to provider object names and owned storage URIs.
 */
public record ModelStorageObjects(String scheme, String containerName, String pathPrefix) {

    public ModelStorageObjects {
        scheme = require(scheme, "scheme");
        containerName = require(containerName, "containerName");
        pathPrefix = StoragePaths.normalizePrefix(pathPrefix);
    }

    public static ModelStorageObjects forContainer(String scheme, String containerName, String pathPrefix) {
        return new ModelStorageObjects(scheme, containerName, pathPrefix);
    }

    public String objectName(String namespace, String modelId, String version) {
        return StoragePaths.modelObjectName(pathPrefix, namespace, modelId, version);
    }

    public String storageUri(String objectName) {
        return StoragePaths.storageUri(scheme, containerName, objectName);
    }

    public String objectNameFromUri(String storageUri) {
        return StoragePaths.objectNameFromUri(storageUri, scheme, containerName);
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
