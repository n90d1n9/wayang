package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dependency-free provider adapter for tenant or key based session config lookup.
 */
public final class SessionConfigLookupProvider implements SessionConfigSourceProvider {

    public static final String TYPE_DATABASE = "database";
    public static final String TYPE_CONFIG_SERVICE = "config-service";
    public static final String KEY_TENANT_ID = "tenantId";
    public static final String KEY_LOOKUP_KEY = "key";
    public static final String DEFAULT_LOOKUP_KEY = "default";

    private final String type;
    private final boolean tenantRequired;
    private final boolean keyRequired;
    private final LookupReader reader;

    public SessionConfigLookupProvider(
            String type,
            boolean tenantRequired,
            boolean keyRequired,
            LookupReader reader) {
        this.type = requireText(type, "type");
        this.tenantRequired = tenantRequired;
        this.keyRequired = keyRequired;
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    public static SessionConfigLookupProvider database(LookupReader reader) {
        return tenantScoped(TYPE_DATABASE, reader);
    }

    public static SessionConfigLookupProvider configService(LookupReader reader) {
        return keyed(TYPE_CONFIG_SERVICE, reader);
    }

    public static SessionConfigLookupProvider tenantScoped(String type, LookupReader reader) {
        return new SessionConfigLookupProvider(type, true, false, reader);
    }

    public static SessionConfigLookupProvider keyed(String type, LookupReader reader) {
        return new SessionConfigLookupProvider(type, false, true, reader);
    }

    @Override
    public SessionConfigSource source(Map<String, Object> values) {
        Map<String, Object> safeValues = safeValues(values);
        requireValid(safeValues);
        String tenantId = tenantId(safeValues);
        String lookupKey = lookupKey(safeValues);
        return SessionConfigSources.json(description(tenantId, lookupKey), () -> {
            Optional<String> json = reader.read(tenantId, lookupKey, Map.copyOf(safeValues));
            return json == null ? Optional.empty() : json;
        });
    }

    @Override
    public List<String> validationErrors(Map<String, Object> values) {
        Map<String, Object> safeValues = safeValues(values);
        List<String> errors = new ArrayList<>();
        if (tenantRequired && tenantId(safeValues).isBlank()) {
            errors.add(type + " source requires tenantId");
        }
        if (keyRequired && rawLookupKey(safeValues).isBlank()) {
            errors.add(type + " source requires key");
        }
        return List.copyOf(errors);
    }

    @Override
    public SessionConfigSourceCapability capability(String type) {
        return SessionConfigSourceCapability.lookup(type, tenantRequired, keyRequired);
    }

    private void requireValid(Map<String, Object> values) {
        List<String> errors = validationErrors(values);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    private String description(String tenantId, String lookupKey) {
        if (tenantId.isBlank()) {
            return type + ":" + lookupKey;
        }
        return type + ":" + tenantId + "/" + lookupKey;
    }

    private static String tenantId(Map<String, Object> values) {
        return text(values, KEY_TENANT_ID, "tenant", "realm");
    }

    private static String lookupKey(Map<String, Object> values) {
        String key = rawLookupKey(values);
        return key.isBlank() ? DEFAULT_LOOKUP_KEY : key;
    }

    private static String rawLookupKey(Map<String, Object> values) {
        return text(values, KEY_LOOKUP_KEY, "configKey", "profile", "name", "id");
    }

    private static String text(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            String value = DecodeValues.text(values.get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Map<String, Object> safeValues(Map<String, Object> values) {
        return values == null ? Map.of() : values;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    @FunctionalInterface
    public interface LookupReader {

        Optional<String> read(String tenantId, String lookupKey, Map<String, Object> values);
    }
}
