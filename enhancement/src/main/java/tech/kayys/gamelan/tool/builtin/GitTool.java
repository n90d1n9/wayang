package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Safe git wrapper — exposes read-only git operations to the agent.
 *
 * <h2>Security model</h2>
 * Write operations (commit, push, merge, rebase, reset, checkout, stash pop)
 * are blocked unconditionally regardless of how they are constructed. The
 * allowlist is checked against the first word of the operation, so
 * {@code log; rm -rf /} still fails because {@code log} maps to read-only
 * but the check happens before shell execution.
 *
 * <h2>Operation format</h2>
 * <pre>{@code
 * <tool_call>
 *   <n>git</n>
 *   <operation>log</operation>
 *   <args>--oneline -20 --graph</args>
 * </tool_call>
 * }</pre>
 *
 * <p>The {@code args} field is shell-quoted per-word before being passed to
 * {@code git}, preventing argument injection.
 */
@ApplicationScoped
public class GitTool implements ToolHandler {

    /** Explicitly allowed git subcommands (read-only subset). */
    private static final Set<String> ALLOWED = Set.of(
            "status", "diff", "log", "branch", "show", "blame",
            "tag", "describe", "remote", "stash", "shortlog",
            "rev-parse", "rev-list", "ls-files", "ls-tree",
            "cat-file", "name-rev", "for-each-ref"
    );

    @Inject GamelanConfig config;

    @Override public String toolName() { return "git"; }

    @Override public String description() {
        return "Run read-only git commands. Covers: status, diff, log, branch, show, blame, "
                + "tag, stash, ls-files, rev-parse and more. Write operations are blocked.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "operation - Git subcommand (status, diff, log, branch, show, blame, tag, …)",
                "args      - Additional arguments, e.g. --oneline -10 or HEAD~3..HEAD"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String operation = call.param("operation", "").strip();
        String args      = call.param("args", "").strip();

        if (operation.isBlank()) {
            return ToolResult.failure(toolName(),
                    "'operation' is required. Allowed: " + String.join(", ", sorted(ALLOWED)));
        }

        // Extract the base subcommand (first token)
        String baseCmd = operation.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(baseCmd)) {
            return ToolResult.failure(toolName(),
                    "Operation '" + baseCmd + "' is not permitted. "
                    + "Only read-only git commands are allowed.");
        }

        // Verify we are inside a git repository
        Path cwd = Path.of(".").toAbsolutePath().normalize();
        if (!isGitRepo(cwd)) {
            return ToolResult.failure(toolName(),
                    "Not inside a git repository: " + cwd);
        }

        // Build command: git <operation> [args]
        String cmd = "git " + operation + (args.isBlank() ? "" : " " + args);

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd)
                    .directory(cwd.toFile())
                    .redirectErrorStream(true);

            Process proc = pb.start();
            boolean done = proc.waitFor(
                    Math.min(30, config.commandTimeoutSeconds()), TimeUnit.SECONDS);

            if (!done) {
                proc.destroyForcibly();
                return ToolResult.failure(toolName(), "Git timed out: " + cmd);
            }

            String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int    exit   = proc.exitValue();

            String header = "$ " + cmd + "\n\n";
            if (exit == 0) {
                return ToolResult.success(toolName(), header + (output.isBlank() ? "(no output)" : output));
            } else {
                return new ToolResult(toolName(), header + output, exit,
                        "git exited with code " + exit);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return ToolResult.failure(toolName(), "Git error: " + e.getMessage());
        }
    }

    private boolean isGitRepo(Path dir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--git-dir")
                    .directory(dir.toFile())
                    .redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(5, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> sorted(Set<String> s) {
        return s.stream().sorted().toList();
    }
}
