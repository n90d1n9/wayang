package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;
import java.nio.file.*;
import java.util.List;

public final class ReadFileTool implements Tool {

    @Override public String id() { return "read_file"; }
    @Override public String name() { return "read_file"; }

    @Override public String description() {
        return "Read the contents of a file from the local filesystem. " +
               "Returns the content with line numbers. Optionally read only a line range.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "path", Schema.string("Path to the file, relative to the working directory or absolute."),
                "start_line", Schema.integer("1-indexed line to start reading from (optional)."),
                "end_line", Schema.integer("1-indexed line to stop reading at, inclusive (optional).")
        ), "path");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String pathStr = (String) params.get("path");
        Path path = context.workingDirectory().resolve(pathStr);
        if (!Files.exists(path)) {
            return ToolResult.error("File not found: " + pathStr);
        }
        if (Files.isDirectory(path)) {
            return ToolResult.error(pathStr + " is a directory, not a file. Use list_dir instead.");
        }

        List<String> lines = Files.readAllLines(path);
        int start = params.containsKey("start_line") ? Math.max(1, ((Number) params.get("start_line")).intValue()) : 1;
        int end = params.containsKey("end_line") ? Math.min(lines.size(), ((Number) params.get("end_line")).intValue()) : lines.size();

        StringBuilder sb = new StringBuilder();
        int width = String.valueOf(end).length();
        for (int i = start; i <= end && i <= lines.size(); i++) {
            sb.append(String.format("%" + width + "d\t%s%n", i, lines.get(i - 1)));
        }
        if (sb.isEmpty()) return ToolResult.success("(file is empty or range out of bounds)");
        return ToolResult.success(sb.toString());
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
