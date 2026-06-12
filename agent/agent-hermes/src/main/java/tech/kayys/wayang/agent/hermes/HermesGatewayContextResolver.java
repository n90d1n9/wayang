package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Resolves gateway aliases from request metadata into a stable Hermes context.
 */
public final class HermesGatewayContextResolver {

    private static final List<String> PLATFORM_KEYS = List.of(
            "hermes.gateway.platform",
            "gateway.platform",
            "gatewayPlatform",
            "platform",
            "source");

    private static final List<String> CHANNEL_KEYS = List.of(
            "hermes.gateway.channelId",
            "gateway.channelId",
            "channelId",
            "channel.id",
            "chatId",
            "chat.id",
            "roomId",
            "room.id");

    private static final List<String> THREAD_KEYS = List.of(
            "hermes.gateway.threadId",
            "gateway.threadId",
            "threadId",
            "thread.id",
            "topicId",
            "topic.id");

    private static final List<String> CONVERSATION_KEYS = List.of(
            "hermes.gateway.conversationId",
            "gateway.conversationId",
            "conversationId",
            "conversation.id",
            "dialogId",
            "dialog.id");

    private static final List<String> MESSAGE_KEYS = List.of(
            "hermes.gateway.messageId",
            "gateway.messageId",
            "messageId",
            "message.id",
            "eventId",
            "event.id");

    private static final List<String> USER_KEYS = List.of(
            "hermes.gateway.userId",
            "gateway.userId",
            "gatewayUserId",
            "senderId",
            "sender.id",
            "authorId",
            "author.id",
            "userId");

    private static final List<String> USERNAME_KEYS = List.of(
            "hermes.gateway.username",
            "gateway.username",
            "username",
            "userName",
            "senderName",
            "authorName",
            "handle");

    private static final List<String> CORRELATION_KEYS = List.of(
            "hermes.gateway.correlationId",
            "gateway.correlationId",
            "correlationId",
            "traceId",
            "requestId");

    private final HermesAgentModeConfig config;
    private final HermesRuntimeCapabilities capabilities;

    public HermesGatewayContextResolver(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
        this.capabilities = this.config.runtimeCapabilities();
    }

    public HermesGatewayContext resolve(AgentRequest request) {
        HermesRequestValues values = HermesRequestValues.from(request);
        String platform = platform(values);
        return new HermesGatewayContext(
                platform,
                channelId(values, platform),
                values.firstText(THREAD_KEYS).orElse(""),
                conversationId(values, request),
                values.firstText(MESSAGE_KEYS).orElse(""),
                request == null ? "default" : request.tenantId(),
                request == null ? "" : HermesText.trimToEmpty(request.sessionId()),
                values.firstText(USER_KEYS)
                        .orElseGet(() -> request == null ? "" : HermesText.trimToEmpty(request.userId())),
                values.firstText(USERNAME_KEYS).orElse(""),
                values.firstText(CORRELATION_KEYS)
                        .orElseGet(() -> request == null ? "" : HermesText.trimToEmpty(request.requestId())),
                capabilities.supportsGatewayPlatform(platform));
    }

    private String platform(HermesRequestValues values) {
        Optional<String> explicit = values.firstText(PLATFORM_KEYS);
        if (explicit.isPresent()) {
            String normalized = canonicalPlatform(explicit.orElseThrow());
            if (supportsOrConfigured(normalized)) {
                return normalized;
            }
            return normalized.isBlank() ? fallbackPlatform() : normalized;
        }
        Optional<String> channel = values.firstText(List.of("channel"));
        if (channel.isPresent()) {
            String normalized = canonicalPlatform(channel.orElseThrow());
            if (supportsOrConfigured(normalized)) {
                return normalized;
            }
        }
        return fallbackPlatform();
    }

    private String fallbackPlatform() {
        return capabilities.supportsGatewayPlatform("cli") ? "cli" : "direct";
    }

    private String channelId(HermesRequestValues values, String platform) {
        Optional<String> explicit = values.firstText(CHANNEL_KEYS);
        if (explicit.isPresent()) {
            return explicit.orElseThrow();
        }
        Optional<String> channel = values.firstText(List.of("channel"));
        if (channel.isPresent() && !canonicalPlatform(channel.orElseThrow()).equals(platform)) {
            return channel.orElseThrow();
        }
        return "";
    }

    private String conversationId(HermesRequestValues values, AgentRequest request) {
        Optional<String> explicit = values.firstText(CONVERSATION_KEYS);
        if (explicit.isPresent()) {
            return explicit.orElseThrow();
        }
        if (request != null && request.sessionId() != null && !request.sessionId().isBlank()) {
            return request.sessionId().trim();
        }
        return "";
    }

    private boolean supportsOrConfigured(String platform) {
        return capabilities.supportsGatewayPlatform(platform)
                || config.gatewayPlatforms().stream().anyMatch(value ->
                HermesRequestValues.normalize(value).equals(HermesRequestValues.normalize(platform)));
    }

    private static String canonicalPlatform(String value) {
        return switch (HermesRequestValues.normalize(value)) {
            case "telegram", "tg" -> "telegram";
            case "discord" -> "discord";
            case "slack" -> "slack";
            case "whatsapp", "wa" -> "whatsapp";
            case "signal" -> "signal";
            case "cli", "terminal", "shell" -> "cli";
            default -> HermesText.trimToEmpty(value).toLowerCase(Locale.ROOT);
        };
    }
}
