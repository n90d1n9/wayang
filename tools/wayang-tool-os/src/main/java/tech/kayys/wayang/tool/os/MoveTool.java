package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;

import java.nio.file.*;

/**
 * Moves or renames a file or directory.
 * Equivalent to the Unix {@code mv} command.
 */
public final class MoveTool implements Tool {

    @Override public String id() { return "move"; }
    @Override public String name() { return "move"; }

    @Override public String description() {
        return "Move or rename a file or directory. " +
               "If the destination already exists and overwrite is false (default), the operation fails. " +
               "Parent directories of the destination are created automatically if they don't exist. " +
               "Use for refactoring: renaming classes, reorganizing packages, moving generated files.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "source",      Schema.string("Path to the file or directory to move."),
                "destination", Schema.string("New path (including new filename if renaming)."),
                "overwrite",   Schema.bool("If true, overwrite an existing destination (default: false).")
        ), "source", "destination");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String srcStr  = (String) params.get("source");
        String dstStr  = (String) params.get("destination");
        boolean overwrite = params.containsKey("overwrite") && ((Boolean) params.get("overwrite")).booleanValue();

        Path src = context.workingDirectory().resolve(srcStr);
        Path dst = context.workingDirectory().resolve(dstStr);

        if (!Files.exists(src)) {
            return ToolResult.error("Source not found: " + srcStr);
        }
        if (Files.exists(dst) && !overwrite) {
            return ToolResult.error("Destination already exists: " + dstStr +
                    " (set overwrite=true to replace it).");
        }

        // Ensure destination parent dirs exist
        if (dst.getParent() != null) {
            Files.createDirectories(dst.getParent());
        }

        CopyOption[] opts = overwrite
                ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING}
                : new CopyOption[0];

        Files.move(src, dst, opts);
        return ToolResult.success("Moved: " + srcStr + " → " + dst.toAbsolutePath());
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
