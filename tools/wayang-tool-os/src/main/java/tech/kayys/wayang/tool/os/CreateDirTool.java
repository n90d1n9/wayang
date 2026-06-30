package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;

import java.nio.file.*;

/**
 * Creates a directory and all required parent directories ({@code mkdir -p}).
 * Safe to call on an already-existing directory.
 */
public final class CreateDirTool implements Tool {

    @Override public String id() { return "create_dir"; }
    @Override public String name() { return "create_dir"; }

    @Override public String description() {
        return "Create a directory and all necessary parent directories (like 'mkdir -p'). " +
               "Safe to call on an already-existing directory — it will succeed without error. " +
               "Use when scaffolding new packages, modules, or output directories.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "path", Schema.string("The directory path to create.")
        ), "path");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String pathStr = (String) params.get("path");
        Path path = context.workingDirectory().resolve(pathStr);

        if (Files.isDirectory(path)) {
            return ToolResult.success("Directory already exists: " + pathStr);
        }
        Files.createDirectories(path);
        return ToolResult.success("Created directory: " + path.toAbsolutePath());
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
