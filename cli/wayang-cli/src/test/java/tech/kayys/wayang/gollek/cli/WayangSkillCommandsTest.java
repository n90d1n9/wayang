package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangSkillCommandsTest {

    @Test
    void skillCommandsAreRegisteredFromDedicatedModule() {
        CommandLine root = commandLine();
        Map<String, CommandLine> commands = root.getSubcommands();
        CommandLine skills = commands.get("skills");
        Map<String, CommandLine> subcommands = skills.getSubcommands();

        assertThat((Object) skills.getCommand())
                .isInstanceOf(WayangSkillCommands.SkillsCommand.class);
        assertThat((Object) commands.get("capabilities").getCommand())
                .isInstanceOf(WayangSkillCommands.SkillsCommand.class);
        assertThat((Object) subcommands.get("list").getCommand())
                .isInstanceOf(WayangSkillCommands.SkillsCommand.ListCommand.class);
        assertThat((Object) subcommands.get("inspect").getCommand())
                .isInstanceOf(WayangSkillCommands.SkillsCommand.InspectCommand.class);
        assertThat((Object) subcommands.get("search").getCommand())
                .isInstanceOf(WayangSkillCommands.SkillsCommand.SearchCommand.class);
    }

    @Test
    void extractedSkillListCommandUsesContextOutputAndSdk() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "skills",
                "list",
                "--surface",
                "assistant-agent",
                "--source",
                "rag",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("\"product\":\"Wayang\"")
                .contains("\"skillIds\":[\"rag.retrieve\"]");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void extractedSkillInspectCommandUsesContextOutputAndSdk() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "skills",
                "inspect",
                "rag",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("\"skillId\":\"rag.retrieve\"")
                .contains("\"source\":\"rag\"");
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
