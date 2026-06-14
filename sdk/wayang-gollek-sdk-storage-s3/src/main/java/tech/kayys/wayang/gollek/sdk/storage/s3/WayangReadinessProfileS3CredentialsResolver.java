package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;

import java.util.Optional;

/**
 * Resolves credentials for S3-compatible readiness profile object storage.
 */
@FunctionalInterface
public interface WayangReadinessProfileS3CredentialsResolver {

    Optional<WayangReadinessProfileS3Credentials> resolve(WayangObjectStorageConfig config);
}
