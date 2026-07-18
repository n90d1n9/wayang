package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliRenderTest {

    @Test
    void jsonOutputUsesLineTerminatedPrint() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        WayangCliRender.jsonOrText(
                stream(output),
                true,
                () -> "{\"ok\":true}",
                () -> {
                    throw new AssertionError("Text renderer should not be evaluated for JSON output.");
                });

        assertThat(output.toString(StandardCharsets.UTF_8))
                .isEqualTo("{\"ok\":true}" + System.lineSeparator());
    }

    @Test
    void textOutputUsesFormatterOwnedSpacing() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        WayangCliRender.jsonOrText(
                stream(output),
                false,
                () -> {
                    throw new AssertionError("JSON renderer should not be evaluated for text output.");
                },
                () -> "plain text");

        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("plain text");
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
