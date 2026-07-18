package tech.kayys.wayang.readiness;

import java.io.IOException;

import tech.kayys.wayang.client.WayangObjectStorageConfig;


/**
 * Reads a readiness profile properties document from a configured object-storage location.
 */
@FunctionalInterface
public interface WayangPlatformReadinessProfileObjectReader {

    String read(WayangObjectStorageConfig config) throws IOException;
}
