package tech.kayys.wayang.tools.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single tool/function call emitted by the LLM.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolCall(
        /** Unique call id provided by the model — used to match results. */
        String id,
        /** Skill/function name to invoke. */
        String name,
        /** Pre-parsed arguments (never null; may be empty). */
        Map<String, Object> arguments,
        /** Raw argument JSON as received from the model (for debugging). */
        String rawArguments) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonCreator
    public ToolCall {
        id = id != null ? id : UUID.randomUUID().toString();
        name = Objects.requireNonNull(name, "ToolCall.name must not be null");
        arguments = arguments != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(arguments))
                : Collections.emptyMap();
        rawArguments = rawArguments != null ? rawArguments : "{}";
    }

    public static ToolCall fromOpenAI(JsonNode node) {
        String id = node.path("id").asText(UUID.randomUUID().toString());
        JsonNode fn = node.path("function");
        String name = fn.path("name").asText();
        String raw = fn.path("arguments").asText("{}");
        return new ToolCall(id, name, parseJsonArgs(raw), raw);
    }

    public static ToolCall fromAnthropic(JsonNode node) {
        String id = node.path("id").asText(UUID.randomUUID().toString());
        String name = node.path("name").asText();
        JsonNode input = node.path("input");
        String raw = input.isMissingNode() ? "{}" : input.toString();
        Map<String, Object> args = input.isMissingNode()
                ? Collections.emptyMap()
                : parseJsonNode(input);
        return new ToolCall(id, name, args, raw);
    }

    public static ToolCall of(String name, Map<String, Object> arguments) {
        return new ToolCall(UUID.randomUUID().toString(), name, arguments, toRaw(arguments));
    }

    @SuppressWarnings("unchecked")
    public <T> T arg(String key, Class<T> type, T defaultValue) {
        Object v = arguments.get(key);
        if (v == null)
            return defaultValue;
        if (type.isInstance(v))
            return type.cast(v);
        if (type == Integer.class && v instanceof Number n)
            return type.cast(n.intValue());
        if (type == Long.class && v instanceof Number n)
            return type.cast(n.longValue());
        if (type == Double.class && v instanceof Number n)
            return type.cast(n.doubleValue());
        if (type == String.class)
            return type.cast(v.toString());
        return defaultValue;
    }

    public String argStr(String key) {
        return arg(key, String.class, null);
    }

    public String argStr(String key, String def) {
        return arg(key, String.class, def);
    }

    public int argInt(String key, int def) {
        Integer r = arg(key, Integer.class, null);
        return r != null ? r : def;
    }

    public boolean argBool(String key, boolean def) {
        Boolean r = arg(key, Boolean.class, null);
        return r != null ? r : def;
    }

    public boolean hasArg(String key) {
        return arguments.containsKey(key) && arguments.get(key) != null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonArgs(String raw) {
        if (raw == null || raw.isBlank() || raw.equals("{}"))
            return Collections.emptyMap();
        try {
            return MAPPER.readValue(raw, LinkedHashMap.class);
        } catch (Exception e) {
            return Collections.singletonMap("_raw", raw);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonNode(JsonNode node) {
        try {
            return MAPPER.convertValue(node, LinkedHashMap.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static String toRaw(Map<String, Object> args) {
        try {
            return MAPPER.writeValueAsString(args);
        } catch (Exception e) {
            return "{}";
        }
    }
}
