package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileObjectReader;
import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapts a provider-neutral object storage service into a readiness profile object reader.
 */
public final class WayangReadinessProfileObjectStorageServiceReader
        implements WayangPlatformReadinessProfileObjectReader {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final WayangReadinessProfileObjectStorageServiceResolver serviceResolver;
    private final String objectKey;
    private final Duration timeout;
    private final Charset charset;

    private WayangReadinessProfileObjectStorageServiceReader(
            WayangReadinessProfileObjectStorageServiceResolver serviceResolver,
            String objectKey,
            Duration timeout,
            Charset charset) {
        this.serviceResolver = Objects.requireNonNull(serviceResolver, "serviceResolver");
        this.objectKey = trimToEmpty(objectKey);
        this.timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? DEFAULT_TIMEOUT
                : timeout;
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public static WayangReadinessProfileObjectStorageServiceReader of(
            ObjectStorageService objectStorageService) {
        return resolving(WayangReadinessProfileObjectStorageServiceRegistry.ofDefault(objectStorageService));
    }

    public static WayangReadinessProfileObjectStorageServiceReader resolving(
            WayangReadinessProfileObjectStorageServiceResolver serviceResolver) {
        return new WayangReadinessProfileObjectStorageServiceReader(
                serviceResolver,
                "",
                DEFAULT_TIMEOUT,
                StandardCharsets.UTF_8);
    }

    public WayangReadinessProfileObjectStorageServiceReader withObjectKey(String objectKey) {
        return new WayangReadinessProfileObjectStorageServiceReader(
                serviceResolver,
                objectKey,
                timeout,
                charset);
    }

    public WayangReadinessProfileObjectStorageServiceReader withTimeout(Duration timeout) {
        return new WayangReadinessProfileObjectStorageServiceReader(
                serviceResolver,
                objectKey,
                timeout,
                charset);
    }

    public WayangReadinessProfileObjectStorageServiceReader withCharset(Charset charset) {
        return new WayangReadinessProfileObjectStorageServiceReader(
                serviceResolver,
                objectKey,
                timeout,
                charset);
    }

    @Override
    public String read(WayangObjectStorageConfig config) throws IOException {
        String key = objectKey(config);
        if (key.isBlank()) {
            throw new IOException("Readiness profile object key is not configured.");
        }
        ObjectStorageService objectStorageService = serviceResolver.resolve(config)
                .orElseThrow(() -> new IOException(
                        "Readiness profile object-storage service is not configured for "
                                + serviceReference(config)
                                + "."));
        try {
            Optional<byte[]> payload = Optional.ofNullable(
                    objectStorageService.getObject(key).await().atMost(timeout))
                    .orElse(Optional.empty());
            if (payload.isEmpty()) {
                throw new FileNotFoundException("Readiness profile object was not found: " + key + ".");
            }
            return new String(payload.get(), charset);
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Object-storage read failed for readiness profile object: " + key + ".",
                    exception);
        }
    }

    private String objectKey(WayangObjectStorageConfig config) {
        if (!objectKey.isBlank()) {
            return objectKey;
        }
        return config == null ? "" : trimToEmpty(config.keyPrefix());
    }

    private static String serviceReference(WayangObjectStorageConfig config) {
        if (config == null) {
            return "default object-storage service";
        }
        if (!config.credentialsRef().isBlank()) {
            return "credentialsRef '" + WayangSecretRedactor.connectionString(config.credentialsRef()) + "'";
        }
        if (!config.provider().isBlank()) {
            return "provider '" + config.provider() + "'";
        }
        return "default object-storage service";
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
