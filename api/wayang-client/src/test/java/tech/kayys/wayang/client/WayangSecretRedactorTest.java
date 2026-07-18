package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for shared diagnostic secret redaction.
 */
class WayangSecretRedactorTest {

    @Test
    void redactsNestedDiagnosticMapsAndLists() {
        Map<String, Object> redacted = WayangSecretRedactor.diagnosticMap(Map.of(
                "databaseUrl", "jdbc:postgresql://ops:inline-password@localhost:5432/wayang",
                "objectStorage", Map.of(
                        "credentialsRef", "accessKeyId=inline-access secretAccessKey=inline-secret"),
                "providers", List.of(
                        Map.of("message", "token=inline-token"),
                        "apiKey:inline-api-key")));

        String output = redacted.toString();

        assertThat(output)
                .contains("<redacted>")
                .doesNotContain("inline-password")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-token")
                .doesNotContain("inline-api-key");
    }
}
