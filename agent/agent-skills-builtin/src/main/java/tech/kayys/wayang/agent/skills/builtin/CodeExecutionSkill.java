package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@SkillDescriptor(id = "code_execution", name = "Code Execution", description = "Executes Python or JavaScript code snippets in a child process and returns the output.", version = "1.0.0", category = SkillCategory.EXECUTION, inputs = {
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
    boolean enabled = true;

    @ConfigProperty(name = "gollek.agent.skills.code-execution.python-executable", defaultValue = "python3")
    String pythonExec = "python3";

    @ConfigProperty(name = "gollek.agent.skills.code-execution.node-executable", defaultValue = "node")
    String nodeExec = "node";

    @ConfigProperty(name = "gollek.agent.skills.code-execution.default-timeout-seconds", defaultValue = "10")
    int defaultTimeout = 10;

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
    public String category() {
        return SkillCategory.EXECUTION.name();
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return enabled && inputs != null && inputs.containsKey("code");
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        Map<String, Object> inputs = context == null ? Map.of() : context;
        String code = BuiltinSkillSupport.stringInput(inputs, "code");
        if (code == null || code.isBlank()) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Input 'code' is required"));
        }
        String language = BuiltinSkillSupport.stringInput(inputs, "language", "python");
        int timeoutSec = BuiltinSkillSupport.intInput(inputs, "timeout_seconds", defaultTimeout);

        return Uni.createFrom().item(() -> runCode(code, language, timeoutSec))
                .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    private Map<String, Object> runCode(String code, String language, int timeoutSec) {
        Instant start = Instant.now();
        Path tempFile = null;
        try {
            String ext = "javascript".equalsIgnoreCase(language) ? ".js" : ".py";
            tempFile = Files.createTempFile("wayang-agent-" + UUID.randomUUID(), ext);
            Files.writeString(tempFile, code, StandardCharsets.UTF_8);

            String interpreter = "javascript".equalsIgnoreCase(language) ? nodeExec : pythonExec;
            Process proc = new ProcessBuilder(interpreter, tempFile.toString()).start();

            StringWriter stdout = new StringWriter();
            StringWriter stderr = new StringWriter();
            Thread outReader = new Thread(() -> captureStream(proc.getInputStream(), stdout));
            Thread errReader = new Thread(() -> captureStream(proc.getErrorStream(), stderr));
            outReader.start();
            errReader.start();

            boolean finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                return BuiltinSkillSupport.failure("Execution timed out after " + timeoutSec + " seconds.");
            }

            outReader.join(1000);
            errReader.join(1000);

            int exitCode = proc.exitValue();
            String out = stdout.toString();
            String err = stderr.toString();
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            Map<String, Object> outputs = new LinkedHashMap<>();
            outputs.put("stdout", out);
            outputs.put("stderr", err);
            outputs.put("exit_code", exitCode);
            outputs.put("language", language);
            outputs.put("durationMs", durationMs);

            if (exitCode == 0) {
                return BuiltinSkillSupport.success(out.isBlank() ? "(no output)" : out, outputs);
            }
            Map<String, Object> failure = BuiltinSkillSupport.failure("Process exited with code " + exitCode + ": " + err);
            failure.putAll(outputs);
            return failure;
        } catch (Exception e) {
            LOG.errorf(e, "Code execution failed");
            return BuiltinSkillSupport.error(e);
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
        return enabled;
    }
}
