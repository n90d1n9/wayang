package tech.kayys.wayang.sdk.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.wayang.sdk.provider.ChatMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight session persistence for the Wayang agentic loop.
 *
 * <p>Saves and loads the conversation history to/from
 * {@code ~/.wayang/sessions/current.json}. Set the environment variable
 * {@code WAYANG_SESSION_PERSIST=false} to disable persistence entirely.
 */
public final class WayangSessionPersistence {

    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean persistEnabled = Boolean.parseBoolean(
            System.getenv().getOrDefault("WAYANG_SESSION_PERSIST", "true"));

    private Path getSessionFilePath() {
        return Path.of(System.getProperty("user.home"), ".wayang", "sessions", "current.json");
    }

    public List<ChatMessage> load() {
        if (!persistEnabled) {
            return new ArrayList<>();
        }
        Path path = getSessionFilePath();
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(path.toFile(), new TypeReference<List<ChatMessage>>() {});
        } catch (IOException e) {
            System.err.println("[WayangSessionPersistence] Failed to load session: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(List<ChatMessage> history) {
        if (!persistEnabled || history == null || history.isEmpty()) {
            return;
        }
        Path path = getSessionFilePath();
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            // Retain last 50 turns to avoid unbounded growth
            int startIdx = Math.max(0, history.size() - 50);
            List<ChatMessage> toSave = history.subList(startIdx, history.size());
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), toSave);
        } catch (IOException e) {
            System.err.println("[WayangSessionPersistence] Failed to save session: " + e.getMessage());
        }
    }
}
