package tech.kayys.wayang.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class WayangJsonSchemaObjectBuilder {

    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();
    private boolean additionalProperties = true;

    private WayangJsonSchemaObjectBuilder() {
    }

    public static WayangJsonSchemaObjectBuilder create() {
        return new WayangJsonSchemaObjectBuilder();
    }

    public WayangJsonSchemaObjectBuilder required(String name, Map<String, Object> property) {
        required.add(addProperty(name, property));
        return this;
    }

    public WayangJsonSchemaObjectBuilder optional(String name, Map<String, Object> property) {
        return property(name, property);
    }

   public  WayangJsonSchemaObjectBuilder additionalProperties(boolean additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }

    public Map<String, Object> properties() {
        return new LinkedHashMap<>(properties);
    }

    public Map<String, Object> objectProperty() {
        return WayangJsonSchemaDocuments.objectProperty(
                List.copyOf(required),
                additionalProperties,
                new LinkedHashMap<>(properties));
    }

    public WayangJsonSchemaObjectBuilder property(String name, Map<String, Object> property) {
        addProperty(name, property);
        return this;
    }

    private String addProperty(String name, Map<String, Object> property) {
        String normalizedName = SdkText.trimToEmpty(name);
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Schema property name is required.");
        }
        if (property == null) {
            throw new IllegalArgumentException("Schema property '" + normalizedName + "' definition is required.");
        }
        if (properties.containsKey(normalizedName)) {
            throw new IllegalArgumentException("Schema property '" + normalizedName + "' is already defined.");
        }
        properties.put(normalizedName, property);
        return normalizedName;
    }
}
