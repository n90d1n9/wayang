package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateServiceTest {

    private final PromptTemplateService service = new PromptTemplateService();

    @Test
    void skipsMalformedHistoryAndContextsWithoutRenderingNulls() {
        String prompt = service.buildUserPrompt(
                null,
                Arrays.asList("First context", null, " ", "Fourth context"),
                Arrays.asList(
                        null,
                        new ConversationTurn("user", "Earlier question", Instant.parse("2026-05-27T00:00:00Z")),
                        new ConversationTurn("", "ignored", Instant.parse("2026-05-27T00:00:00Z")),
                        new ConversationTurn("assistant", null, Instant.parse("2026-05-27T00:00:00Z"))));

        assertThat(prompt)
                .contains("Previous conversation:\nuser: Earlier question\n\n")
                .contains("Context:\n[1] First context\n\n[4] Fourth context\n\n")
                .endsWith("Question: ");
        assertThat(prompt).doesNotContain("null").doesNotContain("[2]").doesNotContain("[3]");
    }

    @Test
    void omitsEmptySectionsWhenNoUsableHistoryOrContextsExist() {
        String prompt = service.buildUserPrompt(
                "What changed?",
                Arrays.asList(null, " "),
                List.of(new ConversationTurn(" ", "ignored", Instant.parse("2026-05-27T00:00:00Z"))));

        assertThat(prompt)
                .doesNotContain("Previous conversation:")
                .doesNotContain("Context:")
                .isEqualTo("Question: What changed?");
    }

    @Test
    void handlesNullCollections() {
        String prompt = service.buildUserPrompt("What changed?", null, null);

        assertThat(prompt).isEqualTo("Question: What changed?");
    }
}
