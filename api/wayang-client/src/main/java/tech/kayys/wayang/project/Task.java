package tech.kayys.wayang.project;

import java.time.Instant;
import java.util.Objects;

public final class Task {
    private final String id;
    private String description;
    private String status; // pending, in_progress, done
    private final Instant createdAt;

    public Task(String id, String description) {
        this.id = Objects.requireNonNull(id);
        this.description = description == null ? "" : description;
        this.status = "pending";
        this.createdAt = Instant.now();
    }

    public String id() { return id; }
    public String description() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String status() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Instant createdAt() { return createdAt; }
}
