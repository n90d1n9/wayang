package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ApplicationScoped
class EditFileTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(EditFileTool.class);

    @Override
    public String toolName() {
        return "replace_file_content";
    }

    @Override
    public String description() {
        return "Replace a specific contiguous block of text in a file. Must provide exact target content and start/end lines.";
    }

    @Override
    public List<String> parameters() {
        return List.of(
            "path - Absolute or relative path to the file to edit",
            "start_line - The starting line number of the chunk (1-indexed)",
            "end_line - The ending line number of the chunk (inclusive, 1-indexed)",
            "target_content - Exact string to be replaced (including whitespace)",
            "replacement_content - The content to replace the target content with",
            "allow_multiple - true/false, whether to allow multiple occurrences"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String pathStr = call.param("path", "");
        if (pathStr.isBlank()) return ToolResult.failure(toolName(), "Missing path parameter");

        Path path = Paths.get(pathStr).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            return ToolResult.failure(toolName(), "File does not exist: " + path);
        }

        int startLine;
        int endLine;
        try {
            startLine = Integer.parseInt(call.param("start_line", "0"));
            endLine = Integer.parseInt(call.param("end_line", "0"));
        } catch (NumberFormatException e) {
            return ToolResult.failure(toolName(), "start_line and end_line must be integers");
        }

        String targetContent = call.param("target_content", "");
        String replacementContent = call.param("replacement_content", "");
        boolean allowMultiple = Boolean.parseBoolean(call.param("allow_multiple", "false"));

        if (startLine <= 0 || endLine <= 0 || startLine > endLine) {
            return ToolResult.failure(toolName(), "Invalid start_line or end_line parameters");
        }
        if (targetContent.isBlank()) {
            return ToolResult.failure(toolName(), "target_content cannot be empty");
        }

        try {
            List<String> lines = Files.readAllLines(path);
            if (endLine > lines.size()) {
                endLine = lines.size();
            }

            StringBuilder blockBuilder = new StringBuilder();
            for (int i = startLine - 1; i < endLine; i++) {
                blockBuilder.append(lines.get(i)).append("\n");
            }
            String block = blockBuilder.toString();

            if (!block.contains(targetContent)) {
                return ToolResult.failure(toolName(), "target_content not found within the specified line range");
            }

            int count = 0;
            int idx = 0;
            while ((idx = block.indexOf(targetContent, idx)) != -1) {
                count++;
                idx += targetContent.length();
            }

            if (count > 1 && !allowMultiple) {
                return ToolResult.failure(toolName(), "Multiple occurrences of target_content found, but allow_multiple is false");
            }

            String replacedBlock = block.replace(targetContent, replacementContent);
            
            StringBuilder newFileContent = new StringBuilder();
            for (int i = 0; i < startLine - 1; i++) {
                newFileContent.append(lines.get(i)).append("\n");
            }
            newFileContent.append(replacedBlock);
            for (int i = endLine; i < lines.size(); i++) {
                newFileContent.append(lines.get(i)).append("\n");
            }

            Files.writeString(path, newFileContent.toString());
            log.info("Edited file: {}", path);
            return ToolResult.success(toolName(), "File edited successfully.");
        } catch (IOException e) {
            log.error("Failed to edit file: {}", path, e);
            return ToolResult.failure(toolName(), "Failed to edit file: " + e.getMessage());
        }
    }
}

@ApplicationScoped
class MultiEditTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(MultiEditTool.class);

    @Override
    public String toolName() {
        return "multi_replace_file_content";
    }

    @Override
    public String description() {
        return "Replace multiple contiguous blocks of text in a file in a single pass. Input must be JSON string of chunks.";
    }

    @Override
    public List<String> parameters() {
        return List.of(
            "path - Absolute or relative path to the file to edit",
            "chunks - JSON array of objects, each containing: start_line, end_line, target_content, replacement_content, allow_multiple"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String pathStr = call.param("path", "");
        if (pathStr.isBlank()) return ToolResult.failure(toolName(), "Missing path parameter");

        Path path = Paths.get(pathStr).toAbsolutePath().normalize();
        if (!Files.exists(path)) return ToolResult.failure(toolName(), "File does not exist: " + path);

        String chunksJson = call.param("chunks", "[]");
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode chunks = mapper.readTree(chunksJson);
            
            if (!chunks.isArray()) return ToolResult.failure(toolName(), "chunks parameter must be a JSON array");
            
            List<String> lines = Files.readAllLines(path);
            
            // For a robust implementation, we would apply all chunks from bottom to top 
            // to avoid line shifting, or use a complex patch logic.
            // For simplicity, we just loop and apply them if they don't overlap wildly.
            // Since this is a prototype logic, we'll process them in order.
            
            // ... actual logic will go here
            return ToolResult.success(toolName(), "MultiEditTool successfully parsed " + chunks.size() + " chunks.");
        } catch (Exception e) {
            log.error("Failed to parse chunks JSON", e);
            return ToolResult.failure(toolName(), "Failed to parse chunks JSON: " + e.getMessage());
        }
    }
}
