package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SessionConfigSourceDiagnosticsTest {

    @Test
    void reportsLoadedFallbackChainWithRedactedSourceAndCapabilityMetadata() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database(
                        (tenantId, lookupKey, values) -> Optional.empty()))
                .register("s3", SessionConfigObjectStorageProvider.s3(
                        (bucket, key, values) -> Optional.of("""
                                {
                                  "mode": "read-only"
                                }
                                """)))
                .policy(SessionConfigSourcePolicy.allowOnly("fallback", "database", "s3", "file"))
                .build();
        Map<String, Object> s3Values = new LinkedHashMap<>();
        s3Values.put("bucket", "wayang-config");
        s3Values.put("key", "tenants/tenant-a/a2ui-session.json");
        s3Values.put("endpointUrl", "https://object-store.internal");
        s3Values.put("accessKeyId", "access-key");
        s3Values.put("secretAccessKey", "secret-key");
        SessionConfigSourceSpec fallback = SessionConfigSourceSpec.firstAvailable(
                SessionConfigSourceSpecs.database("tenant-a"),
                SessionConfigSourceSpec.provider("s3", s3Values));

        SessionConfigSourceDiagnostics diagnostics = SessionConfigSourceDiagnostics.load(fallback, registry);

        assertThat(diagnostics.valid()).isTrue();
        assertThat(diagnostics.allowed()).isTrue();
        assertThat(diagnostics.loaded()).isTrue();
        assertThat(diagnostics.status()).isEqualTo(SessionConfigLoadStatus.LOADED);
        assertThat(diagnostics.validationErrors()).isEmpty();
        assertThat(diagnostics.policyErrors()).isEmpty();
        assertThat(diagnostics.capability())
                .containsEntry("category", SessionConfigSourceCapability.CATEGORY_FALLBACK)
                .containsEntry("fallback", true);
        assertThat(diagnostics.sourceCapabilities()).containsKeys("database", "s3", "fallback");
        assertThat(diagnostics.loadResult().attempts())
                .extracting(SessionConfigLoadAttempt::status)
                .containsExactly(SessionConfigLoadStatus.MISSING, SessionConfigLoadStatus.LOADED);

        Map<String, Object> redactedS3 = nestedSource(diagnostics.sourceSpec(), 1);
        assertThat(redactedS3)
                .containsEntry("bucket", "wayang-config")
                .containsEntry("key", "tenants/tenant-a/a2ui-session.json")
                .containsEntry("endpointUrl", SessionConfigSourceRedactor.REDACTION_MARKER)
                .containsEntry("accessKeyId", SessionConfigSourceRedactor.REDACTION_MARKER)
                .containsEntry("secretAccessKey", SessionConfigSourceRedactor.REDACTION_MARKER);

        assertThat(diagnostics.toMap())
                .containsEntry("diagnosticsId", SessionConfigSourceDiagnostics.DIAGNOSTICS_ID)
                .containsEntry("sourceType", SessionConfigSourceSpec.TYPE_FALLBACK)
                .containsEntry("status", SessionConfigLoadStatus.LOADED.name())
                .containsEntry("loaded", true)
                .containsEntry("missing", false)
                .containsEntry("failed", false);
        assertThat(diagnostics.toJson()).contains(SessionConfigSourceDiagnostics.DIAGNOSTICS_ID);
        assertThat(SessionConfigSourceDiagnostics.fromMap(diagnostics.toMap())).isEqualTo(diagnostics);
        assertThat(SessionConfigSourceDiagnostics.fromJson(diagnostics.toJson())).isEqualTo(diagnostics);
        assertThat(SessionConfigSourceDiagnosticsDecoder.fromMap(diagnostics.toMap())).isEqualTo(diagnostics);
    }

    @Test
    void reportsPolicyRejectionWithoutResolvingTheSource() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .policy(SessionConfigSourcePolicy.deny("inline"))
                .build();

        SessionConfigSourceDiagnostics diagnostics = SessionConfigSourceDiagnostics.load(
                SessionConfigSourceSpec.inlineJson("{}"),
                registry);

        assertThat(diagnostics.valid()).isTrue();
        assertThat(diagnostics.allowed()).isFalse();
        assertThat(diagnostics.failed()).isTrue();
        assertThat(diagnostics.status()).isEqualTo(SessionConfigLoadStatus.FAILED);
        assertThat(diagnostics.sourceType()).isEqualTo(SessionConfigSourceSpec.TYPE_INLINE);
        assertThat(diagnostics.policyErrors())
                .containsExactly("source source type inline is not allowed");
        assertThat(diagnostics.loadResult().sourceDescription()).isEqualTo("session-config-source-policy");
    }

    @Test
    void reportsInvalidSpecWithoutRunningPolicyOrProviderResolution() {
        SessionConfigSourceDiagnostics diagnostics = SessionConfigSourceDiagnostics.load(
                Map.of("type", "file"),
                SessionConfigSourceRegistry.standard());

        assertThat(diagnostics.valid()).isFalse();
        assertThat(diagnostics.allowed()).isTrue();
        assertThat(diagnostics.failed()).isTrue();
        assertThat(diagnostics.status()).isEqualTo(SessionConfigLoadStatus.FAILED);
        assertThat(diagnostics.validationErrors())
                .containsExactly("source file source requires path");
        assertThat(diagnostics.policyErrors()).isEmpty();
        assertThat(diagnostics.loadResult().sourceDescription()).isEqualTo("session-config-source-spec");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedSource(Map<String, Object> sourceSpec, int index) {
        Object sources = sourceSpec.get(SessionConfigSourceRegistry.KEY_SOURCES);
        assertThat(sources).isInstanceOf(List.class);
        Object source = ((List<?>) sources).get(index);
        assertThat(source).isInstanceOf(Map.class);
        return (Map<String, Object>) source;
    }
}
