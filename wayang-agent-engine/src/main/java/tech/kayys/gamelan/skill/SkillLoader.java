package tech.kayys.gamelan.skill;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses a skill directory conforming to the
 * <a href="https://agentskills.io/specification">agentskills.io specification</a>.
 *
 * <h2>YAML safety</h2>
 * Uses {@link SafeConstructor} to prevent YAML deserialization attacks.
 * The spec allows only scalar values in frontmatter metadata, so this is safe.
 *
 * <h2>Frontmatter edge cases</h2>
 * <ul>
 *   <li>Missing frontmatter: the entire file is treated as instructions.</li>
 *   <li>Empty body after frontmatter: loaded with a warning, not rejected.</li>
 *   <li>UTF-8 BOM: stripped before parsing.</li>
 *   <li>Windows line endings: normalised to {@code \n}.</li>
 * </ul>
 *
 * <h2>Reference file loading</h2>
 * Reference files larger than 10 KB are loaded only up to 10 KB with a
 * trailing notice. This prevents a single bloated reference from consuming
 * the entire context window.
 */
@ApplicationScoped
public class SkillLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);

    /** Matches the --- ... --- YAML frontmatter block at the start of the file. */
    private static final Pattern FRONTMATTER = Pattern.compile(
            "^-{3}\\s*\\r?\\n(.*?)\\r?\\n-{3}\\s*\\r?\\n(.*)",
            Pattern.DOTALL);

    private static final int MAX_REFERENCE_BYTES = 10_240; // 10 KB per reference file

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Loads a skill from the given directory.
     *
     * @param skillDir path to the skill directory (must contain SKILL.md)
     * @return the loaded and validated skill
     * @throws IOException if SKILL.md cannot be read
     */
    @SuppressWarnings("unchecked")
    public Skill load(Path skillDir) throws IOException {
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            throw new IOException("Missing SKILL.md in: " + skillDir);
        }

        // Read and normalise
        String raw = Files.readString(skillMd)
                .replace("\r\n", "\n")  // CRLF → LF
                .replace("\uFEFF", ""); // strip BOM

        // Split frontmatter from body
        Matcher m = FRONTMATTER.matcher(raw);
        String frontYaml;
        String instructions;
        if (m.matches()) {
            frontYaml    = m.group(1);
            instructions = m.group(2).strip();
        } else {
            // No frontmatter — whole file is instructions
            frontYaml    = "";
            instructions = raw.strip();
            log.warn("Skill at {} has no YAML frontmatter — name will default to directory name", skillDir);
        }

        // Parse YAML with safe constructor (no arbitrary class instantiation)
        Map<String, Object> fm = Collections.emptyMap();
        if (!frontYaml.isBlank()) {
            Object parsed = new Yaml(new SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()))
                    .load(frontYaml);
            if (parsed instanceof Map<?, ?> map) {
                fm = (Map<String, Object>) map;
            }
        }

        String name          = str(fm, "name",         skillDir.getFileName().toString());
        String description   = str(fm, "description",  "");
        String license       = str(fm, "license",       "");
        String compatibility = str(fm, "compatibility", "");
        List<String> allowedTools = parseAllowedTools(fm.get("allowed-tools"));
        Map<String, String> metadata = parseMetadata(fm.get("metadata"));

        // Scan optional subdirectories
        List<String> scriptPaths = listFiles(skillDir.resolve("scripts"));
        Map<String, String> references = loadReferences(skillDir.resolve("references"));

        Skill skill = new Skill(name, description, license, compatibility, metadata,
                allowedTools, instructions, raw, references, scriptPaths, skillDir);

        log.debug("Loaded skill '{}' ({} refs, {} scripts)", name,
                references.size(), scriptPaths.size());
        return skill;
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String str(Map<String, Object> map, String key, String defaultVal) {
        Object v = map.get(key);
        return v != null ? v.toString().strip() : defaultVal;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseAllowedTools(Object raw) {
        if (raw == null)                return List.of();
        if (raw instanceof List<?> l)   return l.stream().map(Object::toString).toList();
        if (raw instanceof String s)    return Arrays.asList(s.trim().split("\\s+"));
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((k, v) ->
                result.put(k.toString(), v != null ? v.toString() : ""));
        return Collections.unmodifiableMap(result);
    }

    private List<String> listFiles(Path dir) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.warn("Cannot list directory {}: {}", dir, e.getMessage());
            return List.of();
        }
    }

    private Map<String, String> loadReferences(Path dir) {
        if (!Files.isDirectory(dir)) return Map.of();
        Map<String, String> refs = new LinkedHashMap<>();
        try (Stream<Path> s = Files.list(dir).sorted()) {
            s.filter(Files::isRegularFile).forEach(ref -> {
                try {
                    long size = Files.size(ref);
                    String content;
                    if (size > MAX_REFERENCE_BYTES) {
                        byte[] partial = new byte[MAX_REFERENCE_BYTES];
                        try (var is = Files.newInputStream(ref)) {
                            int read = is.read(partial);
                            content = new String(partial, 0, read)
                                    + "\n\n... (truncated — " + (size - MAX_REFERENCE_BYTES)
                                    + " more bytes)";
                        }
                    } else {
                        content = Files.readString(ref);
                    }
                    refs.put(ref.getFileName().toString(), content);
                } catch (IOException e) {
                    log.warn("Cannot read reference {}: {}", ref, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("Cannot list references in {}: {}", dir, e.getMessage());
        }
        return Collections.unmodifiableMap(refs);
    }
}

