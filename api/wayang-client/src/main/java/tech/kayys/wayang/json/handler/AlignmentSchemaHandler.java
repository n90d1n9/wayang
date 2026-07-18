package tech.kayys.wayang.json.handler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tech.kayys.wayang.json.JsonSchemaProperty;
import tech.kayys.wayang.json.SchemaHandler;

public class AlignmentSchemaHandler implements SchemaHandler {

    private static final String DOMAIN_ID = "alignment";
    private static final String DESCRIPTION = "Wayang standard-alignment health envelope schema";

    @Override
    public String getDomainId() {
        return DOMAIN_ID;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Map<String, Object> buildSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("title", "Alignment Schema");
        schema.put("description", DESCRIPTION);
        schema.put("properties", buildProperties());
        schema.put("required", List.of("product", "health"));
        return schema;
    }

    @Override
    public Map<String, JsonSchemaProperty> buildProperties() {
        Map<String, JsonSchemaProperty> properties = new LinkedHashMap<>();
        // Properties are defined inline in buildSchema
        return properties;
    }
}
