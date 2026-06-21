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

