package tech.kayys.wayang.json.handler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tech.kayys.wayang.json.JsonSchemaProperty;
import tech.kayys.wayang.json.SchemaHandler;

public class ContractEnvelopeSchemaHandler implements SchemaHandler {

    private static final String DOMAIN_ID = "contract-envelope";
    private static final String DESCRIPTION = "Wayang contract envelope schema";

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
        schema.put("title", "Contract Envelope Schema");
        schema.put("description", DESCRIPTION);
        schema.put("properties", new LinkedHashMap<>());
        schema.put("required", List.of("contract"));
        return schema;
    }

    @Override
    public Map<String, JsonSchemaProperty> buildProperties() {
        Map<String, JsonSchemaProperty> properties = new LinkedHashMap<>();
        return properties;
    }
}
