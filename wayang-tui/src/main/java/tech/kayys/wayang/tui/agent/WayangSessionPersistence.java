package tech.kayys.wayang.tui.agent;
import tech.kayys.wayang.sdk.provider.ContentBlock;
import tech.kayys.wayang.sdk.provider.ChatMessage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.wayang.sdk.provider.ChatMessage;
import tech.kayys.wayang.sdk.provider.ContentBlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class WayangSessionPersistence {
    
    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean persistEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("WAYANG_SESSION_PERSIST", "true"));
    
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
            System.err.println("Failed to load session history from " + path + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(List<ChatMessage> history) {
        if (!persistEnabled) {
            return;
        }
        Path path = getSessionFilePath();
        try {
            // Write debug marker
            Files.write(Path.of(System.getProperty("user.home"), ".wayang", "sessions", "debug-persistence.log"),
                    ("[save] called with " + (history == null ? "null" : history.size()) + " messages\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            // Retain last 50 turns to avoid infinite growth
            int startIdx = Math.max(0, history.size() - 50);
            List<ChatMessage> toSave = history.subList(startIdx, history.size());
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), toSave);
            
            Files.write(Path.of(System.getProperty("user.home"), ".wayang", "sessions", "debug-persistence.log"),
                    ("[save] successfully saved " + toSave.size() + " messages to " + path + "\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            try {
                Files.write(Path.of(System.getProperty("user.home"), ".wayang", "sessions", "debug-persistence.log"),
                        ("[save] ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n").getBytes(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) { }
            System.err.println("Failed to save session history to " + path + ": " + e.getMessage());
        }
    }
}
