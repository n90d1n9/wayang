package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceReader;
import tech.kayys.wayang.storage.provider.s3.S3ObjectStorageService;
import tech.kayys.wayang.storage.provider.s3.S3StorageConfig;

/**
 * Creates S3-compatible readiness profile object readers from Wayang object-storage config.
 */
public final class WayangReadinessProfileS3ObjectStorageReaderFactory {

    public static final String DEFAULT_REGION = "us-east-1";

    private WayangReadinessProfileS3ObjectStorageReaderFactory() {
    }

    public static WayangReadinessProfileObjectStorageServiceReader reader(
            WayangObjectStorageConfig objectStorage,
            WayangReadinessProfileS3Credentials credentials) {
        S3ObjectStorageService service = service(objectStorage, credentials);
        return WayangReadinessProfileObjectStorageServiceReader.of(service);
    }

    public static WayangReadinessProfileObjectStorageServiceReader reader(
            WayangObjectStorageConfig objectStorage,
            WayangReadinessProfileS3CredentialsResolver credentialsResolver) {
        return reader(objectStorage, resolveCredentials(objectStorage, credentialsResolver));
    }

    public static S3ObjectStorageService service(
            WayangObjectStorageConfig objectStorage,
            WayangReadinessProfileS3Credentials credentials) {
        S3ObjectStorageService service = new S3ObjectStorageService();
        service.initialize(toS3StorageConfig(objectStorage, credentials));
        return service;
    }

    public static S3ObjectStorageService service(
            WayangObjectStorageConfig objectStorage,
            WayangReadinessProfileS3CredentialsResolver credentialsResolver) {
        return service(objectStorage, resolveCredentials(objectStorage, credentialsResolver));
    }

    public static S3StorageConfig toS3StorageConfig(
            WayangObjectStorageConfig objectStorage,
            WayangReadinessProfileS3Credentials credentials) {
        WayangObjectStorageConfig resolved = objectStorage == null
                ? WayangObjectStorageConfig.none()
                : objectStorage;
        WayangReadinessProfileS3Credentials resolvedCredentials =
                credentials == null ? missingCredentials() : credentials;
        return S3StorageConfig.of(
                resolvedCredentials.accessKeyId(),
                resolvedCredentials.secretAccessKey(),
                resolved.bucket(),
                region(resolved),
                resolved.endpoint(),
                "",
                resolved.pathStyleAccess());
    }

    public static S3StorageConfig toS3StorageConfig(
            WayangObjectStorageConfig objectStorage,
            WayangReadinessProfileS3CredentialsResolver credentialsResolver) {
        return toS3StorageConfig(objectStorage, resolveCredentials(objectStorage, credentialsResolver));
    }

    private static WayangReadinessProfileS3Credentials resolveCredentials(
            WayangObjectStorageConfig objectStorage,
            WayangReadinessProfileS3CredentialsResolver credentialsResolver) {
        if (credentialsResolver == null) {
            return missingCredentials();
        }
        return credentialsResolver.resolve(objectStorage)
                .orElseThrow(() -> new IllegalArgumentException(
                        "S3 readiness profile credentials are not configured for "
                                + credentialsReference(objectStorage)
                                + "."));
    }

    private static String region(WayangObjectStorageConfig objectStorage) {
        String region = objectStorage.region();
        return region.isBlank() ? DEFAULT_REGION : region;
    }

    private static WayangReadinessProfileS3Credentials missingCredentials() {
        throw new IllegalArgumentException("S3 readiness profile credentials are required.");
    }

    private static String credentialsReference(WayangObjectStorageConfig objectStorage) {
        if (objectStorage == null) {
            return "default credentials";
        }
        if (!objectStorage.credentialsRef().isBlank()) {
            return "credentialsRef '"
                    + WayangSecretRedactor.connectionString(objectStorage.credentialsRef())
                    + "'";
        }
        if (!objectStorage.provider().isBlank()) {
            return "provider '" + objectStorage.provider() + "'";
        }
        return "default credentials";
    }
}
