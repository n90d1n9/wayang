package tech.kayys.wayang.agent.spi.coordinator;

import java.time.Instant;
import java.util.*;

/**
 * Message passed between agents in multi-agent workflows.
 * Immutable and thread-safe for safe concurrent agent execution.
 */
public final class AgentMessage {

    private final String id;
    private final String sender;
    private final String recipient; // Agent name or "broadcast" for all
    private final MessageType type;
    private final Map<String, Object> payload;
    private final Instant timestamp;
    private final Map<String, String> metadata;

    public enum MessageType {
        REQUEST,     // Agent requesting another agent to perform task
        RESPONSE,    // Agent responding with results
        ERROR,       // Agent reporting error
        CONTEXT,     // Sharing context/state with another agent
        ACKNOWLEDGMENT // Acknowledgment of receipt
    }

    private AgentMessage(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.sender = Objects.requireNonNull(builder.sender, "sender");
        this.recipient = Objects.requireNonNull(builder.recipient, "recipient");
        this.type = Objects.requireNonNull(builder.type, "type");
        this.payload = Map.copyOf(builder.payload);
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.metadata = Map.copyOf(builder.metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public MessageType getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Check if this message is broadcast to all agents.
     */
    public boolean isBroadcast() {
        return "broadcast".equals(recipient);
    }

    /**
     * Get payload value with type safety.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getPayloadValue(String key, Class<T> type) {
        Object value = payload.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Builder for fluent message construction.
     */
    public static class Builder {
        private String id;
        private String sender;
        private String recipient;
        private MessageType type;
        private final Map<String, Object> payload = new HashMap<>();
        private Instant timestamp;
        private final Map<String, String> metadata = new HashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sender(String sender) {
            this.sender = sender;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder payload(String key, Object value) {
            this.payload.put(key, value);
            return this;
        }

        public Builder payloadAll(Map<String, Object> data) {
            this.payload.putAll(data);
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadataAll(Map<String, String> data) {
            this.metadata.putAll(data);
            return this;
        }

        /**
         * Create message to broadcast to all agents.
         */
        public Builder broadcast() {
            this.recipient = "broadcast";
            return this;
        }

        public AgentMessage build() {
            return new AgentMessage(this);
        }
    }

    /**
     * Factory methods for common message types.
     */
    public static AgentMessage request(String sender, String recipient, Map<String, Object> data) {
        return builder()
                .sender(sender)
                .recipient(recipient)
                .type(MessageType.REQUEST)
                .payloadAll(data)
                .build();
    }

    public static AgentMessage response(String sender, String recipient, Map<String, Object> data) {
        return builder()
                .sender(sender)
                .recipient(recipient)
                .type(MessageType.RESPONSE)
                .payloadAll(data)
                .build();
    }

    public static AgentMessage error(String sender, String recipient, String errorMessage) {
        return builder()
                .sender(sender)
                .recipient(recipient)
                .type(MessageType.ERROR)
                .payload("error", errorMessage)
                .build();
    }

    @Override
    public String toString() {
        return "AgentMessage{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", recipient='" + recipient + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", payloadSize=" + payload.size() +
                '}';
    }
}
