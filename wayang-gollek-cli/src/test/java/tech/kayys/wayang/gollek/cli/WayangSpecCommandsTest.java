package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangSpecCommandsTest {

    @Test
    void specCommandsAreRegisteredFromDedicatedModule() {
        CommandLine root = commandLine();
        CommandLine spec = root.getSubcommands().get("spec");
        Map<String, CommandLine> subcommands = spec.getSubcommands();

        assertThat((Object) spec.getCommand())
                .isInstanceOf(WayangSpecCommands.SpecCommand.class);
        assertThat((Object) subcommands.get("validate").getCommand())
                .isInstanceOf(WayangSpecCommands.SpecCommand.ValidateCommand.class);
        assertThat((Object) subcommands.get("template").getCommand())
                .isInstanceOf(WayangSpecCommands.SpecCommand.TemplateCommand.class);
    }

    @Test
    void extractedTemplateCommandUsesContextOutputAndRunSpecs() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "spec",
                "template",
                "--profile",
                "assistant-agent");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("profileId=assistant-agent" + System.lineSeparator())
                .contains("surfaceId=assistant-agent" + System.lineSeparator());
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
