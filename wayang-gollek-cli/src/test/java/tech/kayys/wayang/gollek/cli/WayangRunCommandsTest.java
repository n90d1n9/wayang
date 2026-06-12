package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WayangRunCommandsTest {

    @Test
    void runCommandIsRegisteredFromDedicatedModule() {
        CommandLine root = commandLine();
        CommandLine run = root.getSubcommands().get("run");

        assertThat((Object) run.getCommand())
                .isInstanceOf(WayangRunCommands.RunCommand.class);
    }

    @Test
    void extractedRunCommandKeepsRootInputWiring() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("prompt from stdin".getBytes(StandardCharsets.UTF_8));

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                in,
                stream(out),
                stream(err),
                "run",
                "--stdin",
                "--print-spec");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("specVersion=1" + System.lineSeparator())
                .contains("prompt=prompt from stdin" + System.lineSeparator());
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void runCommandAcceptsStandardHelpOption() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "run",
                "--help");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("Usage: wayang run")
                .contains("Commands:")
                .contains("status");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    private static CommandLine commandLine() {
        return new CommandLine(new WayangGollekCli(
                WayangGollekSdk.local(),
                stream(new ByteArrayOutputStream()),
                stream(new ByteArrayOutputStream())));
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
