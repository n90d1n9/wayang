package tech.kayys.wayang.provider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * Parses a Server-Sent Events stream (the format both the Anthropic and
 * OpenAI-compatible streaming APIs use) and invokes a callback for each
 * (event-name, data) pair. event-name defaults to "message" if the
 * stream doesn't send an explicit `event:` line (this matches the
 * OpenAI/Ollama style, which only sends `data:` lines).
 */
public final class SseReader {

    private SseReader() {}

    public static void read(InputStream rawIn, BiConsumer<String, String> onEvent) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
        String line;
        String currentEvent = "message";
        StringBuilder dataBuf = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                // Blank line = dispatch the accumulated event.
                if (dataBuf.length() > 0) {
                    String data = dataBuf.toString();
                    dataBuf.setLength(0);
                    if (!data.equals("[DONE]")) {
                        onEvent.accept(currentEvent, data);
                    }
                }
                currentEvent = "message";
                continue;
            }
            if (line.startsWith(":")) continue; // comment/heartbeat
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
            } else if (line.startsWith("data:")) {
                String d = line.substring(5);
                if (d.startsWith(" ")) d = d.substring(1);
                if (dataBuf.length() > 0) dataBuf.append('\n');
                dataBuf.append(d);
            }
            // ignore "id:" and "retry:" lines -- not needed for this client
        }
        // Flush any trailing event without a final blank line.
        if (dataBuf.length() > 0) {
            String data = dataBuf.toString();
            if (!data.equals("[DONE]")) onEvent.accept(currentEvent, data);
        }
    }
}
