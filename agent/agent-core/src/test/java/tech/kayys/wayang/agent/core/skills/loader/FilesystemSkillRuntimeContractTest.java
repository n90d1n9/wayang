package tech.kayys.wayang.agent.core.skills.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesystemSkillRuntimeContractTest {

    @TempDir
    Path skillsDir;

    @Test
    void runsResolvedSkillInsideSkillDirectoryWithProjectedArguments() throws Exception {
        Path skillPath = createSkill("runtime", """
                #!/usr/bin/env bash
                printf '%s\\n' "$PWD"
                printf '%s\\n' "$@"
                """);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("zeta", 2);
        parameters.put("alpha", true);

        SkillRuntime runtime = new FilesystemSkillRuntime(skillsDir, 5);

        SkillProcessRunner.ProcessResult result = runtime.execute("runtime", parameters);

        List<String> lines = result.output().lines().toList();
        assertEquals(skillPath.toRealPath().toString(), Path.of(lines.get(0)).toRealPath().toString());
        assertEquals(List.of("--alpha", "true", "--zeta", "2"), lines.subList(1, lines.size()));
        assertEquals(0, result.exitCode());
        assertEquals(false, result.metadata().get(SkillExecutionMetadata.KEY_OUTPUT_TRUNCATED));
    }

    private Path createSkill(String name, String script) throws Exception {
        Path skillPath = skillsDir.resolve(name);
        Files.createDirectories(skillPath);
        Files.writeString(skillPath.resolve("SKILL.md"), """
                ---
                name: %s
                description: Use this skill for runtime execution testing
                ---
                """.formatted(name));
        Path runScript = skillPath.resolve("run.sh");
        Files.writeString(runScript, script);
        assertTrue(runScript.toFile().setExecutable(true));
        return skillPath;
    }
}
