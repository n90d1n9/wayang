package tech.kayys.wayang.gollek.sdk.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import tech.kayys.gollek.spi.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDK-level session & project store. Persists sessions under ~/.wayang/sessions/{project}/
 */
public final class WayangSessionStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Path baseDir;

    public WayangSessionStore() throws IOException {
        this.baseDir = Path.of(System.getProperty("user.home"), ".wayang", "sessions");
        Files.createDirectories(baseDir);
    }

    // Close hook for try-with-resources compatibility in CLI (no-op)
    public void close() throws Exception {
        // no resources to close
    }

    // --- Project metadata ---
    public record ProjectMeta(String id, String name, String directory, Instant createdAt) {}

    private Path projectDir(String projectKey) {
        // Prefer migrated projects/<project>/sessions layout if available, otherwise use legacy ~/.wayang/sessions/<project>
        try {
            Path migrated = baseDir.getParent().resolve("projects").resolve(projectKey).resolve("sessions");
            if (Files.exists(migrated) && Files.isDirectory(migrated)) return migrated;
        } catch (Exception ignored) {}
        return baseDir.resolve(projectKey);
    }

    public static String computeProjectKey(String explicitProjectId, Path workspaceDir) {
        if (explicitProjectId != null && !explicitProjectId.isBlank()) {
            return slugify(explicitProjectId);
        }
        String name = workspaceDir.getFileName() != null ? workspaceDir.getFileName().toString() : "workspace";
        String hash = sha1Hex(workspaceDir.toAbsolutePath().toString());
        return slugify(name) + "-" + hash.substring(0, 8);
    }

    public void ensureProjectDir(String projectKey) throws IOException {
        Files.createDirectories(projectDir(projectKey));
    }

    public List<String> listProjects() throws IOException {
        if (!Files.exists(baseDir)) return List.of();
        return Files.list(baseDir)
                .filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }

    public ProjectMeta addProject(String projectId, Path directory) throws IOException {
        String key = computeProjectKey(projectId, directory);
        Path dir = projectDir(key);
        Files.createDirectories(dir);
        ProjectMeta meta = new ProjectMeta(key, projectId == null || projectId.isBlank() ? directory.getFileName().toString() : projectId, directory.toAbsolutePath().toString(), Instant.now());
        Path metaFile = dir.resolve("project.json");
        MAPPER.writeValue(metaFile.toFile(), meta);
        return meta;
    }

    public void removeProject(String projectKey) throws IOException {
        Path dir = projectDir(projectKey);
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            // Delete all files in project dir (non-recursive safe approach)
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }
    }

    public void renameProject(String projectKey, String newName) throws IOException {
        Path dir = projectDir(projectKey);
        if (!Files.exists(dir)) throw new IOException("Project not found: " + projectKey);
        Path metaFile = dir.resolve("project.json");
        ProjectMeta meta = null;
        if (Files.exists(metaFile)) {
            meta = MAPPER.readValue(metaFile.toFile(), ProjectMeta.class);
        }
        ProjectMeta updated = new ProjectMeta(projectKey, newName, meta != null ? meta.directory() : "", Instant.now());
        MAPPER.writeValue(metaFile.toFile(), updated);
    }

    public void exportProject(String projectKey, Path targetFile) throws IOException {
        Path dir = projectDir(projectKey);
        if (!Files.exists(dir)) throw new IOException("Project not found: " + projectKey);
        // Avoid overwriting target unless explicitly allowed; if exists, append timestamp
        if (Files.exists(targetFile)) {
            String name = targetFile.getFileName().toString();
            String ts = Instant.now().toString().replaceAll("[:T]", "-").replaceAll("Z", "");
            targetFile = targetFile.resolveSibling(name + "-" + ts + ".json");
        }
        // Create a JSON bundle with metadata and list of session files content
        Map<String, Object> bundle = new HashMap<>();
        Path metaFile = dir.resolve("project.json");
        if (Files.exists(metaFile)) {
            bundle.put("meta", MAPPER.readValue(metaFile.toFile(), Map.class));
        }
        Map<String, List<Message>> sessions = new HashMap<>();
        Files.list(dir)
                .filter(p -> p.getFileName().toString().startsWith("session-") && p.getFileName().toString().endsWith(".json"))
                .forEach(p -> {
                    try {
                        Message[] arr = MAPPER.readValue(p.toFile(), Message[].class);
                        sessions.put(p.getFileName().toString(), Arrays.asList(arr));
                    } catch (IOException ignored) {}
                });
        bundle.put("sessions", sessions);
        MAPPER.writeValue(targetFile.toFile(), bundle);
    }

    public void importProject(Path bundleFile) throws IOException {
        Map<?,?> bundle = MAPPER.readValue(bundleFile.toFile(), Map.class);
        Map<?,?> meta = (Map<?,?>) bundle.get("meta");
        String projectKey = meta != null && meta.get("id") != null ? meta.get("id").toString() : UUID.randomUUID().toString();
        Path dir = projectDir(projectKey);
        if (Files.exists(dir) && Files.list(dir).findAny().isPresent()) {
            // Conflict: project dir not empty. Create a new projectKey with suffix
            String suffix = "-import-" + Instant.now().toEpochMilli();
            projectKey = projectKey + suffix;
            dir = projectDir(projectKey);
        }
        Files.createDirectories(dir);
        MAPPER.writeValue(dir.resolve("project.json").toFile(), meta != null ? meta : Map.of("id", projectKey));
        Map<?,?> sessions = (Map<?,?>) bundle.get("sessions");
        if (sessions != null) {
            for (Object k : sessions.keySet()) {
                String fileName = k.toString();
                Object v = sessions.get(k);
                Path out = dir.resolve(fileName);
                MAPPER.writeValue(out.toFile(), v);
            }
        }
    }

    // --- Sessions ---
    public void saveTranscript(String projectKey, String sessionId, List<Message> messages) throws IOException {
        if (projectKey == null || sessionId == null) return;
        // Prefer writing into migrated projects/<project>/sessions folder
        Path migrated = null;
        try { migrated = baseDir.getParent().resolve("projects").resolve(projectKey).resolve("sessions"); } catch (Exception ignored) {}
        Path project = migrated != null ? migrated : projectDir(projectKey);
        Files.createDirectories(project);
        Path file = project.resolve("session-" + sessionId + ".json");
        MAPPER.writeValue(file.toFile(), messages != null ? messages : List.of());
    }

    public List<Message> loadTranscript(String projectKey, String sessionId) throws IOException {
        if (projectKey == null || sessionId == null) return List.of();
        Path migrated = null;
        try { migrated = baseDir.getParent().resolve("projects").resolve(projectKey).resolve("sessions"); } catch (Exception ignored) {}
        Path file = (migrated != null ? migrated : projectDir(projectKey)).resolve("session-" + sessionId + ".json");
        if (!Files.exists(file)) return List.of();
        Message[] arr = MAPPER.readValue(file.toFile(), Message[].class);
        return arr != null ? Arrays.asList(arr) : List.of();
    }

    public List<String> listSessions(String projectKey) throws IOException {
        Path migrated = null;
        try { migrated = baseDir.getParent().resolve("projects").resolve(projectKey).resolve("sessions"); } catch (Exception ignored) {}
        Path project = migrated != null ? migrated : projectDir(projectKey);
        if (!Files.exists(project) || !Files.isDirectory(project)) return List.of();
        return Files.list(project)
                .filter(p -> p.getFileName().toString().startsWith("session-") && p.getFileName().toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replaceFirst("^session-", "").replaceFirst("\\.json$", ""))
                .collect(Collectors.toList());
    }

    // --- utilities ---
    private static String slugify(String s) {
        String t = s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
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
