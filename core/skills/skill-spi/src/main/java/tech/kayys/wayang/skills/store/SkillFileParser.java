package tech.kayys.wayang.skills.store;

import tech.kayys.wayang.skill.spi.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses SKILL.md files (YAML frontmatter + Markdown body) as well as
 * plain JSON / YAML files into {@link SkillDefinition} instances.
 *
 * <h3>SKILL.md format</h3>
 * <pre>
 * ---
 * name: my-skill
 * description: What this skill does
 * metadata:
 *   category: coding
 *   author: me
 *   tags: [java, refactor]
 *   tools: [bash, read_file]
 *   temperature: 0.7
 *   maxTokens: 4096
 *   provider: gollek
 * ---
 *
 * System prompt body goes here.
 * </pre>
 */
public final class SkillFileParser {

    private SkillFileParser() {}

    /** Parse raw SKILL.md text (YAML frontmatter + markdown body). */
    public static SkillDefinition parseSkillMd(String id, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Empty skill content for id: " + id);
        }
        content = content.stripLeading();

        // Extract frontmatter block
        Map<String, Object> frontmatter = Map.of();
        String body = content;

        if (content.startsWith("---")) {
            int end = content.indexOf("\n---", 3);
            if (end > 0) {
                String fm = content.substring(3, end).strip();
                frontmatter = parseSimpleYaml(fm);
                body = content.substring(end + 4).stripLeading();
            }
        }

        return buildFromFrontmatter(id, frontmatter, body);
    }

    /** Parse a JSON-formatted skill definition (uses simple property extraction). */
    public static SkillDefinition parseJson(String id, String json) {
        // Delegate to a minimal JSON extractor (no external deps — matches existing pattern)
        String name         = extractJsonString(json, "name");
        String description  = extractJsonString(json, "description");
        String category     = extractJsonString(json, "category");
        String systemPrompt = extractJsonString(json, "systemPrompt");
        String provider     = extractJsonString(json, "defaultProvider");
        String tempStr      = extractJsonString(json, "temperature");
        String tokensStr    = extractJsonString(json, "maxTokens");

        SkillDefinition.Builder b = SkillDefinition.builder()
                .id(id)
                .name(name.isEmpty() ? id : name)
                .description(description)
                .category(category.isEmpty() ? "custom" : category)
                .systemPrompt(systemPrompt.isEmpty() ? "(no system prompt)" : systemPrompt)
                .defaultProvider(provider.isEmpty() ? null : provider);

        if (!tempStr.isEmpty()) {
            try { b.temperature(Double.parseDouble(tempStr)); } catch (NumberFormatException ignored) {}
        }
        if (!tokensStr.isEmpty()) {
            try { b.maxTokens(Integer.parseInt(tokensStr)); } catch (NumberFormatException ignored) {}
        }
        return b.build();
    }

    /** Parse a YAML-formatted skill definition. */
    public static SkillDefinition parseYaml(String id, String yaml) {
        Map<String, Object> props = parseSimpleYaml(yaml);
        String body = (String) props.getOrDefault("systemPrompt", "");
        return buildFromFrontmatter(id, props, body);
    }

    // ─── internal helpers ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static SkillDefinition buildFromFrontmatter(String id, Map<String, Object> fm, String body) {
        String name        = strOf(fm, "name", id);
        String description = strOf(fm, "description", "");
        String category    = "custom";
        String provider    = null;
        Double temperature  = 0.7;
        Integer maxTokens  = 4096;
        List<String> tools = List.of();

        // Support both top-level keys and nested under "metadata:"
        Object metaObj = fm.get("metadata");
        Map<String, Object> meta = (metaObj instanceof Map<?, ?> m)
                ? (Map<String, Object>) m : Map.of();

        if (fm.containsKey("category"))    category    = strOf(fm, "category", category);
        if (meta.containsKey("category"))  category    = strOf(meta, "category", category);
        if (fm.containsKey("provider"))    provider    = strOf(fm, "provider", null);
        if (meta.containsKey("provider"))  provider    = strOf(meta, "provider", null);

        if (meta.containsKey("temperature")) {
            try { temperature = Double.parseDouble(meta.get("temperature").toString()); } catch (Exception ignored) {}
        }
        if (meta.containsKey("maxTokens")) {
            try { maxTokens = Integer.parseInt(meta.get("maxTokens").toString()); } catch (Exception ignored) {}
        }
        if (meta.containsKey("tools")) {
            Object toolsObj = meta.get("tools");
            if (toolsObj instanceof List<?> l) tools = l.stream().map(Object::toString).toList();
            else if (toolsObj instanceof String s && !s.isBlank())
                tools = List.of(s.split(",")).stream().map(String::strip).filter(t -> !t.isBlank()).toList();
        }

        // Merge all metadata into the definition's metadata map
        Map<String, Object> metadataMap = new LinkedHashMap<>(meta);
        fm.forEach((k, v) -> {
            if (!k.equals("name") && !k.equals("description") && !k.equals("metadata") && !k.equals("systemPrompt")) {
                metadataMap.put(k, v);
            }
        });

        String systemPrompt = body.isBlank() ? strOf(fm, "systemPrompt", "(no system prompt)") : body;

        return SkillDefinition.builder()
                .id(id)
                .name(name)
                .description(description)
                .category(category)
                .systemPrompt(systemPrompt)
                .defaultProvider(provider)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .tools(tools)
                .metadata(metadataMap)
                .build();
    }

    /**
     * Minimal YAML parser — supports:
     * <ul>
     *   <li>scalar keys:  {@code key: value}</li>
     *   <li>nested maps:  {@code metadata:\n  key: value}</li>
     *   <li>inline lists: {@code tags: [a, b, c]}</li>
     *   <li>multi-line lists: {@code tags:\n  - a\n  - b}</li>
     * </ul>
     * No external dependencies.
     */
    static Map<String, Object> parseSimpleYaml(String yaml) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (yaml == null || yaml.isBlank()) return result;

        String[] lines = yaml.split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (line.isBlank() || line.stripLeading().startsWith("#")) { i++; continue; }

            int colonIdx = line.indexOf(':');
            if (colonIdx < 0) { i++; continue; }

            String key   = line.substring(0, colonIdx).strip();
            String value = colonIdx + 1 < line.length() ? line.substring(colonIdx + 1).strip() : "";

            if (value.isEmpty()) {
                // Could be a nested map or a list
                Map<String, Object> nested = new LinkedHashMap<>();
                List<String> listItems = new java.util.ArrayList<>();
                int base = line.length() - line.stripLeading().length();
                i++;
                while (i < lines.length) {
                    String child = lines[i];
                    if (child.isBlank()) { i++; continue; }
                    int childIndent = child.length() - child.stripLeading().length();
                    if (childIndent <= base) break;
                    String stripped = child.strip();
                    if (stripped.startsWith("- ")) {
                        listItems.add(stripped.substring(2).strip());
                        i++;
                    } else {
                        int ci = stripped.indexOf(':');
                        if (ci > 0) {
                            String nk = stripped.substring(0, ci).strip();
                            String nv = ci + 1 < stripped.length() ? stripped.substring(ci + 1).strip() : "";
                            nested.put(nk, unquote(nv));
                            i++;
                        } else { i++; }
                    }
                }
                result.put(key, listItems.isEmpty() ? nested : listItems);
            } else if (value.startsWith("[")) {
                // Inline list: [a, b, c]
                String inner = value.replaceAll("^\\[|\\]$", "").strip();
                List<String> items = List.of(inner.split(",")).stream()
                        .map(s -> unquote(s.strip())).filter(s -> !s.isBlank()).toList();
                result.put(key, items);
                i++;
            } else {
                result.put(key, unquote(value));
                i++;
            }
        }
        return result;
    }

    private static String strOf(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return (v == null || v.toString().isBlank()) ? (def == null ? "" : def) : v.toString();
    }

    private static String unquote(String s) {
        if (s == null) return "";
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))
            return s.substring(1, s.length() - 1);
        return s;
    }

    private static String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return "";
        int start = idx + searchKey.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') break;
            if (c == '\\' && i + 1 < json.length()) sb.append(json.charAt(++i));
            else sb.append(c);
        }
        return sb.toString();
    }
}
