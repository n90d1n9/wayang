package tech.kayys.wayang.gollek.sdk;

import java.io.IOException;

/**
 * Reads a readiness profile properties document from a configured database location.
 */
@FunctionalInterface
public interface WayangPlatformReadinessProfileDatabaseReader {

    String read(String databaseUrl) throws IOException;
}
