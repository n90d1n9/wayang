package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.ConversationTurn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record RagConversationQuery(
        String query,
        Map<String, Object> metadata) {

    static final String SESSION_ID = "sessionId";
    static final String CONVERSATION_HISTORY = "conversationHistory";

    RagConversationQuery {
        query = RagRuntimeText.trimToEmpty(query);
        metadata = copyMetadata(metadata);
    }

    static RagConversationQuery from(String query, String sessionId, List<ConversationTurn> history) {
        String safeQuery = RagRuntimeText.trimToEmpty(query);
        List<ConversationTurn> safeHistory = safeHistory(history);
        Map<String, Object> metadata = new LinkedHashMap<>();
        String safeSessionId = RagRuntimeText.trimToEmpty(sessionId);
        if (!safeSessionId.isBlank()) {
            metadata.put(SESSION_ID, safeSessionId);
        }
        if (!safeHistory.isEmpty()) {
            metadata.put(CONVERSATION_HISTORY, safeHistory);
        }
        return new RagConversationQuery(
                enhanceQueryWithHistory(safeQuery, safeHistory),
                metadata);
    }

    private static List<ConversationTurn> safeHistory(List<?> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return history.stream()
                .filter(ConversationTurn.class::isInstance)
                .map(ConversationTurn.class::cast)
                .filter(ConversationTurn::hasContent)
                .toList();
    }

    private static String enhanceQueryWithHistory(String query, List<ConversationTurn> history) {
        if (history == null || history.isEmpty()) {
            return query;
        }

        StringBuilder enhanced = new StringBuilder();
        enhanced.append("Previous conversation:\n");

        for (ConversationTurn turn : history) {
            enhanced.append(turn.role()).append(": ").append(turn.content()).append("\n");
        }

        enhanced.append("\nCurrent question: ").append(query);

        return enhanced.toString();
    }

    private static Map<String, Object> copyMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (CONVERSATION_HISTORY.equals(key) && value instanceof List<?> historyValue) {
                List<ConversationTurn> safeHistory = safeHistory(historyValue);
                if (!safeHistory.isEmpty()) {
                    copied.put(key, safeHistory);
                }
            } else if (SESSION_ID.equals(key)) {
                String safeSessionId = value instanceof String stringValue
                        ? RagRuntimeText.trimToEmpty(stringValue)
                        : "";
                if (!safeSessionId.isBlank()) {
                    copied.put(key, safeSessionId);
                }
            } else {
                copied.put(key, value);
            }
        });
        return RagRuntimeMetadata.copy(copied);
    }
}
