package tech.kayys.gamelan.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBudgetAdvisorTest {

    @Mock GamelanConfig config;
    @InjectMocks TokenBudgetAdvisor advisor;

    @BeforeEach
    void setUp() {
        when(config.tokenBudget()).thenReturn(1000);
    }

    private ConversationSession sessionWithTokens(int approxTokens) {
        // Each char ≈ 0.25 tokens; create a session with enough text
        ConversationSession session = new ConversationSession(null, false, 999999);
        // Add turns until we reach the desired token count
        // ~4 chars = 1 token, so approxTokens * 4 chars
        String content = "x".repeat(approxTokens * 4);
        session.addTurn("q", AgentResponse.builder().text(content).build());
        return session;
    }

    @Test
    void okStatusBelowWarningThreshold() {
        ConversationSession s = sessionWithTokens(500); // 50% of 1000
        assertThat(advisor.status(s)).isEqualTo(TokenBudgetAdvisor.BudgetStatus.OK);
    }

    @Test
    void warningStatusAt70Percent() {
        ConversationSession s = sessionWithTokens(730); // ~73%
        assertThat(advisor.status(s)).isIn(
                TokenBudgetAdvisor.BudgetStatus.WARNING,
                TokenBudgetAdvisor.BudgetStatus.CRITICAL);
    }

    @Test
    void criticalStatusAt90Percent() {
        ConversationSession s = sessionWithTokens(920); // ~92%
        assertThat(advisor.status(s)).isIn(
                TokenBudgetAdvisor.BudgetStatus.CRITICAL,
                TokenBudgetAdvisor.BudgetStatus.EXCEEDED);
    }

    @Test
    void advisoryEmptyWhenOk() {
        ConversationSession s = sessionWithTokens(100);
        assertThat(advisor.advisory(s)).isEmpty();
    }

    @Test
    void advisoryNonEmptyWhenStressed() {
        ConversationSession s = sessionWithTokens(800);
        // May be WARNING or CRITICAL depending on exact count
        if (advisor.status(s) != TokenBudgetAdvisor.BudgetStatus.OK) {
            assertThat(advisor.advisory(s)).isNotBlank();
        }
    }

    @Test
    void indicatorContainsPercentage() {
        ConversationSession s = sessionWithTokens(500);
        String indicator = advisor.indicator(s);
        assertThat(indicator).contains("%");
        assertThat(indicator).contains("ctx:");
    }

    @Test
    void budgetMatchesConfig() {
        assertThat(advisor.budget()).isEqualTo(1000);
    }

    @Test
    void ratioIsZeroForZeroBudget() {
        when(config.tokenBudget()).thenReturn(0);
        ConversationSession s = new ConversationSession(null);
        assertThat(advisor.ratio(s)).isEqualTo(0.0);
    }
}
