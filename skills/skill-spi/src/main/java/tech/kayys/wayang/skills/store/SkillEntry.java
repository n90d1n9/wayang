package tech.kayys.wayang.skills.store;

import tech.kayys.wayang.skill.spi.SkillDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unified skill entry — represents a skill at runtime regardless of its
 * storage backend (classpath builtin, local SKILL.md file, JSON, YAML, or DB).
 *
 * <p>Source values:
 * <ul>
 *   <li>{@code builtin} — shipped with the platform, read-only</li>
 *   <li>{@code user}    — user-created, stored in ~/.wayang/skills/</li>
 *   <li>{@code custom}  — loaded from a custom load path</li>
 *   <li>{@code db}      — loaded from a database backend (pro/enterprise)</li>
 * </ul>
 */
public record SkillEntry(
        String id,
        String name,
        String description,
        String category,
        String source,        // builtin | user | custom | db
        SkillFormat format,   // SKILL_MD | JSON | YAML
        String path,          // file path or null for db/classpath
        boolean enabled,
        boolean readOnly,     // true for builtins
        Instant createdAt,
        Instant updatedAt,
        SkillDefinition definition
) {
    /** Supported skill file formats. */
    public enum SkillFormat { SKILL_MD, JSON, YAML }

    /** Whether this is a built-in (platform-shipped) skill. */
    public boolean isBuiltin() { return "builtin".equals(source); }

    /** Whether this skill can be modified or deleted. */
    public boolean isMutable() { return !readOnly; }

    /** Convert to a display summary line. */
    public String toSummaryLine() {
        String srcTag = switch (source) {
            case "builtin" -> "[builtin]";
            case "user"    -> "[user]  ";
            case "custom"  -> "[custom]";
            case "db"      -> "[db]    ";
            default        -> "[?]     ";
        };
        String status = enabled ? "" : " (disabled)";
        return String.format("  %-30s %-10s %s%s", id, srcTag, description != null ? description : "", status);
    }

    /** Builder for constructing entries fluently. */
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id, name, description, category, source, path;
        private SkillFormat format = SkillFormat.SKILL_MD;
        private boolean enabled = true, readOnly = false;
        private Instant createdAt = Instant.now(), updatedAt = Instant.now();
        private SkillDefinition definition;

        public Builder id(String v) { id = v; return this; }
        public Builder name(String v) { name = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder category(String v) { category = v; return this; }
        public Builder source(String v) { source = v; return this; }
        public Builder format(SkillFormat v) { format = v; return this; }
        public Builder path(String v) { path = v; return this; }
        public Builder enabled(boolean v) { enabled = v; return this; }
        public Builder readOnly(boolean v) { readOnly = v; return this; }
        public Builder createdAt(Instant v) { createdAt = v; return this; }
        public Builder updatedAt(Instant v) { updatedAt = v; return this; }
        public Builder definition(SkillDefinition v) { definition = v; return this; }

        public SkillEntry build() {
            return new SkillEntry(id, name, description, category, source, format,
                    path, enabled, readOnly, createdAt, updatedAt, definition);
        }
    }
}
