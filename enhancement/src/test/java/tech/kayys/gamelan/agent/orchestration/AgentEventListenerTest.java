package tech.kayys.gamelan.agent.orchestration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AgentEventListener} including the default NOOP, the
 * logging factory method, and correct callback ordering.
 */
class AgentEventListenerTest {

    @Test
    void noopDoesNotThrow() {
        AgentEventListener noop = AgentEventListener.NOOP;
        assertThatCode(() -> {
            noop.onRunStart("task", "model");
            noop.onIterationStart(0, 10);
            noop.onTextChunk("hello");
            noop.onToolStart("read_file", "{}");
            noop.onToolEnd("read_file", "result", false, 100);
            noop.onIterationEnd(0, "stop");
            noop.onComplete("answer", 1);
            noop.onError("oops", 0);
        }).doesNotThrowAnyException();
    }

    @Test
    void customListenerReceivesAllCallbacks() {
        List<String> events = new ArrayList<>();

        AgentEventListener listener = new AgentEventListener() {
            @Override public void onRunStart(String task, String model)
                { events.add("run_start"); }
            @Override public void onIterationStart(int iter, int max)
                { events.add("iter_start:" + iter); }
            @Override public void onTextChunk(String chunk)
                { events.add("chunk:" + chunk); }
            @Override public void onToolStart(String name, String input)
                { events.add("tool_start:" + name); }
            @Override public void onToolEnd(String name, String result, boolean err, long ms)
                { events.add("tool_end:" + name + ":err=" + err); }
            @Override public void onIterationEnd(int iter, String reason)
                { events.add("iter_end:" + reason); }
            @Override public void onComplete(String answer, int iters)
                { events.add("complete:" + iters); }
        };

        // Simulate the order the agent loop calls them
        listener.onRunStart("my task", "llama3");
        listener.onIterationStart(0, 5);
        listener.onTextChunk("hello");
        listener.onTextChunk(" world");
        listener.onToolStart("read_file", "{path:x}");
        listener.onToolEnd("read_file", "content", false, 42);
        listener.onIterationEnd(0, "tool_calls");
        listener.onIterationStart(1, 5);
        listener.onTextChunk("final");
        listener.onIterationEnd(1, "stop");
        listener.onComplete("final answer", 2);

        assertThat(events).containsExactly(
                "run_start",
                "iter_start:0",
                "chunk:hello",
                "chunk: world",
                "tool_start:read_file",
                "tool_end:read_file:err=false",
                "iter_end:tool_calls",
                "iter_start:1",
                "chunk:final",
                "iter_end:stop",
                "complete:2"
        );
    }

    @Test
    void loggingListenerDoesNotThrow() {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentEventListenerTest.class);
        AgentEventListener listener = AgentEventListener.logging(logger);
        assertThatCode(() -> {
            listener.onRunStart("task", "model");
            listener.onIterationStart(0, 10);
            listener.onToolStart("read_file", "{}");
            listener.onToolEnd("read_file", "content", false, 100);
            listener.onComplete("answer", 2);
        }).doesNotThrowAnyException();
    }

    @Test
    void errorCallbackReceivesMessage() {
        List<String> errors = new ArrayList<>();
        AgentEventListener listener = new AgentEventListener() {
            @Override public void onError(String msg, int iter)
                { errors.add(iter + ":" + msg); }
        };
        listener.onError("Model timeout", 3);
        assertThat(errors).containsExactly("3:Model timeout");
    }
}
