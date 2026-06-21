package tech.kayys.wayang.sdk.gollek.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Project {
    private final String id;
    private String name;
    private String directory;
    private final List<Session> sessions = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;

    public Project(String id, String name, String directory) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? id : name;
        this.directory = directory == null ? "." : directory;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; this.updatedAt = Instant.now(); }
    public String directory() { return directory; }
    public void setDirectory(String d) { this.directory = d; this.updatedAt = Instant.now(); }
    public List<Session> sessions() { return sessions; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void addSession(Session s) { if (s != null) { sessions.add(s); this.updatedAt = Instant.now(); } }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"id\":\"").append(escape(id)).append('\"');
        sb.append(',');
        sb.append("\"name\":\"").append(escape(name)).append('\"');
        sb.append(',');
        sb.append("\"directory\":\"").append(escape(directory)).append('\"');
        sb.append(',');
        sb.append("\"createdAt\":\"").append(createdAt.toString()).append('\"');
        sb.append(',');
        sb.append("\"updatedAt\":\"").append(updatedAt.toString()).append('\"');
        sb.append(',');
        sb.append("\"sessions\":[");
        for (int i = 0; i < sessions.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(sessions.get(i).toJson());
        }
        sb.append(']');
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
