package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.Optional;

/**
 * Resolves the object storage service that should satisfy a readiness profile request.
 */
@FunctionalInterface
public interface WayangReadinessProfileObjectStorageServiceResolver {

    Optional<ObjectStorageService> resolve(WayangObjectStorageConfig config);
}
