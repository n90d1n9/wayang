package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigSourceSpecTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsInlineFileClasspathAndProviderSpecs() {
        SessionConfigSourceSpec inline = SessionConfigSourceSpec.inlineJson("""
                {
                  "mode": "read-only"
                }
                """);
        SessionConfigSourceSpec file = SessionConfigSourceSpec.file(tempDir.resolve("session.json"));
        SessionConfigSourceSpec classpath = SessionConfigSourceSpec.classpath("a2ui/session-config-readonly.json");
        SessionConfigSourceSpec database = SessionConfigSourceSpecs.database("tenant-a");

        assertThat(inline.toMap()).containsEntry("type", "inline").containsKey("json");
        assertThat(file.toMap()).containsEntry("type", "file").containsKey("path");
        assertThat(classpath.toMap()).containsEntry("type", "classpath").containsKey("resource");
        assertThat(database.toMap()).containsEntry("type", "database").containsEntry("tenantId", "tenant-a");
    }

    @Test
    void resolvesFallbackSpecThroughRegistry() throws Exception {
        Path fallbackFile = tempDir.resolve("session.json");
        Files.writeString(fallbackFile, """
                {
                  "mode": "run-lifecycle"
                }
                """, StandardCharsets.UTF_8);
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database(
                        (tenantId, lookupKey, values) -> java.util.Optional.empty()))
                .build();

        SessionConfigSourceSpec fallback = SessionConfigSourceSpec.firstAvailable(
                SessionConfigSourceSpec.provider("database", Map.of("tenantId", "tenant-a")),
                SessionConfigSourceSpec.file(fallbackFile));
        WayangA2uiSessionConfig config = fallback.source(registry).loadOrDefault();

        assertThat(fallback.toMap()).containsEntry("type", "fallback").containsKey("sources");
        assertThat(config.actionPolicy().allowedActions())
                .contains(WayangA2uiActions.RUN_WAIT, WayangA2uiActions.RUN_CANCEL);
    }

    @Test
    void validatesBuiltInAndFallbackSpecsBeforeLoading() {
        assertThatThrownBy(() -> SessionConfigSourceSpec.inlineJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inline source requires json");
        assertThatThrownBy(() -> SessionConfigSourceSpec.classpath(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resourceName must not be blank");
        assertThatThrownBy(SessionConfigSourceSpec::firstAvailable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one nested source");
        assertThatThrownBy(() -> new SessionConfigSourceSpec(Map.of(
                "type",
                "fallback",
                "sources",
                java.util.List.of("not-a-source"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a source object");
    }

    @Test
    void exposesValidationErrorsForRawSpecs() {
        assertThat(SessionConfigSourceSpec.validationErrors(Map.of(
                "type",
                "file")))
                .containsExactly("source file source requires path");
        assertThat(SessionConfigSourceSpec.validationErrors(Map.of(
                "type",
                "database",
                "tenantId",
                "tenant-a")))
                .isEmpty();
    }

    @Test
    void exposesRedactedDiagnosticMapWithoutChangingExecutableSpec() {
        SessionConfigSourceSpec spec = SessionConfigSourceSpec.provider("s3", Map.of(
                "bucket", "wayang-config",
                "key", "tenants/tenant-a/a2ui-session.json",
                "endpoint", "https://object-store.internal",
                "accessKey", "access-key"));

        assertThat(spec.toMap())
                .containsEntry("endpoint", "https://object-store.internal")
                .containsEntry("accessKey", "access-key");
        assertThat(spec.toDiagnosticMap())
                .containsEntry("bucket", "wayang-config")
                .containsEntry("key", "tenants/tenant-a/a2ui-session.json")
                .containsEntry("endpoint", SessionConfigSourceRedactor.REDACTION_MARKER)
                .containsEntry("accessKey", SessionConfigSourceRedactor.REDACTION_MARKER);
        assertThat(SessionConfigSourceSpec.redactedMap(spec.toMap()))
                .isEqualTo(spec.toDiagnosticMap());
    }
}
