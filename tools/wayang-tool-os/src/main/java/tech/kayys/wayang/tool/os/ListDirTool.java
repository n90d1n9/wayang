package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public final class ListDirTool implements Tool {

    @Override public String id() { return "list_dir"; }
    @Override public String name() { return "list_dir"; }

    @Override public String description() {
        return "List the contents of a directory (non-recursive). Directories are suffixed with '/'.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "path", Schema.string("Directory path to list. Defaults to the current directory.")
        ));
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String pathStr = params.containsKey("path") ? (String) params.get("path") : ".";
        Path dir = context.workingDirectory().resolve(pathStr);
        if (!Files.exists(dir)) return ToolResult.error("Path not found: " + pathStr);
        if (!Files.isDirectory(dir)) return ToolResult.error(pathStr + " is not a directory.");

        try (Stream<Path> stream = Files.list(dir)) {
            List<String> entries = stream
                    .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p)).thenComparing(Path::getFileName))
                    .map(p -> {
                        String n = p.getFileName().toString();
                        return Files.isDirectory(p) ? n + "/" : n;
                    })
                    .collect(Collectors.toList());
            if (entries.isEmpty()) return ToolResult.success("(empty directory)");
            return ToolResult.success(String.join("\n", entries));
        }
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
