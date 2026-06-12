package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record WayangContractJsonSchemaBundle(
        WayangContractDiscovery discovery,
        List<WayangContractJsonSchema> schemas) {

    public WayangContractJsonSchemaBundle {
        discovery = discovery == null
                ? WayangContractDiscovery.of(WayangContractQuery.all(), List.of(), 0)
                : discovery;
        schemas = SdkLists.copy(schemas);
    }

    public List<String> ids() {
        return schemas.stream()
                .map(WayangContractJsonSchema::id)
                .toList();
    }

    public List<WayangContractKey> keys() {
        return schemas.stream()
                .map(WayangContractJsonSchema::key)
                .toList();
    }

    public Map<String, WayangContractJsonSchema> schemasById() {
        Map<String, WayangContractJsonSchema> values = new LinkedHashMap<>();
        schemas.forEach(schema -> values.put(schema.id(), schema));
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }

    public Map<WayangContractKey, WayangContractJsonSchema> schemasByKey() {
        Map<WayangContractKey, WayangContractJsonSchema> values = new LinkedHashMap<>();
        schemas.forEach(schema -> values.put(schema.key(), schema));
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }

    public Map<String, Map<String, Object>> documentsById() {
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        schemas.forEach(schema -> values.put(schema.id(), schema.document()));
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }

    public Optional<WayangContractJsonSchema> schemaById(String id) {
        String normalized = SdkText.trimToEmpty(id);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        Optional<WayangContractKey> key = WayangContractKey.parseJsonSchemaId(normalized);
        if (key.isPresent()) {
            Optional<WayangContractJsonSchema> schema = schemaByKey(key.get());
            if (schema.isPresent()) {
                return schema;
            }
        }
        return schemas.stream()
                .filter(schema -> schema.id().equals(normalized))
                .findFirst();
    }

    public Optional<WayangContractJsonSchema> schemaByKey(WayangContractKey key) {
        if (key == null) {
            return Optional.empty();
        }
        return schemas.stream()
                .filter(schema -> key.matches(schema.contract()))
                .findFirst();
    }
}
