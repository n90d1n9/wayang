package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@ApplicationScoped
class PromptTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(PromptTemplateService.class);

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

        if (!history.isEmpty()) {
            prompt.append("Previous conversation:\n");
            for (ConversationTurn turn : history) {
                prompt.append(turn.role()).append(": ").append(turn.content()).append("\n");
            }
            prompt.append("\n");
        }

        if (!contexts.isEmpty()) {
            prompt.append("Context:\n");
            for (int i = 0; i < contexts.size(); i++) {
                prompt.append(String.format("[%d] %s\n\n", i + 1, contexts.get(i)));
            }
        }

        prompt.append("Question: ").append(query);

        return prompt.toString();
    }
}
