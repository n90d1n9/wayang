package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Optional JSON-backed source for A2UI session configuration.
 */
public record SessionConfigSource(
        String description,
        Supplier<Optional<String>> jsonSupplier,
        Supplier<SessionConfigLoadResult> loadResultSupplier) {

    public SessionConfigSource(
            String description,
            Supplier<Optional<String>> jsonSupplier) {
        this(description, jsonSupplier, null);
    }

    public SessionConfigSource {
        description = description == null || description.isBlank()
                ? "session-config"
                : description.trim();
        jsonSupplier = Objects.requireNonNull(jsonSupplier, "jsonSupplier");
    }

    public Optional<String> readJson() {
        Optional<String> json = jsonSupplier.get();
        return json == null ? Optional.empty() : json;
    }

    public Optional<WayangA2uiSessionConfig> load() {
        return readJson().map(SessionConfigDecoder::fromJson);
    }

    public SessionConfigLoadResult loadResult() {
        if (loadResultSupplier != null) {
            SessionConfigLoadResult result = loadResultSupplier.get();
            return result == null ? SessionConfigLoadResult.missing(description) : result;
        }
        return defaultLoadResult();
    }

    private SessionConfigLoadResult defaultLoadResult() {
        try {
            Optional<String> json = readJson();
            return json
                    .map(value -> SessionConfigLoadResult.loaded(description, SessionConfigDecoder.fromJson(value)))
                    .orElseGet(() -> SessionConfigLoadResult.missing(description));
        } catch (RuntimeException e) {
            return SessionConfigLoadResult.failed(description, e);
        }
    }

    public WayangA2uiSessionConfig loadOrDefault() {
        return load().orElseGet(WayangA2uiSessionConfig::defaultConfig);
    }
}
