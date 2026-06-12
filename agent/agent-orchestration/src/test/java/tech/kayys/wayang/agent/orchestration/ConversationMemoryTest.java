package tech.kayys.wayang.agent.orchestration;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.InferenceTypes.ChatMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMemoryTest {

    @Test
    void keepsNewestMessagesWithinWindow() {
        ConversationMemory memory = new ConversationMemory(2);

        memory.addUser("first");
        memory.addAssistant("second");
        memory.addUser("third");

        List<ChatMessage> messages = memory.getMessages();
        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(ChatMessage::content)
                .containsExactly("second", "third");
    }

    @Test
    void compressesLargeToolResultsBeforeStoring() {
        ConversationMemory memory = new ConversationMemory(4);
        String largeResult = "x".repeat(7_000);

        memory.addToolResult("call-1", largeResult);

        ChatMessage message = memory.getMessages().getFirst();
        assertThat(message.role()).isEqualTo("tool");
        assertThat(message.content()).contains("[result compressed]");
        assertThat(message.content()).hasSizeLessThan(largeResult.length());
    }
}
