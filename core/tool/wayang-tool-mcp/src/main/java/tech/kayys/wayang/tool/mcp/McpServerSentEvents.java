package tech.kayys.wayang.tool.mcp;

import java.util.ArrayList;
import java.util.List;

final class McpServerSentEvents {

    private static final String DATA_FIELD = "data";
    private static final String DATA_PREFIX = "data:";
    private static final String DONE_EVENT = "[DONE]";

    private McpServerSentEvents() {
    }

    static String extractData(String body) {
        String text = body == null ? "" : body;
        if (!hasDataLine(text)) {
            return text;
        }

        String lastData = "";
        List<String> eventData = new ArrayList<>();
        for (String line : text.split("\\R", -1)) {
            if (isDataLine(line)) {
                eventData.add(dataValue(line));
            } else if (line.isBlank()) {
                lastData = flush(eventData, lastData);
            }
        }
        return flush(eventData, lastData);
    }

    private static boolean hasDataLine(String body) {
        for (String line : body.split("\\R", -1)) {
            if (isDataLine(line)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDataLine(String line) {
        return DATA_FIELD.equals(line) || line.startsWith(DATA_PREFIX);
    }

    private static String dataValue(String line) {
        if (DATA_FIELD.equals(line)) {
            return "";
        }
        String value = line.substring(DATA_PREFIX.length());
        return value.startsWith(" ") ? value.substring(1) : value;
    }

    private static String flush(List<String> eventData, String lastData) {
        if (eventData.isEmpty()) {
            return lastData;
        }
        String data = String.join("\n", eventData);
        eventData.clear();
        if (data.isBlank() || DONE_EVENT.equals(data.strip())) {
            return lastData;
        }
        return data;
    }
}
