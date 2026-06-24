package tech.kayys.wayang.gollek.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Simple session persistence manager storing transcripts as JSON files under
 * ~/.wayang/sessions/{projectKey}/session-{sessionId}.json
 */
final class WayangSessionManager {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path baseDir;

    WayangSessionManager() throws IOException {
        this.baseDir = Path.of(System.getProperty("user.home"), ".wayang", "sessions");
        Files.createDirectories(baseDir);
    }

    static String computeProjectKey(String explicitProjectId, Path workspaceDir) {
        if (explicitProjectId != null && !explicitProjectId.isBlank()) {
            return slugify(explicitProjectId);
        }
        String name = workspaceDir.getFileName() != null ? workspaceDir.getFileName().toString() : "workspace";
        String hash = sha1Hex(workspaceDir.toAbsolutePath().toString());
        return slugify(name) + "-" + hash.substring(0, 8);
    }

    void ensureProjectDir(String projectKey) throws IOException {
        Path projectDir = baseDir.resolve(projectKey);
        Files.createDirectories(projectDir);
    }

    void saveTranscript(String projectKey, String sessionId, List<?> messages) throws IOException {
        if (projectKey == null || sessionId == null) return;
        Path projectDir = baseDir.resolve(projectKey);
        Files.createDirectories(projectDir);
        Path file = projectDir.resolve("session-" + sessionId + ".json");
        // Write a JSON array of messages (generic objects)
        MAPPER.writeValue(file.toFile(), messages != null ? messages : new ArrayList<>());
    }

    List<Object> loadTranscript(String projectKey, String sessionId) throws IOException {
        if (projectKey == null || sessionId == null) return List.of();
        Path file = baseDir.resolve(projectKey).resolve("session-" + sessionId + ".json");
        if (!Files.exists(file)) return List.of();
        Object[] arr = MAPPER.readValue(file.toFile(), Object[].class);
        List<Object> list = new ArrayList<>();
        if (arr != null) {
            for (Object m : arr) list.add(m);
        }
        return list;
    }

    List<String> listSessions(String projectKey) throws IOException {
        Path projectDir = baseDir.resolve(projectKey);
        if (!Files.exists(projectDir) || !Files.isDirectory(projectDir)) return List.of();
        return Files.list(projectDir)
                .filter(p -> p.getFileName().toString().startsWith("session-") && p.getFileName().toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replaceFirst("^session-", "").replaceFirst("\\.json$", ""))
                .collect(Collectors.toList());
    }

    // --- utilities ---

    private static String slugify(String s) {
        String t = s.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return t.isEmpty() ? "project" : t;
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
