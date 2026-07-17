package tech.kayys.wayang.sdk.gollek.model;

import java.time.Instant;
import java.util.Objects;

public final class Session {
    private final String id;
    private String name;
    private final Instant createdAt;
    private Instant updatedAt;
    private boolean isPinned;
    private java.util.List<String> tags = new java.util.ArrayList<>();
    private String systemPrompt;
    // Parent lineage: if this session was forked, parentSessionId points to source session
    private String parentSessionId;
    // If forked from a checkpoint, index of the message (inclusive) used as branch point; null means full transcript
    private Integer parentCheckpointIndex;

    public Session(String id, String name) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? id : name;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.isPinned = false;
        this.parentSessionId = null;
        this.parentCheckpointIndex = null;
    }

    public Session(String id, String name, String parentSessionId, Integer parentCheckpointIndex) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? id : name;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.isPinned = false;
        this.parentSessionId = parentSessionId;
        this.parentCheckpointIndex = parentCheckpointIndex;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void touch() { this.updatedAt = Instant.now(); }
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean isPinned) { this.isPinned = isPinned; }
    public java.util.List<String> tags() { return tags; }
    public void setTags(java.util.List<String> tags) { this.tags = tags != null ? tags : new java.util.ArrayList<>(); }
    public String systemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
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
        sb.append("\"updatedAt\":\"").append(updatedAt.toString()).append('\"');
        sb.append(',');
        sb.append("\"isPinned\":").append(isPinned);
        if (systemPrompt != null) {
            sb.append(',');
            sb.append("\"systemPrompt\":\"").append(escape(systemPrompt)).append('\"');
        }
        sb.append(',');
        sb.append("\"tags\":[");
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('\"').append(escape(tags.get(i))).append('\"');
        }
        sb.append(']');
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
