package tech.kayys.wayang.sdk.gollek.model;

import java.time.Instant;
import java.util.Objects;

public final class Session {
    private final String id;
    private String name;
    private final Instant createdAt;
    private Instant lastAccess;

    public Session(String id, String name) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? id : name;
        this.createdAt = Instant.now();
        this.lastAccess = this.createdAt;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant createdAt() { return createdAt; }
    public Instant lastAccess() { return lastAccess; }
    public void touch() { this.lastAccess = Instant.now(); }

    public String toJson() {
        return "{\"id\":\"" + escape(id) + "\",\"name\":\"" + escape(name) + "\",\"createdAt\":\"" + createdAt.toString() + "\",\"lastAccess\":\"" + lastAccess.toString() + "\"}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
