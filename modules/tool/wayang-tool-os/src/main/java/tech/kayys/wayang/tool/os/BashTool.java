package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public final class BashTool implements Tool {

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;
    private static final int MAX_OUTPUT_CHARS = 30_000;

    @Override public String id() { return "bash"; }
    @Override public String name() { return "bash"; }

    @Override public String description() {
        return "Execute a shell command and return its combined stdout/stderr output. " +
               "Runs via `sh -c`. Has a timeout (default 120s, configurable). Use for builds, " +
               "tests, git commands, running scripts, etc.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "command", Schema.string("The shell command to run."),
                "timeout_seconds", Schema.integer("Max seconds to wait before killing the command (default 120)."),
                "working_dir", Schema.string("Directory to run the command in (optional).")
        ), "command");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String command = (String) params.get("command");
        int timeout = params.containsKey("timeout_seconds") ? ((Number) params.get("timeout_seconds")).intValue() : DEFAULT_TIMEOUT_SECONDS;
        String workingDir = params.containsKey("working_dir") ? (String) params.get("working_dir") : null;

        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.redirectErrorStream(true);
        Path defaultDir = context.workingDirectory() != null ? context.workingDirectory() : Paths.get(System.getProperty("user.dir"));
        if (workingDir != null) {
            pb.directory(defaultDir.resolve(workingDir).toFile());
        } else {
            pb.directory(defaultDir.toFile());
        }

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (var in = process.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    synchronized (output) {
                        if (output.length() < MAX_OUTPUT_CHARS) {
                            output.append(new String(buf, 0, n));
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
        reader.setDaemon(true);
        reader.start();

        boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            reader.join(1000);
            return ToolResult.error("Command timed out after " + timeout + "s. Partial output:\n" + trim(output.toString()));
        }
        reader.join(2000);

        int exitCode = process.exitValue();
        String text = trim(output.toString());
        String result = "(exit code " + exitCode + ")\n" + text;
        return exitCode == 0 ? ToolResult.success(result) : ToolResult.error(result);
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }

    private String trim(String s) {
        if (s.length() <= MAX_OUTPUT_CHARS) return s;
        return s.substring(0, MAX_OUTPUT_CHARS) + "\n... (output truncated)";
    }
}
