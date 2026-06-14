package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangRunInspectionCommandsTest {

    @Test
    void runInspectionCommandsAreRegisteredFromDedicatedModule() {
        CommandLine root = commandLine();
        CommandLine run = root.getSubcommands().get("run");
        Map<String, CommandLine> subcommands = run.getSubcommands();

        assertThat((Object) subcommands.get("status").getCommand())
                .isInstanceOf(WayangRunInspectionCommands.StatusCommand.class);
        assertThat((Object) subcommands.get("inspect").getCommand())
                .isInstanceOf(WayangRunInspectionCommands.InspectCommand.class);
    }

    @Test
    void inspectionCommandsAcceptStandardHelpOption() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "run",
                "status",
                "--help");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("Usage: wayang run status")
                .contains("Show a lifecycle status snapshot");
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
