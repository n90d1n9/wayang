package tech.kayys.wayang.readiness;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import tech.kayys.wayang.client.SdkText;

public final class WayangPlatformReadinessProfileFileSource
        implements WayangPlatformReadinessProfileSource {

    public static final String SOURCE_TYPE = "file";

    private final String sourceId;
    private final Path path;

    private WayangPlatformReadinessProfileFileSource(String sourceId, Path path) {
        this.path = path;
        this.sourceId = SdkText.trimToDefault(sourceId, defaultSourceId(path));
    }

    public static WayangPlatformReadinessProfileFileSource of(String path) {
        return of(path == null || path.isBlank() ? null : Path.of(path));
    }

    public static WayangPlatformReadinessProfileFileSource of(Path path) {
        return new WayangPlatformReadinessProfileFileSource(null, path);
    }

    public static WayangPlatformReadinessProfileFileSource of(String sourceId, Path path) {
        return new WayangPlatformReadinessProfileFileSource(sourceId, path);
    }

    @Override
    public WayangPlatformReadinessProfileSourceResult load() {
        if (path == null) {
            return unavailable("Readiness profile file path is not configured.");
        }
        if (!Files.isRegularFile(path)) {
            return unavailable("Readiness profile file does not exist: " + path + ".");
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return WayangPlatformReadinessProfileSourceResult.available(
                    sourceId,
                    SOURCE_TYPE,
                    path.toString(),
                    WayangPlatformReadinessProfileDocument.fromProperties(properties),
                    "Readiness profile file loaded.");
        } catch (IOException | RuntimeException exception) {
            return unavailable("Readiness profile file could not be loaded: " + exception.getMessage());
        }
    }

    private WayangPlatformReadinessProfileSourceResult unavailable(String message) {
        return WayangPlatformReadinessProfileSourceResult.unavailable(
                sourceId,
                SOURCE_TYPE,
                path == null ? "" : path.toString(),
                message);
    }

    private static String defaultSourceId(Path path) {
        return path == null ? "file" : "file:" + path;
    }
}
