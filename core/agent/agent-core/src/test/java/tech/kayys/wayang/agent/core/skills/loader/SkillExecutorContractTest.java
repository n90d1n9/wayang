package tech.kayys.wayang.agent.core.skills.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillExecutorContractTest {

    @TempDir
    Path skillsDir;

    @Test
    void projectsParametersToDeterministicCliArguments() throws Exception {
        createSkill("echo-args", """
                #!/usr/bin/env bash
                printf '%s\\n' "$@"
                """);
        SkillExecutor executor = new SkillExecutor(skillsDir, 5);
        executor.loadAllSkills();

        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("x", 1);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("zeta", 2);
        parameters.put("alpha", nested);
        parameters.put("maybe", null);
        parameters.put("enabled", true);

        SkillExecutionOutcome result = executor.executeSkill("echo-args", parameters);

        assertTrue(result.success(), result.error());
        assertEquals(List.of(
                "--alpha",
                "{\"x\":1}",
                "--enabled",
                "true",
                "--maybe",
                "null",
                "--zeta",
                "2"), result.output().lines().toList());
    }

    @Test
    void rejectsInvalidParameterNamesBeforeLaunchingProcess() throws Exception {
        Path skillPath = createSkill("guarded", """
                #!/usr/bin/env bash
                touch ran.txt
                printf '%s\\n' "$@"
                """);
        SkillExecutor executor = new SkillExecutor(skillsDir, 5);
        executor.loadAllSkills();

        SkillExecutionOutcome result = executor.executeSkill(
                "guarded",
                Map.of("bad key", "value"));

        assertFalse(result.success());
        assertTrue(result.error().contains("Invalid skill parameter name"));
        assertEquals(SkillFailureType.INVALID_INPUT, SkillExecutionOutcomes.failureType(result).orElseThrow());
        assertFalse(Files.exists(skillPath.resolve("ran.txt")));
    }

    @Test
    void timesOutProcessBeforeWaitingForOutputToClose() throws Exception {
        Path skillPath = createSkill("slow", """
                #!/usr/bin/env bash
                printf 'before sleep\\n'
                sleep 4
                touch after-timeout.txt
                printf 'after sleep\\n'
                """);
        SkillExecutor executor = new SkillExecutor(skillsDir, 1);
        executor.loadAllSkills();

        long started = System.nanoTime();
        SkillExecutionOutcome result = executor.executeSkill("slow", Map.of());
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        assertFalse(result.success());
        assertTrue(result.error().contains("timeout after 1 seconds"));
        assertEquals(SkillFailureType.TIMEOUT, SkillExecutionOutcomes.failureType(result).orElseThrow());
        assertEquals(1, SkillExecutionMetadata.timeoutSeconds(result).orElseThrow());
        assertTrue(elapsedMs < 3_000, "timeout should not wait for script output to close; elapsed=" + elapsedMs);
        assertFalse(Files.exists(skillPath.resolve("after-timeout.txt")));
    }

    @Test
    void reportsNonZeroExitWithOutputContextAndMetadata() throws Exception {
        createSkill("failing", """
                #!/usr/bin/env bash
                printf 'failure context\\n'
                exit 7
                """);
        SkillExecutor executor = new SkillExecutor(skillsDir, 5);
        executor.loadAllSkills();

        SkillExecutionOutcome result = executor.executeSkill("failing", Map.of());

        assertFalse(result.success());
        assertEquals("failure context", result.output());
        assertTrue(result.error().contains("exit code 7"));
        assertTrue(result.error().contains("failure context"));
        assertEquals(SkillFailureType.PROCESS_EXIT, SkillExecutionOutcomes.failureType(result).orElseThrow());
        assertEquals(7, SkillExecutionMetadata.exitCode(result).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> result.metadata().put("later", true));
    }

    @Test
    void reportsSkillLayoutFailureWhenExecutableIsMissing() throws Exception {
        createSkillManifestOnly("empty");
        SkillExecutor executor = new SkillExecutor(skillsDir, 5);
        executor.loadAllSkills();

        SkillExecutionOutcome result = executor.executeSkill("empty", Map.of());

        assertFalse(result.success());
        assertTrue(result.error().contains("No executable found"));
        assertEquals(SkillFailureType.SKILL_LAYOUT, SkillExecutionOutcomes.failureType(result).orElseThrow());
        assertEquals("missing_executable", SkillExecutionMetadata.layoutError(result).orElseThrow());
    }

    private Path createSkill(String name, String script) throws Exception {
        Path skillPath = createSkillManifestOnly(name);
        Path runScript = skillPath.resolve("run.sh");
        Files.writeString(runScript, script);
        assertTrue(runScript.toFile().setExecutable(true));
        return skillPath;
    }

    private Path createSkillManifestOnly(String name) throws Exception {
        Path skillPath = skillsDir.resolve(name);
        Files.createDirectories(skillPath);
        Files.writeString(skillPath.resolve("SKILL.md"), """
                ---
                name: %s
                description: Use this skill for testing process argument projection
                metadata:
                  version: "1.0.0"
                ---

                ## Usage
                Use this skill for test execution.
                """.formatted(name));
        return skillPath;
    }
}
