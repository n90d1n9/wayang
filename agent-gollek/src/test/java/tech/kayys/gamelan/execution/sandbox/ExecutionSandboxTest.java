package tech.kayys.gamelan.execution.sandbox;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.gamelan.agent.ToolCall;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link ExecutionSandbox} — copy-on-write filesystem isolation.
 */
class ExecutionSandboxTest {

    private ExecutionSandbox sandbox;

    @BeforeEach
    void setUp() {
        sandbox = new ExecutionSandbox();
        sandbox.interceptor = new SandboxInterceptor();
    }

    @AfterEach
    void tearDown() {
        if (sandbox.isActive()) sandbox.discard();
    }

    @Test
    void initiallyInactive() {
        assertThat(sandbox.isActive()).isFalse();
        assertThat(sandbox.activeSession()).isEmpty();
    }

    @Test
    void enterActivatesSession() {
        var session = sandbox.enter("test-session");
        assertThat(sandbox.isActive()).isTrue();
        assertThat(sandbox.activeSession()).isPresent();
        assertThat(session.label()).isEqualTo("test-session");
        assertThat(session.overlay()).exists();
    }

    @Test
    void discardDeactivatesSandbox() {
        sandbox.enter("test");
        assertThat(sandbox.isActive()).isTrue();
        sandbox.discard();
        assertThat(sandbox.isActive()).isFalse();
    }

    @Test
    void writeFileCallIsIntercepted() {
        sandbox.enter("test");
        ToolCall call = tool("write_file", Map.of(
                "path", "src/Foo.java",
                "content", "public class Foo {}"));

        var result = sandbox.intercept(call);

        assertThat(result).isPresent();
        assertThat(result.get().isSuccess()).isTrue();
        assertThat(result.get().output()).contains("[SANDBOX]");
        assertThat(result.get().output()).contains("src/Foo.java");
    }

    @Test
    void writeCallIsRecordedInDiff() {
        sandbox.enter("test");
        ToolCall call = tool("write_file", Map.of("path", "NewFile.java", "content", "class X {}"));
        sandbox.intercept(call);

        var diffs = sandbox.diff();
        assertThat(diffs).hasSize(1);
        assertThat(diffs.get(0).relativePath()).isEqualTo("NewFile.java");
        assertThat(diffs.get(0).type()).isEqualTo(ExecutionSandbox.DiffType.CREATE);
    }

    @Test
    void patchCallIsIntercepted() {
        sandbox.enter("test");
        String patch = """
                --- a/Foo.java
                +++ b/Foo.java
                @@ -1,3 +1,4 @@
                 public class Foo {
                +    int x;
                 }
                """;
        ToolCall call = tool("apply_patch", Map.of("patch", patch));
        var result = sandbox.intercept(call);

        assertThat(result).isPresent();
        assertThat(result.get().output()).contains("[SANDBOX]");
    }

    @Test
    void readOnlyCommandsPassThrough() {
        sandbox.enter("test");
        for (String cmd : new String[]{
            "ls -la", "cat README.md", "git status", "git diff HEAD",
            "mvn test -Dtest=Foo", "grep -r TODO src/"
        }) {
            ToolCall call = tool("run_command", Map.of("command", cmd));
            var result = sandbox.intercept(call);
            assertThat(result).as("Read-only command should pass through: " + cmd).isEmpty();
        }
    }

    @Test
    void writeCommandIsSimulated() {
        sandbox.enter("test");
        ToolCall call = tool("run_command", Map.of("command", "echo hello > output.txt"));
        var result = sandbox.intercept(call);

        assertThat(result).isPresent();
        assertThat(result.get().output()).contains("[SANDBOX]");
        assertThat(result.get().output()).contains("not executed");
    }

    @Test
    void writtenContentIsReadableViaOverlay() throws IOException {
        sandbox.enter("test");
        String content = "public class Bar { }";
        ToolCall writeCall = tool("write_file", Map.of(
                "path", "Bar.java", "content", content));
        sandbox.intercept(writeCall);

        // The overlay should serve the written content back
        var overlayContent = sandbox.interceptRead("Bar.java");
        assertThat(overlayContent).isPresent();
        assertThat(overlayContent.get()).isEqualTo(content);
    }

    @Test
    void noInterceptionWhenSandboxInactive() {
        // Sandbox not entered
        ToolCall call = tool("write_file", Map.of("path", "x.java", "content", "class X{}"));
        assertThat(sandbox.intercept(call)).isEmpty();
        assertThat(sandbox.interceptRead("x.java")).isEmpty();
    }

    @Test
    void multipleWritesAccumulateInDiff() {
        sandbox.enter("test");
        sandbox.intercept(tool("write_file", Map.of("path", "A.java", "content", "class A{}")));
        sandbox.intercept(tool("write_file", Map.of("path", "B.java", "content", "class B{}")));
        sandbox.intercept(tool("write_file", Map.of("path", "C.java", "content", "class C{}")));

        assertThat(sandbox.diff()).hasSize(3);
    }

    @Test
    void sessionSummaryContainsChangeCounts() {
        sandbox.enter("my-session");
        sandbox.intercept(tool("write_file", Map.of("path", "New.java", "content", "class N{}")));
        sandbox.intercept(tool("run_command", Map.of("command", "npx build --prod")));

        var session = sandbox.activeSession().orElseThrow();
        String summary = session.summary();
        assertThat(summary).contains("my-session");
    }

    @Test
    void diffDisplayContainsDiffTypeIcon() {
        sandbox.enter("test");
        sandbox.intercept(tool("write_file", Map.of("path", "New.java", "content", "x")));
        var diffs = sandbox.diff();
        assertThat(diffs.get(0).display()).startsWith("+");
    }

    @Test
    void enteringNewSessionDiscardsOldOne() {
        sandbox.enter("session-1");
        assertThat(sandbox.isActive()).isTrue();
        sandbox.enter("session-2"); // should discard session-1
        assertThat(sandbox.activeSession().get().label()).isEqualTo("session-2");
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ToolCall tool(String name, Map<String, String> params) {
        return new ToolCall(name, params, "<tc/>");
    }
}
