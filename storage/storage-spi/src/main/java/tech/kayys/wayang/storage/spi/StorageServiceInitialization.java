package tech.kayys.wayang.storage.spi;

/**
 * Shared lifecycle guard for provider services with explicit initialize methods.
 */
public final class StorageServiceInitialization {

    private StorageServiceInitialization() {
    }

    public static <T> T requireInitialized(T value, String serviceName) {
        if (value == null) {
            throw uninitialized(serviceName);
        }
        return value;
    }

    public static String requireTextInitialized(String value, String serviceName) {
        if (value == null || value.isBlank()) {
            throw uninitialized(serviceName);
        }
        return value;
    }

    private static IllegalStateException uninitialized(String serviceName) {
        return new IllegalStateException(serviceName + " has not been initialized");
    }
}
