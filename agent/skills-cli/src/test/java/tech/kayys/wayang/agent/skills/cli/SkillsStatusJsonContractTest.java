package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsStatusJsonContractTest {

    @Test
    void defaultStatusJsonMatchesCliContract() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--json")).isZero();

        assertThat(console.out().trim()).isEqualTo(defaultStatusJson());
        assertThat(console.err()).isEmpty();
    }

    @Test
    void defaultPreflightStatusJsonMatchesCliContract() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--preflight", "--json")).isZero();

        assertThat(console.out().trim()).isEqualTo(defaultPreflightStatusJson());
        assertThat(console.err()).isEmpty();
    }

    @Test
    void defaultDiagnosticsStatusJsonMatchesCliContract() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--diagnostics", "--json")).isZero();

        assertThat(console.out().trim()).isEqualTo(defaultDiagnosticsStatusJson());
        assertThat(console.err()).isEmpty();
    }

    @Test
    void hybridProfileStatusJsonMatchesCliContract() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--profile", "hybrid", "--json")).isZero();

        assertThat(console.out().trim()).isEqualTo(hybridProfileStatusJson());
        assertThat(console.err()).isEmpty();
    }

    private CommandLine command(TestConsole console) {
        SkillsCommandHandler handler = SkillsCommandHandler.inMemory(console.outStream(), console.errStream());
        return new CommandLine(new SkillsCommand(handler));
    }

    private static String defaultStatusJson() {
        return """
                {"source":"default","profile":"","runtime":false,"preflightAvailable":false,"diagnosticsAvailable":false,"strategy":"ephemeral","fullyDurable":false,"hasEphemeralRole":true,"hasDurableFallback":false,"hasExternalProvider":false,"hasCustomProvider":false,"hasCompositeProvider":false,"hasMirroredProvider":false,"roleCount":4,"durableRoleCount":0,"ephemeralRoleCount":3,"disabledRoleCount":1,"customRoleCount":0,"warningCount":2,"warnings":["Disabled skill persistence roles: event-history","Ephemeral skill persistence roles: definition, lifecycle-state, artifact"],"roles":[{"role":"definition","path":"definition","provider":"registry","persistenceClass":"runtime-registry","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"lifecycle-state","path":"lifecycle-state","provider":"memory","persistenceClass":"memory","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"event-history","path":"event-history","provider":"none","persistenceClass":"disabled","strategy":"disabled","disabled":true,"ephemeral":false,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":[],"children":[]},{"role":"artifact","path":"artifact","provider":"memory","persistenceClass":"memory","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]}]}
                """.strip();
    }

    private static String defaultPreflightStatusJson() {
        return """
                {"source":"default","profile":"","runtime":false,"preflightAvailable":true,"preflight":{"ready":true,"deployable":true,"errorCount":0,"message":"","errors":[],"configuration":{"valid":true,"errorCount":0,"message":"","errors":[]},"targetStores":{"valid":true,"errorCount":0,"message":"","errors":[]},"sourceStores":{"valid":true,"errorCount":0,"message":"","errors":[]},"capabilities":{"valid":true,"errorCount":0,"message":"","errors":[]}},"diagnosticsAvailable":false,"strategy":"ephemeral","fullyDurable":false,"hasEphemeralRole":true,"hasDurableFallback":false,"hasExternalProvider":false,"hasCustomProvider":false,"hasCompositeProvider":false,"hasMirroredProvider":false,"roleCount":4,"durableRoleCount":0,"ephemeralRoleCount":3,"disabledRoleCount":1,"customRoleCount":0,"warningCount":2,"warnings":["Disabled skill persistence roles: event-history","Ephemeral skill persistence roles: definition, lifecycle-state, artifact"],"roles":[{"role":"definition","path":"definition","provider":"registry","persistenceClass":"runtime-registry","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"lifecycle-state","path":"lifecycle-state","provider":"memory","persistenceClass":"memory","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"event-history","path":"event-history","provider":"none","persistenceClass":"disabled","strategy":"disabled","disabled":true,"ephemeral":false,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":[],"children":[]},{"role":"artifact","path":"artifact","provider":"memory","persistenceClass":"memory","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]}]}
                """.strip();
    }

    private static String defaultDiagnosticsStatusJson() {
        return """
                {"source":"default","profile":"","runtime":false,"preflightAvailable":false,"diagnosticsAvailable":true,"diagnostics":{"lifecycleReconcile":"inspect-only","createMissingStates":false,"removeOrphanedStates":false,"stores":[{"role":"definition","kind":"registry","target":"registry","initializeJdbcSchema":false,"maxEvents":0,"children":[]},{"role":"lifecycle-state","kind":"memory","target":"memory","initializeJdbcSchema":false,"maxEvents":0,"children":[]},{"role":"event-history","kind":"none","target":"none","initializeJdbcSchema":false,"maxEvents":0,"children":[]},{"role":"artifact","kind":"memory","target":"memory","initializeJdbcSchema":false,"maxEvents":0,"children":[]}]},"strategy":"ephemeral","fullyDurable":false,"hasEphemeralRole":true,"hasDurableFallback":false,"hasExternalProvider":false,"hasCustomProvider":false,"hasCompositeProvider":false,"hasMirroredProvider":false,"roleCount":4,"durableRoleCount":0,"ephemeralRoleCount":3,"disabledRoleCount":1,"customRoleCount":0,"warningCount":2,"warnings":["Disabled skill persistence roles: event-history","Ephemeral skill persistence roles: definition, lifecycle-state, artifact"],"roles":[{"role":"definition","path":"definition","provider":"registry","persistenceClass":"runtime-registry","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"lifecycle-state","path":"lifecycle-state","provider":"memory","persistenceClass":"memory","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"event-history","path":"event-history","provider":"none","persistenceClass":"disabled","strategy":"disabled","disabled":true,"ephemeral":false,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":[],"children":[]},{"role":"artifact","path":"artifact","provider":"memory","persistenceClass":"memory","strategy":"ephemeral","disabled":false,"ephemeral":true,"durable":false,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]}]}
                """.strip();
    }

    private static String hybridProfileStatusJson() {
        return """
                {"source":"profile","profile":"hybrid-object-file","runtime":false,"preflightAvailable":false,"diagnosticsAvailable":false,"strategy":"hybrid-fallback","fullyDurable":true,"hasEphemeralRole":false,"hasDurableFallback":true,"hasExternalProvider":true,"hasCustomProvider":false,"hasCompositeProvider":true,"hasMirroredProvider":false,"roleCount":4,"durableRoleCount":4,"ephemeralRoleCount":0,"disabledRoleCount":0,"customRoleCount":0,"warningCount":0,"warnings":[],"roles":[{"role":"definition","path":"definition","provider":"hybrid","persistenceClass":"composed","strategy":"hybrid-fallback","disabled":false,"ephemeral":false,"durable":true,"durableFallback":true,"external":true,"custom":false,"composite":true,"mirrored":false,"capabilities":["read","write","delete","list","primary-fallback"],"children":[{"role":"definition","path":"definition.primary","provider":"object-storage","persistenceClass":"object-storage","strategy":"object-storage","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":true,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"definition","path":"definition.fallback","provider":"filesystem","persistenceClass":"filesystem","strategy":"local-filesystem","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]}]},{"role":"lifecycle-state","path":"lifecycle-state","provider":"hybrid","persistenceClass":"composed","strategy":"hybrid-fallback","disabled":false,"ephemeral":false,"durable":true,"durableFallback":true,"external":true,"custom":false,"composite":true,"mirrored":false,"capabilities":["read","write","delete","list","primary-fallback"],"children":[{"role":"lifecycle-state","path":"lifecycle-state.primary","provider":"object-storage","persistenceClass":"object-storage","strategy":"object-storage","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":true,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"lifecycle-state","path":"lifecycle-state.fallback","provider":"filesystem","persistenceClass":"filesystem","strategy":"local-filesystem","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]}]},{"role":"event-history","path":"event-history","provider":"hybrid","persistenceClass":"composed","strategy":"hybrid-fallback","disabled":false,"ephemeral":false,"durable":true,"durableFallback":true,"external":true,"custom":false,"composite":true,"mirrored":false,"capabilities":["write","query-events","prune-events","composite"],"children":[{"role":"event-history","path":"event-history.primary","provider":"object-storage","persistenceClass":"object-storage","strategy":"object-storage","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":true,"custom":false,"composite":false,"mirrored":false,"capabilities":["write","query-events","prune-events"],"children":[]},{"role":"event-history","path":"event-history.fallback","provider":"filesystem","persistenceClass":"filesystem","strategy":"local-filesystem","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["write","query-events","prune-events"],"children":[]}]},{"role":"artifact","path":"artifact","provider":"hybrid","persistenceClass":"composed","strategy":"hybrid-fallback","disabled":false,"ephemeral":false,"durable":true,"durableFallback":true,"external":true,"custom":false,"composite":true,"mirrored":false,"capabilities":["read","write","delete","list","primary-fallback"],"children":[{"role":"artifact","path":"artifact.primary","provider":"object-storage","persistenceClass":"object-storage","strategy":"object-storage","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":true,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]},{"role":"artifact","path":"artifact.fallback","provider":"filesystem","persistenceClass":"filesystem","strategy":"local-filesystem","disabled":false,"ephemeral":false,"durable":true,"durableFallback":false,"external":false,"custom":false,"composite":false,"mirrored":false,"capabilities":["read","write","delete","list"],"children":[]}]}]}
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
