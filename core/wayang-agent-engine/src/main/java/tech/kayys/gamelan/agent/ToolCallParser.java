package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolCallParser {

    // Matches the full <tool_call>...</tool_call> block
    private static final Pattern BLOCK = Pattern.compile(
            "<tool_call(?:\\s+name=\"([^\"]+)\")?\\s*>(.*?)</tool_call>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // Matches inner <tag>value</tag> pairs (value may span lines)
    private static final Pattern INNER_TAG = Pattern.compile(
            "<(\\w[\\w_-]*)>(.*?)</\\1>",
            Pattern.DOTALL);

    /**
     * Extracts all tool calls from an LLM response string.
     *
     * @param text LLM output (may contain zero or more tool call blocks)
     * @return ordered list of tool calls; empty if none found
     */
    public List<ToolCall> parse(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<ToolCall> calls = new ArrayList<>();
        Matcher blockMatcher = BLOCK.matcher(text);

        while (blockMatcher.find()) {
            String nameAttr  = blockMatcher.group(1); // from name="..." attribute
            String innerXml  = blockMatcher.group(2);
            String rawXml    = blockMatcher.group(0);

            // Parse inner key-value tags — preserve insertion order
            Map<String, String> params = new LinkedHashMap<>();
            Matcher tagMatcher = INNER_TAG.matcher(innerXml);
            while (tagMatcher.find()) {
                String key   = tagMatcher.group(1).trim();
                String value = tagMatcher.group(2).trim();
                // Strip CDATA wrappers if present
                if (value.startsWith("<![CDATA[") && value.endsWith("]]>")) {
                    value = value.substring(9, value.length() - 3);
                }
                params.put(key, value);
            }

            // Resolve tool name: prefer <name> inner tag, fall back to attribute
            String toolName = params.remove("name");
            if ((toolName == null || toolName.isBlank()) && nameAttr != null) {
                toolName = nameAttr.trim();
            }

            if (toolName != null && !toolName.isBlank()) {
                calls.add(new ToolCall(toolName, Collections.unmodifiableMap(params), rawXml));
            }
        }

        return calls;
    }
}
