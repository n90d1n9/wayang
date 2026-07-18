package tech.kayys.gamelan.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ToolResultTest {

    @Test
    void successToXmlHasNoStatusAttribute() {
        ToolResult r = ToolResult.success("read_file", "file content here");
        String xml = r.toXml();
        assertThat(xml).contains("<tool_result name=\"read_file\">");
        assertThat(xml).doesNotContain("status=");
        assertThat(xml).contains("file content here");
        assertThat(xml).contains("</tool_result>");
    }

    @Test
    void failureToXmlHasErrorStatus() {
        ToolResult r = ToolResult.failure("run_command", "command not found");
        String xml = r.toXml();
        assertThat(xml).contains("status=\"error\"");
        assertThat(xml).contains("command not found");
    }

    @Test
    void longOutputIsTruncatedInXml() {
        String bigOutput = "x".repeat(60_000);
        ToolResult r = ToolResult.success("glob", bigOutput);
        String xml = r.toXml();
        assertThat(xml).contains("OUTPUT TRUNCATED");
        // The XML must not contain the full 60KB output
        assertThat(xml.length()).isLessThan(55_000);
    }

    @Test
    void nullOutputHandledGracefully() {
        ToolResult r = new ToolResult("tool", null, 0, null);
        // toXml must not throw NPE
        assertThatCode(r::toXml).doesNotThrowAnyException();
    }

    @Test
    void failureWithPartialOutputIncludesBoth() {
        ToolResult r = new ToolResult("run_command", "partial stdout", 1, "error msg");
        String xml = r.toXml();
        assertThat(xml).contains("error msg");
        assertThat(xml).contains("partial stdout");
    }

    @Test
    void isSuccessReturnsTrueOnlyForExitZero() {
        assertThat(ToolResult.success("t", "").isSuccess()).isTrue();
        assertThat(ToolResult.failure("t", "e").isSuccess()).isFalse();
        assertThat(new ToolResult("t", "", 2, "oops").isSuccess()).isFalse();
    }
}
