package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfigDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryResolution;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Preflights readiness profile object-storage wiring before SDK bootstrap.
 *
 * <p>The preflight verifies registry configuration, object-storage service
 * resolution, object readability, and readiness-profile document validation
 * through the same object-reader boundary used by the SDK runtime.</p>
 */
public final class WayangReadinessProfileObjectStoragePreflight {

    private final WayangReadinessProfileObjectStorageServiceResolver serviceResolver;
    private final String objectKey;
    private final Duration timeout;
    private final Charset charset;

    private WayangReadinessProfileObjectStoragePreflight(
            WayangReadinessProfileObjectStorageServiceResolver serviceResolver,
            String objectKey,
            Duration timeout,
            Charset charset) {
        this.serviceResolver = Objects.requireNonNull(serviceResolver, "serviceResolver");
        this.objectKey = trimToEmpty(objectKey);
        this.timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? WayangReadinessProfileObjectStorageServiceReader.DEFAULT_TIMEOUT
                : timeout;
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public static WayangReadinessProfileObjectStoragePreflight resolving(
            WayangReadinessProfileObjectStorageServiceResolver serviceResolver) {
        return new WayangReadinessProfileObjectStoragePreflight(
                serviceResolver,
                "",
                WayangReadinessProfileObjectStorageServiceReader.DEFAULT_TIMEOUT,
                StandardCharsets.UTF_8);
    }

    public static WayangReadinessProfileObjectStoragePreflight registry(
            WayangReadinessProfileObjectStorageServiceRegistry registry) {
        return resolving(registry == null
                ? WayangReadinessProfileObjectStorageServiceRegistry.empty()
                : registry);
    }

    public WayangReadinessProfileObjectStoragePreflight withObjectKey(String objectKey) {
        return new WayangReadinessProfileObjectStoragePreflight(
                serviceResolver,
                objectKey,
                timeout,
                charset);
    }

    public WayangReadinessProfileObjectStoragePreflight withTimeout(Duration timeout) {
        return new WayangReadinessProfileObjectStoragePreflight(
                serviceResolver,
                objectKey,
                timeout,
                charset);
    }

    public WayangReadinessProfileObjectStoragePreflight withCharset(Charset charset) {
        return new WayangReadinessProfileObjectStoragePreflight(
                serviceResolver,
                objectKey,
                timeout,
                charset);
    }

    public WayangReadinessProfileObjectStoragePreflightReport check(
            WayangObjectStorageConfig config) {
        WayangObjectStorageConfig resolved = config == null ? WayangObjectStorageConfig.none() : config;
        String resolvedObjectKey = objectKey(resolved);
        WayangPlatformReadinessProfileRegistryConfig registryConfig =
                WayangPlatformReadinessProfileRegistryConfig.objectStorage(resolved, false);
        WayangPlatformReadinessProfileRegistryConfigDiagnostics configDiagnostics =
                registryConfig.diagnostics();
        WayangReadinessProfileObjectStorageServiceResolution serviceResolution =
                serviceResolution(resolved);

        String document = "";
        boolean objectReadable = false;
        Throwable readFailure = null;
        if (configDiagnostics.valid() && serviceResolution.available()) {
            try {
                document = reader().read(resolved);
                objectReadable = true;
            } catch (IOException | RuntimeException exception) {
                readFailure = exception;
            }
        }

        String resolvedDocument = document;
        WayangPlatformReadinessProfileRegistryResolution registryResolution =
                resolvedDocument.isBlank()
                        ? null
                        : registryConfig.registry(ignored -> resolvedDocument).resolve(registryConfig.validationPolicy());
        return WayangReadinessProfileObjectStoragePreflightReport.from(
                resolved,
                resolvedObjectKey,
                configDiagnostics,
                serviceResolution,
                registryResolution,
                resolvedDocument,
                objectReadable,
                charset,
                readFailure,
                skippedReasons(configDiagnostics));
    }

    private WayangReadinessProfileObjectStorageServiceReader reader() {
        return WayangReadinessProfileObjectStorageServiceReader.resolving(serviceResolver)
                .withObjectKey(objectKey)
                .withTimeout(timeout)
                .withCharset(charset);
    }

    private WayangReadinessProfileObjectStorageServiceResolution serviceResolution(
            WayangObjectStorageConfig config) {
        if (serviceResolver instanceof WayangReadinessProfileObjectStorageServiceRegistry registry) {
            return registry.resolveReport(config);
        }
        return serviceResolver.resolve(config)
                .map(ignored -> new WayangReadinessProfileObjectStorageServiceResolution(
                        credentialsRef(config),
                        provider(config),
                        "external",
                        "resolver",
                        true,
                        List.of("external"),
                        "Readiness profile object-storage service resolved by external resolver."))
                .orElseGet(() -> new WayangReadinessProfileObjectStorageServiceResolution(
                        credentialsRef(config),
                        provider(config),
                        "",
                        "resolver",
                        false,
                        List.of(),
                        "External readiness profile object-storage resolver did not return a service."));
    }

    private List<String> skippedReasons(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics configDiagnostics) {
        java.util.ArrayList<String> reasons = new java.util.ArrayList<>();
        if (!configDiagnostics.valid()) {
            reasons.add("Readiness profile object-storage config is invalid.");
        }
        return List.copyOf(reasons);
    }

    private String objectKey(WayangObjectStorageConfig config) {
        return objectKey.isBlank() ? config.keyPrefix() : objectKey;
    }

    private static String credentialsRef(WayangObjectStorageConfig config) {
        return config == null ? "" : trimToEmpty(config.credentialsRef()).toLowerCase(java.util.Locale.ROOT);
    }

    private static String provider(WayangObjectStorageConfig config) {
        return config == null ? "" : trimToEmpty(config.provider()).toLowerCase(java.util.Locale.ROOT);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
