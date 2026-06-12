package tech.kayys.wayang.agent.core.skills.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Executes filesystem-backed skills as local processes.
 */
final class FilesystemSkillRuntime implements SkillRuntime {

    private final SkillExecutableResolver executableResolver;
    private final int executionTimeoutSeconds;

    FilesystemSkillRuntime(Path skillsDirectory, int executionTimeoutSeconds) {
        this(new SkillExecutableResolver(skillsDirectory), executionTimeoutSeconds);
    }

    FilesystemSkillRuntime(SkillExecutableResolver executableResolver, int executionTimeoutSeconds) {
        this.executableResolver = Objects.requireNonNull(executableResolver, "executableResolver");
        this.executionTimeoutSeconds = executionTimeoutSeconds;
    }

    @Override
    public SkillProcessRunner.ProcessResult execute(String skillName, Map<String, Object> parameters)
            throws IOException, InterruptedException, TimeoutException {
        SkillExecutableResolver.ResolvedExecutable executable = executableResolver.resolve(skillName);
        List<String> command = SkillProcessInput.of(executable.executable(), parameters).command();

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(executable.skillPath().toFile());
        return SkillProcessRunner.run(processBuilder, executionTimeoutSeconds);
    }
}
