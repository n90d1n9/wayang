package tech.kayys.gamelan.agent.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Dedicated tests for {@link OrchestratorSelector} auto-selection heuristics
 * and explicit strategy routing.
 *
 * Uses stub implementations to avoid bringing in the full CDI container.
 */
class OrchestratorSelectorTest {

    private OrchestratorSelector selector;

    @BeforeEach
    void setUp() {
        selector = new OrchestratorSelector();
        setField(selector, "direct",     stub("direct", false));
        setField(selector, "react",      stub("react",  true));
        setField(selector, "reflexion",  stub("reflexion", false));
        setField(selector, "multiAgent", stub("multi-agent", true));
    }

    // ── Explicit strategy routing ──────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "direct,   direct",
        "simple,   direct",
        "1,        direct",
        "react,    react",
        "agent,    react",
        "2,        react",
        "reflexion,reflexion",
        "reflect,  reflexion",
        "multi,    multi-agent",
        "multi-agent, multi-agent",
        "3,        multi-agent"
    })
    void explicitStrategyMapsCorrectly(String input, String expected) {
        assertThat(selector.select(input.strip(), "any task").strategyId())
                .isEqualTo(expected.strip());
    }

    @Test
    void unknownStrategyFallsBackToReact() {
        assertThat(selector.select("gpt-5-turbo-pro", "task").strategyId())
                .isEqualTo("react");
    }

    @Test
    void nullStrategyTriggersAutoSelect() {
        // "what is Java?" — short, no action words → direct
        assertThat(selector.select(null, "what is Java?").strategyId())
                .isEqualTo("direct");
    }

    @Test
    void blankStrategyTriggersAutoSelect() {
        assertThat(selector.select("  ", "what is Java?").strategyId())
                .isEqualTo("direct");
    }

    // ── Auto-selection: Tier 1 (direct) ───────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
        "what is Java?",
        "how does Spring work?",
        "explain dependency injection",
        "what is the difference between interface and abstract class?",
        "is Quarkus faster than Spring?"
    })
    void autoSelectsDirectForShortQuestions(String task) {
        assertThat(selector.select(null, task).strategyId()).isEqualTo("direct");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "read the Main.java file",
        "fix the bug in UserService",
        "write unit tests for OrderController",
        "search for TODO comments",
        "refactor the authentication module",
        "build and run the application",
        "update the pom.xml dependencies"
    })
    void autoSelectsReactForActionTasks(String task) {
        assertThat(selector.select(null, task).strategyId()).isEqualTo("react");
    }

    // ── Auto-selection: Tier 3 (multi-agent) ──────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
        "full review of the entire codebase",
        "comprehensive audit of src/",
        "analyse all services for correctness and security and performance",
        "review all modules for security and documentation issues",
        "full audit of the microservices"
    })
    void autoSelectsMultiAgentForCrossDomain(String task) {
        assertThat(selector.select(null, task).strategyId()).isEqualTo("multi-agent");
    }

    // ── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void nullTaskDefaultsToReact() {
        assertThat(selector.select(null, null).strategyId()).isEqualTo("react");
    }

    @Test
    void blankTaskDefaultsToReact() {
        assertThat(selector.select(null, "   ").strategyId()).isEqualTo("react");
    }

    @Test
    void longTaskBodyIsNotDirect() {
        String longTask = "a".repeat(81); // > 80 chars → not direct
        assertThat(selector.select(null, longTask).strategyId()).isEqualTo("react");
    }

    @Test
    void allReturnsAllFourOrchestrators() {
        assertThat(selector.all()).hasSize(4);
        assertThat(selector.all()).extracting(AgentOrchestrator::strategyId)
                .containsExactlyInAnyOrder("direct", "react", "reflexion", "multi-agent");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static AgentOrchestrator stub(String id, boolean tools) {
        return new AgentOrchestrator() {
            @Override public String strategyId()    { return id; }
            @Override public boolean supportsTools(){ return tools; }
            @Override public OrchestratorResult execute(AgentRequest r) {
                return OrchestratorResult.ok("stub", id, 1, List.of(), java.time.Duration.ZERO);
            }
        };
    }

    private static void setField(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set " + field, e);
        }
    }
}
