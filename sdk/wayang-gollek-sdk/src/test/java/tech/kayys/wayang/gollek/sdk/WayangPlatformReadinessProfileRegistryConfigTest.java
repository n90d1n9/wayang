package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangPlatformReadinessProfileRegistryConfigTest {

    @Test
    void normalizesRegistryModeAliases() {
        assertThat(WayangPlatformReadinessProfileRegistryMode.from("builtin"))
                .isEqualTo(WayangPlatformReadinessProfileRegistryMode.BUILTIN);
        assertThat(WayangPlatformReadinessProfileRegistryMode.from("filesystem"))
                .isEqualTo(WayangPlatformReadinessProfileRegistryMode.FILE);
        assertThat(WayangPlatformReadinessProfileRegistryMode.from("postgres"))
                .isEqualTo(WayangPlatformReadinessProfileRegistryMode.DATABASE);
        assertThat(WayangPlatformReadinessProfileRegistryMode.from("rustfs"))
                .isEqualTo(WayangPlatformReadinessProfileRegistryMode.OBJECT_STORAGE);
        assertThat(WayangPlatformReadinessProfileRegistryMode.from("file-with-fallback"))
                .isEqualTo(WayangPlatformReadinessProfileRegistryMode.HYBRID);
    }

    @Test
    void parsesFileRegistryConfigWithPolicyAndFallback() {
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "file-with-fallback",
                        "path", "profiles.properties",
                        "validationPolicy", "relaxed"));

        assertThat(config.mode()).isEqualTo(WayangPlatformReadinessProfileRegistryMode.HYBRID);
        assertThat(config.filePath()).isEqualTo("profiles.properties");
        assertThat(config.fallbackToBuiltIn()).isTrue();
        assertThat(config.validationPolicy().policyId()).isEqualTo("relaxed");
        assertThat(config.toMap())
                .containsEntry("mode", "hybrid")
                .containsEntry("filePath", "profiles.properties")
                .containsEntry("fallbackToBuiltIn", true)
                .containsEntry("validationPolicyId", "relaxed");
    }

    @Test
    void validatesRegistryConfigRequiredFieldsAndPolicyId() {
        WayangPlatformReadinessProfileRegistryConfig file =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "file",
                        "validationPolicy", "future"));
        WayangPlatformReadinessProfileRegistryConfig object =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs"));

        assertThat(file.diagnostics().valid()).isFalse();
        assertThat(file.diagnostics().issues())
                .extracting(WayangPlatformReadinessProfileRegistryConfigIssue::code)
                .containsExactly(
                        "readiness_profile_file_path_required",
                        "readiness_profile_validation_policy_unknown");
        assertThat(object.diagnostics().valid()).isFalse();
        assertThat(object.diagnostics().issues())
                .extracting(WayangPlatformReadinessProfileRegistryConfigIssue::code)
                .containsExactly(
                        "readiness_profile_object_bucket_required",
                        "readiness_profile_object_endpoint_required",
                        "readiness_profile_object_key_required");
    }

    @Test
    void parsesObjectStorageRegistryObjectKeyAliases() {
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "s3",
                        "bucket", "wayang",
                        "objectKey", "profiles/default.properties"));

        assertThat(config.mode()).isEqualTo(WayangPlatformReadinessProfileRegistryMode.OBJECT_STORAGE);
        assertThat(config.objectStorage().provider()).isEqualTo("s3");
        assertThat(config.objectStorage().keyPrefix()).isEqualTo("profiles/default.properties");
        assertThat(config.diagnostics().valid()).isTrue();
    }

    @Test
    void objectStorageRegistryConfigReportsUnavailableProviderWithFallback() {
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", "profiles/default",
                        "fallbackToBuiltIn", true));

        WayangPlatformReadinessProfileRegistryResolution resolution = config.registry().resolve();

        assertThat(config.mode()).isEqualTo(WayangPlatformReadinessProfileRegistryMode.OBJECT_STORAGE);
        assertThat(config.objectStorage().provider()).isEqualTo("rustfs");
        assertThat(config.objectStorage().pathStyleAccess()).isTrue();
        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isTrue();
        assertThat(resolution.activeSourceId()).isEqualTo("builtin");
        assertThat(resolution.sources())
                .hasSize(2)
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("rustfs");
                    assertThat(source.sourceType()).isEqualTo("object_storage");
                    assertThat(source.location()).isEqualTo("rustfs://wayang/profiles/default");
                    assertThat(source.available()).isFalse();
                    assertThat(source.valid()).isFalse();
                    assertThat(source.message()).contains("loader is not wired");
                })
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("builtin");
                    assertThat(source.selected()).isTrue();
                    assertThat(source.valid()).isTrue();
                });
    }

    @Test
    void objectStorageRegistryCanLoadProfilesFromReader() {
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", "profiles/default.properties",
                        "validationPolicy", "strict"));

        WayangPlatformReadinessProfileRegistryResolution resolution = config.registry(
                storage -> objectStorageProfileDocument()).resolve();

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.activeSourceId()).isEqualTo("rustfs");
        assertThat(resolution.activeSourceType()).isEqualTo("object_storage");
        assertThat(resolution.activeSourceLocation()).isEqualTo("rustfs://wayang/profiles/default.properties");
        assertThat(resolution.profiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("object-default", "object-production");
        assertThat(resolution.sources())
                .singleElement()
                .satisfies(source -> {
                    assertThat(source.selected()).isTrue();
                    assertThat(source.available()).isTrue();
                    assertThat(source.valid()).isTrue();
                    assertThat(source.message()).isEqualTo("Object-storage readiness profile loaded.");
                });
    }

    @Test
    void objectStorageRegistryFallsBackWhenReaderFails() {
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "minio",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", "profiles/default.properties",
                        "fallbackToBuiltIn", true));

        WayangPlatformReadinessProfileRegistryResolution resolution = config.registry(storage -> {
            throw new IllegalStateException("cannot reach bucket");
        }).resolve();

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isTrue();
        assertThat(resolution.activeSourceId()).isEqualTo("builtin");
        assertThat(resolution.sources())
                .hasSize(2)
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("minio");
                    assertThat(source.sourceType()).isEqualTo("object_storage");
                    assertThat(source.location()).isEqualTo("minio://wayang/profiles/default.properties");
                    assertThat(source.available()).isFalse();
                    assertThat(source.message()).contains("cannot reach bucket");
                })
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("builtin");
                    assertThat(source.selected()).isTrue();
                    assertThat(source.valid()).isTrue();
                });
    }

    @Test
    void databaseRegistryReportsUnavailableWithoutReaderAndRedactsLocation() {
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl",
                        "jdbc:postgresql://localhost:5432/wayang?user=ops&password=secret",
                        "fallbackToBuiltIn", true));

        WayangPlatformReadinessProfileRegistryResolution resolution = config.registry().resolve();

        assertThat(config.mode()).isEqualTo(WayangPlatformReadinessProfileRegistryMode.DATABASE);
        assertThat(config.diagnostics().valid()).isTrue();
        assertThat(WayangPlatformReadinessProfileDatabaseSource.locationOf(config.databaseUrl()))
                .isEqualTo("jdbc:postgresql://localhost:5432/wayang?user=ops&password=<redacted>");
        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isTrue();
        assertThat(resolution.sources())
                .hasSize(2)
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("database");
                    assertThat(source.sourceType()).isEqualTo("database");
                    assertThat(source.location()).doesNotContain("secret");
                    assertThat(source.available()).isFalse();
                    assertThat(source.message()).contains("loader is not wired");
                })
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("builtin");
                    assertThat(source.selected()).isTrue();
                    assertThat(source.valid()).isTrue();
                });
    }

    @Test
    void databaseRegistryConfigProjectionsRedactInlineSecrets() {
        String databaseUrl =
                "jdbc:postgresql://ops:super-secret@localhost:5432/wayang?user=ops&password=top-secret&token=api-secret";
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl", databaseUrl,
                        "fallbackToBuiltIn", true));
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(config));

        assertThat(config.databaseUrl()).contains("top-secret");
        assertThat(String.valueOf(config.toMap()))
                .contains("password=<redacted>")
                .contains("token=<redacted>")
                .contains("ops:<redacted>@localhost")
                .doesNotContain("top-secret")
                .doesNotContain("api-secret")
                .doesNotContain("super-secret");
        assertThat(String.valueOf(config.diagnostics().toMap()))
                .doesNotContain("top-secret")
                .doesNotContain("api-secret")
                .doesNotContain("super-secret");
        assertThat(String.valueOf(sdk.platformReadinessProfileRegistryPreflight().toMap()))
                .doesNotContain("top-secret")
                .doesNotContain("api-secret")
                .doesNotContain("super-secret");
    }

    @Test
    void databaseRegistryFailureDiagnosticsRedactReaderExceptionSecrets() {
        String databaseUrl =
                "jdbc:postgresql://ops:super-secret@localhost:5432/wayang?user=ops&password=top-secret&token=api-secret";
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl", databaseUrl,
                        "fallbackToBuiltIn", false));
        WayangPlatformReadinessProfileExternalReaders readers =
                WayangPlatformReadinessProfileExternalReaders.database(url -> {
                    throw new IOException("password=reader-secret token=reader-token " + url);
                });
        WayangGollekSdk sdk = new LocalWayangGollekSdk(
                WayangGollekSdkConfig.local().withReadinessProfileRegistry(config),
                readers);

        WayangPlatformReadinessProfileRegistryResolution resolution =
                sdk.platformReadinessProfileRegistryResolution();
        WayangPlatformReadinessProfileRegistryPreflightReport preflight =
                sdk.platformReadinessProfileRegistryPreflight();
        WayangReadinessReport readiness =
                WayangPlatformReadinessProfileRegistryReadiness.assess(resolution);
        String output = resolution.toString()
                + preflight.toMap()
                + readiness.toMap();

        assertThat(resolution.valid()).isFalse();
        assertThat(resolution.activeSourceLocation()).contains("<redacted>");
        assertThat(resolution.sources())
                .singleElement()
                .satisfies(source -> assertThat(source.message()).contains("<redacted>"));
        assertThat(output)
                .contains("<redacted>")
                .doesNotContain("super-secret")
                .doesNotContain("top-secret")
                .doesNotContain("api-secret")
                .doesNotContain("reader-secret")
                .doesNotContain("reader-token");
    }

    @Test
    void databaseRegistryCanLoadProfilesFromReader() {
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "postgres",
                        "databaseUrl", "jdbc:postgresql://localhost:5432/wayang",
                        "validationPolicy", "strict"));

        WayangPlatformReadinessProfileRegistryResolution resolution = config.registry(
                WayangPlatformReadinessProfileExternalReaders.database(
                        databaseUrl -> databaseProfileDocument())).resolve();

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.activeSourceId()).isEqualTo("database");
        assertThat(resolution.activeSourceType()).isEqualTo("database");
        assertThat(resolution.activeSourceLocation()).isEqualTo("jdbc:postgresql://localhost:5432/wayang");
        assertThat(resolution.profiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("database-default", "database-production");
        assertThat(resolution.sources())
                .singleElement()
                .satisfies(source -> {
                    assertThat(source.selected()).isTrue();
                    assertThat(source.available()).isTrue();
                    assertThat(source.valid()).isTrue();
                    assertThat(source.message()).isEqualTo("Database readiness profile loaded.");
                });
    }

    @Test
    void hybridRegistryCanUseDatabaseReaderAfterObjectStorageIsAbsent() {
        WayangPlatformReadinessProfileRegistryConfig config =
                WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "hybrid",
                        "databaseUrl", "jdbc:postgresql://localhost:5432/wayang",
                        "validationPolicy", "strict"));

        WayangPlatformReadinessProfileRegistryResolution resolution = config.registry(
                WayangPlatformReadinessProfileExternalReaders.database(
                        databaseUrl -> databaseProfileDocument())).resolve();

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.activeSourceId()).isEqualTo("database");
        assertThat(resolution.profiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("database-default", "database-production");
    }

    @Test
    void localSdkLoadsDatabaseRegistryWithExternalReaders() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl", "jdbc:postgresql://localhost:5432/wayang",
                        "validationPolicy", "strict"))),
                WayangPlatformReadinessProfileExternalReaders.database(
                        databaseUrl -> databaseProfileDocument()));

        WayangReadinessReport report = sdk.platformReadiness();
        WayangPlatformReadinessProfileRegistryResolution resolution =
                sdk.platformReadinessProfileRegistryResolution();

        assertThat(sdk.platformReadinessProfiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("database-default", "database-production");
        assertThat(resolution.activeSourceId()).isEqualTo("database");
        assertThat(resolution.activeSourceType()).isEqualTo("database");
        assertThat(report.ready()).isTrue();
        assertThat(report.attributes())
                .containsEntry("readinessProfileId", "database-default")
                .containsEntry("componentCount", 2);
    }

    @Test
    void localSdkDiscoversDatabaseRegistryReaderProvider() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl", TestReadinessProfileExternalReaderProvider.DATABASE_URL,
                        "validationPolicy", "strict"))));

        WayangPlatformReadinessProfileRegistryResolution resolution =
                sdk.platformReadinessProfileRegistryResolution();

        assertThat(sdk.platformReadinessProfiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("discovered-default", "discovered-production");
        assertThat(resolution.activeSourceId()).isEqualTo("database");
        assertThat(resolution.activeSourceType()).isEqualTo("database");
        assertThat(resolution.activeSourceLocation())
                .isEqualTo(TestReadinessProfileExternalReaderProvider.DATABASE_URL);
    }

    @Test
    void externalReaderProviderDiscoveryReportsRequiredDatabaseReader() {
        WayangGollekSdkConfig config = WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl", TestReadinessProfileExternalReaderProvider.DATABASE_URL,
                        "validationPolicy", "strict")));

        WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport report =
                WayangPlatformReadinessProfileExternalReaderProviders.discoveryReport(config);

        assertThat(report.ready()).isTrue();
        assertThat(report.exitCode()).isZero();
        assertThat(report.requiredReaderTypes()).containsExactly("database");
        assertThat(report.availableReaderTypes()).contains("database");
        assertThat(report.availableProviderIds()).contains("test-readiness-profile-provider");
        assertThat(report.providers())
                .anySatisfy(provider -> {
                    assertThat(provider.providerId()).isEqualTo("test-readiness-profile-provider");
                    assertThat(provider.available()).isTrue();
                    assertThat(provider.readerTypes()).containsExactly("database");
                    assertThat(provider.toMap())
                            .containsEntry("available", true)
                            .containsEntry("readerTypeCount", 1);
                });
        assertThat(report.toMap())
                .containsEntry("ready", true)
                .containsEntry("required", true);
    }

    @Test
    void platformApiRendersExternalReaderProviderDiscoveryEnvelope() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl", TestReadinessProfileExternalReaderProvider.DATABASE_URL,
                        "validationPolicy", "strict"))));
        WayangPlatformApi platform = WayangClient.of(sdk).platform();

        WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport report =
                platform.readinessProfileExternalReaderProviderDiscovery();
        String json = platform.readinessProfileExternalReaderProviderDiscoveryJson(report);

        assertThat(report.ready()).isTrue();
        assertThat(json)
                .contains("\"product\":\"Wayang\"")
                .contains("\"requiredReaderTypes\":[\"database\"]")
                .contains("\"providerId\":\"test-readiness-profile-provider\"");
    }

    @Test
    void registryPreflightFailsWhenRequiredExternalReaderIsMissingWithoutFallback() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl", "jdbc:postgresql://localhost:5432/wayang",
                        "validationPolicy", "strict"))));

        WayangPlatformReadinessProfileRegistryPreflightReport report =
                sdk.platformReadinessProfileRegistryPreflight();

        assertThat(report.ready()).isFalse();
        assertThat(report.exitCode()).isEqualTo(1);
        assertThat(report.providerDiscovery().requiredReaderTypes()).containsExactly("database");
        assertThat(report.providerDiscovery().missingRequiredReaderTypes()).containsExactly("database");
        assertThat(report.issues())
                .extracting(issue -> issue.get("code"))
                .contains(
                        "readiness_profile_external_reader_missing",
                        "readiness_profile_registry_empty");
        assertThat(report.toMap())
                .containsEntry("ready", false)
                .containsEntry("providerDiscoveryRequired", true)
                .containsEntry("providerDiscoveryReady", false);
    }

    @Test
    void registryPreflightWarnsWhenMissingExternalReaderFallsBackToBuiltInProfiles() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", "profiles/default.properties",
                        "fallbackToBuiltIn", true))));
        WayangPlatformApi platform = WayangClient.of(sdk).platform();

        WayangPlatformReadinessProfileRegistryPreflightReport report =
                platform.readinessProfileRegistryPreflight();
        String json = platform.readinessProfileRegistryPreflightJson(report);

        assertThat(report.ready()).isTrue();
        assertThat(report.exitCode()).isZero();
        assertThat(report.warningCount()).isGreaterThanOrEqualTo(2);
        assertThat(report.fallbackUsed()).isTrue();
        assertThat(report.providerDiscovery().missingRequiredReaderTypes()).containsExactly("object_storage");
        assertThat(report.warnings())
                .extracting(warning -> warning.get("code"))
                .contains(
                        "readiness_profile_external_reader_missing_with_fallback",
                        "readiness_profile_registry_fallback_used");
        assertThat(json)
                .contains("\"ready\":true")
                .contains("\"warningCount\":")
                .contains("\"fallbackUsed\":true")
                .contains("\"missingRequiredReaderTypes\":[\"object_storage\"]");
    }

    @Test
    void localSdkPrefersExplicitReaderOverDiscoveredReaderProvider() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl", TestReadinessProfileExternalReaderProvider.DATABASE_URL,
                        "validationPolicy", "strict"))),
                WayangPlatformReadinessProfileExternalReaders.database(
                        ignored -> databaseProfileDocument()));

        assertThat(sdk.platformReadinessProfiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("database-default", "database-production");
    }

    @Test
    void localSdkLoadsObjectStorageRegistryWithReader() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", "profiles/default.properties",
                        "validationPolicy", "strict"))),
                storage -> objectStorageProfileDocument());

        WayangReadinessReport report = sdk.platformReadiness();
        WayangPlatformReadinessProfileRegistryResolution resolution =
                sdk.platformReadinessProfileRegistryResolution();

        assertThat(sdk.platformReadinessProfiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("object-default", "object-production");
        assertThat(resolution.activeSourceId()).isEqualTo("rustfs");
        assertThat(resolution.activeSourceType()).isEqualTo("object_storage");
        assertThat(report.ready()).isTrue();
        assertThat(report.attributes())
                .containsEntry("readinessProfileId", "object-default")
                .containsEntry("componentCount", 2);
    }

    @Test
    void localSdkUsesConfiguredReadinessProfileRegistry(@TempDir Path tempDir) throws Exception {
        Path catalog = tempDir.resolve("readiness-profiles.properties");
        Files.writeString(
                catalog,
                """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=file-default,file-production
                defaultProfileId=file-default
                productionProfileId=file-production
                profile.file-default.description=File default profile.
                profile.file-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.file-production.description=File production profile.
                profile.file-production.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness,wayang.contract.coverage.readiness,wayang.skill-catalog.readiness,wayang.provider-capability.readiness,wayang.standard-alignment.readiness
                """);
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.hybridFile(
                        catalog.toString())));

        WayangReadinessReport report = sdk.platformReadiness();

        assertThat(sdk.platformReadinessProfiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("file-default", "file-production");
        assertThat(sdk.platformReadinessProfileValidation().valid()).isTrue();
        assertThat(report.ready()).isTrue();
        assertThat(report.attributes())
                .containsEntry("readinessProfileId", "file-default")
                .containsEntry("readinessProfileDefault", true)
                .containsEntry("readinessProfileProduction", false)
                .containsEntry("componentCount", 2);
        assertThat(report.probes())
                .extracting(probe -> probe.get("probe"))
                .containsExactly(
                        WayangStorageReadiness.READINESS_ID,
                        WayangContractIntegrityReadiness.READINESS_ID);
    }

    @Test
    void localSdkReturnsBlockedReadinessWhenConfiguredRegistryIsUnavailable(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing-readiness-profiles.properties");
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.file(
                        missing.toString())));

        WayangReadinessReport report = sdk.platformReadiness();

        assertThat(report.readinessId()).isEqualTo(WayangPlatformReadiness.READINESS_ID);
        assertThat(report.ready()).isFalse();
        assertThat(report.exitCode()).isEqualTo(1);
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.attributes())
                .containsEntry("readinessProfileId", "unavailable")
                .containsEntry("readinessProfileRegistryValid", false)
                .containsEntry("readinessProfileRegistryActiveSourceId", "file");
        assertThat(report.attributes().get("componentReadinessIds"))
                .asList()
                .containsExactly(WayangPlatformReadinessProfileRegistryReadiness.READINESS_ID);
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "readiness_profile_source_unavailable")
                        .containsEntry("componentReadinessId",
                                WayangPlatformReadinessProfileRegistryReadiness.READINESS_ID)
                        .containsEntry("sourceId", "file")
                        .containsEntry("sourceType", "file"));
    }

    @Test
    void localSdkReturnsBlockedReadinessWhenRegistryConfigIsInvalid() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "builtin",
                        "validationPolicy", "future"))));

        WayangReadinessReport report = sdk.platformReadiness();

        assertThat(sdk.platformReadinessProfileRegistryConfigDiagnostics().valid()).isFalse();
        assertThat(report.ready()).isFalse();
        assertThat(report.exitCode()).isEqualTo(1);
        assertThat(report.attributes())
                .containsEntry("readinessProfileId", "unavailable")
                .containsEntry("readinessProfileRegistryConfigValid", false)
                .containsEntry("readinessProfileRegistryConfigIssueCount", 1);
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "readiness_profile_validation_policy_unknown")
                        .containsEntry("componentReadinessId",
                                WayangPlatformReadinessProfileRegistryConfigReadiness.READINESS_ID)
                        .containsEntry("field", "validationPolicyId"));
    }

    private static String objectStorageProfileDocument() {
        return """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=object-default,object-production
                defaultProfileId=object-default
                productionProfileId=object-production
                profile.object-default.description=Object default profile.
                profile.object-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.object-production.description=Object production profile.
                profile.object-production.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness,wayang.contract.coverage.readiness,wayang.skill-catalog.readiness,wayang.provider-capability.readiness,wayang.standard-alignment.readiness
                """;
    }

    private static String databaseProfileDocument() {
        return """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=database-default,database-production
                defaultProfileId=database-default
                productionProfileId=database-production
                profile.database-default.description=Database default profile.
                profile.database-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.database-production.description=Database production profile.
                profile.database-production.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness,wayang.contract.coverage.readiness,wayang.skill-catalog.readiness,wayang.provider-capability.readiness,wayang.standard-alignment.readiness
                """;
    }
}
