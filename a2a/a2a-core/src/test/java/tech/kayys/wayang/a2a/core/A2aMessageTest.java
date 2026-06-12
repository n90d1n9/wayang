package tech.kayys.wayang.a2a.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2aMessageTest {

    @Test
    void roundTripsProtoJsonMessageWithTextAndDataParts() {
        A2aMessage message = new A2aMessage(
                "message-1",
                "context-1",
                "task-1",
                A2aRole.ROLE_USER,
                List.of(
                        A2aPart.text("Hello"),
                        A2aPart.data(Map.of("city", "Jakarta"))),
                Map.of("traceId", "trace-1"),
                List.of("https://example.test/extensions/location/v1"),
                List.of("task-0"));

        A2aMessage decoded = A2aMessage.fromJson(message.toJson());

        assertThat(decoded.messageId()).isEqualTo("message-1");
        assertThat(decoded.role()).isEqualTo(A2aRole.ROLE_USER);
        assertThat(decoded.parts()).hasSize(2);
        assertThat(decoded.parts().get(0).text()).isEqualTo("Hello");
        assertThat(decoded.parts().get(1).data()).isEqualTo(Map.of("city", "Jakarta"));
        assertThat(decoded.metadata()).containsEntry("traceId", "trace-1");
        assertThat(decoded.extensions()).containsExactly("https://example.test/extensions/location/v1");
        assertThat(decoded.referenceTaskIds()).containsExactly("task-0");
    }

    @Test
    void mapsFilePartsWithoutLegacyKindDiscriminator() {
        A2aPart file = A2aPart.fileWithUrl("https://example.test/report.pdf", "report.pdf", "application/pdf");

        Map<String, Object> payload = file.toMap();

        assertThat(payload)
                .containsEntry("url", "https://example.test/report.pdf")
                .containsEntry("filename", "report.pdf")
                .containsEntry("mediaType", "application/pdf");
        assertThat(payload).doesNotContainKey("kind");
    }

    @Test
    void rejectsPartsWithMultiplePayloads() {
        assertThatThrownBy(() -> new A2aPart("hello", null, null, Map.of("also", "data"), true, null, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }
}
