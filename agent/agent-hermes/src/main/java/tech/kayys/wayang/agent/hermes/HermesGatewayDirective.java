package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral outbound delivery instruction for Hermes gateway channels.
 */
public record HermesGatewayDirective(
        boolean gatewayEnabled,
        boolean supportedPlatform,
        boolean active,
        String operation,
        String platform,
        String destinationType,
        String destinationId,
        String continuityKey,
        String channelId,
        String threadId,
        String conversationId,
        String messageId,
        String tenantId,
        String sessionId,
        String userId,
        String username,
        String correlationId,
        String reason) {

    public HermesGatewayDirective {
        operation = HermesDirectiveSupport.clean(operation, active ? "deliver" : "none");
        platform = HermesDirectiveSupport.clean(platform, "cli");
        destinationType = HermesDirectiveSupport.clean(destinationType, "none");
        destinationId = HermesDirectiveSupport.clean(destinationId, "");
        continuityKey = HermesDirectiveSupport.clean(continuityKey, platform + ":default");
        channelId = HermesDirectiveSupport.clean(channelId, "");
        threadId = HermesDirectiveSupport.clean(threadId, "");
        conversationId = HermesDirectiveSupport.clean(conversationId, "");
        messageId = HermesDirectiveSupport.clean(messageId, "");
        tenantId = HermesDirectiveSupport.clean(tenantId, "default");
        sessionId = HermesDirectiveSupport.clean(sessionId, "");
        userId = HermesDirectiveSupport.clean(userId, "");
        username = HermesDirectiveSupport.clean(username, "");
        correlationId = HermesDirectiveSupport.clean(correlationId, "");
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "gateway delivery requested" : "no gateway delivery requested");
    }

    public static HermesGatewayDirective from(
            HermesGatewayContext context,
            AgentRequest request,
            HermesAgentModeConfig config) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesRuntimeCapabilities capabilities = effectiveConfig.runtimeCapabilities();
        HermesGatewayContext effectiveContext = context == null
                ? new HermesGatewayContext(
                "cli", "", "", "", "", "default", "", "", "", "", capabilities.supportsGatewayPlatform("cli"))
                : context;
        boolean requestPresent = request != null;
        boolean gatewayEnabled = capabilities.supportsGateway();
        boolean supported = effectiveContext.supportedPlatform();
        Destination destination = destination(effectiveContext);
        boolean active = requestPresent && gatewayEnabled && supported && !"none".equals(destination.type());
        return new HermesGatewayDirective(
                gatewayEnabled,
                supported,
                active,
                active ? "deliver" : "none",
                effectiveContext.platform(),
                destination.type(),
                destination.id(),
                effectiveContext.continuityKey(),
                effectiveContext.channelId(),
                effectiveContext.threadId(),
                effectiveContext.conversationId(),
                effectiveContext.messageId(),
                effectiveContext.tenantId(),
                effectiveContext.sessionId(),
                effectiveContext.userId(),
                effectiveContext.username(),
                effectiveContext.correlationId(),
                reason(requestPresent, gatewayEnabled, supported, destination));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("gatewayEnabled", gatewayEnabled);
        metadata.put("supportedPlatform", supportedPlatform);
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("platform", platform);
        metadata.put("destinationType", destinationType);
        metadata.put("destinationId", destinationId);
        metadata.put("continuityKey", continuityKey);
        metadata.put("channelId", channelId);
        metadata.put("threadId", threadId);
        metadata.put("conversationId", conversationId);
        metadata.put("messageId", messageId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("username", username);
        metadata.put("correlationId", correlationId);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }

    private static Destination destination(HermesGatewayContext context) {
        if (!context.conversationId().isBlank()) {
            return new Destination("conversation", context.conversationId());
        }
        if (!context.channelId().isBlank() && !context.threadId().isBlank()) {
            return new Destination("thread", context.threadId());
        }
        if (!context.channelId().isBlank()) {
            return new Destination("channel", context.channelId());
        }
        if (!context.sessionId().isBlank()) {
            return new Destination("session", context.sessionId());
        }
        if (!context.userId().isBlank()) {
            return new Destination("user", context.userId());
        }
        if ("cli".equals(context.platform()) || "direct".equals(context.platform())) {
            return new Destination("local", context.tenantId());
        }
        return new Destination("none", "");
    }

    private static String reason(
            boolean requestPresent,
            boolean gatewayEnabled,
            boolean supported,
            Destination destination) {
        if (!requestPresent) {
            return "default plan only";
        }
        if (!gatewayEnabled) {
            return "gateway disabled";
        }
        if (!supported) {
            return "unsupported gateway platform";
        }
        if ("none".equals(destination.type())) {
            return "no gateway destination detected";
        }
        return "gateway delivery requested";
    }
    private record Destination(String type, String id) {
    }
}
