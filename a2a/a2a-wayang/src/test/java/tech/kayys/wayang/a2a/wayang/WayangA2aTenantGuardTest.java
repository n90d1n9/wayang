package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTenantGuardTest {

    @Test
    void readsTenantFromCachedSendMessageRequestBeforeRawBody() {
        WayangA2aTenantGuard guard = WayangA2aTenantGuard.fromAgentCard(cardForTenant("tenant-a"));
        WayangA2aHttpRequest request = WayangA2aHttpRequest
                .sendMessage("{")
                .withAttribute(WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE, requestForTenant("tenant-b"));

        WayangA2aHttpResponse rejected = guard.validateHttp(request).orElseThrow();

        assertThat(errorCode(rejected)).isEqualTo("tenant_not_supported");
        assertThat(errorMetadata(rejected).keySet()).containsExactly("tenant", "supportedTenants");
        assertThat(errorMetadata(rejected))
                .containsEntry("tenant", "tenant-b")
                .containsEntry("supportedTenants", List.of("tenant-a"));
    }

    @Test
    void validatesJsonRpcTenantFromParsedSendMessageRequest() {
        WayangA2aTenantGuard guard = WayangA2aTenantGuard.fromAgentCard(cardForTenant("tenant-a"));

        WayangA2aJsonRpcError rejected = guard.validateJsonRpc(requestForTenant("tenant-b")).orElseThrow();

        assertThat(rejected.code()).isEqualTo(WayangA2aJsonRpcError.INVALID_PARAMS);
        assertThat(rejected.message()).isEqualTo("A2A tenant is not advertised by Agent Card: tenant-b.");
    }

    @SuppressWarnings("unchecked")
    private static String errorCode(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return String.valueOf(((Map<String, Object>) payload.get("error")).get("code"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> errorMetadata(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        Map<String, Object> error = (Map<String, Object>) payload.get("error");
        return WayangA2aMaps.copyMap((Map<?, ?>) error.get("metadata"));
    }

    private static A2aSendMessageRequest requestForTenant(String tenant) {
        return new A2aSendMessageRequest(
                tenant,
                new A2aMessage(
                        "message-" + tenant,
                        "context-" + tenant,
                        "task-" + tenant,
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                null,
                Map.of());
    }

    private static A2aAgentCard cardForTenant(String tenant) {
        return new A2aAgentCard(
                "Wayang Tenant",
                "A tenant-scoped A2A card",
                List.of(new A2aAgentInterface(
                        "https://wayang.test/a2a/" + tenant,
                        A2aProtocol.BINDING_HTTP_JSON,
                        tenant,
                        A2aProtocol.VERSION)),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(false, false, List.of(), false),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }
}
