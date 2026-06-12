package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcErrorTest {

    @Test
    void preservesVersionErrorMetadataOrder() {
        WayangA2aJsonRpcError error = WayangA2aJsonRpcError.versionNotSupported("0.5");

        assertThat(error.toMap().keySet()).containsExactly("code", "message", "data");
        assertThat(metadata(error).keySet()).containsExactly(
                "requestedVersion",
                "supportedVersions",
                "timestamp");
        assertThat(metadata(error))
                .containsEntry("requestedVersion", "0.5")
                .containsEntry("supportedVersions", List.of(A2aProtocol.VERSION));
    }

    @Test
    void preservesExtensionErrorMetadataOrder() {
        WayangA2aJsonRpcError error = WayangA2aJsonRpcError.extensionSupportRequired(
                List.of("ext-a"),
                List.of("ext-a", "ext-b"),
                List.of("ext-b"));

        assertThat(metadata(error).keySet()).containsExactly(
                "missingExtensions",
                "requiredExtensions",
                "providedExtensions",
                "timestamp");
        assertThat(metadata(error))
                .containsEntry("missingExtensions", List.of("ext-a"))
                .containsEntry("requiredExtensions", List.of("ext-a", "ext-b"))
                .containsEntry("providedExtensions", List.of("ext-b"));
    }

    @Test
    void preservesAuthenticationErrorMetadataOrder() {
        WayangA2aJsonRpcError error = WayangA2aJsonRpcError.authenticationRequired("auth required");

        assertThat(metadata(error).keySet()).containsExactly("scheme", "timestamp");
        assertThat(metadata(error)).containsEntry("scheme", "Bearer");
    }

    private static Map<String, Object> metadata(WayangA2aJsonRpcError error) {
        Map<String, Object> detail = error.data().getFirst();
        return WayangA2aMaps.copyMap((Map<?, ?>) detail.get("metadata"));
    }
}
