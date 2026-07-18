package tech.kayys.wayang.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonSchemaBuilder {

    private String domainName;
    private Map<String, Object> schemaDefinition = new LinkedHashMap<>();
    private List<SchemaHandler> handlers = new ArrayList<>();
    private boolean cachingEnabled = true;

    private JsonSchemaBuilder(String domainName) {
        this.domainName = domainName;
    }

    public static JsonSchemaBuilder forDomain(String domainName) {
        if (domainName == null || domainName.isBlank()) {
            throw new IllegalArgumentException("Domain name is required");
        }
        return new JsonSchemaBuilder(domainName);
    }

    public JsonSchemaBuilder withHandler(SchemaHandler handler) {
        if (handler != null) {
            this.handlers.add(handler);
        }
        return this;
    }

    public JsonSchemaBuilder withCache(boolean enabled) {
        this.cachingEnabled = enabled;
        return this;
    }

    public JsonSchemaBuilder withType(String type) {
        schemaDefinition.put("type", type);
        return this;
    }

    public JsonSchemaBuilder withTitle(String title) {
        schemaDefinition.put("title", title);
        return this;
    }

    public JsonSchemaBuilder withDescription(String description) {
        schemaDefinition.put("description", description);
        return this;
    }

    public JsonSchemaBuilder withProperties(Map<String, Object> properties) {
        if (properties != null) {
            schemaDefinition.put("properties", new LinkedHashMap<>(properties));
        }
        return this;
    }

    public JsonSchemaBuilder withRequired(List<String> required) {
        if (required != null && !required.isEmpty()) {
            schemaDefinition.put("required", new ArrayList<>(required));
        }
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> result = new LinkedHashMap<>(schemaDefinition);

        if (!handlers.isEmpty()) {
            SchemaHandler primaryHandler = handlers.get(0);
            Map<String, Object> handlerSchema = primaryHandler.buildSchema();
            if (handlerSchema != null) {
                result.putAll(handlerSchema);
            }
        }

        if (cachingEnabled && domainName != null) {
            JsonSchemaRegistry.getHandler(domainName);
        }

        return result;
    }

    public static class DomainBuilder {

        private final String domainName;
        private final Map<String, Object> schemaDefinition = new LinkedHashMap<>();

        public DomainBuilder(String domainName) {
            this.domainName = domainName;
        }

        public DomainBuilder withProperty(String key, Object value) {
            schemaDefinition.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return new LinkedHashMap<>(schemaDefinition);
        }
    }
}
