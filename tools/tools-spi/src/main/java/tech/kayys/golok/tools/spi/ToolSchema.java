package tech.kayys.golok.tools.spi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * JSON Schema descriptor for a tool's parameters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolSchema(
        String name,
        String description,
        List<ParameterDef> parameters,
        List<String> required) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ObjectNode toOpenAIFormat() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "function");
        ObjectNode fn = root.putObject("function");
        fn.put("name", name);
        fn.put("description", description != null ? description : "");

        ObjectNode params = fn.putObject("parameters");
        params.put("type", "object");
        ObjectNode props = params.putObject("properties");

        for (ParameterDef p : parameters) {
            ObjectNode prop = props.putObject(p.name());
            prop.put("type", p.type());
            if (p.description() != null && !p.description().isBlank())
                prop.put("description", p.description());
            if (p.enumValues() != null && !p.enumValues().isEmpty()) {
                ArrayNode en = prop.putArray("enum");
                p.enumValues().forEach(en::add);
            }
            if (p.items() != null)
                prop.put("items", p.items());
        }

        if (required != null && !required.isEmpty()) {
            ArrayNode req = params.putArray("required");
            required.forEach(req::add);
        } else {
            params.putArray("required");
        }
        return root;
    }

    public ObjectNode toAnthropicFormat() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", name);
        root.put("description", description != null ? description : "");
        ObjectNode schema = root.putObject("input_schema");
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        for (ParameterDef p : parameters) {
            ObjectNode prop = props.putObject(p.name());
            prop.put("type", p.type());
            if (p.description() != null)
                prop.put("description", p.description());
        }
        if (required != null && !required.isEmpty()) {
            ArrayNode req = schema.putArray("required");
            required.forEach(req::add);
        }
        return root;
    }

    public String toTextDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool: ").append(name).append("\n");
        if (description != null)
            sb.append("Description: ").append(description).append("\n");
        sb.append("Parameters:\n");
        for (ParameterDef p : parameters) {
            boolean req = required != null && required.contains(p.name());
            sb.append("  - ").append(p.name()).append(" (").append(p.type()).append(req ? ", required" : "")
                    .append(")");
            if (p.description() != null)
                sb.append(": ").append(p.description());
            if (p.enumValues() != null && !p.enumValues().isEmpty())
                sb.append(" [one of: ").append(String.join(", ", p.enumValues())).append("]");
            sb.append("\n");
        }
        return sb.toString();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private String description;
        private final List<ParameterDef> params = new ArrayList<>();
        private final List<String> required = new ArrayList<>();

        Builder(String name) {
            this.name = name;
        }

        public Builder description(String d) {
            this.description = d;
            return this;
        }

        public Builder param(String name, String type, String description) {
            params.add(new ParameterDef(name, type, description, null, null));
            return this;
        }

        public Builder requiredParam(String name, String type, String description) {
            params.add(new ParameterDef(name, type, description, null, null));
            required.add(name);
            return this;
        }

        public Builder enumParam(String name, String description, String... values) {
            params.add(new ParameterDef(name, "string", description, List.of(values), null));
            return this;
        }

        public Builder requiredEnumParam(String name, String description, String... values) {
            params.add(new ParameterDef(name, "string", description, List.of(values), null));
            required.add(name);
            return this;
        }

        public Builder arrayParam(String name, String itemType, String description) {
            params.add(new ParameterDef(name, "array", description, null,
                    MAPPER.createObjectNode().put("type", itemType).toString()));
            return this;
        }

        public ToolSchema build() {
            return new ToolSchema(name, description, List.copyOf(params), List.copyOf(required));
        }
    }

    public record ParameterDef(
            String name,
            String type,
            String description,
            List<String> enumValues,
            String items) {
    }
}
