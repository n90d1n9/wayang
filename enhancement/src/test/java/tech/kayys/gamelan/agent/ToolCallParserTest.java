package tech.kayys.gamelan.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ToolCallParser}.
 */
class ToolCallParserTest {

    private final ToolCallParser parser = new ToolCallParser();

    @Test
    void parsesBasicToolCall() {
        String text = """
                Let me read that file.
                <tool_call>
                  <n>read_file</n>
                  <path>src/Main.java</path>
                </tool_call>
                """;
        List<ToolCall> calls = parser.parse(text);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).name()).isEqualTo("read_file");
        assertThat(calls.get(0).param("path")).isEqualTo("src/Main.java");
    }

    @Test
    void parsesNameAttribute() {
        String text = """
                <tool_call name="write_file">
                  <path>out.txt</path>
                  <content>hello world</content>
                </tool_call>
                """;
        List<ToolCall> calls = parser.parse(text);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).name()).isEqualTo("write_file");
        assertThat(calls.get(0).param("content")).isEqualTo("hello world");
    }

    @Test
    void innerNameTagTakesPrecedenceOverAttribute() {
        String text = """
                <tool_call name="wrong_tool">
                  <n>right_tool</n>
                  <arg>value</arg>
                </tool_call>
                """;
        List<ToolCall> calls = parser.parse(text);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).name()).isEqualTo("right_tool");
    }

    @Test
    void parsesMultipleToolCalls() {
        String text = """
                <tool_call>
                  <n>read_file</n>
                  <path>a.txt</path>
                </tool_call>
                <tool_call>
                  <n>run_command</n>
                  <command>ls -la</command>
                </tool_call>
                """;
        List<ToolCall> calls = parser.parse(text);
        assertThat(calls).hasSize(2);
        assertThat(calls.get(0).name()).isEqualTo("read_file");
        assertThat(calls.get(1).name()).isEqualTo("run_command");
        assertThat(calls.get(1).param("command")).isEqualTo("ls -la");
    }

    @Test
    void parsesMultiLineContent() {
        String text = """
                <tool_call>
                  <n>write_file</n>
                  <path>hello.py</path>
                  <content>def hello():
                    print("world")
                  </content>
                </tool_call>
                """;
        List<ToolCall> calls = parser.parse(text);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).param("content")).contains("def hello():");
        assertThat(calls.get(0).param("content")).contains("print(\"world\")");
    }

    @Test
    void returnsEmptyForNull()       { assertThat(parser.parse(null)).isEmpty(); }
    @Test
    void returnsEmptyForBlank()      { assertThat(parser.parse("   ")).isEmpty(); }
    @Test
    void returnsEmptyForNoBlocks()   { assertThat(parser.parse("Hello! 42")).isEmpty(); }

    @Test
    void ignoresMalformedBlockWithNoName() {
        String text = "<tool_call><path>test.txt</path></tool_call>";
        assertThat(parser.parse(text)).isEmpty();
    }

    @Test
    void paramDefaultOnMissingKey() {
        ToolCall call = parser.parse(
                "<tool_call><n>read_file</n><path>x</path></tool_call>").get(0);
        assertThat(call.param("missing")).isEqualTo("");
        assertThat(call.param("missing", "default")).isEqualTo("default");
    }

    @Test
    void hasParamCorrectness() {
        ToolCall call = parser.parse(
                "<tool_call><n>read_file</n><path>x</path></tool_call>").get(0);
        assertThat(call.hasParam("path")).isTrue();
        assertThat(call.hasParam("start_line")).isFalse();
    }

    @Test
    void parsesCaseInsensitiveTag() {
        String text = "<TOOL_CALL><n>glob</n><pattern>**/*.java</pattern></TOOL_CALL>";
        List<ToolCall> calls = parser.parse(text);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).name()).isEqualTo("glob");
    }

    @Test
    void preservesParameterOrder() {
        ToolCall call = parser.parse("""
                <tool_call>
                  <n>run_command</n>
                  <command>mvn test</command>
                  <workdir>./backend</workdir>
                  <timeout>120</timeout>
                </tool_call>
                """).get(0);
        List<String> keys = List.copyOf(call.parameters().keySet());
        assertThat(keys).containsExactly("command", "workdir", "timeout");
    }

    @Test
    void parsesToolCallAmidstNaturalLanguage() {
        String text = """
                I'll read the file first to understand the current implementation.
                
                <tool_call>
                  <n>read_file</n>
                  <path>src/Service.java</path>
                  <start_line>1</start_line>
                  <end_line>50</end_line>
                </tool_call>
                
                Once I see the content I'll suggest changes.
                """;
        List<ToolCall> calls = parser.parse(text);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).param("start_line")).isEqualTo("1");
        assertThat(calls.get(0).param("end_line")).isEqualTo("50");
    }
}
