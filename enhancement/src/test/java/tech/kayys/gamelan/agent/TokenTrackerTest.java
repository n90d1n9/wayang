package tech.kayys.gamelan.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TokenTrackerTest {

    private TokenTracker tracker;

    @BeforeEach
    void setUp() { tracker = new TokenTracker(); }

    @Test
    void startsAtZero() {
        assertThat(tracker.inputTokens()).isEqualTo(0);
        assertThat(tracker.outputTokens()).isEqualTo(0);
        assertThat(tracker.toolCalls()).isEqualTo(0);
        assertThat(tracker.llmCalls()).isEqualTo(0);
    }

    @Test
    void recordAccumulatesTokens() {
        // 400 system chars / 4 = 100 input tokens
        // 300 message chars / 4 = 75 input tokens  → 175 total input
        // 600 response chars / 3 = 200 output tokens
        tracker.record(400, 300, 600, 2);

        assertThat(tracker.inputTokens()).isEqualTo(175);
        assertThat(tracker.outputTokens()).isEqualTo(200);
        assertThat(tracker.totalTokens()).isEqualTo(375);
        assertThat(tracker.toolCalls()).isEqualTo(2);
        assertThat(tracker.llmCalls()).isEqualTo(1);
    }

    @Test
    void multipleRecordsAccumulate() {
        tracker.record(400, 0, 300, 1);
        tracker.record(400, 0, 300, 1);

        assertThat(tracker.llmCalls()).isEqualTo(2);
        assertThat(tracker.toolCalls()).isEqualTo(2);
    }

    @Test
    void resetClearsAllCounters() {
        tracker.record(1000, 500, 800, 3);
        tracker.reset();

        assertThat(tracker.inputTokens()).isEqualTo(0);
        assertThat(tracker.outputTokens()).isEqualTo(0);
        assertThat(tracker.llmCalls()).isEqualTo(0);
        assertThat(tracker.toolCalls()).isEqualTo(0);
    }

    @Test
    void oneLinerIsNonBlank() {
        tracker.record(400, 300, 600, 1);
        String summary = tracker.oneLiner();
        assertThat(summary).isNotBlank();
        assertThat(summary).contains("tokens");
        assertThat(summary).contains("calls");
    }

    @Test
    void fullSummaryContainsAllFields() {
        tracker.record(4000, 2000, 3000, 5);
        String summary = tracker.fullSummary();
        assertThat(summary).contains("Input tokens");
        assertThat(summary).contains("Output tokens");
        assertThat(summary).contains("LLM calls");
        assertThat(summary).contains("Tool calls");
    }

    @Test
    void sessionElapsedSecondsIsNonNegative() {
        assertThat(tracker.sessionElapsedSeconds()).isGreaterThanOrEqualTo(0);
    }
}
