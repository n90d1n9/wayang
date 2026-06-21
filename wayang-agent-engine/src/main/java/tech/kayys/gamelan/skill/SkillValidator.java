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
 * Validates a skill directory against the agentskills.io specification.
 *
 * <p>Returns a list of human-readable errors. An empty list means the skill
 * is spec-compliant and ready to use.
 */
@ApplicationScoped
public class SkillValidator {

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
