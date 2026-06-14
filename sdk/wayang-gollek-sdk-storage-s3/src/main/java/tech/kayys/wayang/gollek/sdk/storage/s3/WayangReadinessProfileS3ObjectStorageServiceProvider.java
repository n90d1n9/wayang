package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceProvider;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceRegistration;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;

/**
 * Provides S3-compatible object-storage services for readiness profile discovery.
 *
 * <p>The provider reads credential material from operator-configurable
 * environment variables or system properties, initializes an S3-compatible
 * service, and registers it with the neutral object-storage bridge.</p>
 */
public final class WayangReadinessProfileS3ObjectStorageServiceProvider
        implements WayangReadinessProfileObjectStorageServiceProvider {

    private final Map<String, String> environment;
    private final Map<?, ?> properties;
    private final BiFunction<
            WayangObjectStorageConfig,
            WayangReadinessProfileS3Credentials,
            ObjectStorageService> serviceFactory;

    public WayangReadinessProfileS3ObjectStorageServiceProvider() {
        this(
                System.getenv(),
                System.getProperties(),
                WayangReadinessProfileS3ObjectStorageReaderFactory::service);
    }

    WayangReadinessProfileS3ObjectStorageServiceProvider(
            Map<String, String> environment,
            Map<?, ?> properties,
            BiFunction<
                    WayangObjectStorageConfig,
                    WayangReadinessProfileS3Credentials,
                    ObjectStorageService> serviceFactory) {
        this.environment = environment == null ? Map.of() : Map.copyOf(environment);
        this.properties = properties == null ? Map.of() : copy(properties);
        this.serviceFactory = serviceFactory == null
                ? WayangReadinessProfileS3ObjectStorageReaderFactory::service
                : serviceFactory;
    }

    @Override
    public String providerId() {
        return "wayang-readiness-profile-s3-object-storage";
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public List<WayangReadinessProfileObjectStorageServiceRegistration> services(
            WayangGollekSdkConfig config) {
        return evaluate(config).services();
    }

    public WayangReadinessProfileS3ObjectStorageServiceProviderReport report(
            WayangGollekSdkConfig config) {
        return evaluate(config).report();
    }

    @Override
    public Map<String, Object> diagnostics(WayangGollekSdkConfig config) {
        return report(config).toMap();
    }

    private ServiceEvaluation evaluate(WayangGollekSdkConfig config) {
        WayangObjectStorageConfig objectStorage = objectStorage(config);
        boolean objectConfigured = readinessProfileObjectConfigured(objectStorage);
        boolean supportedProvider = s3Compatible(objectStorage.provider());
        WayangReadinessProfileS3CredentialsRegistry credentialsRegistry =
                credentialsRegistry(objectStorage);
        WayangReadinessProfileS3CredentialsResolution credentialsResolution =
                credentialsRegistry.resolveReport(objectStorage);
        if (!objectConfigured || !supportedProvider || !credentialsResolution.available()) {
            return ServiceEvaluation.unavailable(WayangReadinessProfileS3ObjectStorageServiceProviderReport.from(
                    objectStorage,
                    supportedProvider,
                    objectConfigured,
                    credentialsResolution,
                    credentialsRegistry.credentialSourceDiagnostics(),
                    false,
                    serviceId(objectStorage),
                    null));
        }
        try {
            WayangReadinessProfileObjectStorageServiceRegistration registration =
                    WayangReadinessProfileObjectStorageServiceRegistration.of(
                    serviceId(objectStorage),
                    serviceFactory.apply(
                            objectStorage,
                            credentialsRegistry.find(credentialsResolution.selectedCredentialsId()).orElseThrow()));
            return new ServiceEvaluation(
                    List.of(registration),
                    WayangReadinessProfileS3ObjectStorageServiceProviderReport.from(
                            objectStorage,
                            supportedProvider,
                            objectConfigured,
                            credentialsResolution,
                            credentialsRegistry.credentialSourceDiagnostics(),
                            true,
                            registration.serviceId(),
                            null));
        } catch (RuntimeException exception) {
            return ServiceEvaluation.unavailable(WayangReadinessProfileS3ObjectStorageServiceProviderReport.from(
                    objectStorage,
                    supportedProvider,
                    objectConfigured,
                    credentialsResolution,
                    credentialsRegistry.credentialSourceDiagnostics(),
                    false,
                    serviceId(objectStorage),
                    exception));
        }
    }

    private WayangReadinessProfileS3CredentialsRegistry credentialsRegistry(
            WayangObjectStorageConfig objectStorage) {
        List<WayangReadinessProfileS3CredentialSource> sources = new ArrayList<>();
        addSources(sources, objectStorage.credentialsRef());
        addSources(sources, objectStorage.provider());
        addSources(sources, WayangReadinessProfileS3CredentialsRegistry.DEFAULT_CREDENTIALS_ID);
        return WayangReadinessProfileS3CredentialsRegistry.fromSources(sources);
    }

    private void addSources(
            List<WayangReadinessProfileS3CredentialSource> sources,
            String credentialsId) {
        String id = trimToEmpty(credentialsId);
        if (id.isBlank()) {
            return;
        }
        sources.add(WayangReadinessProfileS3CredentialSource.fromEnvironment(id, environment));
        sources.add(WayangReadinessProfileS3CredentialSource.fromMap(id, properties));
    }

    private static WayangObjectStorageConfig objectStorage(WayangGollekSdkConfig config) {
        return config == null
                ? WayangObjectStorageConfig.none()
                : config.readinessProfileRegistry().objectStorage();
    }

    private static boolean s3Compatible(String provider) {
        return switch (trimToEmpty(provider).toLowerCase(Locale.ROOT)) {
            case "s3", "aws-s3", "rustfs", "minio" -> true;
            default -> false;
        };
    }

    private static boolean readinessProfileObjectConfigured(WayangObjectStorageConfig objectStorage) {
        return objectStorage.configured()
                && !objectStorage.bucket().isBlank()
                && !objectStorage.keyPrefix().isBlank();
    }

    private static String serviceId(WayangObjectStorageConfig objectStorage) {
        if (!objectStorage.credentialsRef().isBlank()) {
            return objectStorage.credentialsRef();
        }
        if (!objectStorage.provider().isBlank()) {
            return objectStorage.provider();
        }
        return WayangReadinessProfileS3CredentialsRegistry.DEFAULT_CREDENTIALS_ID;
    }

    private static Map<?, ?> copy(Map<?, ?> values) {
        if (values instanceof Properties properties) {
            return Map.copyOf(properties);
        }
        return Map.copyOf(values);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record ServiceEvaluation(
            List<WayangReadinessProfileObjectStorageServiceRegistration> services,
            WayangReadinessProfileS3ObjectStorageServiceProviderReport report) {

        private ServiceEvaluation {
            services = services == null || services.isEmpty() ? List.of() : List.copyOf(services);
        }

        private static ServiceEvaluation unavailable(
                WayangReadinessProfileS3ObjectStorageServiceProviderReport report) {
            return new ServiceEvaluation(List.of(), report);
        }
    }
}
