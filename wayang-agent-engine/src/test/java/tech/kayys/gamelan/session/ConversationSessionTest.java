package tech.kayys.gamelan.session;

import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.agent.AgentResponse;
import tech.kayys.gamelan.agent.ConversationMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ConversationSessionTest {

    private AgentResponse resp(String text) {
        return AgentResponse.builder().text(text).build();
    }

    @Test void newSessionGetsRandomId() {
        assertThat(new ConversationSession(null).id()).startsWith("sess-");
        assertThat(new ConversationSession(null).id())
                .isNotEqualTo(new ConversationSession(null).id());
    }

    @Test void resumesExistingId() {
        assertThat(new ConversationSession("my-id").id()).isEqualTo("my-id");
    }

    @Test void addTurnIncrementsTurnCount() {
        ConversationSession s = new ConversationSession(null);
        assertThat(s.turnCount()).isEqualTo(0);
        s.addTurn("hello", resp("hi"));
        assertThat(s.turnCount()).isEqualTo(1);
    }

    @Test void toMessagesAlternatesRoles() {
        ConversationSession s = new ConversationSession(null);
        s.addTurn("user1", resp("asst1"));
        s.addTurn("user2", resp("asst2"));
        List<ConversationMessage> msgs = s.toMessages();
        assertThat(msgs).hasSize(4);
        assertThat(msgs.get(0).role()).isEqualTo("user");
        assertThat(msgs.get(1).role()).isEqualTo("assistant");
        assertThat(msgs.get(2).role()).isEqualTo("user");
        assertThat(msgs.get(3).role()).isEqualTo("assistant");
    }

    @Test void clearResetsHistory() {
        ConversationSession s = new ConversationSession(null);
        s.addTurn("q", resp("a"));
        s.clear();
        assertThat(s.turnCount()).isEqualTo(0);
        assertThat(s.toMessages()).isEmpty();
    }

    @Test void tokenBudgetTrimsOldTurns() {
        ConversationSession s = new ConversationSession(null, false, 100);
        for (int i = 0; i < 30; i++) {
            s.addTurn("user message number " + i, resp("assistant reply number " + i));
        }
        assertThat(s.tokenCount()).isLessThanOrEqualTo(120);
        assertThat(s.turnCount()).isLessThan(30);
    }

    @Test void toMessagesEmptyInitially() {
        assertThat(new ConversationSession(null).toMessages()).isEmpty();
    }
}
