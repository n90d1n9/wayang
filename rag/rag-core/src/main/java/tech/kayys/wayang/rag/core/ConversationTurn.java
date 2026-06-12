package tech.kayys.wayang.rag.core;

import java.time.Instant;

public record ConversationTurn(
                String role,
                String content,
                Instant timestamp) {

    public ConversationTurn {
        role = role == null ? "" : role.trim();
        content = content == null ? "" : content;
        timestamp = timestamp == null ? Instant.EPOCH : timestamp;
    }

    public boolean hasContent() {
        return !role.isBlank() && !content.isBlank();
    }
}
