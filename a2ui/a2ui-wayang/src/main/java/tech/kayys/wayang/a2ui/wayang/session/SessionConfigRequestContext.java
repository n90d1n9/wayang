package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves A2UI session configuration from an agent request context.
 */
public final class SessionConfigRequestContext {

    private final String contextKey;
    private final String configKey;
    private final String sourceKey;
    private final SessionConfigSourceRegistry sourceRegistry;

    public SessionConfigRequestContext(
            String contextKey,
            String configKey,
            String sourceKey,
            SessionConfigSourceRegistry sourceRegistry) {
        this.contextKey = requireText(contextKey, "contextKey");
        this.configKey = requireText(configKey, "configKey");
        this.sourceKey = requireText(sourceKey, "sourceKey");
        this.sourceRegistry = sourceRegistry == null
                ? SessionConfigSourceRegistry.standard()
                : sourceRegistry;
    }

    public Optional<WayangA2uiSessionConfig> config(AgentRequest request) {
        Optional<Map<?, ?>> context = nestedContext(request);
        if (context.isEmpty()) {
            return Optional.empty();
        }
        Map<?, ?> values = context.orElseThrow();
        if (values.containsKey(configKey)) {
            return Optional.of(config(values.get(configKey)));
        }
        if (values.containsKey(sourceKey)) {
            return source(values.get(sourceKey)).load();
        }
        return Optional.empty();
    }

    public SessionConfigLoadResult loadResult(AgentRequest request) {
        Optional<Map<?, ?>> context = nestedContext(request);
        if (context.isEmpty()) {
            return SessionConfigLoadResult.missing(configDescription());
        }
        Map<?, ?> values = context.orElseThrow();
        if (values.containsKey(configKey)) {
            return directConfigLoadResult(values.get(configKey));
        }
        if (values.containsKey(sourceKey)) {
            try {
                return source(values.get(sourceKey)).loadResult();
            } catch (RuntimeException e) {
                return SessionConfigLoadResult.failed(sourceDescription(), e);
            }
        }
        return SessionConfigLoadResult.missing(configDescription());
    }

    public Optional<SessionConfigSourceDiagnostics> sourceDiagnostics(AgentRequest request) {
        Optional<Map<?, ?>> context = nestedContext(request);
        if (context.isEmpty()) {
            return Optional.empty();
        }
        Map<?, ?> values = context.orElseThrow();
        if (values.containsKey(configKey) || !values.containsKey(sourceKey)) {
            return Optional.empty();
        }
        return Optional.of(sourceDiagnostics(values.get(sourceKey)));
    }

    public SessionConfigRequestDiagnostics requestDiagnostics(AgentRequest request) {
        Optional<Map<?, ?>> context = nestedContext(request);
        if (context.isEmpty()) {
            return new SessionConfigRequestDiagnostics(
                    SessionConfigRequestDiagnostics.DIAGNOSTICS_ID,
                    contextKey,
                    configKey,
                    sourceKey,
                    false,
                    false,
                    false,
                    SessionConfigRequestDiagnostics.ACTIVE_NONE,
                    SessionConfigLoadResult.missing(configDescription()),
                    null);
        }
        Map<?, ?> values = context.orElseThrow();
        boolean configPresent = values.containsKey(configKey);
        boolean sourcePresent = values.containsKey(sourceKey);
        if (configPresent) {
            return new SessionConfigRequestDiagnostics(
                    SessionConfigRequestDiagnostics.DIAGNOSTICS_ID,
                    contextKey,
                    configKey,
                    sourceKey,
                    true,
                    true,
                    sourcePresent,
                    SessionConfigRequestDiagnostics.ACTIVE_DIRECT_CONFIG,
                    directConfigLoadResult(values.get(configKey)),
                    null);
        }
        if (sourcePresent) {
            SessionConfigSourceDiagnostics diagnostics = sourceDiagnostics(values.get(sourceKey));
            return new SessionConfigRequestDiagnostics(
                    SessionConfigRequestDiagnostics.DIAGNOSTICS_ID,
                    contextKey,
                    configKey,
                    sourceKey,
                    true,
                    false,
                    true,
                    SessionConfigRequestDiagnostics.ACTIVE_SOURCE,
                    diagnostics.loadResult(),
                    diagnostics);
        }
        return new SessionConfigRequestDiagnostics(
                SessionConfigRequestDiagnostics.DIAGNOSTICS_ID,
                contextKey,
                configKey,
                sourceKey,
                true,
                false,
                false,
                SessionConfigRequestDiagnostics.ACTIVE_NONE,
                SessionConfigLoadResult.missing(configDescription()),
                null);
    }

    private Optional<Map<?, ?>> nestedContext(AgentRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Map<String, Object> requestContext = request.context();
        if (requestContext == null) {
            return Optional.empty();
        }
        Object rawContext = requestContext.get(contextKey);
        if (rawContext instanceof Map<?, ?> context) {
            return Optional.of(context);
        }
        return Optional.empty();
    }

    private WayangA2uiSessionConfig config(Object rawConfig) {
        if (rawConfig instanceof Map<?, ?> config) {
            return SessionConfigDecoder.fromMap(config);
        }
        if (rawConfig instanceof CharSequence json) {
            return SessionConfigDecoder.fromJson(json.toString());
        }
        throw new IllegalArgumentException(configDescription() + " must be a config object or JSON string");
    }

    private SessionConfigLoadResult directConfigLoadResult(Object rawConfig) {
        try {
            return SessionConfigLoadResult.loaded(configDescription(), config(rawConfig));
        } catch (RuntimeException e) {
            return SessionConfigLoadResult.failed(configDescription(), e);
        }
    }

    private SessionConfigSource source(Object rawSource) {
        if (rawSource instanceof Map<?, ?> sourceSpec) {
            return sourceRegistry.source(sourceSpec);
        }
        if (rawSource instanceof CharSequence json) {
            return sourceRegistry.sourceFromJson(json.toString());
        }
        throw new IllegalArgumentException(sourceDescription() + " must be a source object or JSON string");
    }

    private SessionConfigSourceDiagnostics sourceDiagnostics(Object rawSource) {
        try {
            if (rawSource instanceof Map<?, ?> sourceSpec) {
                return SessionConfigSourceDiagnostics.load(sourceSpec, sourceRegistry);
            }
            if (rawSource instanceof CharSequence json) {
                return SessionConfigSourceDiagnostics.load(TransportJson.map(
                        json.toString(),
                        sourceDescription() + " JSON must not be blank",
                        "Unable to decode " + sourceDescription() + " JSON"), sourceRegistry);
            }
            throw new IllegalArgumentException(sourceDescription() + " must be a source object or JSON string");
        } catch (RuntimeException e) {
            return SessionConfigSourceDiagnostics.failed(sourceDescription(), e, sourceRegistry);
        }
    }

    private String configDescription() {
        return contextKey + "." + configKey;
    }

    private String sourceDescription() {
        return contextKey + "." + sourceKey;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }
}
