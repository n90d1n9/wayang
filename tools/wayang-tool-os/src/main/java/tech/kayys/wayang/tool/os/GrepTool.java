package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

public final class GrepTool implements Tool {

    private static final Set<String> SKIP_DIRS = Set.of(
            ".git", "node_modules", "target", "build", "out", ".gradle", ".idea", "dist", "__pycache__");
    private static final int MAX_MATCHES = 200;

    @Override public String id() { return "grep"; }
    @Override public String name() { return "grep"; }

    @Override public String description() {
        return "Recursively search for a regex pattern in files under a directory. " +
               "Returns matching lines as 'path:lineNumber: line content'. Skips common " +
               "build/dependency directories (.git, node_modules, target, build, etc).";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "pattern", Schema.string("Regular expression to search for (Java regex syntax)."),
                "path", Schema.string("Directory to search under. Defaults to the current directory."),
                "file_glob", Schema.string("Optional simple glob to filter filenames, e.g. '*.java'."),
                "case_insensitive", Schema.bool("Whether to match case-insensitively. Default false.")
        ), "pattern");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String patternStr = (String) params.get("pattern");
        String root = params.containsKey("path") ? (String) params.get("path") : ".";
        String fileGlob = params.containsKey("file_glob") ? (String) params.get("file_glob") : null;
        boolean ci = params.containsKey("case_insensitive") && ((Boolean) params.get("case_insensitive"));

        Pattern pattern;
        try {
            pattern = Pattern.compile(patternStr, ci ? Pattern.CASE_INSENSITIVE : 0);
        } catch (PatternSyntaxException e) {
            return ToolResult.error("Invalid regex: " + e.getMessage());
        }

        Path rootPath = context.workingDirectory().resolve(root);
        if (!Files.exists(rootPath)) return ToolResult.error("Path not found: " + root);

        PathMatcher globMatcher = fileGlob != null
                ? FileSystems.getDefault().getPathMatcher("glob:" + fileGlob)
                : null;

        List<String> results = new ArrayList<>();
        int filesScanned = 0;

        try (Stream<Path> walk = Files.walk(rootPath)) {
            Iterator<Path> it = walk.iterator();
            while (it.hasNext() && results.size() < MAX_MATCHES) {
                Path p = it.next();
                if (Files.isDirectory(p)) {
                    if (SKIP_DIRS.contains(p.getFileName().toString())) {
                        // Files.walk doesn't support pruning directly via iterator skip easily;
                        // we just rely on the SKIP_DIRS check per-entry below for files within them.
                    }
                    continue;
                }
                if (isInSkippedDir(rootPath, p)) continue;
                if (globMatcher != null && !globMatcher.matches(p.getFileName())) continue;
                if (!looksLikeText(p)) continue;

                filesScanned++;
                List<String> lines;
                try {
                    lines = Files.readAllLines(p);
                } catch (IOException e) {
                    continue; // binary or unreadable; skip
                }
                for (int i = 0; i < lines.size() && results.size() < MAX_MATCHES; i++) {
                    if (pattern.matcher(lines.get(i)).find()) {
                        results.add(p + ":" + (i + 1) + ": " + lines.get(i).strip());
                    }
                }
            }
        }

        if (results.isEmpty()) {
            return ToolResult.success("No matches found (scanned " + filesScanned + " files).");
        }
        String suffix = results.size() >= MAX_MATCHES ? "\n... (truncated at " + MAX_MATCHES + " matches)" : "";
        return ToolResult.success(String.join("\n", results) + suffix);
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }

    private boolean isInSkippedDir(Path root, Path file) {
        Path rel = root.relativize(file);
        for (Path part : rel) {
            if (SKIP_DIRS.contains(part.toString())) return true;
        }
        return false;
    }

    private boolean looksLikeText(Path p) {
        try {
            if (Files.size(p) > 2_000_000) return false; // skip huge files
            byte[] head = new byte[512];
            int n;
            try (var in = Files.newInputStream(p)) {
                n = in.read(head);
            }
            if (n <= 0) return true;
            for (int i = 0; i < n; i++) {
                if (head[i] == 0) return false; // NUL byte -> binary
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
