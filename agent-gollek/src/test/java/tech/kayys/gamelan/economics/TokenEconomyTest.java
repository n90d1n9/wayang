package tech.kayys.gamelan.economics;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenEconomyTest {

    @Mock GamelanConfig  config;
    @Mock EpisodicMemory episodic;

    @InjectMocks TokenEconomy economy;

    @BeforeEach
    void setUp() {
        when(config.tokenBudget()).thenReturn(6000);
        when(config.defaultModel()).thenReturn("llama3");
    }

    // ── Budget checks ─────────────────────────────────────────────────────

    @Test
    void budgetCheckOkWhenAmpleRemaining() {
        var check = economy.checkBudget(500);
        assertThat(check.status()).isEqualTo(TokenEconomy.BudgetCheck.Status.OK);
        assertThat(check.canProceed()).isTrue();
    }

    @Test
    void budgetCheckTightWhenRequestIsMoreThanHalfRemaining() {
        economy.consume(3500, "previous"); // only 2500 left
        var check = economy.checkBudget(2000); // > 50% of remaining
        assertThat(check.status()).isEqualTo(TokenEconomy.BudgetCheck.Status.TIGHT);
        assertThat(check.canProceed()).isTrue();
    }

    @Test
    void budgetCheckInsufficientWhenRequestExceedsRemaining() {
        economy.consume(5000, "previous"); // only 1000 left
        var check = economy.checkBudget(2000); // more than remaining
        assertThat(check.status()).isEqualTo(TokenEconomy.BudgetCheck.Status.INSUFFICIENT);
        assertThat(check.canProceed()).isTrue(); // can still try but tight
    }

    @Test
    void budgetCheckExhaustedWhenOverBudget() {
        economy.consume(6001, "over-limit");
        var check = economy.checkBudget(100);
        assertThat(check.status()).isEqualTo(TokenEconomy.BudgetCheck.Status.EXHAUSTED);
        assertThat(check.canProceed()).isFalse();
    }

    @Test
    void consumeAccumulatesSpend() {
        economy.consume(500, "planning");
        economy.consume(1500, "execution");
        assertThat(economy.sessionSpend()).isEqualTo(2000);
        assertThat(economy.remaining()).isEqualTo(4000);
    }

    @Test
    void resetClearsSessionSpend() {
        economy.consume(3000, "work");
        economy.reset();
        assertThat(economy.sessionSpend()).isEqualTo(0);
        assertThat(economy.remaining()).isEqualTo(6000);
    }

    // ── Model selection ───────────────────────────────────────────────────

    @Test
    void simpleTaskSelectsFastTier() {
        economy.configureModelLadder(TokenEconomy.ModelTier.FAST, "qwen-mini");
        var alloc = economy.selectModel("what is Java?", true);
        assertThat(alloc.isViable()).isTrue();
        assertThat(alloc.tier()).isEqualTo(TokenEconomy.ModelTier.FAST);
    }

    @Test
    void complexTaskSelectsExpertTier() {
        economy.configureModelLadder(TokenEconomy.ModelTier.EXPERT, "qwen-72b");
        var alloc = economy.selectModel(
                "Perform a comprehensive architecture review and refactor all services across " +
                "the entire codebase to improve maintainability and security posture", true);
        assertThat(alloc.tier()).isEqualTo(TokenEconomy.ModelTier.EXPERT);
    }

    @Test
    void degradesToFastTierWhenBudgetExhausted() {
        economy.consume(6001, "over");
        var alloc = economy.selectModel("fix the bug in UserService", true);
        assertThat(alloc.isViable()).isFalse(); // exhausted
    }

    @Test
    void escalationMovesUpTierLadder() {
        economy.configureModelLadder(TokenEconomy.ModelTier.FAST,     "fast-model");
        economy.configureModelLadder(TokenEconomy.ModelTier.STANDARD, "standard-model");
        economy.configureModelLadder(TokenEconomy.ModelTier.EXPERT,   "expert-model");

        var escalated = economy.escalate(TokenEconomy.ModelTier.FAST);
        assertThat(escalated).contains("standard-model");

        var escalatedAgain = economy.escalate(TokenEconomy.ModelTier.STANDARD);
        assertThat(escalatedAgain).contains("expert-model");

        var atMax = economy.escalate(TokenEconomy.ModelTier.EXPERT);
        assertThat(atMax).isEmpty();
    }

    // ── Budget allocation ─────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "what is Java?,         SIMPLE",
        "fix the null bug,      MODERATE",
        "Comprehensive refactor of the entire codebase architecture including all services, COMPLEX"
    })
    void allocateCorrectlyClassifiesComplexity(String task, String expectedComplexity) {
        var alloc = economy.allocate(task, 6000);
        assertThat(alloc.complexity().name()).isEqualTo(expectedComplexity);
        assertThat(alloc.total()).isEqualTo(6000);
    }

    @Test
    void allocationBudgetSumsToTotal() {
        var alloc = economy.allocate("moderate task to implement something", 3000);
        assertThat(alloc.planningBudget() + alloc.executionBudget() + alloc.reflectionBudget())
                .isEqualTo(3000);
    }

    // ── Report ────────────────────────────────────────────────────────────

    @Test
    void reportContainsAllRelevantInfo() {
        economy.consume(1000, "skill-a");
        economy.consume(500, "skill-b");
        var report = economy.report();
        assertThat(report.sessionSpend()).isEqualTo(1500);
        assertThat(report.remaining()).isEqualTo(4500);
        assertThat(report.skillCosts()).containsKey("skill-a");
        assertThat(report.summary()).contains("1500");
    }
}
