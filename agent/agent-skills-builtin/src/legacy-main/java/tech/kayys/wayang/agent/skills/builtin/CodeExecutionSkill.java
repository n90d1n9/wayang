package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Executes Python or JavaScript snippets in a sandboxed child process and
 * returns stdout as the observation.
 *
 * <h2>Security</h2>
 * <p>
 * The skill writes code to a temp file and spawns an external interpreter
 * process. The process is subject to a wall-clock timeout (default 10 s) after
 * which it is killed. Network access and file system writes are <em>not</em>
 * restricted by this implementation — operators should layer OS-level sandbox
 * policies (seccomp, AppArmor, Docker) around the Gollek JVM process for
 * production workloads.
 * </p>
 *
 * <h2>Inputs</h2>
 * <table border="1">
 * <tr>
 * <th>Key</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>code</td>
 * <td>yes</td>
 * <td>Source code to execute</td>
 * </tr>
 * <tr>
 * <td>language</td>
 * <td>no</td>
 * <td>python (default) | javascript</td>
 * </tr>
 * <tr>
 * <td>timeout_seconds</td>
 * <td>no</td>
 * <td>Execution timeout (default 10)</td>
 * </tr>
 * </table>
 *
 * <h2>Outputs</h2>
 * <ul>
 * <li>{@code stdout} – captured standard output</li>
 * <li>{@code stderr} – captured standard error (empty on success)</li>
 * <li>{@code exit_code} – process exit code</li>
 * <li>{@code language} – interpreter used</li>
 * </ul>
 */
@ApplicationScoped
@SkillDescriptor(id = "code_execution", name = "Code Execution", description = "Executes Python or JavaScript code snippets in a sandboxed process and returns the output.", version = "1.0.0", category = SkillCategory.EXECUTION, inputs = {
        @SkillDescriptor.Input(name = "code", description = "The source code to execute"),
        @SkillDescriptor.Input(name = "language", required = false, description = "python (default) | javascript"),
        @SkillDescriptor.Input(name = "timeout_seconds", type = "integer", required = false, description = "Execution timeout in seconds")
}, outputs = {
        @SkillDescriptor.Output(name = "stdout", description = "Captured standard output"),
        @SkillDescriptor.Output(name = "stderr", description = "Captured standard error"),
        @SkillDescriptor.Output(name = "exit_code", type = "integer", description = "Process exit code")
}, triggers = { "execute", "run code", "python", "javascript", "compute", "calculate" }, priority = 60)
public class CodeExecutionSkill implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(CodeExecutionSkill.class);

    @ConfigProperty(name = "gollek.agent.skills.code-execution.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "gollek.agent.skills.code-execution.python-executable", defaultValue = "python3")
    String pythonExec;

    @ConfigProperty(name = "gollek.agent.skills.code-execution.node-executable", defaultValue = "node")
    String nodeExec;

    @ConfigProperty(name = "gollek.agent.skills.code-execution.default-timeout-seconds", defaultValue = "10")
    int defaultTimeout;

    // ── AgentSkill lifecycle ──────────────────────────────────────────────────

    @Override
    public String id() {
        return "code_execution";
    }

    @Override
    public String name() {
        return "Code Execution";
    }

    @Override
    public String description() {
        return "Executes Python or JavaScript code and returns stdout.";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public SkillCategory category() {
        return SkillCategory.EXECUTION;
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return enabled && inputs.containsKey("code");
    }

    // ── Execution ─────────────────────────────────────────────────────────────

    @Override
    public Uni<SkillResult> execute(SkillContext ctx) {
        String code = ctx.requireInput("code", String.class);
        String language = ctx.getStringInput("language", "python");
        int timeoutSec = ctx.getIntInput("timeout_seconds", defaultTimeout);

        return Uni.createFrom().item(() -> runCode(ctx, code, language, timeoutSec))
                .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private SkillResult runCode(SkillContext ctx, String code, String language, int timeoutSec) {
        Instant start = Instant.now();
        Path tempFile = null;
        try {
            // Write code to temp file
            String ext = "javascript".equalsIgnoreCase(language) ? ".js" : ".py";
            tempFile = Files.createTempFile("gollek-agent-" + UUID.randomUUID(), ext);
            Files.writeString(tempFile, code, StandardCharsets.UTF_8);

            String interpreter = "javascript".equalsIgnoreCase(language) ? nodeExec : pythonExec;
            ProcessBuilder pb = new ProcessBuilder(interpreter, tempFile.toString());
            pb.redirectErrorStream(false);
            Process proc = pb.start();

            // Capture output in parallel threads to avoid blocking
            StringWriter stdout = new StringWriter();
            StringWriter stderr = new StringWriter();

            Thread outReader = new Thread(() -> captureStream(proc.getInputStream(), stdout));
            Thread errReader = new Thread(() -> captureStream(proc.getErrorStream(), stderr));
            outReader.start();
            errReader.start();

            boolean finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS);

            if (!finished) {
                proc.destroyForcibly();
                return SkillResult.builder()
                        .skillId(ctx.skillId())
                        .invocationId(ctx.invocationId())
                        .status(SkillResult.Status.FAILURE)
                        .observation("Execution timed out after " + timeoutSec + " seconds.")
                        .durationMs(Duration.between(start, Instant.now()).toMillis())
                        .build();
            }

            outReader.join(1000);
            errReader.join(1000);

            int exitCode = proc.exitValue();
            String out = stdout.toString();
            String err = stderr.toString();
            long durationMs = Duration.between(start, Instant.now()).toMillis();

            if (exitCode == 0) {
                return SkillResult.builder()
                        .skillId(ctx.skillId())
                        .invocationId(ctx.invocationId())
                        .status(SkillResult.Status.SUCCESS)
                        .observation(out.isBlank() ? "(no output)" : out)
                        .output("stdout", out)
                        .output("stderr", err)
                        .output("exit_code", exitCode)
                        .output("language", language)
                        .durationMs(durationMs)
                        .build();
            } else {
                return SkillResult.builder()
                        .skillId(ctx.skillId())
                        .invocationId(ctx.invocationId())
                        .status(SkillResult.Status.FAILURE)
                        .observation("Process exited with code " + exitCode + ": " + err)
                        .durationMs(durationMs)
                        .build();
            }

        } catch (Exception e) {
            LOG.errorf(e, "Code execution failed for skill %s", ctx.skillId());
            return SkillResult.builder()
                    .skillId(ctx.skillId())
                    .invocationId(ctx.invocationId())
                    .status(SkillResult.Status.ERROR)
                    .observation("Execution error: " + e.getMessage())
                    .error(e)
                    .durationMs(Duration.between(start, Instant.now()).toMillis())
                    .build();
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void captureStream(InputStream in, StringWriter out) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.write(line);
                out.write('\n');
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean isHealthy() {
        if (!enabled)
            return false;
        try {
            Process p = new ProcessBuilder(pythonExec, "--version").start();
            return p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            LOG.debugf("Python health check failed: %s", e.getMessage());
            return false;
        }
    }
}
