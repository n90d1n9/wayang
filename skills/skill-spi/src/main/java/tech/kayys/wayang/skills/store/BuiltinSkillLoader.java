package tech.kayys.wayang.skills.store;

import tech.kayys.wayang.skill.spi.SkillDefinition;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads built-in skills from the classpath resource folder {@code default-skills/}.
 *
 * <p>The {@code skills/skills/} source directory is configured in the CLI's
 * {@code pom.xml} to be bundled into the uber-jar under {@code default-skills/}:
 * <pre>
 * &lt;resource&gt;
 *   &lt;directory&gt;../../skills/skills&lt;/directory&gt;
 *   &lt;targetPath&gt;default-skills&lt;/targetPath&gt;
 * &lt;/resource&gt;
 * </pre>
 *
 * <p>Each skill subdirectory must contain a {@code SKILL.md} file.
 * The skill id is derived from the directory name.
 *
 * <p>Builtin skills are marked as read-only and cannot be deleted from the CLI.
 */
public final class BuiltinSkillLoader {

    private static final Logger LOG = Logger.getLogger(BuiltinSkillLoader.class.getName());

    /**
     * Well-known list of built-in skill IDs.
     *
     * <p>These correspond to the directory names inside {@code skills/skills/}.
     * Enumerating classpath directories is JVM-dependent, so we keep a static
     * list and fall back gracefully for any that are missing at runtime.
     */
    private static final List<String> BUILTIN_IDS = List.of(
            "run-inference",
            "load-model-from-repository",
            "configure-plugin",
            "handle-multi-tenancy",
            "monitor-inference",
            "google-agents-cli-adk-code",
            "google-agents-cli-deploy",
            "google-agents-cli-eval",
            "google-agents-cli-observability",
            "google-agents-cli-publish",
            "google-agents-cli-scaffold",
            "google-agents-cli-workflow"
    );

    private BuiltinSkillLoader() {}

    /**
     * Load all built-in skills from the classpath and return them as
     * {@link SkillEntry} instances with {@code source=builtin} and
     * {@code readOnly=true}.
     *
     * @return list of loaded builtin skill entries (never null, may be empty)
     */
    public static List<SkillEntry> load() {
        List<SkillEntry> entries = new ArrayList<>();
        ClassLoader cl = BuiltinSkillLoader.class.getClassLoader();

        for (String id : BUILTIN_IDS) {
            String resourcePath = "default-skills/" + id + "/SKILL.md";
            InputStream is = cl.getResourceAsStream(resourcePath);
            if (is == null) {
                LOG.fine("Built-in skill not found on classpath: " + resourcePath);
                continue;
            }
            try {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                SkillDefinition def = SkillFileParser.parseSkillMd(id, content);
                SkillEntry entry = SkillEntry.builder()
                        .id(id)
                        .name(def.name())
                        .description(def.description())
                        .category(def.category())
                        .source("builtin")
                        .format(SkillEntry.SkillFormat.SKILL_MD)
                        .path(resourcePath)
                        .readOnly(true)
                        .enabled(true)
                        .definition(def)
                        .build();
                entries.add(entry);
                LOG.fine("Loaded builtin skill: " + id);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to load builtin skill '" + id + "': " + e.getMessage(), e);
            }
        }
        return entries;
    }
}
