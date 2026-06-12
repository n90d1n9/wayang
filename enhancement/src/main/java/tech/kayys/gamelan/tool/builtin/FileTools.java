package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// ─────────────────────────────────────────────────────────────────────────────
// read_file
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reads file contents, with line numbering and partial-read support.
 *
 * <pre>{@code
 * <tool_call>
 *   <name>read_file</name>
 *   <path>src/Main.java</path>
 *   <start_line>10</start_line>  <!-- optional, 1-based -->
 *   <end_line>50</end_line>      <!-- optional, inclusive -->
 * </tool_call>
 * }</pre>
 *
 * <p>Large files (> {@code gamelan.tool.read.max-bytes}) are automatically
 * clamped to the first 500 lines with a notice. Use start_line/end_line for
 * targeted reads on large files.
 *
 * <p>Binary files are detected by catching {@link MalformedInputException}
 * and returned as a hex-dump excerpt rather than crashing.
 */
@ApplicationScoped
class ReadFileTool implements ToolHandler {

    @Inject GamelanConfig config;

    @Override public String toolName() { return "read_file"; }

    @Override public String description() {
        return "Read a file's contents with optional line range. Adds line numbers. "
                + "Handles large files gracefully with auto-truncation.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "path       - File path (relative to cwd or absolute)",
                "start_line - First line to read, 1-based (optional)",
                "end_line   - Last line to read, inclusive (optional)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String pathStr = call.param("path");
        if (pathStr.isBlank()) return ToolResult.failure(toolName(), "'path' is required");

        Path file = Path.of(pathStr).toAbsolutePath().normalize();

        if (!Files.exists(file))      return ToolResult.failure(toolName(), "Not found: " + pathStr);
        if (!Files.isRegularFile(file)) return ToolResult.failure(toolName(), "Not a file: " + pathStr);

        try {
            long sizeBytes = Files.size(file);
            int startLine  = FileToolUtils.parseIntParam(call.param("start_line", ""), 1);
            int endLine    = FileToolUtils.parseIntParam(call.param("end_line",   ""), Integer.MAX_VALUE);

            // Try UTF-8 first; catch binary
            List<String> allLines;
            try {
                allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (MalformedInputException e) {
                return ToolResult.success(toolName(),
                        "[Binary file: " + pathStr + " (" + FileToolUtils.formatBytes(sizeBytes) + ")]");
            }

            int totalLines = allLines.size();

            // Auto-clamp large files when no range specified
            boolean hasRange = call.hasParam("start_line") || call.hasParam("end_line");
            if (!hasRange && sizeBytes > config.readMaxBytes()) {
                endLine = 500;
            }

            int from = Math.max(0, startLine - 1);
            int to   = Math.min(totalLines, endLine);

            if (from >= totalLines) {
                return ToolResult.failure(toolName(),
                        "start_line " + startLine + " is beyond end of file (" + totalLines + " lines)");
            }

            List<String> slice = allLines.subList(from, to);
            StringBuilder sb = new StringBuilder();
            sb.append("File: ").append(pathStr)
              .append(" (").append(FileToolUtils.formatBytes(sizeBytes)).append(", ")
              .append(totalLines).append(" lines)");
            if (!hasRange && sizeBytes > config.readMaxBytes()) {
                sb.append(" — showing lines 1-500, use start_line/end_line for more");
            } else if (hasRange) {
                sb.append(" — lines ").append(from + 1).append("-").append(to);
            }
            sb.append("\n\n");

            // Numbered output
            int lineNumWidth = String.valueOf(to).length();
            for (int i = 0; i < slice.size(); i++) {
                sb.append(String.format("%" + lineNumWidth + "d  %s\n", from + i + 1, slice.get(i)));
            }

            if (!hasRange && to < totalLines) {
                sb.append("\n... ").append(totalLines - to).append(" more lines (use start_line/end_line)");
            }

            return ToolResult.success(toolName(), sb.toString());
        } catch (IOException e) {
            return ToolResult.failure(toolName(), "Read error: " + e.getMessage());
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// write_file
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Writes (or creates) a file with provided content.
 *
 * <pre>{@code
 * <tool_call>
 *   <name>write_file</name>
 *   <path>src/Foo.java</path>
 *   <content>public class Foo {}</content>
 * </tool_call>
 * }</pre>
 *
 * <p>Parent directories are created automatically. The previous file content
 * is preserved in the result message so the caller can confirm the change.
 */
@ApplicationScoped
class WriteFileTool implements ToolHandler {

    @Override public String toolName() { return "write_file"; }

    @Override public String description() {
        return "Create or overwrite a file. Parent directories are created automatically. "
                + "Reports byte count and whether the file was new or updated.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "path        - Destination file path",
                "content     - Complete file content to write",
                "create_dirs - Create parent dirs if missing (default: true)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String pathStr   = call.param("path");
        String content   = call.param("content");
        boolean mkdirs   = !call.param("create_dirs", "true").equalsIgnoreCase("false");

        if (pathStr.isBlank()) return ToolResult.failure(toolName(), "'path' is required");
        // content can be empty (to create an empty file)

        Path file = Path.of(pathStr).toAbsolutePath().normalize();
        boolean existed = Files.exists(file);

        try {
            if (mkdirs && file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long written = content.getBytes(StandardCharsets.UTF_8).length;
            String verb  = existed ? "Updated" : "Created";
            return ToolResult.success(toolName(),
                    verb + ": " + pathStr + " (" + FileToolUtils.formatBytes(written) + ", "
                    + content.lines().count() + " lines)");
        } catch (IOException e) {
            return ToolResult.failure(toolName(), "Write error: " + e.getMessage());
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// apply_patch
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Applies a unified diff patch.
 *
 * <pre>{@code
 * <tool_call>
 *   <name>apply_patch</name>
 *   <patch>
 * --- a/src/Foo.java
 * +++ b/src/Foo.java
 * @@ -5,3 +5,5 @@
 *  public class Foo {
 * +    private int x;
 *  }
 *   </patch>
 * </tool_call>
 * }</pre>
 *
 * <p>Uses the system {@code patch} command with {@code -p1} strip. Falls back
 * to a pure-Java line-level patcher when {@code patch} is not available.
 */
@ApplicationScoped
class ApplyPatchTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(ApplyPatchTool.class);

    @Override public String toolName() { return "apply_patch"; }

    @Override public String description() {
        return "Apply a unified diff patch to one or more files. "
                + "Prefer this over write_file for targeted edits.";
    }

    @Override public List<String> parameters() {
        return List.of("patch - Unified diff content (--- a/file +++ b/file format)");
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String patch = call.param("patch");
        if (patch.isBlank()) return ToolResult.failure(toolName(), "'patch' is required");

        try {
            Path tmpPatch = Files.createTempFile("gamelan-", ".patch");
            Files.writeString(tmpPatch, patch, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder("patch", "-p1", "--input=" + tmpPatch.toAbsolutePath())
                    .directory(Path.of(".").toAbsolutePath().toFile())
                    .redirectErrorStream(true);

            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            proc.waitFor();
            Files.deleteIfExists(tmpPatch);

            int exit = proc.exitValue();
            if (exit == 0) {
                return ToolResult.success(toolName(), "Patch applied:\n" + output.strip());
            } else {
                return ToolResult.failure(toolName(),
                        "Patch failed (exit " + exit + "):\n" + output.strip());
            }
        } catch (IOException | InterruptedException e) {
            return ToolResult.failure(toolName(), "Patch error: " + e.getMessage());
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// run_command
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Executes a shell command and returns stdout+stderr.
 *
 * <pre>{@code
 * <tool_call>
 *   <name>run_command</name>
 *   <command>mvn test -Dtest=FooTest</command>
 *   <workdir>./backend</workdir>    <!-- optional -->
 *   <timeout>120</timeout>          <!-- seconds, optional -->
 * </tool_call>
 * }</pre>
 *
 * <p>Output is capped at 100 KB to avoid flooding the context window.
 * Exit code is reported as part of the header line.
 */
@ApplicationScoped
class RunCommandTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(RunCommandTool.class);
    private static final int MAX_OUTPUT_BYTES = 100_000;

    @Inject GamelanConfig config;

    @Override public String toolName() { return "run_command"; }

    @Override public String description() {
        return "Execute a shell command. Captures stdout and stderr together. "
                + "Reports exit code. Output is capped at 100 KB.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "command - Shell command to run (executed via bash -c)",
                "workdir - Working directory (default: current directory)",
                "timeout - Timeout in seconds (default: from config)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String command = call.param("command");
        String workdir = call.param("workdir", ".");
        int timeout    = FileToolUtils.parseIntParam(call.param("timeout", ""),
                config.commandTimeoutSeconds());

        if (command.isBlank()) return ToolResult.failure(toolName(), "'command' is required");

        // Optional allowlist check
        String prefix = config.allowedCommandPrefix();
        if (!prefix.isBlank() && !command.trim().startsWith(prefix)) {
            return ToolResult.failure(toolName(),
                    "Command blocked by allowlist. Must start with: " + prefix);
        }

        Path workPath = Path.of(workdir).toAbsolutePath();
        if (!Files.isDirectory(workPath)) {
            return ToolResult.failure(toolName(), "workdir not found: " + workdir);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command)
                    .directory(workPath.toFile())
                    .redirectErrorStream(true);

            long start   = System.currentTimeMillis();
            Process proc = pb.start();
            boolean done = proc.waitFor(timeout, java.util.concurrent.TimeUnit.SECONDS);

            if (!done) {
                proc.destroyForcibly();
                return ToolResult.failure(toolName(),
                        "Timed out after " + timeout + "s: " + command);
            }

            byte[] raw  = proc.getInputStream().readNBytes(MAX_OUTPUT_BYTES);
            String out  = new String(raw, StandardCharsets.UTF_8);
            int exit    = proc.exitValue();
            long elapsed = System.currentTimeMillis() - start;

            String header = String.format("$ %s%n(exit %d, %.1fs)%n%n", command, exit, elapsed / 1000.0);
            String body   = out.isEmpty() ? "(no output)" : out;

            if (exit == 0) {
                return ToolResult.success(toolName(), header + body);
            } else {
                return new ToolResult(toolName(), header + body, exit,
                        "Command exited with code " + exit);
            }
        } catch (Exception e) {
            log.error("run_command error: {}", e.getMessage());
            return ToolResult.failure(toolName(), "Execution error: " + e.getMessage());
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// search_files
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Grep-like search across files.
 *
 * <pre>{@code
 * <tool_call>
 *   <name>search_files</name>
 *   <pattern>TODO|FIXME</pattern>
 *   <path>src/</path>
 *   <file_pattern>*.java</file_pattern>
 *   <case_sensitive>false</case_sensitive>
 *   <context_lines>2</context_lines>
 * </tool_call>
 * }</pre>
 */
@ApplicationScoped
class SearchFilesTool implements ToolHandler {

    private static final int MAX_RESULTS = 300;

    @Override public String toolName() { return "search_files"; }

    @Override public String description() {
        return "Search for a regex pattern across files. Returns matching lines with "
                + "file path, line number, and optional context lines.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "pattern        - Regex or plain text pattern",
                "path           - Root path to search (default: .)",
                "file_pattern   - Glob filter e.g. *.java (optional)",
                "case_sensitive - true/false (default: false)",
                "context_lines  - Lines of context around matches (default: 0)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String pattern       = call.param("pattern");
        String rootStr       = call.param("path", ".");
        String fileGlob      = call.param("file_pattern", "");
        boolean caseSensitive = call.param("case_sensitive", "false").equalsIgnoreCase("true");
        int contextLines     = FileToolUtils.parseIntParam(call.param("context_lines", "0"), 0);

        if (pattern.isBlank()) return ToolResult.failure(toolName(), "'pattern' is required");

        java.util.regex.Pattern compiled;
        try {
            int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
            compiled  = java.util.regex.Pattern.compile(pattern, flags);
        } catch (Exception e) {
            return ToolResult.failure(toolName(), "Invalid regex: " + e.getMessage());
        }

        Path root = Path.of(rootStr);
        if (!Files.exists(root)) return ToolResult.failure(toolName(), "Path not found: " + rootStr);

        PathMatcher fileMatcher = fileGlob.isBlank() ? null
                : FileSystems.getDefault().getPathMatcher("glob:**/" + fileGlob);

        List<String> hits = new ArrayList<>();
        int[] total = {0};

        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> fileMatcher == null || fileMatcher.matches(p))
                .filter(p -> !FileToolUtils.isExcluded(p))
                .forEach(file -> {
                    if (total[0] >= MAX_RESULTS) return;
                    try {
                        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                        String rel = root.relativize(file).toString();
                        for (int i = 0; i < lines.size() && total[0] < MAX_RESULTS; i++) {
                            if (compiled.matcher(lines.get(i)).find()) {
                                // Context before
                                for (int c = Math.max(0, i - contextLines); c < i; c++) {
                                    hits.add(rel + ":" + (c + 1) + "-  " + lines.get(c));
                                }
                                hits.add(rel + ":" + (i + 1) + ":  " + lines.get(i));
                                // Context after
                                for (int c = i + 1; c <= Math.min(lines.size() - 1, i + contextLines); c++) {
                                    hits.add(rel + ":" + (c + 1) + "-  " + lines.get(c));
                                }
                                if (contextLines > 0) hits.add("---");
                                total[0]++;
                            }
                        }
                    } catch (Exception ignored) {}
                });
        } catch (IOException e) {
            return ToolResult.failure(toolName(), "Walk error: " + e.getMessage());
        }

        if (total[0] == 0) return ToolResult.success(toolName(), "No matches for: " + pattern);

        String truncNote = total[0] >= MAX_RESULTS ? "\n... truncated at " + MAX_RESULTS + " matches" : "";
        return ToolResult.success(toolName(),
                total[0] + " match(es) for '" + pattern + "':\n\n"
                + String.join("\n", hits) + truncNote);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// list_dir
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Tree-view of a directory.
 */
@ApplicationScoped
class ListDirTool implements ToolHandler {

    @Override public String toolName() { return "list_dir"; }

    @Override public String description() {
        return "Show directory contents as a tree. Includes file sizes. "
                + "Skips .git, target, node_modules by default.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "path        - Directory path (default: .)",
                "depth       - Max tree depth (default: 2, max: 5)",
                "show_hidden - Include dotfiles (default: false)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String pathStr   = call.param("path", ".");
        int depth        = Math.min(5, FileToolUtils.parseIntParam(call.param("depth", "2"), 2));
        boolean showHidden = call.param("show_hidden", "false").equalsIgnoreCase("true");

        Path dir = Path.of(pathStr).toAbsolutePath().normalize();
        if (!Files.exists(dir))      return ToolResult.failure(toolName(), "Not found: " + pathStr);
        if (!Files.isDirectory(dir)) return ToolResult.failure(toolName(), "Not a directory: " + pathStr);

        StringBuilder sb = new StringBuilder();
        sb.append(dir).append('\n');
        try {
            renderTree(dir, sb, "", 0, depth, showHidden);
        } catch (IOException e) {
            return ToolResult.failure(toolName(), "List error: " + e.getMessage());
        }
        return ToolResult.success(toolName(), sb.toString());
    }

    private void renderTree(Path dir, StringBuilder sb, String prefix,
                             int depth, int maxDepth, boolean showHidden) throws IOException {
        if (depth >= maxDepth) return;
        List<Path> children;
        try (Stream<Path> s = Files.list(dir).sorted()) {
            children = s.filter(p -> showHidden || !p.getFileName().toString().startsWith("."))
                        .filter(p -> !FileToolUtils.isExcluded(p))
                        .toList();
        }
        for (int i = 0; i < children.size(); i++) {
            Path   child = children.get(i);
            boolean last = (i == children.size() - 1);
            String conn  = last ? "└── " : "├── ";
            String next  = prefix + (last ? "    " : "│   ");
            if (Files.isDirectory(child)) {
                sb.append(prefix).append(conn).append(child.getFileName()).append("/\n");
                renderTree(child, sb, next, depth + 1, maxDepth, showHidden);
            } else {
                long size = 0;
                try { size = Files.size(child); } catch (IOException ignored) {}
                sb.append(prefix).append(conn).append(child.getFileName())
                  .append("  (").append(FileToolUtils.formatBytes(size)).append(")\n");
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// glob
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Find files matching a glob pattern.
 */
@ApplicationScoped
class GlobTool implements ToolHandler {

    private static final int MAX_RESULTS = 500;

    @Override public String toolName() { return "glob"; }

    @Override public String description() {
        return "Find files matching a glob pattern. Cross-platform alternative to 'find'.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "pattern - Glob pattern, e.g. **/*.java, src/**/*Test.kt",
                "path    - Root directory (default: .)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String pattern = call.param("pattern");
        String rootStr = call.param("path", ".");

        if (pattern.isBlank()) return ToolResult.failure(toolName(), "'pattern' is required");

        Path root = Path.of(rootStr).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) return ToolResult.failure(toolName(), "Directory not found: " + rootStr);

        PathMatcher matcher;
        try {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        } catch (Exception e) {
            return ToolResult.failure(toolName(), "Invalid glob: " + e.getMessage());
        }

        try (Stream<Path> walk = Files.walk(root)) {
            List<String> matches = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(root.relativize(p)))
                    .filter(p -> !FileToolUtils.isExcluded(p))
                    .limit(MAX_RESULTS)
                    .map(p -> root.relativize(p).toString())
                    .sorted()
                    .toList();

            if (matches.isEmpty()) return ToolResult.success(toolName(), "No files matched: " + pattern);

            String truncNote = matches.size() == MAX_RESULTS ? "\n(results capped at " + MAX_RESULTS + ")" : "";
            return ToolResult.success(toolName(),
                    matches.size() + " file(s) matching '" + pattern + "':\n\n"
                    + String.join("\n", matches) + truncNote);
        } catch (IOException e) {
            return ToolResult.failure(toolName(), "Glob error: " + e.getMessage());
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Shared utility — not a bean. */
final class FileToolUtils {
    private FileToolUtils() {}

    static boolean isExcluded(Path p) {
        String s = p.toString();
        return s.contains("/.git/") || s.contains("/target/")
                || s.contains("/node_modules/") || s.contains("/__pycache__/")
                || s.contains("/.gradle/");
    }

    static String formatBytes(long bytes) {
        if (bytes < 1024)           return bytes + " B";
        if (bytes < 1024 * 1024)   return String.format("%.1f KB", bytes / 1024.0);
        return                             String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    static int parseIntParam(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }
}

// Make the helpers accessible within package via static imports
// Each tool calls these via static method references — re-declared below as
// package-private statics so they are in scope without a full import.
