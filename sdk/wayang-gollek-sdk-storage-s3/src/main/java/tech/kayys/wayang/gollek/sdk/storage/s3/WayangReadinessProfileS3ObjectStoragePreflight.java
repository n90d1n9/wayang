package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStoragePreflight;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStoragePreflightReport;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceReader;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceRegistry;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Preflights S3-compatible readiness profile storage with redacted credential diagnostics.
 *
 * <p>The preflight resolves S3 credentials, creates the configured
 * object-storage service, then delegates object reads and readiness-profile
 * document validation to the neutral storage preflight boundary.</p>
 */
public final class WayangReadinessProfileS3ObjectStoragePreflight {

    private final WayangReadinessProfileS3CredentialsResolver credentialsResolver;
    private final BiFunction<
            WayangObjectStorageConfig,
            WayangReadinessProfileS3Credentials,
            ObjectStorageService> serviceFactory;
    private final String objectKey;
    private final Duration timeout;
    private final Charset charset;

    private WayangReadinessProfileS3ObjectStoragePreflight(
            WayangReadinessProfileS3CredentialsResolver credentialsResolver,
            BiFunction<
                    WayangObjectStorageConfig,
                    WayangReadinessProfileS3Credentials,
                    ObjectStorageService> serviceFactory,
            String objectKey,
            Duration timeout,
            Charset charset) {
        this.credentialsResolver = credentialsResolver == null
                ? ignored -> Optional.empty()
                : credentialsResolver;
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "serviceFactory");
        this.objectKey = trimToEmpty(objectKey);
        this.timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? WayangReadinessProfileObjectStorageServiceReader.DEFAULT_TIMEOUT
                : timeout;
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public static WayangReadinessProfileS3ObjectStoragePreflight resolving(
            WayangReadinessProfileS3CredentialsResolver credentialsResolver) {
        return usingServiceFactory(
                credentialsResolver,
                WayangReadinessProfileS3ObjectStorageReaderFactory::service);
    }

    public static WayangReadinessProfileS3ObjectStoragePreflight registry(
            WayangReadinessProfileS3CredentialsRegistry credentialsRegistry) {
        return resolving(credentialsRegistry == null
                ? WayangReadinessProfileS3CredentialsRegistry.empty()
                : credentialsRegistry);
    }

    public static WayangReadinessProfileS3ObjectStoragePreflight usingServiceFactory(
            WayangReadinessProfileS3CredentialsResolver credentialsResolver,
            BiFunction<
                    WayangObjectStorageConfig,
                    WayangReadinessProfileS3Credentials,
                    ObjectStorageService> serviceFactory) {
        return new WayangReadinessProfileS3ObjectStoragePreflight(
                credentialsResolver,
                serviceFactory,
                "",
                WayangReadinessProfileObjectStorageServiceReader.DEFAULT_TIMEOUT,
                StandardCharsets.UTF_8);
    }

    public WayangReadinessProfileS3ObjectStoragePreflight withObjectKey(String objectKey) {
        return new WayangReadinessProfileS3ObjectStoragePreflight(
                credentialsResolver,
                serviceFactory,
                objectKey,
                timeout,
                charset);
    }

    public WayangReadinessProfileS3ObjectStoragePreflight withTimeout(Duration timeout) {
        return new WayangReadinessProfileS3ObjectStoragePreflight(
                credentialsResolver,
                serviceFactory,
                objectKey,
                timeout,
                charset);
    }

    public WayangReadinessProfileS3ObjectStoragePreflight withCharset(Charset charset) {
        return new WayangReadinessProfileS3ObjectStoragePreflight(
                credentialsResolver,
                serviceFactory,
                objectKey,
                timeout,
                charset);
    }

    public WayangReadinessProfileS3ObjectStoragePreflightReport check(
            WayangObjectStorageConfig config) {
        WayangObjectStorageConfig resolved = config == null ? WayangObjectStorageConfig.none() : config;
        CredentialEvaluation credentials = credentials(resolved);
        ObjectStorageService service = null;
        Throwable serviceFailure = null;
        if (credentials.credentials().isPresent()) {
            try {
                service = Objects.requireNonNull(
                        serviceFactory.apply(resolved, credentials.credentials().get()),
                        "serviceFactory returned null");
            } catch (RuntimeException exception) {
                serviceFailure = exception;
            }
        }

        WayangReadinessProfileObjectStoragePreflightReport objectStoragePreflight = service == null
                ? null
                : WayangReadinessProfileObjectStoragePreflight
                        .registry(WayangReadinessProfileObjectStorageServiceRegistry.ofDefault(service))
                        .withObjectKey(objectKey)
                        .withTimeout(timeout)
                        .withCharset(charset)
                        .check(resolved);
        return WayangReadinessProfileS3ObjectStoragePreflightReport.from(
                resolved,
                credentials.resolution(),
                credentials.sourceDiagnostics(),
                service != null,
                serviceFailure,
                objectStoragePreflight);
    }

    private CredentialEvaluation credentials(WayangObjectStorageConfig config) {
        if (credentialsResolver instanceof WayangReadinessProfileS3CredentialsRegistry registry) {
            WayangReadinessProfileS3CredentialsResolution resolution = registry.resolveReport(config);
            Optional<WayangReadinessProfileS3Credentials> credentials = resolution.available()
                    ? registry.find(resolution.selectedCredentialsId())
                    : Optional.empty();
            return new CredentialEvaluation(
                    resolution,
                    credentials,
                    registry.credentialSourceDiagnostics());
        }
        try {
            Optional<WayangReadinessProfileS3Credentials> credentials = credentialsResolver.resolve(config);
            return credentials
                    .map(value -> new CredentialEvaluation(
                            availableExternal(config),
                            Optional.of(value),
                            List.of()))
                    .orElseGet(() -> new CredentialEvaluation(
                            missingExternal(config),
                            Optional.empty(),
                            List.of()));
        } catch (RuntimeException exception) {
            return new CredentialEvaluation(
                    failedExternal(config, exception),
                    Optional.empty(),
                    List.of());
        }
    }

    private static WayangReadinessProfileS3CredentialsResolution availableExternal(
            WayangObjectStorageConfig config) {
        return new WayangReadinessProfileS3CredentialsResolution(
                credentialsRef(config),
                provider(config),
                "external",
                "resolver",
                true,
                List.of("external"),
                "S3 readiness profile credentials resolved by external resolver.");
    }

    private static WayangReadinessProfileS3CredentialsResolution missingExternal(
            WayangObjectStorageConfig config) {
        return new WayangReadinessProfileS3CredentialsResolution(
                credentialsRef(config),
                provider(config),
                "",
                "resolver",
                false,
                List.of(),
                "External S3 readiness profile credential resolver did not return credentials.");
    }

    private static WayangReadinessProfileS3CredentialsResolution failedExternal(
            WayangObjectStorageConfig config,
            RuntimeException exception) {
        return new WayangReadinessProfileS3CredentialsResolution(
                credentialsRef(config),
                provider(config),
                "",
                "resolver",
                false,
                List.of(),
                "External S3 readiness profile credential resolver failed: "
                        + exception.getMessage());
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

    private record CredentialEvaluation(
            WayangReadinessProfileS3CredentialsResolution resolution,
            Optional<WayangReadinessProfileS3Credentials> credentials,
            List<Map<String, Object>> sourceDiagnostics) {

        private CredentialEvaluation {
            credentials = credentials == null ? Optional.empty() : credentials;
            sourceDiagnostics = sourceDiagnostics == null ? List.of() : List.copyOf(sourceDiagnostics);
        }
    }
}
