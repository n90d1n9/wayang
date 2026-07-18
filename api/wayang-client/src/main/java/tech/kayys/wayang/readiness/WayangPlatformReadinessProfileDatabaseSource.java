package tech.kayys.wayang.readiness;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangSecretRedactor;

/**
 * Loads readiness profile catalogs from a database through a pluggable reader.
 */
public final class WayangPlatformReadinessProfileDatabaseSource
        implements WayangPlatformReadinessProfileSource {

    public static final String SOURCE_ID = "database";
    public static final String SOURCE_TYPE = "database";

    private final String databaseUrl;
    private final WayangPlatformReadinessProfileDatabaseReader reader;

    private WayangPlatformReadinessProfileDatabaseSource(
            String databaseUrl,
            WayangPlatformReadinessProfileDatabaseReader reader) {
        this.databaseUrl = SdkText.trimToEmpty(databaseUrl);
        this.reader = reader;
    }

    public static WayangPlatformReadinessProfileDatabaseSource of(
            String databaseUrl,
            WayangPlatformReadinessProfileDatabaseReader reader) {
        return new WayangPlatformReadinessProfileDatabaseSource(databaseUrl, reader);
    }

    public static String locationOf(String databaseUrl) {
        return WayangSecretRedactor.connectionString(databaseUrl);
    }

    @Override
    public WayangPlatformReadinessProfileSourceResult load() {
        if (reader == null) {
            return unavailable("Database readiness profile loader is not wired.");
        }
        try {
            String document = reader.read(databaseUrl);
            Properties properties = new Properties();
            properties.load(new StringReader(SdkText.trimToEmpty(document)));
            return WayangPlatformReadinessProfileSourceResult.available(
                    SOURCE_ID,
                    SOURCE_TYPE,
                    location(),
                    WayangPlatformReadinessProfileDocument.fromProperties(properties),
                    "Database readiness profile loaded.");
        } catch (IOException | RuntimeException exception) {
            return unavailable("Database readiness profile could not be loaded: "
                    + WayangSecretRedactor.connectionString(exception.getMessage()));
        }
    }

    private WayangPlatformReadinessProfileSourceResult unavailable(String message) {
        return WayangPlatformReadinessProfileSourceResult.unavailable(
                SOURCE_ID,
                SOURCE_TYPE,
                location(),
                message);
    }

    private String location() {
        return locationOf(databaseUrl);
    }
}
