package tech.kayys.wayang.rag.core;

import java.time.Instant;

public record ConversationTurn(
                String role,
                String content,
                Instant timestamp) {
}