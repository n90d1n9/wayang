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
                allowedTools, instructions, raw, references, scriptPaths);

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

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Selects which skills are relevant to a user message.
 *
 * <h2>Scoring algorithm</h2>
 * A simple weighted keyword overlap:
 * <ul>
 *   <li>+10  — exact skill name appears in the input</li>
 *   <li>+3   — keyword (≥5 chars) from description matches in the input</li>
 *   <li>+1   — short keyword (4 chars) matches</li>
 *   <li>Bonus for input words that also appear in the skill name</li>
 * </ul>
 *
 * <p>This intentionally errs on the side of activating slightly more skills
 * rather than fewer — an irrelevant skill wastes a few hundred tokens,
 * while a missing relevant skill makes the model unaware of key context.
 *
 * <p>Future enhancement: replace keyword matching with cosine similarity
 * over Gollek embeddings when an embedding model is available.
 */
@ApplicationScoped
class SkillSelector {

    private static final int MAX_SKILLS = 3;
    private static final int MIN_SCORE  = 1;

    public List<Skill> select(String userMessage, List<Skill> allSkills) {
        if (allSkills.isEmpty() || userMessage == null || userMessage.isBlank()) {
            return List.of();
        }
        String input = userMessage.toLowerCase();
        Set<String> inputWords = tokenize(input);

        record Scored(Skill skill, int score) {}

        return allSkills.stream()
                .map(s -> new Scored(s, score(input, inputWords, s)))
                .filter(s -> s.score() >= MIN_SCORE)
                .sorted(Comparator.comparingInt(Scored::score).reversed())
                .limit(MAX_SKILLS)
                .map(Scored::skill)
                .toList();
    }

    private int score(String input, Set<String> inputWords, Skill skill) {
        int score = 0;

        // Exact skill name match in input → big boost
        if (input.contains(skill.name().replace("-", " "))
                || input.contains(skill.name())) {
            score += 10;
        }

        // Keyword overlap: description words found in the input
        Set<String> descWords = tokenize(skill.description() + " " + skill.name());
        for (String word : descWords) {
            if (!inputWords.contains(word)) continue;
            score += word.length() >= 5 ? 3 : 1;
        }

        return score;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s,;.!?\\-_/]+"))
                .filter(w -> w.length() >= 4)
                .collect(Collectors.toSet());
    }
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Validates a skill directory against the agentskills.io specification.
 *
 * <p>Returns a list of human-readable errors. An empty list means the skill
 * is spec-compliant and ready to use.
 */
@ApplicationScoped
class SkillValidator {

    /** Spec §name: lowercase alphanumeric + hyphens, 1–64 chars, no leading/trailing hyphen. */
    private static final Pattern VALID_NAME = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,62}[a-z0-9])?$");

    public List<String> validate(Path skillDir) {
        List<String> errors = new ArrayList<>();

        // 1. SKILL.md must exist
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            errors.add("Missing SKILL.md in " + skillDir);
            return errors;
        }

        // 2. Must be readable
        String content;
        try {
            content = Files.readString(skillMd);
        } catch (IOException e) {
            errors.add("Cannot read SKILL.md: " + e.getMessage());
            return errors;
        }

        // 3. Must start with frontmatter
        if (!content.startsWith("---")) {
            errors.add("SKILL.md must start with YAML frontmatter (---)");
        }

        // 4. Parse and validate fields
        SkillLoader loader = new SkillLoader();
        Skill skill;
        try {
            skill = loader.load(skillDir);
        } catch (Exception e) {
            errors.add("Failed to parse SKILL.md: " + e.getMessage());
            return errors;
        }

        // name
        if (skill.name().isBlank()) {
            errors.add("'name' field is required");
        } else if (skill.name().length() > 64) {
            errors.add("'name' must be ≤64 chars, got " + skill.name().length());
        } else if (!VALID_NAME.matcher(skill.name()).matches()) {
            errors.add("'name' must be lowercase alphanumeric + hyphens: " + skill.name());
        }

        // name must match directory name
        String dirName = skillDir.getFileName().toString();
        if (!dirName.equals(skill.name())) {
            errors.add("Directory name '" + dirName + "' must match skill name '" + skill.name() + "'");
        }

        // description
        if (skill.description().isBlank()) {
            errors.add("'description' field is required");
        } else if (skill.description().length() > 1024) {
            errors.add("'description' must be ≤1024 chars, got " + skill.description().length());
        }

        // compatibility (optional, max 500)
        if (skill.compatibility().length() > 500) {
            errors.add("'compatibility' must be ≤500 chars, got " + skill.compatibility().length());
        }

        // instructions body must be non-empty
        if (skill.instructions().isBlank()) {
            errors.add("SKILL.md body (instructions after frontmatter) is empty");
        }

        // Warn if scripts reference non-existent files
        Path scriptsDir = skillDir.resolve("scripts");
        if (!skill.scriptPaths().isEmpty() && !Files.isDirectory(scriptsDir)) {
            errors.add("'scripts/' directory referenced but does not exist");
        }

        return errors;
    }
}
