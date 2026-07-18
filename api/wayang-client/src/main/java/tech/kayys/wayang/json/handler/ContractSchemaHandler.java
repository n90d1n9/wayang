package tech.kayys.wayang.json.handler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tech.kayys.wayang.json.JsonSchemaProperty;
import tech.kayys.wayang.json.SchemaHandler;

public class ContractSchemaHandler implements SchemaHandler {

    private static final String DOMAIN_ID = "contract";
    private static final String DESCRIPTION = "Wayang contract schema";

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
        schema.put("title", "Contract Schema");
        schema.put("description", DESCRIPTION);
        schema.put("properties", new LinkedHashMap<>());
        schema.put("required", List.of());
        return schema;
    }

    @Override
    public Map<String, JsonSchemaProperty> buildProperties() {
        Map<String, JsonSchemaProperty> properties = new LinkedHashMap<>();
        return properties;
    }
}
