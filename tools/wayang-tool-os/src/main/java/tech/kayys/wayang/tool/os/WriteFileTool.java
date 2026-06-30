package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class WriteFileTool implements Tool {

    @Override public String id() { return "write_file"; }
    @Override public String name() { return "write_file"; }

    @Override public String description() {
        return "Create a new file or overwrite an existing file with the given content. " +
               "Creates parent directories if needed.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "path", Schema.string("Path to the file to write, relative or absolute."),
                "content", Schema.string("Full content to write to the file.")
        ), "path", "content");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String pathStr = (String) params.get("path");
        String content = (String) params.get("content");
        Path path = context.workingDirectory().resolve(pathStr);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        boolean existed = Files.exists(path);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        int lines = content.isEmpty() ? 0 : (int) content.lines().count();
        return ToolResult.success((existed ? "Overwrote " : "Created ") + pathStr + " (" + lines + " lines).");
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
