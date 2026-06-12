package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTenantHintsTest {

    @Test
    void extractsDirectAndMetadataTenantAliasesFromMaps() {
        assertThat(WayangA2aTenantHints.fromMap(Map.of("tenant", "tenant-direct")))
                .hasValue("tenant-direct");
        assertThat(WayangA2aTenantHints.fromMap(Map.of("metadata", Map.of("tenantId", "tenant-metadata"))))
                .hasValue("tenant-metadata");
    }

    @Test
    void prioritizesHttpAttributesBeforeCachedSendMessageAndBody() {
        WayangA2aHttpRequest request = WayangA2aHttpRequest
                .postJson("/tasks/task-1", WayangA2aHttpJson.write(Map.of("tenant", "tenant-body")))
                .withAttribute("metadata", Map.of("tenantId", "tenant-attribute"))
                .withAttribute(WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE, requestForTenant("tenant-cached"));

        assertThat(WayangA2aTenantHints.fromHttpRequest(request)).hasValue("tenant-attribute");
    }

    @Test
    void readsCachedSendMessageTenantBeforeMalformedBody() {
        WayangA2aHttpRequest request = WayangA2aHttpRequest
                .postJson("/message:send", "{")
                .withAttribute(WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE, requestForTenant("tenant-cached"));

        assertThat(WayangA2aTenantHints.fromHttpRequest(request)).hasValue("tenant-cached");
    }

    @Test
    void readsTenantFromSendMessageMetadata() {
        assertThat(WayangA2aTenantHints.fromSendMessageRequest(requestWithMetadataTenant("tenant-metadata")))
                .hasValue("tenant-metadata");
    }

    private static A2aSendMessageRequest requestForTenant(String tenant) {
        return new A2aSendMessageRequest(
                tenant,
                message("message-" + tenant),
                null,
                Map.of());
    }

    private static A2aSendMessageRequest requestWithMetadataTenant(String tenant) {
        return new A2aSendMessageRequest(
                null,
                message("message-metadata-" + tenant),
                null,
                Map.of("tenantId", tenant));
    }

    private static A2aMessage message(String messageId) {
        return new A2aMessage(
                messageId,
                "context-" + messageId,
                "task-" + messageId,
                A2aRole.ROLE_USER,
                List.of(A2aPart.text("ping")),
                Map.of(),
                List.of(),
                List.of());
    }
}
