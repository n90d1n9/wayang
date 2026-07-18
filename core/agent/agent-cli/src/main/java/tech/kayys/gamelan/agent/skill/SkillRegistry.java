package tech.kayys.gamelan.agent.skill;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Discovers, loads, caches, and manages Agent Skills.
 *
 * <p>Skills are searched in (priority order):
 * <ol>
 *   <li>Project-local {@code .gamelan/skills/} directory</li>
 *   <li>User-global {@code ~/.gamelan/skills/} directory</li>
 *   <li>Built-in bundled skills (shipped with the binary)</li>
 * </ol>
 *
 * <p>Skills are loaded lazily on first access and cached for the lifetime
 * of the process. The cache can be invalidated explicitly with {@link #reload()}.
 */
@ApplicationScoped
public class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    @Inject
    GamelanConfig config;

    @Inject
    SkillLoader loader;

    /** name → Skill, loaded on demand */
    private final Map<String, Skill> cache = new ConcurrentHashMap<>();
    private boolean initialized = false;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Returns all installed skills, sorted by name.
     */
    public List<Skill> listAll() {
        ensureInitialized();
        return cache.values().stream()
                .sorted(Comparator.comparing(Skill::name))
                .toList();
    }

    /**
     * Returns only enabled skills, sorted by name.
     */
    public List<Skill> listEnabled() {
        return listAll().stream().filter(Skill::enabled).toList();
    }

    /**
     * Finds a skill by exact name.
     */
    public Optional<Skill> find(String name) {
        ensureInitialized();
        return Optional.ofNullable(cache.get(name));
    }

    /**
     * Installs a skill from a local directory path or a GitHub URL.
     *
     * @param source local path or GitHub tree URL
     * @return the installed skill
     */
    public Skill install(String source) throws IOException {
        Path targetDir = globalSkillsDir();
        Files.createDirectories(targetDir);

        Path skillDir;
        if (source.startsWith("http://") || source.startsWith("https://")) {
            skillDir = installFromUrl(source, targetDir);
        } else {
            skillDir = installFromLocal(Path.of(source), targetDir);
        }

        Skill skill = loader.load(skillDir);
        cache.put(skill.name(), skill);
        log.info("Installed skill: {} from {}", skill.name(), source);
        return skill;
    }

    /**
     * Installs a skill from a local path into the given target directory, with optional name override.
     */
    public Skill install(Path sourcePath, Path targetDir, String overrideName) throws IOException {
        Files.createDirectories(targetDir);
        Path skillDir = installFromLocal(sourcePath, targetDir);
        Skill loaded = loader.load(skillDir);
        if (overrideName != null && !overrideName.isBlank() && !overrideName.equals(loaded.name())) {
            // Rename directory
            Path renamed = skillDir.getParent().resolve(overrideName);
            Files.move(skillDir, renamed);
            loaded = loader.load(renamed);
        }
        cache.put(loaded.name(), loaded);
        log.info("Installed skill: {} from {}", loaded.name(), sourcePath);
        return loaded;
    }

    /**
     * Removes an installed skill. Returns true if removed, false if not found.
     */
    public boolean remove(String name) throws IOException {
        Skill skill = cache.remove(name);
        if (skill == null) return false;
        for (Path dir : searchDirs()) {
            Path candidate = dir.resolve(name);
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("SKILL.md"))) {
                deleteDirectory(candidate);
                log.info("Removed skill directory: {}", candidate);
                return true;
            }
        }
        return true;
    }

    /**
     * Enables a skill by name (persists enabled state via cache replacement).
     */
    public void enable(String name) {
        ensureInitialized();
        Skill skill = cache.get(name);
        if (skill == null) throw new NoSuchElementException("Skill not found: " + name);
        if (!skill.enabled()) {
            cache.put(name, new Skill(skill.name(), skill.description(), skill.version(),
                    skill.source(), true, skill.path(), skill.license(), skill.compatibility(),
                    skill.metadata(), skill.allowedTools(), skill.keywords(), skill.commands(),
                    skill.dependencies(), skill.instructions(), skill.rawContent(),
                    skill.references(), skill.scriptPaths()));
        }
    }

    /**
     * Disables a skill by name.
     */
    public void disable(String name) {
        ensureInitialized();
        Skill skill = cache.get(name);
        if (skill == null) throw new NoSuchElementException("Skill not found: " + name);
        if (skill.enabled()) {
            cache.put(name, new Skill(skill.name(), skill.description(), skill.version(),
                    skill.source(), false, skill.path(), skill.license(), skill.compatibility(),
                    skill.metadata(), skill.allowedTools(), skill.keywords(), skill.commands(),
                    skill.dependencies(), skill.instructions(), skill.rawContent(),
                    skill.references(), skill.scriptPaths()));
        }
    }

    /**
     * Clears the cache and re-scans skill directories.
     */
    public void reload() {
        cache.clear();
        initialized = false;
        ensureInitialized();
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private synchronized void ensureInitialized() {
        if (initialized) return;
        for (Path dir : searchDirs()) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> entries = Files.list(dir)) {
                entries.filter(Files::isDirectory)
                       .filter(p -> Files.exists(p.resolve("SKILL.md")))
                       .forEach(skillDir -> {
                           try {
                               Skill skill = loader.load(skillDir);
                               cache.put(skill.name(), skill);
                               log.debug("Loaded skill: {} from {}", skill.name(), skillDir);
                           } catch (Exception e) {
                               log.warn("Failed to load skill from {}: {}", skillDir, e.getMessage());
                           }
                       });
            } catch (IOException e) {
                log.warn("Cannot scan skill directory {}: {}", dir, e.getMessage());
            }
        }
        initialized = true;
        log.info("Skill registry initialized: {} skills loaded", cache.size());
    }

    private List<Path> searchDirs() {
        return List.of(
                // Project-local
                Path.of(".gamelan", "skills"),
                // User-global (from config)
                globalSkillsDir(),
                // Built-in (bundled in jar)
                Path.of(System.getProperty("user.home"), ".gamelan", "skills", "_builtin")
        );
    }

    private Path globalSkillsDir() {
        String configured = config.skillsDir();
        return configured != null && !configured.isBlank()
                ? Path.of(configured)
                : Path.of(System.getProperty("user.home"), ".gamelan", "skills");
    }

    private Path installFromLocal(Path source, Path targetDir) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Not a directory: " + source);
        }
        if (!Files.exists(source.resolve("SKILL.md"))) {
            throw new IllegalArgumentException("No SKILL.md in: " + source);
        }
        String skillName = source.getFileName().toString();
        Path dest = targetDir.resolve(skillName);
        copyDirectory(source, dest);
        return dest;
    }

    private Path installFromUrl(String url, Path targetDir) throws IOException {
        // Download the SKILL.md from a GitHub raw URL pattern
        // GitHub URL: https://github.com/org/repo/tree/main/skill-name
        // → convert to: https://raw.githubusercontent.com/org/repo/main/skill-name/SKILL.md
        String rawBase = url.replace("https://github.com/", "https://raw.githubusercontent.com/")
                           .replace("/tree/", "/");
        if (!rawBase.endsWith("/")) rawBase += "/";

        String skillName = rawBase.substring(rawBase.lastIndexOf('/', rawBase.length() - 2) + 1,
                                              rawBase.length() - 1);
        Path skillDir = targetDir.resolve(skillName);
        Files.createDirectories(skillDir);

        HttpClient http = HttpClient.newHttpClient();
        downloadFile(http, rawBase + "SKILL.md", skillDir.resolve("SKILL.md"));

        // Attempt to download common optional files
        for (String optional : List.of("scripts/main.py", "scripts/main.sh",
                "references/REFERENCE.md", "assets/template.md")) {
            try {
                Path dest = skillDir.resolve(optional);
                Files.createDirectories(dest.getParent());
                downloadFile(http, rawBase + optional, dest);
            } catch (Exception ignored) { /* optional */ }
        }
        return skillDir;
    }

    private void downloadFile(HttpClient http, String url, Path dest) throws IOException {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200) {
                Files.write(dest, resp.body());
            } else {
                throw new IOException("HTTP " + resp.statusCode() + " for " + url);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    private void copyDirectory(Path src, Path dst) throws IOException {
        Files.walk(src).forEach(source -> {
            Path destination = dst.resolve(src.relativize(source));
            try {
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walk(dir)
             .sorted(Comparator.reverseOrder())
             .forEach(p -> {
                 try { Files.delete(p); }
                 catch (IOException e) { throw new RuntimeException(e); }
             });
    }
}
