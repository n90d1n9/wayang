package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
class PromptTemplateService {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful AI assistant that answers questions based on the provided context.

            Instructions:
            - Answer the question using ONLY the information from the provided context
            - If the context doesn't contain enough information, say so clearly
            - Be concise and accurate
            - Cite your sources when making specific claims using [1], [2], etc.
            - If you're unsure, acknowledge the uncertainty
            - Do not make up information not present in the context
            """;

    public String getSystemPrompt(GenerationConfig config) {
        return DEFAULT_SYSTEM_PROMPT;
    }

    public String buildUserPrompt(
            String query, List<String> contexts, List<ConversationTurn> history) {

        StringBuilder prompt = new StringBuilder();

        if (history != null && hasValidHistory(history)) {
            prompt.append("Previous conversation:\n");
            for (ConversationTurn turn : history) {
                if (isValidTurn(turn)) {
                    prompt.append(turn.role()).append(": ").append(turn.content()).append("\n");
                }
            }
            prompt.append("\n");
        }

        if (contexts != null && hasValidContexts(contexts)) {
            prompt.append("Context:\n");
            for (int i = 0; i < contexts.size(); i++) {
                String context = contexts.get(i);
                if (!isBlank(context)) {
                    prompt.append(String.format("[%d] %s\n\n", i + 1, context));
                }
            }
        }

        prompt.append("Question: ").append(query == null ? "" : query);

        return prompt.toString();
    }

    private boolean hasValidHistory(List<ConversationTurn> history) {
        for (ConversationTurn turn : history) {
            if (isValidTurn(turn)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidTurn(ConversationTurn turn) {
        return turn != null && turn.hasContent();
    }

    private boolean hasValidContexts(List<String> contexts) {
        for (String context : contexts) {
            if (!isBlank(context)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
