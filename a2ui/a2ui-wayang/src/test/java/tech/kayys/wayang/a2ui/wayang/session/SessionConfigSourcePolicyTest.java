package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigSourcePolicyTest {

    @Test
    void allowAllPolicyDoesNotRejectAnyStructuredSourceType() {
        SessionConfigSourcePolicy policy = SessionConfigSourcePolicy.allowAll();

        assertThat(policy.allowAllTypes()).isTrue();
        assertThat(policy.validationErrors(Map.of(
                "type", "inline",
                "json", "{}"))).isEmpty();
        assertThat(policy.validationErrors(Map.of(
                "type", "database",
                "tenantId", "tenant-a"))).isEmpty();
    }

    @Test
    void allowOnlyPolicyAcceptsAliasesThroughCanonicalTypes() {
        SessionConfigSourcePolicy policy = SessionConfigSourcePolicy.allowOnly(
                "fallback",
                "database",
                "resource");

        assertThat(policy.allows("chain")).isTrue();
        assertThat(policy.allows("first_available")).isTrue();
        assertThat(policy.allows("classpath")).isTrue();
        assertThat(policy.allows("json")).isFalse();
        assertThat(policy.validationErrors(Map.of(
                "type",
                "chain",
                "sources",
                List.of(
                        Map.of("type", "database", "tenantId", "tenant-a"),
                        Map.of("type", "resource", "resource", "a2ui/session-config-readonly.json")))))
                .isEmpty();
    }

    @Test
    void allowOnlyPolicyReportsNestedRejectedSources() {
        SessionConfigSourcePolicy policy = SessionConfigSourcePolicy.allowOnly(
                "fallback",
                "database",
                "s3",
                "file");

        assertThat(policy.validationErrors(Map.of(
                "type",
                "fallback",
                "sources",
                List.of(
                        Map.of("type", "database", "tenantId", "tenant-a"),
                        Map.of("type", "inline", "json", "{}")))))
                .containsExactly("source.sources[1] source type inline is not allowed");
    }

    @Test
    void denyPolicyRejectsDeniedTypesEvenWhenAllOthersAreAllowed() {
        SessionConfigSourcePolicy policy = SessionConfigSourcePolicy.deny("inline", "file");

        assertThat(policy.allows("database")).isTrue();
        assertThat(policy.allows("json")).isFalse();
        assertThatThrownBy(() -> policy.requireAllowed(Map.of(
                "type", "file",
                "path", "/etc/wayang/a2ui-session.json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source type file is not allowed");
    }

    @Test
    void exportsStablePolicyMap() {
        SessionConfigSourcePolicy policy = new SessionConfigSourcePolicy(
                new java.util.LinkedHashSet<>(List.of("json", "database")),
                new java.util.LinkedHashSet<>(List.of("file")));

        assertThat(policy.toMap())
                .containsEntry("allowedTypes", List.of("inline", "database"))
                .containsEntry("deniedTypes", List.of("file"))
                .containsEntry("allowAll", false);
    }
}
