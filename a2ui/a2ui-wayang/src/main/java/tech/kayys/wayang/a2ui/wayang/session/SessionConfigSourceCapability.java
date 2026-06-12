package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the shape and diagnostic policy of an A2UI session config source type.
 */
public record SessionConfigSourceCapability(
        String type,
        String category,
        List<String> requiredFields,
        Map<String, List<String>> fieldAliases,
        List<String> diagnosticSafeFields,
        boolean tenantScoped,
        boolean lookupKeyScoped,
        boolean objectStorage,
        boolean fallback) {

    public static final String CATEGORY_CUSTOM = "custom";
    public static final String CATEGORY_INLINE = "inline";
    public static final String CATEGORY_FILE = "file";
    public static final String CATEGORY_CLASSPATH = "classpath";
    public static final String CATEGORY_LOOKUP = "lookup";
    public static final String CATEGORY_OBJECT_STORAGE = "object-storage";
    public static final String CATEGORY_FALLBACK = "fallback";

    public SessionConfigSourceCapability {
        type = requireText(type, "type");
        category = category == null || category.isBlank() ? CATEGORY_CUSTOM : category.trim();
        requiredFields = copyStrings(requiredFields);
        fieldAliases = copyAliases(fieldAliases);
        diagnosticSafeFields = copyStrings(diagnosticSafeFields);
    }

    public static SessionConfigSourceCapability generic(String type) {
        return new SessionConfigSourceCapability(
                type,
                CATEGORY_CUSTOM,
                List.of(),
                Map.of(),
                List.of(SessionConfigSourceRegistry.KEY_TYPE, SessionConfigSourceRegistry.KEY_KIND),
                false,
                false,
                false,
                false);
    }

    public static SessionConfigSourceCapability inline(String type) {
        return new SessionConfigSourceCapability(
                type,
                CATEGORY_INLINE,
                List.of("json"),
                Map.of("json", List.of("value")),
                List.of(SessionConfigSourceRegistry.KEY_TYPE, "description"),
                false,
                false,
                false,
                false);
    }

    public static SessionConfigSourceCapability file(String type) {
        return new SessionConfigSourceCapability(
                type,
                CATEGORY_FILE,
                List.of("path"),
                Map.of("path", List.of("file")),
                List.of(SessionConfigSourceRegistry.KEY_TYPE, "path", "file"),
                false,
                false,
                false,
                false);
    }

    public static SessionConfigSourceCapability classpath(String type) {
        return new SessionConfigSourceCapability(
                type,
                CATEGORY_CLASSPATH,
                List.of("resource"),
                Map.of("resource", List.of("classpath")),
                List.of(SessionConfigSourceRegistry.KEY_TYPE, "resource", "classpath"),
                false,
                false,
                false,
                false);
    }

    public static SessionConfigSourceCapability lookup(
            String type,
            boolean tenantRequired,
            boolean keyRequired) {
        List<String> requiredFields = new ArrayList<>();
        if (tenantRequired) {
            requiredFields.add(SessionConfigLookupProvider.KEY_TENANT_ID);
        }
        if (keyRequired) {
            requiredFields.add(SessionConfigLookupProvider.KEY_LOOKUP_KEY);
        }
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        aliases.put(SessionConfigLookupProvider.KEY_TENANT_ID, List.of("tenant", "realm"));
        aliases.put(SessionConfigLookupProvider.KEY_LOOKUP_KEY, List.of("configKey", "profile", "name", "id"));
        return new SessionConfigSourceCapability(
                type,
                CATEGORY_LOOKUP,
                requiredFields,
                aliases,
                List.of(
                        SessionConfigSourceRegistry.KEY_TYPE,
                        SessionConfigLookupProvider.KEY_TENANT_ID,
                        "tenant",
                        "realm",
                        SessionConfigLookupProvider.KEY_LOOKUP_KEY,
                        "configKey",
                        "profile",
                        "name",
                        "id"),
                tenantRequired,
                true,
                false,
                false);
    }

    public static SessionConfigSourceCapability objectStorage(String type) {
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        aliases.put(SessionConfigObjectStorageProvider.KEY_BUCKET, List.of("container"));
        aliases.put(SessionConfigObjectStorageProvider.KEY_KEY, List.of("objectKey", "path"));
        return new SessionConfigSourceCapability(
                type,
                CATEGORY_OBJECT_STORAGE,
                List.of(
                        SessionConfigObjectStorageProvider.KEY_BUCKET,
                        SessionConfigObjectStorageProvider.KEY_KEY),
                aliases,
                List.of(
                        SessionConfigSourceRegistry.KEY_TYPE,
                        SessionConfigObjectStorageProvider.KEY_BUCKET,
                        "container",
                        SessionConfigObjectStorageProvider.KEY_KEY,
                        "objectKey",
                        "path"),
                false,
                false,
                true,
                false);
    }

    public static SessionConfigSourceCapability fallback(String type) {
        return new SessionConfigSourceCapability(
                type,
                CATEGORY_FALLBACK,
                List.of(SessionConfigSourceRegistry.KEY_SOURCES),
                Map.of(),
                List.of(SessionConfigSourceRegistry.KEY_TYPE, SessionConfigSourceRegistry.KEY_SOURCES),
                false,
                false,
                false,
                true);
    }

    public boolean requiresTenant() {
        return requiredFields.contains(SessionConfigLookupProvider.KEY_TENANT_ID);
    }

    public boolean requiresLookupKey() {
        return requiredFields.contains(SessionConfigLookupProvider.KEY_LOOKUP_KEY);
    }

    public boolean requiresBucketKey() {
        return requiredFields.contains(SessionConfigObjectStorageProvider.KEY_BUCKET)
                && requiredFields.contains(SessionConfigObjectStorageProvider.KEY_KEY);
    }

    public boolean supportsFallback() {
        return fallback;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", type);
        values.put("category", category);
        values.put("requiredFields", requiredFields);
        values.put("fieldAliases", fieldAliases);
        values.put("diagnosticSafeFields", diagnosticSafeFields);
        values.put("tenantScoped", tenantScoped);
        values.put("lookupKeyScoped", lookupKeyScoped);
        values.put("objectStorage", objectStorage);
        values.put("fallback", fallback);
        return TransportMaps.freeze(values);
    }

    private static List<String> copyStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static Map<String, List<String>> copyAliases(Map<String, List<String>> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        aliases.forEach((key, values) -> {
            if (key != null && !key.isBlank()) {
                copy.put(key.trim(), copyStrings(values));
            }
        });
        return Collections.unmodifiableMap(copy);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }
}
