package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Expands a full path glob pattern (e.g. {@code src/**&#47;*.java}) and returns the
 * sorted list of matching paths relative to the given directory.
 *
 * <p>Unlike {@link SearchFilesTool} which matches only the filename, this tool
 * matches the full path so recursive patterns work correctly.
 */
public final class GlobTool implements Tool {

    private static final int DEFAULT_MAX_RESULTS = 200;

    @Override public String id() { return "glob"; }
    @Override public String name() { return "glob"; }

    @Override public String description() {
        return "Expand a full-path glob pattern and return matching file paths. " +
               "Supports '**' for recursive matching (e.g. 'src/**/*.java'). " +
               "Returns a sorted, newline-separated list of paths relative to the search directory. " +
               "Use when you need to enumerate files matching a structural path pattern.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "pattern",     Schema.string("Glob pattern relative to directory (e.g. 'src/**/*.java', '**/*Test.kt')."),
                "directory",   Schema.string("Root directory to resolve the pattern against (default: current directory '.')."),
                "max_results", Schema.integer("Maximum results to return (default 200).")
        ), "pattern");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String pattern = (String) params.get("pattern");
        String dirStr  = params.containsKey("directory") ? (String) params.get("directory") : ".";
        int    maxRes  = params.containsKey("max_results") ? ((Number) params.get("max_results")).intValue() : DEFAULT_MAX_RESULTS;

        Path root = context.workingDirectory().resolve(dirStr).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return ToolResult.error("Not a directory: " + dirStr);
        }

        PathMatcher matcher = root.getFileSystem().getPathMatcher("glob:" + pattern);
        List<String> matches = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (matches.size() >= maxRes) return FileVisitResult.TERMINATE;
                Path rel = root.relativize(file);
                if (matcher.matches(rel)) {
                    matches.add(rel.toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                if (name.equals(".git") || name.equals("node_modules") || name.equals(".DS_Store")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        if (matches.isEmpty()) {
            return ToolResult.success("No files matched pattern '" + pattern + "' in " + dirStr);
        }
        Collections.sort(matches);
        String truncNote = matches.size() == maxRes ? "\n(results capped at " + maxRes + ")" : "";
        return ToolResult.success(String.join("\n", matches) + truncNote);
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
