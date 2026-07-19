package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerSentEventsTest {

    @Test
    void returnsPlainBodyWhenNoDataLinesExist() {
        assertEquals("{\"ok\":true}", McpServerSentEvents.extractData("{\"ok\":true}"));
        assertEquals("", McpServerSentEvents.extractData(null));
    }

    @Test
    void extractsSingleDataEvent() {
        String body = "event: message\n"
                + "data: {\"ok\":true}\n\n";

        assertEquals("{\"ok\":true}", McpServerSentEvents.extractData(body));
    }

    @Test
    void joinsMultilineDataEvents() {
        String body = ": keepalive\n"
                + "event: message\n"
                + "data: {\"jsonrpc\":\"2.0\",\n"
                + "data: \"id\":\"1\",\n"
                + "data: \"result\":{\"text\":\"hello\"}}\n\n";

        assertEquals(
                "{\"jsonrpc\":\"2.0\",\n\"id\":\"1\",\n\"result\":{\"text\":\"hello\"}}",
                McpServerSentEvents.extractData(body));
    }

    @Test
    void returnsLastNonDoneDataEvent() {
        String body = "data: {\"first\":true}\n\n"
                + "data: {\"second\":true}\n\n"
                + "data: [DONE]\n\n";

        assertEquals("{\"second\":true}", McpServerSentEvents.extractData(body));
    }
}
