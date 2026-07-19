package tech.kayys.wayang.project.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import tech.kayys.wayang.project.Project;
import tech.kayys.wayang.project.Session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * JSON file persistence using Jackson for robust parsing/serialization.
 * Each project stored as projects/<filename>.json under baseDir.
 * Current project id stored in current-project.txt
 */
public class JsonFilePersistence implements PersistenceStrategy {
    private final Path baseDir;
    private final Path projectsDir;
    private final Path currentFile;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);

    public JsonFilePersistence(Path baseDir) throws IOException {
        this.baseDir = baseDir;
        this.projectsDir = baseDir.resolve("projects");
        this.currentFile = baseDir.resolve("current-project.txt");
        if (!Files.exists(projectsDir)) Files.createDirectories(projectsDir);
    }

    @Override
    public List<Project> listProjects() throws IOException {
        if (!Files.exists(projectsDir)) return Collections.emptyList();
        List<Project> out = new ArrayList<>();
        Files.list(projectsDir).filter(p -> p.getFileName().toString().endsWith(".json"))
                .forEach(p -> {
                    try {
                        String s = Files.readString(p);
                        Project pr = parseProject(s);
                        if (pr != null) out.add(pr);
                    } catch (Exception ignored) {}
                });
        return out;
    }

    @Override
    public void saveProject(Project project) throws IOException {
        Path f = projectsDir.resolve(project.id() + ".json");
        // build JSON node
        var root = mapper.createObjectNode();
        root.put("id", project.id());
        root.put("name", project.name());
        root.put("directory", project.directory());
        root.put("createdAt", project.createdAt().toString());
        root.put("updatedAt", project.updatedAt().toString());
        var arr = mapper.createArrayNode();
        project.sessions().forEach(s -> {
            var sn = mapper.createObjectNode();
            sn.put("id", s.id());
            sn.put("name", s.name());
            sn.put("createdAt", s.createdAt().toString());
            sn.put("updatedAt", s.updatedAt().toString());
            sn.put("isPinned", s.isPinned());
            if (s.systemPrompt() != null) sn.put("systemPrompt", s.systemPrompt());
            var tagsArr = mapper.createArrayNode();
            s.tags().forEach(tagsArr::add);
            sn.set("tags", tagsArr);
            if (s.parentSessionId() != null) sn.put("parentSessionId", s.parentSessionId());
            if (s.parentCheckpointIndex() != null) sn.put("parentCheckpointIndex", s.parentCheckpointIndex());
            arr.add(sn);
        });
        root.set("sessions", arr);
        Files.writeString(f, mapper.writeValueAsString(root));
    }

    @Override
    public void removeProject(String projectId) throws IOException {
        // Find files whose contained project id matches and delete
        if (Files.exists(projectsDir)) {
            Files.list(projectsDir).filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                try {
                    String s = Files.readString(p);
                    Project pr = parseProject(s);
                    if (pr != null && projectId.equals(pr.id())) {
                        try { Files.deleteIfExists(p); } catch (IOException ignore) {}
                    }
                } catch (IOException ignored) {}
            });
        }
        String cur = null;
        try { cur = getCurrentProjectId(); } catch (Exception ignored) {}
        if (projectId.equals(cur)) Files.deleteIfExists(currentFile);
    }

    @Override
    public Path exportProject(String projectId, Path destFile) throws IOException {
        // find the file matching projectId
        if (!Files.exists(projectsDir)) throw new IOException("project not found: " + projectId);
        var found = Files.list(projectsDir).filter(p -> p.getFileName().toString().endsWith(".json")).filter(p -> {
            try { String s = Files.readString(p); Project pr = parseProject(s); return pr != null && projectId.equals(pr.id()); } catch (Exception e) { return false; }
        }).findFirst();
        if (found.isEmpty()) throw new IOException("project not found: " + projectId);
        Files.copy(found.get(), destFile, StandardCopyOption.REPLACE_EXISTING);
        return destFile;
    }

    @Override
    public Project importProject(Path srcFile) throws IOException {
        String s = Files.readString(srcFile);
        Project p = parseProject(s);
        if (p == null) throw new IOException("invalid project file");
        Path dest = projectsDir.resolve(p.id() + ".json");
        int i = 1;
        while (Files.exists(dest)) {
            dest = projectsDir.resolve(p.id() + "-" + i + ".json");
            i++;
        }
        Files.writeString(dest, s);
        return p;
    }

    @Override
    public void setCurrentProjectId(String projectId) throws IOException {
        Files.writeString(currentFile, projectId);
    }

    @Override
    public String getCurrentProjectId() throws IOException {
        if (!Files.exists(currentFile)) return null;
        String s = Files.readString(currentFile).trim();
        return s.isEmpty() ? null : s;
    }

    private Project parseProject(String json) {
        try {
            JsonNode n = mapper.readTree(json);
            String id = n.hasNonNull("id") ? n.get("id").asText() : null;
            String name = n.hasNonNull("name") ? n.get("name").asText() : null;
            String dir = n.hasNonNull("directory") ? n.get("directory").asText() : null;
            if (id == null) return null;
            Project p = new Project(id, name, dir);
            if (n.has("sessions")) {
                for (JsonNode s : n.get("sessions")) {
                    String sid = s.hasNonNull("id") ? s.get("id").asText() : null;
                    String sname = s.hasNonNull("name") ? s.get("name").asText() : sid;
                    String parentId = s.hasNonNull("parentSessionId") ? s.get("parentSessionId").asText() : null;
                    Integer parentCheckpoint = s.hasNonNull("parentCheckpointIndex") ? s.get("parentCheckpointIndex").asInt() : null;
                    if (sid != null) {
                        Session session = new Session(sid, sname, parentId, parentCheckpoint);
                        if (s.hasNonNull("updatedAt")) session.setUpdatedAt(java.time.Instant.parse(s.get("updatedAt").asText()));
                        else if (s.hasNonNull("lastAccess")) session.setUpdatedAt(java.time.Instant.parse(s.get("lastAccess").asText()));
                        if (s.hasNonNull("isPinned")) session.setPinned(s.get("isPinned").asBoolean());
                        if (s.hasNonNull("systemPrompt")) session.setSystemPrompt(s.get("systemPrompt").asText());
                        if (s.hasNonNull("tags")) {
                            List<String> tags = new ArrayList<>();
                            for (JsonNode t : s.get("tags")) tags.add(t.asText());
                            session.setTags(tags);
                        }
                        p.addSession(session);
                    }
                }
            }
            return p;
        } catch (Throwable t) {
            return null;
        }
    }
}
