package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentExtension;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpAccessRejectionsTest {

    private static final String REQUIRED_EXTENSION = "https://wayang.test/extensions/human-approval/v1";

    @Test
    void extensionNegotiatorProjectsStableHttpErrorMetadataAndHeaders() {
        WayangA2aExtensionNegotiator negotiator = WayangA2aExtensionNegotiator.fromAgentCard(cardRequiringExtension());
        WayangA2aHttpRequest request = new WayangA2aHttpRequest(
                "POST",
                "/message:send",
                "{}",
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE,
                        A2aProtocol.HEADER_EXTENSIONS, "https://wayang.test/extensions/other/v1"),
                Map.of());

        WayangA2aHttpResponse response = negotiator.validateHttp(request).orElseThrow();

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(error(response)).containsEntry("code", "extension_support_required");
        assertThat(errorMetadata(response).keySet()).containsExactly(
                "missingExtensions",
                "requiredExtensions",
                "providedExtensions");
        assertThat(errorMetadata(response))
                .containsEntry("missingExtensions", List.of(REQUIRED_EXTENSION))
                .containsEntry("requiredExtensions", List.of(REQUIRED_EXTENSION))
                .containsEntry("providedExtensions", List.of("https://wayang.test/extensions/other/v1"));
        assertThat(response.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS, REQUIRED_EXTENSION);
    }

    @Test
    void extendedAgentCardAuthorizerProjectsStableHttpErrorMetadataAndHeaders() {
        WayangA2aExtendedAgentCardAuthorizer authorizer =
                WayangA2aExtendedAgentCardAuthorizer.requireBearerToken("secret-token");

        WayangA2aHttpResponse response = authorizer.authorize(WayangA2aHttpRequest.get("/extendedAgentCard"))
                .orElseThrow();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(error(response)).containsEntry("code", "extended_agent_card_unauthorized");
        assertThat(errorMetadata(response).keySet()).containsExactly("scheme");
        assertThat(errorMetadata(response)).containsEntry("scheme", "Bearer");
        assertThat(response.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE,
                        "Bearer realm=\"" + WayangA2aExtendedAgentCardAuthorizer.BEARER_REALM + "\"");
    }

    @Test
    void extendedAgentCardAuthorizerAcceptsMatchingBearerToken() {
        WayangA2aExtendedAgentCardAuthorizer authorizer =
                WayangA2aExtendedAgentCardAuthorizer.requireBearerToken("secret-token");

        assertThat(authorizer.authorize(new WayangA2aHttpRequest(
                "GET",
                "/extendedAgentCard",
                "",
                Map.of(WayangA2aHttpResponse.HEADER_AUTHORIZATION, "Bearer secret-token"),
                Map.of()))).isEmpty();
    }

    private static A2aAgentCard cardRequiringExtension() {
        return new A2aAgentCard(
                "Wayang",
                "A2A endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(
                        false,
                        false,
                        List.of(new A2aAgentExtension(
                                REQUIRED_EXTENSION,
                                "Requires explicit client support",
                                true,
                                Map.of())),
                        false),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return WayangA2aMaps.copyMap((Map<?, ?>) payload.get("error"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> errorMetadata(WayangA2aHttpResponse response) {
        return WayangA2aMaps.copyMap((Map<?, ?>) error(response).get("metadata"));
    }
}
