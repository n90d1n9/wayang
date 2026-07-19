package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Recursively searches for files whose name matches a given glob pattern.
 * Analogous to {@code find <dir> -name "<pattern>"} but implemented in
 * pure Java so it works cross-platform without spawning a shell.
 */
public final class SearchFilesTool implements Tool {

    private static final int DEFAULT_MAX_RESULTS = 100;

    @Override public String id() { return "search_files"; }
    @Override public String name() { return "search_files"; }

    @Override public String description() {
        return "Recursively search for files whose name matches a pattern (e.g. '*.java', 'Config.kt'). " +
               "Returns a newline-separated list of matching relative paths. " +
               "Use this to locate source files before reading or editing them.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "pattern",     Schema.string("File name glob pattern (e.g. '*.java', 'pom.xml', '*Test*')."),
                "directory",   Schema.string("Directory to search in (default: current directory '.')."),
                "max_results", Schema.integer("Maximum number of results to return (default 100).")
        ), "pattern");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String pattern   = (String) params.get("pattern");
        String dirStr    = params.containsKey("directory") ? (String) params.get("directory") : ".";
        int    maxRes    = params.containsKey("max_results") ? ((Number) params.get("max_results")).intValue() : DEFAULT_MAX_RESULTS;

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
                if (matcher.matches(file.getFileName())) {
                    matches.add(root.relativize(file).toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE; // skip unreadable
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Skip common noise directories
                String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                if (name.equals(".git") || name.equals("target") || name.equals("node_modules")
                        || name.equals(".gradle") || name.equals("build") || name.equals(".DS_Store")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        if (matches.isEmpty()) {
            return ToolResult.success("No files found matching '" + pattern + "' in " + dirStr);
        }

        String truncNote = matches.size() == maxRes ? "\n(results capped at " + maxRes + ")" : "";
        return ToolResult.success(String.join("\n", matches) + truncNote);
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
