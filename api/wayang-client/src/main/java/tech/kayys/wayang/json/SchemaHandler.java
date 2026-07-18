package tech.kayys.wayang.json;

import java.util.Map;

public interface SchemaHandler {

    String getDomainId();

    Map<String, Object> buildSchema();

    Map<String, JsonSchemaProperty> buildProperties();

    String getDescription();
}
