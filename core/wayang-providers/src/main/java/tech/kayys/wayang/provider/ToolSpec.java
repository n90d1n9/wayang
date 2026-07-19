package tech.kayys.wayang.provider;

import java.util.Map;

/**
 * Describes a tool the model may call, in provider-agnostic form.
 * Each Provider implementation translates this into its own wire format
 * (Anthropic's `tools` array vs OpenAI's `tools`/function-calling format).
 * The inputSchema follows JSON Schema (object with "properties", "required", etc.)
 * and matches the contract of {@code tech.kayys.wayang.tools.spi.Tool#inputSchema()}.
 */
public record ToolSpec(String name, String description, Map<String, Object> inputSchema) {
}
