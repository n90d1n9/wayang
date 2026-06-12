package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionConfigSourceCapabilityTest {

    @Test
    void describesLookupProviderRequirementsAndAliases() {
        SessionConfigSourceCapability capability =
                SessionConfigSourceCapability.lookup("database", true, false);

        assertThat(capability.category()).isEqualTo(SessionConfigSourceCapability.CATEGORY_LOOKUP);
        assertThat(capability.tenantScoped()).isTrue();
        assertThat(capability.lookupKeyScoped()).isTrue();
        assertThat(capability.requiresTenant()).isTrue();
        assertThat(capability.requiresLookupKey()).isFalse();
        assertThat(capability.fieldAliases())
                .containsEntry("tenantId", List.of("tenant", "realm"))
                .containsEntry("key", List.of("configKey", "profile", "name", "id"));
        assertThat(capability.diagnosticSafeFields())
                .contains("type", "tenantId", "tenant", "realm", "key", "profile");
    }

    @Test
    void describesObjectStorageProviderRequirements() {
        SessionConfigSourceCapability capability = SessionConfigSourceCapability.objectStorage("s3");

        assertThat(capability.category()).isEqualTo(SessionConfigSourceCapability.CATEGORY_OBJECT_STORAGE);
        assertThat(capability.objectStorage()).isTrue();
        assertThat(capability.requiresBucketKey()).isTrue();
        assertThat(capability.fieldAliases())
                .containsEntry("bucket", List.of("container"))
                .containsEntry("key", List.of("objectKey", "path"));
        assertThat(capability.toMap())
                .containsEntry("type", "s3")
                .containsEntry("objectStorage", true)
                .containsEntry("fallback", false);
    }

    @Test
    void describesFallbackSourceCapability() {
        SessionConfigSourceCapability capability = SessionConfigSourceCapability.fallback("fallback");

        assertThat(capability.supportsFallback()).isTrue();
        assertThat(capability.requiredFields()).containsExactly("sources");
        assertThat(capability.toMap())
                .containsEntry("category", SessionConfigSourceCapability.CATEGORY_FALLBACK)
                .containsEntry("fallback", true);
    }

    @Test
    void normalizesCollectionsForStableDiagnosticOutput() {
        SessionConfigSourceCapability capability = new SessionConfigSourceCapability(
                "custom",
                " custom-kind ",
                List.of(" key ", "key", "tenantId", " "),
                Map.of(" key ", List.of(" alias ", "alias")),
                List.of("type", "type", "tenantId"),
                true,
                true,
                false,
                false);

        assertThat(capability.requiredFields()).containsExactly("key", "tenantId");
        assertThat(capability.fieldAliases()).containsEntry("key", List.of("alias"));
        assertThat(capability.diagnosticSafeFields()).containsExactly("type", "tenantId");
    }
}
