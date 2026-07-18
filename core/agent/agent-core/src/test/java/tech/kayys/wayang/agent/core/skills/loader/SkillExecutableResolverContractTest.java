package tech.kayys.wayang.agent.core.skills.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillExecutableResolverContractTest {

    @TempDir
    Path skillsDir;

    @Test
    void resolvesFirstExecutableCandidateByPriority() throws Exception {
        Path skillPath = createSkillDirectory("priority");
        Path runSh = Files.writeString(skillPath.resolve("run.sh"), "#!/usr/bin/env bash\n");
        Path mainPy = Files.writeString(skillPath.resolve("main.py"), "print('hello')\n");
        Path indexJs = Files.writeString(skillPath.resolve("index.js"), "console.log('hello')\n");
        assertTrue(mainPy.toFile().setExecutable(true));
        assertTrue(indexJs.toFile().setExecutable(true));

        SkillExecutableResolver resolver = new SkillExecutableResolver(skillsDir);

        SkillExecutableResolver.ResolvedExecutable resolved = resolver.resolve("priority");
        assertEquals(mainPy, resolved.executable());

        assertTrue(runSh.toFile().setExecutable(true));
        resolved = resolver.resolve("priority");
        assertEquals(runSh, resolved.executable());
    }

    @Test
    void rejectsSkillNamesThatEscapeTheSkillsDirectory() {
        SkillExecutableResolver resolver = new SkillExecutableResolver(skillsDir);

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("../outside"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("/tmp/outside"));
    }

    @Test
    void reportsMissingManifestAsLayoutFailure() throws Exception {
        Files.createDirectories(skillsDir.resolve("empty"));
        SkillExecutableResolver resolver = new SkillExecutableResolver(skillsDir);

        SkillExecutableResolver.SkillLayoutException error =
                assertThrows(SkillExecutableResolver.SkillLayoutException.class, () -> resolver.resolve("empty"));

        assertEquals(
                SkillFailureType.SKILL_LAYOUT.code(),
                error.metadata().get(SkillExecutionOutcomes.KEY_FAILURE_TYPE));
        assertEquals("missing_manifest", error.metadata().get(SkillExecutionMetadata.KEY_LAYOUT_ERROR));
    }

    private Path createSkillDirectory(String name) throws Exception {
        Path skillPath = skillsDir.resolve(name);
        Files.createDirectories(skillPath);
        Files.writeString(skillPath.resolve("SKILL.md"), """
                ---
                name: %s
                description: Resolver contract fixture
                ---
                """.formatted(name));
        return skillPath;
    }
}
