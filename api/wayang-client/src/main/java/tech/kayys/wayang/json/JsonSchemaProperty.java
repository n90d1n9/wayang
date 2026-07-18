package tech.kayys.wayang.json;

import java.util.Map;

public interface JsonSchemaProperty {

    String getName();

    String getType();

    String getDescription();

    boolean isRequired();

    Map<String, JsonSchemaProperty> getProperties();

    Map<String, Object> toSchema();
}
