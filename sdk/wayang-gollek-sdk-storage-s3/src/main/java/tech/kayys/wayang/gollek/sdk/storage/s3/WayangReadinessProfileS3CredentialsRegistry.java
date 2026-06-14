package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable credential registry for S3-compatible readiness profile storage.
 */
public final class WayangReadinessProfileS3CredentialsRegistry
        implements WayangReadinessProfileS3CredentialsResolver {

    public static final String DEFAULT_CREDENTIALS_ID = "default";

    private final Map<String, WayangReadinessProfileS3Credentials> credentials;
    private final List<WayangReadinessProfileS3CredentialSource> credentialSources;

    private WayangReadinessProfileS3CredentialsRegistry(
            Map<String, WayangReadinessProfileS3Credentials> credentials,
            List<WayangReadinessProfileS3CredentialSource> credentialSources) {
        this.credentials = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(credentials));
        this.credentialSources = credentialSources == null || credentialSources.isEmpty()
                ? List.of()
                : List.copyOf(credentialSources);
    }

    public static WayangReadinessProfileS3CredentialsRegistry empty() {
        return new WayangReadinessProfileS3CredentialsRegistry(Map.of(), List.of());
    }

    public static WayangReadinessProfileS3CredentialsRegistry ofDefault(
            WayangReadinessProfileS3Credentials credentials) {
        return builder().register(DEFAULT_CREDENTIALS_ID, credentials).build();
    }

    public static WayangReadinessProfileS3CredentialsRegistry fromSources(
            List<WayangReadinessProfileS3CredentialSource> credentialSources) {
        return builder().registerAvailable(credentialSources).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<WayangReadinessProfileS3Credentials> resolve(WayangObjectStorageConfig config) {
        WayangReadinessProfileS3CredentialsResolution resolution = resolveReport(config);
        return resolution.available() ? find(resolution.selectedCredentialsId()) : Optional.empty();
    }

    public WayangReadinessProfileS3CredentialsResolution resolveReport(
            WayangObjectStorageConfig config) {
        String credentialsRef = config == null ? "" : normalize(config.credentialsRef());
        String provider = config == null ? "" : normalize(config.provider());
        if (config != null) {
            Optional<WayangReadinessProfileS3Credentials> refCredentials = find(config.credentialsRef());
            if (refCredentials.isPresent()) {
                return available(credentialsRef, provider, credentialsRef, "credentialsRef");
            }
            Optional<WayangReadinessProfileS3Credentials> providerCredentials = find(config.provider());
            if (providerCredentials.isPresent()) {
                return available(credentialsRef, provider, provider, "provider");
            }
        }
        if (find(DEFAULT_CREDENTIALS_ID).isPresent()) {
            return available(credentialsRef, provider, DEFAULT_CREDENTIALS_ID, "default");
        }
        return new WayangReadinessProfileS3CredentialsResolution(
                credentialsRef,
                provider,
                "",
                "none",
                false,
                credentialsIds(),
                "No S3 readiness profile credentials matched the configured credentialsRef, provider, or default.");
    }

    public Optional<WayangReadinessProfileS3Credentials> find(String credentialsId) {
        return Optional.ofNullable(credentials.get(normalize(credentialsId)));
    }

    public List<String> credentialsIds() {
        return List.copyOf(credentials.keySet());
    }

    public int size() {
        return credentials.size();
    }

    public List<Map<String, Object>> credentialSourceDiagnostics() {
        return credentialSources.stream()
                .map(WayangReadinessProfileS3CredentialSource::toMap)
                .toList();
    }

    private WayangReadinessProfileS3CredentialsResolution available(
            String credentialsRef,
            String provider,
            String credentialsId,
            String selectedBy) {
        return new WayangReadinessProfileS3CredentialsResolution(
                credentialsRef,
                provider,
                credentialsId,
                selectedBy,
                true,
                credentialsIds(),
                "S3 readiness profile credentials selected by " + selectedBy + ".");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Builder for immutable S3-compatible readiness profile credential registries.
     */
    public static final class Builder {

        private final Map<String, WayangReadinessProfileS3Credentials> credentials = new LinkedHashMap<>();
        private final List<WayangReadinessProfileS3CredentialSource> credentialSources =
                new java.util.ArrayList<>();

        public Builder register(
                String credentialsId,
                WayangReadinessProfileS3Credentials value) {
            String id = normalize(credentialsId);
            if (id.isBlank()) {
                throw new IllegalArgumentException("S3 readiness profile credentials id is required.");
            }
            credentials.put(id, Objects.requireNonNull(value, "value"));
            return this;
        }

        public Builder registerAvailable(WayangReadinessProfileS3CredentialSource source) {
            WayangReadinessProfileS3CredentialSource resolved = Objects.requireNonNull(source, "source");
            credentialSources.add(resolved);
            resolved.credentials().ifPresent(value -> register(resolved.credentialsId(), value));
            return this;
        }

        public Builder registerAvailable(List<WayangReadinessProfileS3CredentialSource> sources) {
            if (sources == null || sources.isEmpty()) {
                return this;
            }
            sources.forEach(this::registerAvailable);
            return this;
        }

        public WayangReadinessProfileS3CredentialsRegistry build() {
            return new WayangReadinessProfileS3CredentialsRegistry(credentials, credentialSources);
        }
    }
}
