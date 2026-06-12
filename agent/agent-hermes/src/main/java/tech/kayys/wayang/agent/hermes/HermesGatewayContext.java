package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Normalized gateway/session identity for Hermes multi-platform continuity.
 */
public record HermesGatewayContext(
        String platform,
        String channelId,
        String threadId,
        String conversationId,
        String messageId,
        String tenantId,
        String sessionId,
        String userId,
        String username,
        String correlationId,
        boolean supportedPlatform) {

    public HermesGatewayContext {
        platform = HermesText.trimOr(platform, "cli");
        channelId = HermesText.trimOr(channelId, "");
        threadId = HermesText.trimOr(threadId, "");
        conversationId = HermesText.trimOr(conversationId, "");
        messageId = HermesText.trimOr(messageId, "");
        tenantId = HermesText.trimOr(tenantId, "default");
        sessionId = HermesText.trimOr(sessionId, "");
        userId = HermesText.trimOr(userId, "");
        username = HermesText.trimOr(username, "");
        correlationId = HermesText.trimOr(correlationId, "");
    }

    public String continuityKey() {
        if (!conversationId.isBlank()) {
            return platform + ":" + conversationId;
        }
        if (!channelId.isBlank() && !threadId.isBlank()) {
            return platform + ":" + channelId + ":" + threadId;
        }
        if (!channelId.isBlank()) {
            return platform + ":" + channelId;
        }
        if (!sessionId.isBlank()) {
            return platform + ":" + sessionId;
        }
        return platform + ":" + tenantId;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("platform", platform);
        metadata.put("channelId", channelId);
        metadata.put("threadId", threadId);
        metadata.put("conversationId", conversationId);
        metadata.put("messageId", messageId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("username", username);
        metadata.put("correlationId", correlationId);
        metadata.put("supportedPlatform", supportedPlatform);
        metadata.put("continuityKey", continuityKey());
        return Map.copyOf(metadata);
    }
}
