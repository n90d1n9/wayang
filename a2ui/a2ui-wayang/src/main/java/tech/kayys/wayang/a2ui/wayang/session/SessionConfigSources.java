package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Factory methods for composing file, classpath, inline, and adapter-backed session config sources.
 */
public final class SessionConfigSources {

    public static SessionConfigSource json(
            String description,
            Supplier<Optional<String>> jsonSupplier) {
        return new SessionConfigSource(description, jsonSupplier);
    }

    public static SessionConfigSource inlineJson(String description, String json) {
        return json(description, () -> Optional.ofNullable(json));
    }

    public static SessionConfigSource file(Path path) {
        Path resolved = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        return json("file:" + resolved, () -> readFile(resolved));
    }

    public static SessionConfigSource classpath(String resourceName) {
        String normalized = normalizeResourceName(resourceName);
        return json("classpath:" + normalized, () -> readResource(normalized));
    }

    public static SessionConfigSource firstAvailable(SessionConfigSource... sources) {
        List<SessionConfigSource> ordered = Arrays.stream(sources == null ? new SessionConfigSource[0] : sources)
                .filter(Objects::nonNull)
                .toList();
        return new SessionConfigSource(
                fallbackDescription(ordered),
                () -> firstJson(ordered),
                () -> loadFirstResult(ordered));
    }

    public static SessionConfigSource fromSpec(Map<?, ?> spec) {
        return SessionConfigSourceRegistry.standard().source(spec);
    }

    public static SessionConfigSource fromSpec(SessionConfigSourceSpec spec) {
        return SessionConfigSourceRegistry.standard().source(spec);
    }

    public static SessionConfigSource fromSpecJson(String json) {
        return SessionConfigSourceRegistry.standard().sourceFromJson(json);
    }

    public static WayangA2uiSessionConfig loadOrDefault(SessionConfigSource source) {
        return source == null ? WayangA2uiSessionConfig.defaultConfig() : source.loadOrDefault();
    }

    public static SessionConfigLoadResult loadResult(SessionConfigSource source) {
        return source == null ? SessionConfigLoadResult.missing("session-config") : source.loadResult();
    }

    public static WayangA2uiSessionConfig loadFirstOrDefault(SessionConfigSource... sources) {
        return firstAvailable(sources).loadOrDefault();
    }

    public static SessionConfigLoadResult loadFirstResult(SessionConfigSource... sources) {
        List<SessionConfigSource> ordered = Arrays.stream(sources == null ? new SessionConfigSource[0] : sources)
                .filter(Objects::nonNull)
                .toList();
        return loadFirstResult(ordered);
    }

    private static SessionConfigLoadResult loadFirstResult(List<SessionConfigSource> ordered) {
        List<SessionConfigLoadAttempt> attempts = new ArrayList<>();
        for (SessionConfigSource source : ordered) {
            SessionConfigLoadResult result = source.loadResult();
            attempts.addAll(attemptsFor(result));
            if (!result.missing()) {
                return result.withAttempts(attempts);
            }
        }
        return SessionConfigLoadResult.missing(fallbackDescription(ordered)).withAttempts(attempts);
    }

    private static List<SessionConfigLoadAttempt> attemptsFor(SessionConfigLoadResult result) {
        if (result.attempts().isEmpty()) {
            return List.of(SessionConfigLoadAttempt.from(result));
        }
        return result.attempts();
    }

    private static Optional<String> firstJson(List<SessionConfigSource> sources) {
        for (SessionConfigSource source : sources) {
            Optional<String> json = source.readJson();
            if (json.isPresent()) {
                return json;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> readFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read A2UI session config file " + path, e);
        }
    }

    private static Optional<String> readResource(String resourceName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = SessionConfigSources.class.getClassLoader();
        }
        try (InputStream stream = loader.getResourceAsStream(resourceName)) {
            if (stream == null) {
                return Optional.empty();
            }
            return Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read A2UI session config resource " + resourceName, e);
        }
    }

    private static String normalizeResourceName(String resourceName) {
        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("resourceName must not be blank");
        }
        return resourceName.replace('\\', '/').replaceFirst("^/+", "");
    }

    private static String fallbackDescription(List<SessionConfigSource> sources) {
        if (sources.isEmpty()) {
            return "first-available:empty";
        }
        return sources.stream()
                .map(SessionConfigSource::description)
                .collect(Collectors.joining(", ", "first-available[", "]"));
    }

    private SessionConfigSources() {
    }
}
