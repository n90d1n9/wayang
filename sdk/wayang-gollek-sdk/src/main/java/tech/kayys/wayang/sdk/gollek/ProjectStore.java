package tech.kayys.wayang.sdk.gollek;

import tech.kayys.wayang.sdk.gollek.model.Project;
import tech.kayys.wayang.sdk.gollek.model.Session;
import tech.kayys.wayang.sdk.gollek.persistence.JsonFilePersistence;
import tech.kayys.wayang.sdk.gollek.persistence.PersistenceStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import tech.kayys.gollek.spi.Message;
import java.util.Arrays;
import java.util.stream.Collectors;
public class ProjectStore {
    private final PersistenceStrategy persistence;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);

    public ProjectStore(Path baseDir) throws Exception {
        Path base = baseDir != null ? baseDir : Paths.get(System.getProperty("user.home"), ".wayang");
        this.persistence = new JsonFilePersistence(base);
        // If no projects exist yet, attempt to migrate legacy session-store layout into the new projects store
        try {
            if (persistence.listProjects().isEmpty()) {
                migrateFromLegacySessions(base);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Public migration entrypoint. Returns number of projects migrated.
     */
    public int migrateLegacySessions() {
        try {
            Path base = Paths.get(System.getProperty("user.home"), ".wayang");
            return migrateFromLegacySessions(base);
        } catch (Exception e) {
            return 0;
        }
    }

    private int migrateFromLegacySessions(Path base) {
        int migrated = 0;
        try {
            Path legacy = Paths.get(System.getProperty("user.home"), ".wayang", "sessions");
            Path projectsDir = base.resolve("projects");
            if (!Files.exists(legacy) || !Files.isDirectory(legacy)) return 0;
            // For each project folder under legacy, create project JSON and copy session files
            var stream = Files.list(legacy).filter(Files::isDirectory).iterator();
            while (stream.hasNext()) {
                Path pd = stream.next();
                try {
                    String projectKey = pd.getFileName().toString();
                    // try read legacy project metadata
                    Path metaFile = pd.resolve("project.json");
                    String displayName = projectKey;
                    String directory = "";
                    if (Files.exists(metaFile)) {
                        String mj = Files.readString(metaFile);
                        String nm = extract(mj, "name");
                        String dir = extract(mj, "directory");
                        if (nm != null && !nm.isBlank()) displayName = nm;
                        if (dir != null) directory = dir;
                    }
                    Project p = new Project(projectKey, displayName, directory);
                    // prepare per-project sessions dir under projects/<projectKey>/sessions
                    Path targetProjectSessions = projectsDir.resolve(projectKey).resolve("sessions");
                    Files.createDirectories(targetProjectSessions);
                    Files.list(pd).filter(pf -> pf.getFileName().toString().startsWith("session-") && pf.getFileName().toString().endsWith(".json")).forEach(sf -> {
                        try {
                            // copy transcript
                            Path dest = targetProjectSessions.resolve(sf.getFileName().toString());
                            Files.copy(sf, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            // add session metadata
                            String fname = sf.getFileName().toString();
                            String sid = fname.replaceFirst("^session-", "").replaceFirst("\\.json$", "");
                            p.addSession(new Session(sid, sid));
                        } catch (Exception ignored2) {}
                    });
                    // save project metadata
                    persistence.saveProject(p);
                    migrated++;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return migrated;
    }

    public Project createProject(String id, String name, String directory) throws Exception {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        Project p = new Project(id, name, directory);
        persistence.saveProject(p);
        return p;
    }

    public void removeProject(String id) throws Exception { persistence.removeProject(id); }
    public void renameProject(String id, String newName) throws Exception {
        List<Project> list = persistence.listProjects();
        for (Project p : list) if (p.id().equals(id)) { p.setName(newName); persistence.saveProject(p); return; }
        throw new IllegalArgumentException("project not found: " + id);
    }

    public List<Project> listProjects() throws Exception { return persistence.listProjects(); }

    public Path exportProject(String id, Path dest) throws Exception { return persistence.exportProject(id, dest); }
    public Project importProject(Path src) throws Exception { return persistence.importProject(src); }

    public void switchProject(String id) throws Exception { persistence.setCurrentProjectId(id); }
    public String currentProject() throws Exception { return persistence.getCurrentProjectId(); }

    public Session createSession(String projectId, String name) throws Exception {
        String sid = UUID.randomUUID().toString();
        Session s = new Session(sid, name);
        List<Project> list = persistence.listProjects();
        for (Project p : list) if (p.id().equals(projectId)) { p.addSession(s); persistence.saveProject(p); return s; }
        throw new IllegalArgumentException("project not found: " + projectId);
    }

    // Session storage helpers (use migrated projects/<id>/sessions where available)
    public void saveTranscript(String projectId, String sessionId, java.util.List<Message> messages) throws Exception {
        Path sessionsDir = getSessionsDir(projectId);
        Files.createDirectories(sessionsDir);
        Path file = sessionsDir.resolve("session-" + sessionId + ".json");
        mapper.writeValue(file.toFile(), messages != null ? messages : List.of());
    }

    public java.util.List<Message> loadTranscript(String projectId, String sessionId) throws Exception {
        Path sessionsDir = getSessionsDir(projectId);
        Path file = sessionsDir.resolve("session-" + sessionId + ".json");
        if (!Files.exists(file)) return List.of();
        Message[] arr = mapper.readValue(file.toFile(), Message[].class);
        return arr != null ? Arrays.asList(arr) : List.of();
    }

    public java.util.List<String> listSessions(String projectId) throws Exception {
        Path sessionsDir = getSessionsDir(projectId);
        if (!Files.exists(sessionsDir) || !Files.isDirectory(sessionsDir)) return List.of();
        return Files.list(sessionsDir)
                .filter(p -> p.getFileName().toString().startsWith("session-") && p.getFileName().toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replaceFirst("^session-", "").replaceFirst("\\.json$", ""))
                .collect(Collectors.toList());
    }

    public boolean deleteSession(String projectId, String sessionId) throws Exception {
        Path sessionsDir = getSessionsDir(projectId);
        Path file = sessionsDir.resolve("session-" + sessionId + ".json");
        return Files.deleteIfExists(file);
    }

    /**
     * Clone (fork) an existing session transcript into a new session under the same project.
     * Returns the newly created Session metadata.
     */
    public Session cloneSession(String projectId, String sessionId, String newName) throws Exception {
        // Load existing transcript (empty list if missing)
        java.util.List<Message> transcript = loadTranscript(projectId, sessionId);
        if (transcript == null) transcript = List.of();
        // Create new session metadata
        Session s = createSession(projectId, newName == null ? sessionId : newName);
        // Persist transcript for new session id
        saveTranscript(projectId, s.id(), transcript);
        return s;
    }

    private Path getSessionsDir(String projectId) {
        try {
            Path base = Paths.get(System.getProperty("user.home"), ".wayang");
            Path migrated = base.resolve("projects").resolve(projectId).resolve("sessions");
            if (Files.exists(migrated) && Files.isDirectory(migrated)) return migrated;
        } catch (Exception ignored) {}
        // fallback to legacy location
        return Paths.get(System.getProperty("user.home"), ".wayang", "sessions", projectId);
    }

    private static String extract(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }
}
