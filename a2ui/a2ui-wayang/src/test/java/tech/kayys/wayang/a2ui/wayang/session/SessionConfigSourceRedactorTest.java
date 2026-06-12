package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionConfigSourceRedactorTest {

    @Test
    void masksCredentialAndEndpointFieldsWithoutHidingOperationalLocation() {
        Map<String, Object> raw = Map.of(
                "type", "s3",
                "bucket", "wayang-config",
                "key", "tenants/tenant-a/a2ui-session.json",
                "endpointUrl", "https://object-store.internal",
                "accessKeyId", "AKIA_TEST",
                "secretAccessKey", "secret",
                "sessionToken", "token");

        Map<String, Object> redacted = SessionConfigSourceRedactor.redact(raw);

        assertThat(redacted)
                .containsEntry("type", "s3")
                .containsEntry("bucket", "wayang-config")
                .containsEntry("key", "tenants/tenant-a/a2ui-session.json")
                .containsEntry("endpointUrl", SessionConfigSourceRedactor.REDACTION_MARKER)
                .containsEntry("accessKeyId", SessionConfigSourceRedactor.REDACTION_MARKER)
                .containsEntry("secretAccessKey", SessionConfigSourceRedactor.REDACTION_MARKER)
                .containsEntry("sessionToken", SessionConfigSourceRedactor.REDACTION_MARKER);
    }

    @Test
    void masksNestedMapsAndFallbackSourceLists() {
        Map<String, Object> redacted = SessionConfigSourceRedactor.redact(Map.of(
                "type", "fallback",
                "sources", List.of(
                        Map.of(
                                "type", "database",
                                "tenantId", "tenant-a",
                                "password", "database-password",
                                "headers", Map.of("Authorization", "Bearer token")),
                        Map.of(
                                "type", "s3",
                                "bucket", "wayang-config",
                                "objectKey", "tenant-a/session.json",
                                "metadata", List.of(Map.of("api-key", "api-secret"))))));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) redacted.get("sources");
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) sources.get(0).get("headers");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metadata = (List<Map<String, Object>>) sources.get(1).get("metadata");

        assertThat(sources.get(0))
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("password", SessionConfigSourceRedactor.REDACTION_MARKER);
        assertThat(headers).containsEntry("Authorization", SessionConfigSourceRedactor.REDACTION_MARKER);
        assertThat(sources.get(1)).containsEntry("objectKey", "tenant-a/session.json");
        assertThat(metadata.get(0)).containsEntry("api-key", SessionConfigSourceRedactor.REDACTION_MARKER);
    }

    @Test
    void recognizesCommonSensitiveKeyStyles() {
        assertThat(SessionConfigSourceRedactor.sensitiveKey("x-api-key")).isTrue();
        assertThat(SessionConfigSourceRedactor.sensitiveKey("client_secret")).isTrue();
        assertThat(SessionConfigSourceRedactor.sensitiveKey("baseUrl")).isTrue();
        assertThat(SessionConfigSourceRedactor.sensitiveKey("lookupKey")).isFalse();
        assertThat(SessionConfigSourceRedactor.sensitiveKey("objectKey")).isFalse();
    }
}
