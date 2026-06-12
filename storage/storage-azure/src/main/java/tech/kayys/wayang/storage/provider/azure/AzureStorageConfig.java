package tech.kayys.wayang.storage.provider.azure;

import java.util.Map;
import java.util.Properties;

/**
 * Typed Azure Blob Storage provider configuration.
 */
public record AzureStorageConfig(
        String connectionString,
        String containerName,
        String pathPrefix) {

    public static final String CONNECTION_STRING_PROPERTY = "wayang.storage.azure.connection-string";
    public static final String CONTAINER_PROPERTY = "wayang.storage.azure.container";
    public static final String PATH_PREFIX_PROPERTY = "wayang.storage.azure.path-prefix";

    public static final String CONNECTION_STRING_ENV = "WAYANG_STORAGE_AZURE_CONNECTION_STRING";
    public static final String CONTAINER_ENV = "WAYANG_STORAGE_AZURE_CONTAINER";
    public static final String PATH_PREFIX_ENV = "WAYANG_STORAGE_AZURE_PATH_PREFIX";

    public AzureStorageConfig {
        connectionString = require(connectionString, "connectionString");
        containerName = require(containerName, "containerName");
        pathPrefix = optional(pathPrefix);
    }

    public static AzureStorageConfig of(String connectionString, String containerName, String pathPrefix) {
        return new AzureStorageConfig(connectionString, containerName, pathPrefix);
    }

    public static AzureStorageConfig fromProperties(Properties properties) {
        return fromMap((Map<?, ?>) properties);
    }

    public static AzureStorageConfig fromMap(Map<?, ?> values) {
        return new AzureStorageConfig(
                value(values, CONNECTION_STRING_PROPERTY),
                value(values, CONTAINER_PROPERTY),
                value(values, PATH_PREFIX_PROPERTY));
    }

    public static AzureStorageConfig fromEnvironment(Map<String, String> environment) {
        return new AzureStorageConfig(
                value(environment, CONNECTION_STRING_ENV),
                value(environment, CONTAINER_ENV),
                value(environment, PATH_PREFIX_ENV));
    }

    private static String value(Map<?, ?> values, String key) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private static String require(String value, String name) {
        String resolved = optional(value);
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return resolved;
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
