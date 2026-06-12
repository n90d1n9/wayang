package tech.kayys.wayang.gollek.sdk;

import java.io.IOException;

/**
 * Reads a readiness profile properties document from a configured object-storage location.
 */
@FunctionalInterface
public interface WayangPlatformReadinessProfileObjectReader {

    String read(WayangObjectStorageConfig config) throws IOException;
}
