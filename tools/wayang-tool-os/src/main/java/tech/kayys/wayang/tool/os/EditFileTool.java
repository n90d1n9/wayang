package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class EditFileTool implements Tool {

    @Override public String id() { return "edit_file"; }
    @Override public String name() { return "edit_file"; }

    @Override public String description() {
        return "Replace an exact, unique substring within an existing file. " +
               "old_str must match the file content exactly (including whitespace) and must " +
               "appear exactly once -- include enough surrounding context to make it unique. " +
               "Use this instead of write_file for targeted changes.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "path", Schema.string("Path to the file to edit."),
                "old_str", Schema.string("Exact text to find; must appear exactly once in the file."),
                "new_str", Schema.string("Text to replace it with.")
        ), "path", "old_str", "new_str");
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        String pathStr = (String) params.get("path");
        String oldStr = (String) params.get("old_str");
        String newStr = (String) params.get("new_str");

        Path path = context.workingDirectory().resolve(pathStr);
        if (!Files.exists(path)) return ToolResult.error("File not found: " + pathStr);

        String content = Files.readString(path, StandardCharsets.UTF_8);
        int first = content.indexOf(oldStr);
        if (first == -1) {
            return ToolResult.error("old_str not found in " + pathStr + ". " +
                    "Make sure it matches the file content exactly, including whitespace and indentation.");
        }
        int second = content.indexOf(oldStr, first + 1);
        if (second != -1) {
            return ToolResult.error("old_str is not unique in " + pathStr +
                    " (found multiple matches). Include more surrounding context to make it unique.");
        }

        String updated = content.substring(0, first) + newStr + content.substring(first + oldStr.length());
        Files.writeString(path, updated, StandardCharsets.UTF_8);

        int oldLines = (int) oldStr.lines().count();
        int newLines = (int) newStr.lines().count();
        return ToolResult.success("Edited " + pathStr + " (" + oldLines + " lines -> " + newLines + " lines).");
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
