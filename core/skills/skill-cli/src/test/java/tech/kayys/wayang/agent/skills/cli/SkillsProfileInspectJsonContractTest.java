package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsProfileInspectJsonContractTest {

    @Test
    void objectStorageProfileInspectJsonMatchesCliContract() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("profile", "inspect", "rustfs", "--json")).isZero();

        assertThat(console.out().trim()).isEqualTo(objectStorageProfileInspectJson());
        assertThat(console.err()).isEmpty();
    }

    private CommandLine command(TestConsole console) {
        SkillsCommandHandler handler = SkillsCommandHandler.inMemory(console.outStream(), console.errStream());
        return new CommandLine(new SkillsCommand(handler));
    }

    private static String objectStorageProfileInspectJson() {
        return """
                {"label":"object-storage","aliases":["object","s3","rustfs","cloud","cloud-storage"],"description":"Durable S3/RustFS-compatible object-storage profile using the configured object prefix.","status":{"source":"profile","profile":"object-storage","runtime":false,"preflightAvailable":false,"diagnosticsAvailable":false,"strategy":"object-storage","fullyDurable":true,"hasEphemeralRole":false,"hasDurableFallback":false,"hasExternalProvider":true,"hasCustomProvider":false,"hasCompositeProvider":false,"hasMirroredProvider":false,"roleCount":4,"durableRoleCount":4,"ephemeralRoleCount":0,"disabledRoleCount":0,"customRoleCount":0,"warningCount":0,"warnings":[],"roles":[{"role":"definition","path":"definition","provider":"object-storage","persistenceClass":"object-storage","strategy":"object-storage","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":true,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"lifecycle-state","path":"lifecycle-state","provider":"object-storage","persistenceClass":"object-storage","strategy":"object-storage","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":true,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"event-history","path":"event-history","provider":"object-storage","persistenceClass":"object-storage","strategy":"object-storage","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":true,"custom":false,"composite":false,"mirrored":false,"capabilities":["write","query-events","prune-events"],"children":[]},{"role":"artifact","path":"artifact","provider":"object-storage","persistenceClass":"object-storage","strategy":"object-storage","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":true,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]}]}}
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
