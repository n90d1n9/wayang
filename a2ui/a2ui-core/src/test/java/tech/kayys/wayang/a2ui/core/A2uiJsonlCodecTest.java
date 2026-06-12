package tech.kayys.wayang.a2ui.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2uiJsonlCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final A2uiJsonlCodec codec = new A2uiJsonlCodec(objectMapper);

    @Test
    void encodesServerMessagesAsJsonLines() throws Exception {
        String jsonl = codec.stream(List.of(
                A2uiSurfaceUpdate.of(
                        "main",
                        A2uiComponents.column("root", List.of("title")),
                        A2uiComponents.text("title", "Hello A2UI")),
                A2uiBeginRendering.standard("main", "root")));

        String[] lines = jsonl.strip().split("\\R");

        assertThat(lines).hasSize(2);
        JsonNode update = objectMapper.readTree(lines[0]);
        assertThat(update.at("/surfaceUpdate/surfaceId").asText()).isEqualTo("main");
        assertThat(update.at("/surfaceUpdate/components/0/component/Column/children/explicitList/0").asText())
                .isEqualTo("title");
        assertThat(update.at("/surfaceUpdate/components/1/component/Text/text/literalString").asText())
                .isEqualTo("Hello A2UI");

        JsonNode begin = objectMapper.readTree(lines[1]);
        assertThat(begin.at("/beginRendering/root").asText()).isEqualTo("root");
        assertThat(begin.at("/beginRendering/catalogId").asText())
                .isEqualTo(A2uiProtocol.STANDARD_CATALOG_ID);
    }

    @Test
    void encodesDataModelUpdateWithTypedEntries() throws Exception {
        String line = codec.line(A2uiDataModelUpdate.root(
                "main",
                A2uiDataEntry.map("user", List.of(
                        A2uiDataEntry.string("name", "Alice"),
                        A2uiDataEntry.bool("verified", true)))));

        JsonNode update = objectMapper.readTree(line);

        assertThat(update.at("/dataModelUpdate/contents/0/key").asText()).isEqualTo("user");
        assertThat(update.at("/dataModelUpdate/contents/0/valueMap/0/valueString").asText())
                .isEqualTo("Alice");
        assertThat(update.at("/dataModelUpdate/contents/0/valueMap/1/valueBoolean").asBoolean())
                .isTrue();
    }

    @Test
    void encodesClientUserActionAndDataPart() throws Exception {
        A2uiUserAction action = new A2uiUserAction(
                "submit_form",
                "main",
                "submit_btn",
                java.time.Instant.parse("2025-09-19T17:05:00Z"),
                Map.of("formId", "f-123"));

        JsonNode event = objectMapper.readTree(codec.line(action));
        assertThat(event.at("/userAction/name").asText()).isEqualTo("submit_form");
        assertThat(event.at("/userAction/context/formId").asText()).isEqualTo("f-123");

        JsonNode dataPart = objectMapper.readTree(codec.dataPart(action));
        assertThat(dataPart.at("/kind").asText()).isEqualTo("data");
        assertThat(dataPart.at("/metadata/mimeType").asText()).isEqualTo(A2uiProtocol.MIME_TYPE);
    }

    @Test
    void decodesClientUserActionFromJsonLineAndDataPart() {
        String line = """
                {"userAction":{"name":"wayang.run.inspect","surfaceId":"main","sourceComponentId":"inspect","timestamp":"2025-09-19T17:05:00Z","context":{"runId":"run-1"}}}
                """;

        A2uiClientMessage message = codec.clientMessage(line);

        assertThat(message).isInstanceOf(A2uiUserAction.class);
        A2uiUserAction action = (A2uiUserAction) message;
        assertThat(action.name()).isEqualTo("wayang.run.inspect");
        assertThat(action.context()).containsEntry("runId", "run-1");

        A2uiClientMessage dataPart = codec.clientDataPart(codec.dataPart(action));
        assertThat(dataPart).isEqualTo(action);
    }

    @Test
    void decodesClientUserActionContextEntryList() {
        String line = """
                {"userAction":{
                  "name":"wayang.run.events",
                  "surfaceId":"main",
                  "sourceComponentId":"events",
                  "timestamp":"2025-09-19T17:05:00Z",
                  "context":[
                    {"key":"runId","value":{"literalString":"run-1"}},
                    {"key":"afterSequence","value":{"literalNumber":2}},
                    {"key":"refresh","value":{"literalBoolean":true}}
                  ]
                }}
                """;

        A2uiClientMessage message = codec.clientMessage(line);

        assertThat(message).isInstanceOf(A2uiUserAction.class);
        A2uiUserAction action = (A2uiUserAction) message;
        assertThat(action.context())
                .containsEntry("runId", "run-1")
                .containsEntry("afterSequence", 2)
                .containsEntry("refresh", true);
    }

    @Test
    void decodesClientStreamAndDataPartMaps() {
        A2uiUserAction first = new A2uiUserAction(
                "wayang.run.inspect",
                "main",
                "inspect",
                java.time.Instant.parse("2025-09-19T17:05:00Z"),
                Map.of("runId", "run-1"));
        A2uiUserAction second = new A2uiUserAction(
                "wayang.run.wait",
                "main",
                "wait",
                java.time.Instant.parse("2025-09-19T17:05:01Z"),
                Map.of("runId", "run-1"));

        List<A2uiClientMessage> messages = codec.clientStream(
                codec.line(first) + "\n\n" + codec.line(second) + "\n");

        assertThat(messages).containsExactly(first, second);
        assertThat(codec.clientDataPart(A2uiDataPart.of(first).toPayload()))
                .isEqualTo(first);
    }

    @Test
    void decodesClientErrorAndRejectsUnknownClientMessages() {
        A2uiClientMessage message = codec.clientMessage("""
                {"error":{"surfaceId":"main","message":"Render failed","details":{"code":"catalog_missing"}}}
                """);

        assertThat(message).isInstanceOf(A2uiClientError.class);
        A2uiClientError error = (A2uiClientError) message;
        assertThat(error.message()).isEqualTo("Render failed");
        assertThat(error.details()).containsEntry("code", "catalog_missing");

        assertThatThrownBy(() -> codec.clientMessage("{\"surfaceUpdate\":{}}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported A2UI client message");
    }

    @Test
    void validatesSingleComponentType() {
        assertThatThrownBy(() -> new A2uiComponent("broken", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one component type");
    }
}
