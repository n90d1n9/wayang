package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * Diff tool — show file differences without applying them.
 *
 * <h2>Why this matters</h2>
 * Code review agents need to see what changed between file versions or git
 * commits. The existing {@code apply_patch} tool only APPLIES diffs. This
 * tool READS them — essential for understanding changes before acting on them.
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li>{@code diff_files} — compare two files directly</li>
 *   <li>{@code git_diff} — diff against git HEAD or between commits</li>
 * </ul>
 *
 * <pre>{@code
 * <!-- Compare two files -->
 * <tool_call>
 *   <n>diff_files</n>
 *   <file_a>src/UserService.java</file_a>
 *   <file_b>src/UserService.java.bak</file_b>
 * </tool_call>
 *
 * <!-- Git diff of unstaged changes -->
 * <tool_call>
 *   <n>git_diff</n>
 *   <path>src/</path>
 * </tool_call>
 *
 * <!-- Git diff between commits -->
 * <tool_call>
 *   <n>git_diff</n>
 *   <from>HEAD~3</from>
 *   <to>HEAD</to>
 * </tool_call>
 * }</pre>
 */
@ApplicationScoped
public class DiffTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(DiffTool.class);
    private static final int MAX_DIFF_BYTES = 100_000;

    @Override public String toolName()  { return "diff_files"; }

    @Override
    public List<String> toolNames() { return List.of("diff_files", "git_diff"); }

    @Override
    public String description() {
        return "Show differences between files or git commits (read-only). "
                + "Use diff_files to compare two files, git_diff for git changes.";
    }

    @Override
    public List<String> parameters() {
        return List.of(
                "file_a  - First file path (for diff_files)",
                "file_b  - Second file path (for diff_files)",
                "path    - Path to diff against HEAD (for git_diff, default: .)",
                "from    - Start commit/ref (for git_diff between commits)",
                "to      - End commit/ref (for git_diff between commits, default: HEAD)",
                "context - Number of context lines (default: 3)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        return switch (call.name()) {
            case "diff_files" -> diffFiles(call);
            case "git_diff"   -> gitDiff(call);
            default -> ToolResult.failure(call.name(), "Unknown diff operation: " + call.name());
        };
    }

    private ToolResult diffFiles(ToolCall call) {
        String pathA = call.param("file_a").strip();
        String pathB = call.param("file_b").strip();
        int context  = FileToolUtils.parseIntParam(call.param("context", "3"), 3);

        if (pathA.isBlank()) return ToolResult.failure("diff_files", "'file_a' is required");
        if (pathB.isBlank()) return ToolResult.failure("diff_files", "'file_b' is required");

        if (!Files.exists(Path.of(pathA)))
            return ToolResult.failure("diff_files", "file_a not found: " + pathA);
        if (!Files.exists(Path.of(pathB)))
            return ToolResult.failure("diff_files", "file_b not found: " + pathB);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "diff", "-u", "--label", pathA, "--label", pathB,
                    "-U", String.valueOf(context),
                    Path.of(pathA).toAbsolutePath().toString(),
                    Path.of(pathB).toAbsolutePath().toString())
                    .redirectErrorStream(true);
            return runAndCapture("diff_files", pb, pathA + " → " + pathB);
        } catch (Exception e) {
            return ToolResult.failure("diff_files", "diff failed: " + e.getMessage());
        }
    }

    private ToolResult gitDiff(ToolCall call) {
        String path    = call.param("path", ".").strip();
        String from    = call.param("from", "").strip();
        String to      = call.param("to", "HEAD").strip();
        int    context = FileToolUtils.parseIntParam(call.param("context", "3"), 3);

        try {
            ProcessBuilder pb;
            if (!from.isBlank()) {
                // Diff between two commits
                pb = new ProcessBuilder("git", "diff",
                        "-U" + context, from, to, "--", path.equals(".") ? "" : path)
                        .directory(Path.of(".").toAbsolutePath().toFile())
                        .redirectErrorStream(true);
            } else {
                // Diff against HEAD (unstaged + staged)
                pb = new ProcessBuilder("git", "diff",
                        "-U" + context, "HEAD", "--", path)
                        .directory(Path.of(".").toAbsolutePath().toFile())
                        .redirectErrorStream(true);
            }
            return runAndCapture("git_diff", pb, path);
        } catch (Exception e) {
            return ToolResult.failure("git_diff", "git diff failed: " + e.getMessage());
        }
    }

    private ToolResult runAndCapture(String toolName, ProcessBuilder pb, String label) {
        try {
            Process proc = pb.start();
            byte[] raw   = proc.getInputStream().readNBytes(MAX_DIFF_BYTES);
            proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            int exit     = proc.exitValue();
            String output = new String(raw, StandardCharsets.UTF_8);

            // diff exits 0 = no differences, 1 = differences, 2 = error
            if (exit == 2) {
                return ToolResult.failure(toolName, "Command error:\n" + output);
            }
            if (output.isBlank()) {
                return ToolResult.success(toolName, "No differences found for: " + label);
            }
            String header = "Diff: " + label + "\n\n";
            if (raw.length >= MAX_DIFF_BYTES) {
                output += "\n\n… [truncated at " + MAX_DIFF_BYTES + " bytes]";
            }
            return ToolResult.success(toolName, header + output);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return ToolResult.failure(toolName, "Error: " + e.getMessage());
        }
    }
}
