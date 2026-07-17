package tech.kayys.wayang.skills.store;

import tech.kayys.wayang.skill.spi.SkillDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Serialises a {@link SkillDefinition} back to SKILL.md, JSON, or YAML format.
 */
public final class SkillFileWriter {

    private SkillFileWriter() {}

    // ─── SKILL.md ───────────────────────────────────────────────────────────

    /** Render a SkillDefinition as a SKILL.md document. */
    public static String toSkillMd(SkillDefinition def) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(def.name() != null ? def.name() : def.id()).append("\n");
        if (def.description() != null && !def.description().isBlank())
            sb.append("description: ").append(def.description()).append("\n");
        sb.append("metadata:\n");
        if (def.category() != null && !def.category().isBlank())
            sb.append("  category: ").append(def.category()).append("\n");
        if (def.defaultProvider() != null && !def.defaultProvider().isBlank())
            sb.append("  provider: ").append(def.defaultProvider()).append("\n");
        if (def.temperature() != null)
            sb.append("  temperature: ").append(def.temperature()).append("\n");
        if (def.maxTokens() != null)
            sb.append("  maxTokens: ").append(def.maxTokens()).append("\n");
        if (!def.tools().isEmpty()) {
            sb.append("  tools: [").append(String.join(", ", def.tools())).append("]\n");
        }
        // Write any extra metadata from the definition's metadata map
        for (Map.Entry<String, Object> e : def.metadata().entrySet()) {
            String key = e.getKey();
            if (key.equals("category") || key.equals("provider") || key.equals("temperature")
                    || key.equals("maxTokens") || key.equals("tools")) continue;
            sb.append("  ").append(key).append(": ").append(e.getValue()).append("\n");
        }
        sb.append("---\n\n");
        sb.append(def.systemPrompt() != null ? def.systemPrompt() : "").append("\n");
        return sb.toString();
    }

    // ─── JSON ───────────────────────────────────────────────────────────────

    /** Render a SkillDefinition as a JSON document. */
    public static String toJson(SkillDefinition def) {
        StringBuilder sb = new StringBuilder("{\n");
        appendJsonStr(sb, "  ", "id", def.id(), false);
        appendJsonStr(sb, "  ", "name", def.name(), false);
        appendJsonStr(sb, "  ", "description", def.description(), false);
        appendJsonStr(sb, "  ", "category", def.category(), false);
        appendJsonStr(sb, "  ", "systemPrompt", def.systemPrompt(), false);
        appendJsonStr(sb, "  ", "defaultProvider", def.defaultProvider(), false);
        appendJsonStr(sb, "  ", "fallbackProvider", def.fallbackProvider(), false);
        if (def.temperature() != null)
            sb.append("  \"temperature\": ").append(def.temperature()).append(",\n");
        if (def.maxTokens() != null)
            sb.append("  \"maxTokens\": ").append(def.maxTokens()).append(",\n");
        if (!def.tools().isEmpty()) {
            sb.append("  \"tools\": [");
            StringJoiner sj = new StringJoiner(", ");
            def.tools().forEach(t -> sj.add("\"" + escape(t) + "\""));
            sb.append(sj).append("],\n");
        }
        // metadata object
        sb.append("  \"metadata\": {\n");
        boolean first = true;
        for (Map.Entry<String, Object> e : def.metadata().entrySet()) {
            if (!first) sb.append(",\n");
            sb.append("    \"").append(escape(e.getKey())).append("\": ");
            Object v = e.getValue();
            if (v instanceof Number) sb.append(v);
            else if (v instanceof Boolean) sb.append(v);
            else sb.append("\"").append(escape(String.valueOf(v))).append("\"");
            first = false;
        }
        sb.append("\n  }\n}\n");
        return sb.toString();
    }

    // ─── YAML ───────────────────────────────────────────────────────────────

    /** Render a SkillDefinition as a YAML document. */
    public static String toYaml(SkillDefinition def) {
        StringBuilder sb = new StringBuilder();
        appendYaml(sb, "id", def.id());
        appendYaml(sb, "name", def.name());
        appendYaml(sb, "description", def.description());
        appendYaml(sb, "category", def.category());
        appendYaml(sb, "defaultProvider", def.defaultProvider());
        if (def.temperature() != null) sb.append("temperature: ").append(def.temperature()).append("\n");
        if (def.maxTokens() != null)   sb.append("maxTokens: ").append(def.maxTokens()).append("\n");
        if (!def.tools().isEmpty()) {
            sb.append("tools:\n");
            def.tools().forEach(t -> sb.append("  - ").append(t).append("\n"));
        }
        if (!def.metadata().isEmpty()) {
            sb.append("metadata:\n");
            def.metadata().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        }
        if (def.systemPrompt() != null && !def.systemPrompt().isBlank()) {
            sb.append("systemPrompt: |\n");
            for (String line : def.systemPrompt().split("\n"))
                sb.append("  ").append(line).append("\n");
        }
        return sb.toString();
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private static void appendJsonStr(StringBuilder sb, String indent, String key, String value, boolean last) {
        if (value == null || value.isBlank()) return;
        sb.append(indent).append("\"").append(escape(key)).append("\": \"").append(escape(value)).append("\"").append(",\n");
    }

    private static void appendYaml(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) return;
        sb.append(key).append(": ").append(yamlQuote(value)).append("\n");
    }

    private static String yamlQuote(String s) {
        if (s.contains(":") || s.contains("#") || s.startsWith(" "))
            return "\"" + escape(s) + "\"";
        return s;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
