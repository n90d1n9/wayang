package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsProfilesJsonContractTest {

    @Test
    void profilesJsonMatchesCliContract() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("profiles", "--json")).isZero();

        assertThat(console.out().trim()).isEqualTo(profilesJson());
        assertThat(console.err()).isEmpty();
    }

    private CommandLine command(TestConsole console) {
        SkillsCommandHandler handler = SkillsCommandHandler.inMemory(console.outStream(), console.errStream());
        return new CommandLine(new SkillsCommand(handler));
    }

    private static String profilesJson() {
        return """
                {"profileCount":6,"durableProfileCount":5,"externalProfileCount":4,"compositeProfileCount":2,"mirroredProfileCount":1,"durableFallbackProfileCount":2,"profiles":[{"label":"default","aliases":["runtime","registry","memory","ephemeral","dev","development"],"description":"Ephemeral runtime profile using registry definitions, memory state, disabled events, and memory artifacts.","strategy":"ephemeral","fullyDurable":false,"hasExternalProvider":false,"hasCompositeProvider":false,"hasMirroredProvider":false,"hasDurableFallback":false,"roleCount":4,"warningCount":2},{"label":"local-filesystem","aliases":["local","filesystem","file","files","disk"],"description":"Durable local-file profile using the configured skills base directory.","strategy":"local-filesystem","fullyDurable":true,"hasExternalProvider":false,"hasCompositeProvider":false,"hasMirroredProvider":false,"hasDurableFallback":false,"roleCount":4,"warningCount":0},{"label":"object-storage","aliases":["object","s3","rustfs","cloud","cloud-storage"],"description":"Durable S3/RustFS-compatible object-storage profile using the configured object prefix.","strategy":"object-storage","fullyDurable":true,"hasExternalProvider":true,"hasCompositeProvider":false,"hasMirroredProvider":false,"hasDurableFallback":false,"roleCount":4,"warningCount":0},{"label":"jdbc","aliases":["database","db","sql","postgres","postgresql"],"description":"Durable database profile using JDBC-backed definition, lifecycle, event, and artifact tables.","strategy":"database","fullyDurable":true,"hasExternalProvider":true,"hasCompositeProvider":false,"hasMirroredProvider":false,"hasDurableFallback":false,"roleCount":4,"warningCount":0},{"label":"hybrid-object-file","aliases":["hybrid","object-file","object-with-file-fallback","cloud-file-fallback"],"description":"Durable hybrid profile that prefers object storage and falls back to local files.","strategy":"hybrid-fallback","fullyDurable":true,"hasExternalProvider":true,"hasCompositeProvider":true,"hasMirroredProvider":false,"hasDurableFallback":true,"roleCount":4,"warningCount":0},{"label":"mirrored-object-file","aliases":["mirrored","mirror","replicated","object-file-mirror","cloud-file-mirror"],"description":"Durable mirrored profile that writes object storage and local files together.","strategy":"mirrored","fullyDurable":true,"hasExternalProvider":true,"hasCompositeProvider":true,"hasMirroredProvider":true,"hasDurableFallback":true,"roleCount":4,"warningCount":0}]}
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
