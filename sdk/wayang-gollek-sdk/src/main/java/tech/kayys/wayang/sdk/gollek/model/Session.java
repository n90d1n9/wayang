package tech.kayys.wayang.sdk.gollek.model;

import java.time.Instant;
import java.util.Objects;

public final class Session {
    private final String id;
    private String name;
    private final Instant createdAt;
    private Instant lastAccess;
    // Parent lineage: if this session was forked, parentSessionId points to source session
    private String parentSessionId;
    // If forked from a checkpoint, index of the message (inclusive) used as branch point; null means full transcript
    private Integer parentCheckpointIndex;

    public Session(String id, String name) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? id : name;
        this.createdAt = Instant.now();
        this.lastAccess = this.createdAt;
        this.parentSessionId = null;
        this.parentCheckpointIndex = null;
    }

    public Session(String id, String name, String parentSessionId, Integer parentCheckpointIndex) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? id : name;
        this.createdAt = Instant.now();
        this.lastAccess = this.createdAt;
        this.parentSessionId = parentSessionId;
        this.parentCheckpointIndex = parentCheckpointIndex;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant createdAt() { return createdAt; }
    public Instant lastAccess() { return lastAccess; }
    public void touch() { this.lastAccess = Instant.now(); }
    public String parentSessionId() { return parentSessionId; }
    public Integer parentCheckpointIndex() { return parentCheckpointIndex; }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"id\":\"").append(escape(id)).append('\"');
        sb.append(',');
        sb.append("\"name\":\"").append(escape(name)).append('\"');
        sb.append(',');
        sb.append("\"createdAt\":\"").append(createdAt.toString()).append('\"');
        sb.append(',');
        sb.append("\"lastAccess\":\"").append(lastAccess.toString()).append('\"');
        if (parentSessionId != null) {
            sb.append(',');
            sb.append("\"parentSessionId\":\"").append(escape(parentSessionId)).append('\"');
        }
        if (parentCheckpointIndex != null) {
            sb.append(',');
            sb.append("\"parentCheckpointIndex\":").append(parentCheckpointIndex);
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
