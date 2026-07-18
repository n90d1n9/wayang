package tech.kayys.wayang.agent.core.skills.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.validation.SkillValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillExecutorInjectionContractTest {

    @TempDir
    Path skillsDir;

    @Test
    void usesInjectedRuntimeAfterCatalogLookupAndValidation() throws Exception {
        FakeSkillsLoaderService loaderService = new FakeSkillsLoaderService(skillsDir, manifestWithRequiredText());
        SkillManifestCatalog catalog = new SkillManifestCatalog(skillsDir, loaderService);
        RecordingRuntime runtime = new RecordingRuntime();
        SkillExecutor executor = new SkillExecutor(catalog, runtime, new SkillValidator(), 5);
        executor.loadAllSkills();

        SkillExecutionOutcome valid = executor.executeSkill("injected", Map.of("text", "hello"));

        assertTrue(valid.success(), valid.error());
        assertEquals("runtime ok", valid.output());
        assertEquals(1, runtime.calls);
        assertEquals("injected", runtime.skillName);
        assertEquals(Map.of("text", "hello"), runtime.parameters);

        SkillExecutionOutcome invalid = executor.executeSkill("injected", Map.of());

        assertFalse(invalid.success());
        assertEquals(SkillFailureType.PARAMETER_VALIDATION, SkillExecutionOutcomes.failureType(invalid).orElseThrow());
        assertEquals(1, runtime.calls, "runtime should not run when validation fails");
    }

    private static SkillManifest manifestWithRequiredText() {
        return SkillManifest.builder()
                .name("injected")
                .description("Injected executor fixture")
                .metadata(Map.of(
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of("text", Map.of("type", "string")),
                                "required", List.of("text"))))
                .build();
    }

    private static final class RecordingRuntime implements SkillRuntime {
        private int calls;
        private String skillName;
        private Map<String, Object> parameters;

        @Override
        public SkillProcessRunner.ProcessResult execute(String skillName, Map<String, Object> parameters)
                throws IOException, InterruptedException, TimeoutException {
            this.calls++;
            this.skillName = skillName;
            this.parameters = Map.copyOf(parameters);
            return new SkillProcessRunner.ProcessResult("runtime ok", 0, false, 10);
        }
    }

    private static final class FakeSkillsLoaderService implements SkillsLoaderService {
        private final Path skillsBaseDir;
        private final List<SkillManifest> manifests;

        private FakeSkillsLoaderService(Path skillsBaseDir, SkillManifest... manifests) {
            this.skillsBaseDir = skillsBaseDir;
            this.manifests = List.of(manifests);
        }

        @Override
        public SkillLoaderResult installLocal(String localPath, String skillFilter) {
            throw new UnsupportedOperationException("not needed for injection contract");
        }

        @Override
        public SkillLoaderResult installGit(String repoUrl, String skillFilter) {
            throw new UnsupportedOperationException("not needed for injection contract");
        }

        @Override
        public SkillLoaderResult update(String repoName) {
            throw new UnsupportedOperationException("not needed for injection contract");
        }

        @Override
        public boolean remove(String repoName) {
            throw new UnsupportedOperationException("not needed for injection contract");
        }

        @Override
        public List<String> listInstalledRepos() {
            return List.of();
        }

        @Override
        public List<SkillManifest> loadSkillsFromDirectory(Path directory) {
            return new ArrayList<>(manifests);
        }

        @Override
        public Path getSkillsBaseDir() {
            return skillsBaseDir;
        }
    }
}
