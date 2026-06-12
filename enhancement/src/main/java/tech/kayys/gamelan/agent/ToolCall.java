package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable representation of a tool call emitted by the LLM.
 *
 * @param name       tool name (e.g. "read_file")
 * @param parameters key-value parameters extracted from the XML block
 * @param rawXml     original XML (for debugging/logging)
 */
public record ToolCall(String name, Map<String, String> parameters, String rawXml) {

    public String param(String key) {
        return parameters.getOrDefault(key, "");
    }

    public String param(String key, String defaultValue) {
        String v = parameters.get(key);
        return (v != null) ? v : defaultValue;
    }

    public boolean hasParam(String key) {
        return parameters.containsKey(key) && !parameters.get(key).isBlank();
    }

    @Override public String toString() {
        return "ToolCall{name=" + name + ", params=" + parameters.keySet() + "}";
    }
}

// ──────────────────────────────────────────────────────────────────────────────

/**
 * Parses {@code <tool_call>} XML blocks from LLM output.
 *
 * <h2>Supported formats</h2>
 * <pre>
 * &lt;tool_call&gt;
 *   &lt;name&gt;read_file&lt;/name&gt;
 *   &lt;path&gt;src/Main.java&lt;/path&gt;
 * &lt;/tool_call&gt;
 * </pre>
 *
 * Also accepts the common LLM variant where the tool name appears as an
 * attribute: {@code <tool_call name="read_file">}.
 *
 * <h2>CDATA and multi-line content</h2>
 * Parameter values may span multiple lines (e.g. {@code <content>} for file
 * writes). The pattern uses DOTALL so newlines are included.
 *
 * <h2>Idempotency</h2>
 * The parser is stateless and thread-safe — it can be called from multiple
 * goroutines simultaneously (e.g. the workflow engine).
 */
@ApplicationScoped
class ToolCallParser {

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
