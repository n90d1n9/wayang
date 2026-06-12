package tech.kayys.gamelan.agent.skill;

import jakarta.enterprise.context.ApplicationScoped;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Loads a {@link Skill} from a directory conforming to the agentskills.io specification.
 *
 * <p>Parsing strategy:
 * <ol>
 *   <li>Read {@code SKILL.md}</li>
 *   <li>Extract YAML frontmatter (between first pair of {@code ---} delimiters)</li>
 *   <li>The remainder is the Markdown instruction body</li>
 *   <li>Scan {@code scripts/} for script file names</li>
 *   <li>Read all files in {@code references/} into a name→content map</li>
 * </ol>
 */
@ApplicationScoped
public class SkillLoader {

    private static final Pattern FRONTMATTER = Pattern.compile(
            "^---\\s*\\n(.*?)\\n---\\s*\\n(.*)", Pattern.DOTALL);

    /**
     * Loads a skill from the given directory.
     *
     * @param skillDir path to the skill directory (must contain SKILL.md)
     * @return the loaded skill
     * @throws IOException if files cannot be read
     */
    @SuppressWarnings("unchecked")
    public Skill load(Path skillDir) throws IOException {
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            throw new IOException("No SKILL.md in: " + skillDir);
        }

        String rawContent = Files.readString(skillMd);
        Matcher m = FRONTMATTER.matcher(rawContent);

        String frontmatterYaml = "";
        String instructions = rawContent;

        if (m.matches()) {
            frontmatterYaml = m.group(1);
            instructions = m.group(2).strip();
        }

        // Parse YAML frontmatter
        Map<String, Object> fm = frontmatterYaml.isBlank()
                ? Map.of()
                : new Yaml().load(frontmatterYaml);
        fm = fm != null ? fm : Map.of();

        String name = getString(fm, "name", skillDir.getFileName().toString());
        String description = getString(fm, "description", "");
        String version = getString(fm, "version", null);
        String license = getString(fm, "license", "");
        String compatibility = getString(fm, "compatibility", "");
        List<String> allowedTools = parseList(fm.get("allowed-tools"));
        List<String> keywords = parseList(fm.get("keywords"));
        List<String> commands = parseList(fm.get("commands"));
        List<String> dependencies = parseList(fm.get("dependencies"));
        Map<String, String> metadata = parseMetadata(fm.get("metadata"));

        String source = resolveSource(skillDir);

        // Scan scripts/
        List<String> scriptPaths = scanDirectory(skillDir.resolve("scripts"));

        // Load references/
        Map<String, String> references = loadReferences(skillDir.resolve("references"));

        return new Skill(name, description, version, source, true, skillDir,
                license, compatibility, metadata, allowedTools, keywords, commands,
                dependencies, instructions, rawContent, references, scriptPaths);
    }

    private String resolveSource(Path skillDir) {
        String home = System.getProperty("user.home");
        String abs = skillDir.toAbsolutePath().toString();
        if (abs.contains(home + "/.gamelan/skills/_builtin")) return "builtin";
        if (abs.startsWith(home)) return "global";
        return "local";
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseList(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) return list.stream().map(Object::toString).toList();
        if (raw instanceof String s) return Arrays.asList(s.split("\\s+"));
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(Object raw) {
        if (raw == null) return Map.of();
        if (raw instanceof Map<?, ?> map) {
            Map<String, String> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(k.toString(), v != null ? v.toString() : ""));
            return Collections.unmodifiableMap(result);
        }
        return Map.of();
    }

    private List<String> scanDirectory(Path dir) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private Map<String, String> loadReferences(Path dir) {
        if (!Files.isDirectory(dir)) return Map.of();
        Map<String, String> refs = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                 .sorted()
                 .forEach(p -> {
                     try {
                         refs.put(p.getFileName().toString(), Files.readString(p));
                     } catch (IOException e) { /* skip */ }
                 });
        } catch (IOException e) { /* empty */ }
        return Collections.unmodifiableMap(refs);
    }
}
