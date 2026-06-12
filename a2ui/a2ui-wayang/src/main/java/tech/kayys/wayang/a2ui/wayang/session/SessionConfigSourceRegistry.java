package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves declarative session config source specifications into loadable sources.
 */
public final class SessionConfigSourceRegistry {

    public static final String KEY_TYPE = "type";
    public static final String KEY_KIND = "kind";
    public static final String KEY_SOURCES = "sources";

    private static final Set<String> FALLBACK_TYPES = Set.of(
            "chain",
            "fallback",
            "first-available");

    private static final SessionConfigSourceRegistry STANDARD = standardBuilder().build();

    private final Map<String, SessionConfigSourceProvider> providers;
    private final Map<String, SessionConfigSourceCapability> providerCapabilities;
    private final SessionConfigSourcePolicy sourcePolicy;

    private SessionConfigSourceRegistry(
            Map<String, SessionConfigSourceProvider> providers,
            Map<String, SessionConfigSourceCapability> providerCapabilities,
            SessionConfigSourcePolicy sourcePolicy) {
        this.providers = Collections.unmodifiableMap(new LinkedHashMap<>(providers));
        this.providerCapabilities = Collections.unmodifiableMap(new LinkedHashMap<>(providerCapabilities));
        this.sourcePolicy = sourcePolicy == null ? SessionConfigSourcePolicy.allowAll() : sourcePolicy;
    }

    public static SessionConfigSourceRegistry standard() {
        return STANDARD;
    }

    public static Builder builder() {
        return new Builder(Map.of(), Map.of(), SessionConfigSourcePolicy.allowAll());
    }

    public static Builder standardBuilder() {
        return builder()
                .register(
                        "inline",
                        SessionConfigSourceRegistry::inlineSource,
                        SessionConfigSourceCapability.inline("inline"))
                .register(
                        "json",
                        SessionConfigSourceRegistry::inlineSource,
                        SessionConfigSourceCapability.inline("json"))
                .register(
                        "file",
                        SessionConfigSourceRegistry::fileSource,
                        SessionConfigSourceCapability.file("file"))
                .register(
                        "classpath",
                        SessionConfigSourceRegistry::classpathSource,
                        SessionConfigSourceCapability.classpath("classpath"))
                .register(
                        "resource",
                        SessionConfigSourceRegistry::classpathSource,
                        SessionConfigSourceCapability.classpath("resource"));
    }

    public SessionConfigSource source(Map<?, ?> spec) {
        Map<String, Object> values = new LinkedHashMap<>(TransportMaps.copy(spec));
        SessionConfigSourceSpec.requireValid(values);
        sourcePolicy.requireAllowed(values);
        String type = sourceType(values);
        if (FALLBACK_TYPES.contains(type) || values.containsKey(KEY_SOURCES)) {
            return chain(values.get(KEY_SOURCES));
        }
        SessionConfigSourceProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No A2UI session config source provider registered for " + type);
        }
        validateProviderSpec(type, provider, values);
        return provider.source(Map.copyOf(values));
    }

    public SessionConfigSource source(SessionConfigSourceSpec spec) {
        return source(Objects.requireNonNull(spec, "spec").toMap());
    }

    public SessionConfigSource sourceFromJson(String json) {
        return source(TransportJson.map(
                json,
                "A2UI session config source JSON must not be blank",
                "Unable to decode A2UI session config source JSON"));
    }

    public SessionConfigSource chain(Object rawSources) {
        List<SessionConfigSource> sources = specs(rawSources).stream()
                .map(this::source)
                .toList();
        return SessionConfigSources.firstAvailable(sources.toArray(SessionConfigSource[]::new));
    }

    public Set<String> providerNames() {
        return providers.keySet();
    }

    public Map<String, SessionConfigSourceCapability> providerCapabilities() {
        return providerCapabilities;
    }

    public Map<String, SessionConfigSourceCapability> sourceCapabilities() {
        Map<String, SessionConfigSourceCapability> capabilities = new LinkedHashMap<>(providerCapabilities);
        FALLBACK_TYPES.forEach(type -> capabilities.putIfAbsent(
                type,
                SessionConfigSourceCapability.fallback(type)));
        return Collections.unmodifiableMap(capabilities);
    }

    public Optional<SessionConfigSourceCapability> capability(String type) {
        String normalizedType = normalizeType(DecodeValues.text(type));
        if (normalizedType.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sourceCapabilities().get(normalizedType));
    }

    public SessionConfigSourcePolicy sourcePolicy() {
        return sourcePolicy;
    }

    public SessionConfigSourceRegistry withPolicy(SessionConfigSourcePolicy policy) {
        return new Builder(providers, providerCapabilities, sourcePolicy).policy(policy).build();
    }

    public SessionConfigSourceRegistry withProvider(String type, SessionConfigSourceProvider provider) {
        return new Builder(providers, providerCapabilities, sourcePolicy).register(type, provider).build();
    }

    private static SessionConfigSource inlineSource(Map<String, Object> values) {
        return SessionConfigSources.inlineJson(description(values, "inline"), rawText(values, "json", "value"));
    }

    private static SessionConfigSource fileSource(Map<String, Object> values) {
        String path = text(values, "path", "file");
        if (path.isBlank()) {
            throw new IllegalArgumentException("file session config source requires path");
        }
        return SessionConfigSources.file(Path.of(path));
    }

    private static SessionConfigSource classpathSource(Map<String, Object> values) {
        String resource = text(values, "resource", "classpath");
        if (resource.isBlank()) {
            throw new IllegalArgumentException("classpath session config source requires resource");
        }
        return SessionConfigSources.classpath(resource);
    }

    private static List<Map<?, ?>> specs(Object rawSources) {
        if (rawSources instanceof List<?> list) {
            List<Map<?, ?>> specs = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    specs.add(map);
                }
            }
            return specs;
        }
        if (rawSources instanceof Map<?, ?> map) {
            return List.of(map);
        }
        return List.of();
    }

    private static String sourceType(Map<String, Object> values) {
        String type = text(values, KEY_TYPE, KEY_KIND);
        if (type.isBlank()) {
            type = inferredType(values);
        }
        if (type.isBlank()) {
            throw new IllegalArgumentException("A2UI session config source type must not be blank");
        }
        return normalizeType(type);
    }

    private static String inferredType(Map<String, Object> values) {
        if (values.containsKey(KEY_SOURCES)) {
            return "first-available";
        }
        if (values.containsKey("path") || values.containsKey("file")) {
            return "file";
        }
        if (values.containsKey("resource") || values.containsKey("classpath")) {
            return "classpath";
        }
        if (values.containsKey("json") || values.containsKey("value")) {
            return "inline";
        }
        return "";
    }

    private static String normalizeType(String type) {
        return type.trim()
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);
    }

    private static String description(Map<String, Object> values, String fallback) {
        return DecodeValues.text(values.get("description"), fallback);
    }

    private static String text(Map<String, Object> values, String... keys) {
        return Arrays.stream(keys)
                .map(values::get)
                .map(DecodeValues::text)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElse("");
    }

    private static String rawText(Map<String, Object> values, String... keys) {
        return Arrays.stream(keys)
                .filter(values::containsKey)
                .map(values::get)
                .map(DecodeValues::rawText)
                .findFirst()
                .orElse(null);
    }

    private static void validateProviderSpec(
            String type,
            SessionConfigSourceProvider provider,
            Map<String, Object> values) {
        List<String> errors = provider.validationErrors(Map.copyOf(values));
        List<String> messages = errors == null
                ? List.of()
                : errors.stream()
                        .map(DecodeValues::text)
                        .filter(error -> !error.isBlank())
                        .toList();
        if (messages.isEmpty()) {
            return;
        }
        String prefix = "Invalid A2UI session config source provider spec for " + type + ": ";
        throw new IllegalArgumentException(prefix + String.join("; ", messages));
    }

    public static final class Builder {
        private final Map<String, SessionConfigSourceProvider> providers;
        private final Map<String, SessionConfigSourceCapability> providerCapabilities;
        private SessionConfigSourcePolicy sourcePolicy;

        private Builder(
                Map<String, SessionConfigSourceProvider> providers,
                Map<String, SessionConfigSourceCapability> providerCapabilities,
                SessionConfigSourcePolicy sourcePolicy) {
            this.providers = new LinkedHashMap<>(providers);
            this.providerCapabilities = new LinkedHashMap<>(providerCapabilities);
            this.sourcePolicy = sourcePolicy == null ? SessionConfigSourcePolicy.allowAll() : sourcePolicy;
        }

        public Builder register(String type, SessionConfigSourceProvider provider) {
            String normalizedType = normalizeType(DecodeValues.text(type));
            if (normalizedType.isBlank()) {
                throw new IllegalArgumentException("session config source provider type must not be blank");
            }
            SessionConfigSourceProvider resolvedProvider = Objects.requireNonNull(provider, "provider");
            providers.put(normalizedType, resolvedProvider);
            providerCapabilities.put(normalizedType, resolvedProvider.capability(normalizedType));
            return this;
        }

        public Builder register(
                String type,
                SessionConfigSourceProvider provider,
                SessionConfigSourceCapability capability) {
            String normalizedType = normalizeType(DecodeValues.text(type));
            if (normalizedType.isBlank()) {
                throw new IllegalArgumentException("session config source provider type must not be blank");
            }
            providers.put(normalizedType, Objects.requireNonNull(provider, "provider"));
            providerCapabilities.put(
                    normalizedType,
                    capability == null ? SessionConfigSourceCapability.generic(normalizedType) : capability);
            return this;
        }

        public Builder policy(SessionConfigSourcePolicy policy) {
            sourcePolicy = policy == null ? SessionConfigSourcePolicy.allowAll() : policy;
            return this;
        }

        public SessionConfigSourceRegistry build() {
            return new SessionConfigSourceRegistry(providers, providerCapabilities, sourcePolicy);
        }
    }
}
