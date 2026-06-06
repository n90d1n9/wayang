package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiClientCapabilities;
import tech.kayys.wayang.a2ui.core.A2uiDataPart;
import tech.kayys.wayang.a2ui.core.A2uiProtocol;
import tech.kayys.wayang.a2ui.core.A2uiServerCapabilities;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Wayang request/context adapter for A2UI v0.8.
 */
public final class WayangA2ui {

    public static final String CONTEXT_KEY = "a2ui";
    public static final String EXTENSION_ACTIVE_KEY = "extensionActive";
    public static final String CLIENT_CAPABILITIES_KEY = "clientCapabilities";
    public static final String DATA_PARTS_KEY = "dataParts";
    public static final String SESSION_CONFIG_KEY = "sessionConfig";

    private WayangA2ui() {
    }

    public static Map<String, Object> agentExtension(A2uiServerCapabilities capabilities) {
        A2uiServerCapabilities resolved = capabilities == null ? A2uiServerCapabilities.standard() : capabilities;
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("uri", A2uiProtocol.EXTENSION_URI);
        extension.put("description", "Ability to render A2UI");
        extension.put("required", false);
        extension.put("params", resolved.toParams());
        return WayangA2uiTransportMaps.freeze(extension);
    }

    public static Map<String, Object> dataPart(A2uiServerMessage message) {
        return A2uiDataPart.of(message).toPayload();
    }

    public static AgentRequest activate(AgentRequest request) {
        return withContextValue(request, EXTENSION_ACTIVE_KEY, true);
    }

    public static AgentRequest withClientCapabilities(
            AgentRequest request,
            A2uiClientCapabilities capabilities) {
        return withContextValue(
                request,
                CLIENT_CAPABILITIES_KEY,
                (capabilities == null ? A2uiClientCapabilities.standard() : capabilities).toMetadata());
    }

    public static Optional<A2uiClientCapabilities> clientCapabilities(AgentRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Object rawA2ui = request.context().get(CONTEXT_KEY);
        if (rawA2ui instanceof Map<?, ?> a2ui) {
            Object rawCapabilities = a2ui.get(CLIENT_CAPABILITIES_KEY);
            if (rawCapabilities instanceof Map<?, ?> capabilities) {
                return Optional.of(A2uiClientCapabilities.fromMap(capabilities));
            }
        }
        Object rawMetadata = request.context().get(A2uiProtocol.CLIENT_CAPABILITIES_KEY);
        if (rawMetadata instanceof Map<?, ?> capabilities) {
            return Optional.of(A2uiClientCapabilities.fromMap(capabilities));
        }
        return Optional.empty();
    }

    public static AgentRequest withSessionConfig(
            AgentRequest request,
            WayangA2uiSessionConfig config) {
        return withContextValue(
                request,
                SESSION_CONFIG_KEY,
                (config == null ? WayangA2uiSessionConfig.inspectOnly() : config).toMap());
    }

    public static Optional<WayangA2uiSessionConfig> sessionConfig(AgentRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Object rawA2ui = request.context().get(CONTEXT_KEY);
        if (rawA2ui instanceof Map<?, ?> a2ui) {
            Object rawConfig = a2ui.get(SESSION_CONFIG_KEY);
            if (rawConfig instanceof Map<?, ?> config) {
                return Optional.of(WayangA2uiSessionConfig.fromMap(config));
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static AgentRequest withContextValue(AgentRequest request, String key, Object value) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        Map<String, Object> context = new LinkedHashMap<>(request.context());
        Map<String, Object> a2ui = new LinkedHashMap<>();
        Object current = context.get(CONTEXT_KEY);
        if (current instanceof Map<?, ?> currentMap) {
            currentMap.forEach((entryKey, entryValue) -> {
                if (entryKey != null) {
                    a2ui.put(String.valueOf(entryKey), entryValue);
                }
            });
        }
        a2ui.put(key, value);
        context.put(CONTEXT_KEY, WayangA2uiTransportMaps.freeze(a2ui));
        return new AgentRequest(
                request.requestId(),
                request.prompt(),
                request.systemPrompt(),
                request.strategy(),
                request.allowedSkills(),
                context,
                request.parameters(),
                request.tenantId(),
                request.sessionId(),
                request.userId(),
                request.stream(),
                request.verbose(),
                request.timeout(),
                request.memoryConfig(),
                request.modelId(),
                request.timestamp());
    }
}
