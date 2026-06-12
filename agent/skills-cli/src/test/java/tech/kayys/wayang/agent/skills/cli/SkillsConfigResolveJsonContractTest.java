package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsConfigResolveJsonContractTest {

    @Test
    void objectStorageConfigResolveJsonMatchesCliContract() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "resolve", "--profile", "rustfs", "--json")).isZero();

        assertThat(console.out().trim()).isEqualTo(objectStorageConfigResolveJson());
        assertThat(console.err()).isEmpty();
    }

    private CommandLine command(TestConsole console) {
        SkillsCommandHandler handler = SkillsCommandHandler.inMemory(console.outStream(), console.errStream());
        return new CommandLine(new SkillsCommand(handler));
    }

    private static String objectStorageConfigResolveJson() {
        return """
                {"source":"profile","profile":"object-storage","runtime":false,"valid":true,"errorCount":0,"errors":[],"diagnostics":{"lifecycleReconcile":"inspect-only","createMissingStates":false,"removeOrphanedStates":false,"stores":[{"role":"definition","kind":"object-storage","target":"wayang/skills/definitions","initializeJdbcSchema":false,"maxEvents":0,"children":[]},{"role":"lifecycle-state","kind":"object-storage","target":"wayang/skills/lifecycle","initializeJdbcSchema":false,"maxEvents":0,"children":[]},{"role":"event-history","kind":"object-storage","target":"wayang/skills/events","initializeJdbcSchema":false,"maxEvents":10000,"children":[]},{"role":"artifact","kind":"object-storage","target":"wayang/skills/artifacts","initializeJdbcSchema":false,"maxEvents":0,"children":[]}]},"strategy":"object-storage","fullyDurable":true,"warningCount":0,"warnings":[]}
                """.strip();
    }

    private static final class TestConsole {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();
        private final PrintStream outStream = new PrintStream(out);
        private final PrintStream errStream = new PrintStream(err);

        PrintStream outStream() {
            return outStream;
        }

        PrintStream errStream() {
            return errStream;
        }

        String out() {
            return out.toString();
        }

        String err() {
            return err.toString();
        }
    }
}
