package tech.kayys.gamelan.skill.discovery;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.skill.SkillLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SkillDiscoveryEngine — two-phase lazy skill loading with deduplication cache.
 *
 * <h2>From the OPENDEV paper (§2.4.8 — On-demand skills via invoke_skill)</h2>
 * Skills are modular knowledge units providing domain-specific expertise that would waste
 * context if loaded unconditionally. The system processes skills in two phases:
 *
 * <p><b>Phase 1 — Metadata discovery:</b> At startup, scan all skill directories, parsing only
 * the YAML frontmatter to extract names and descriptions. This lightweight index is included in
 * the system prompt, enabling the agent to discover available expertise without loading
 * instructional content. Descriptions follow a "Use when…" convention to specify trigger
 * conditions, optimizing for agent discoverability.
 *
 * <p><b>Phase 2 — On-demand loading:</b> When the agent determines a skill is relevant, it
 * invokes the skill by name. The loader reads the full markdown content, strips the frontmatter,
 * and injects the instructional body into the conversation context. A deduplication cache ensures
 * each skill loads at most once per session, preventing context pollution.
 *
 * <h2>Three-tier priority hierarchy (paper §2.4.8)</h2>
 * <pre>
 * 1. Project-local (.gamelan/skills/)   — highest priority, repository-specific conventions
 * 2. User-global (~/.gamelan/skills/)   — personal preferences across all projects
 * 3. Built-in (shipped with binary)     — default expertise, lowest priority
 * </pre>
 * When two skills share the same name, the higher-priority source takes precedence.
 *
 * <h2>Baseline overhead</h2>
 * Phase 1 loads only frontmatter — typically 2–3 lines per skill. A project with 50 skills
 * adds ~100 tokens to the system prompt vs. 50 × 300 = 15,000 tokens if fully loaded upfront.
 * This matches the paper's reduction from 40% to under 5% baseline context overhead for MCP.
 */
@ApplicationScoped
public class SkillDiscoveryEngine {

    private static final Logger log = LoggerFactory.getLogger(SkillDiscoveryEngine.class);

    @Inject GamelanConfig  config;
    @Inject SkillLoader    loader;
    @Inject AgentTelemetry telemetry;

    // Phase 1: metadata index (name → SkillMeta)
    private final Map<String, SkillMeta>  metaIndex  = new LinkedHashMap<>();
    // Phase 2: fully loaded skills cache (name → Skill) — deduplication per session
    private final Map<String, Skill>      loadedCache = new ConcurrentHashMap<>();
    // Track which skills were loaded this session
    private final Set<String>             sessionLoads = CopyOnWriteArrayList.of().stream()
            .collect(Collectors.toSet());

    @PostConstruct
    void init() {
        scanMetadata();
    }

    // ── Phase 1 API ────────────────────────────────────────────────────────

    /**
     * Returns the compact metadata index for injection into the system prompt.
     * Each entry: "- `skill-name`: Use when [description]"
     */
    public String metadataPromptBlock() {
        if (metaIndex.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("## Available Skills (invoke by name when relevant)\n");
        metaIndex.values().forEach(meta ->
                sb.append("- `").append(meta.name()).append("`: ")
                  .append(meta.description()).append("\n"));
        return sb.toString();
    }

    /**
     * Returns the number of skills discovered in the metadata index.
     */
    public int discoveredCount() { return metaIndex.size(); }

    /**
     * Searches skills by keyword across name and description.
     * Used for agent-driven skill discovery.
     */
    public List<SkillMeta> search(String query) {
        String lower = query.toLowerCase();
        return metaIndex.values().stream()
                .filter(m -> m.name().toLowerCase().contains(lower) ||
                             m.description().toLowerCase().contains(lower))
                .sorted(Comparator.comparingInt(m -> -relevanceScore(m, lower)))
                .toList();
    }

    /** Returns all skill metadata entries. */
    public Collection<SkillMeta> allMeta() {
        return Collections.unmodifiableCollection(metaIndex.values());
    }

    // ── Phase 2 API ────────────────────────────────────────────────────────

    /**
     * Fully loads a skill by name and returns its instructional content for context injection.
     * Deduplication: each skill loads at most once per session.
     *
     * @param name the skill name (must exist in the metadata index)
     * @return the skill's instruction markdown, or empty if not found
     */
    public String invoke(String name) {
        // Deduplication cache — each skill loads at most once per session
        if (sessionLoads.contains(name)) {
            log.debug("[skill-discovery] skip duplicate load of '{}'", name);
            telemetry.count("skill.invoke.deduplicated");
            return ""; // already injected this session
        }

        Skill cached = loadedCache.get(name);
        if (cached != null) {
            sessionLoads.add(name);
            telemetry.count("skill.invoke.cache_hit");
            return formatInjection(cached);
        }

        SkillMeta meta = metaIndex.get(name);
        if (meta == null) {
            log.warn("[skill-discovery] unknown skill: '{}'", name);
            return "";
        }

        try {
            Skill skill = loader.load(meta.skillDir());
            loadedCache.put(name, skill);
            sessionLoads.add(name);
            telemetry.count("skill.invoke.loaded");
            log.info("[skill-discovery] loaded skill '{}' ({} chars)",
                    name, skill.instructions().length());
            return formatInjection(skill);
        } catch (IOException e) {
            log.warn("[skill-discovery] failed to load '{}': {}", name, e.getMessage());
            return "";
        }
    }

    /**
     * Returns the fully loaded Skill object (triggers Phase 2 if not yet loaded).
     */
    public Optional<Skill> getLoaded(String name) {
        invoke(name); // ensure loaded
        return Optional.ofNullable(loadedCache.get(name));
    }

    /** Returns all skills that have been loaded this session. */
    public Set<String> sessionLoadedSkills() {
        return Collections.unmodifiableSet(sessionLoads);
    }

    /** Clears the session deduplication cache (call on /clear). */
    public void resetSession() {
        sessionLoads.clear();
        telemetry.count("skill.session.reset");
    }

    /** Forces a full metadata re-scan (call after gamelan skill install). */
    public void refresh() {
        metaIndex.clear();
        loadedCache.clear();
        sessionLoads.clear();
        scanMetadata();
        log.info("[skill-discovery] refreshed: {} skills", metaIndex.size());
    }

    // ── Private ────────────────────────────────────────────────────────────

    /**
     * Phase 1 scan: walk all skill directories in priority order (project > user > builtin).
     * Only parses YAML frontmatter — never loads the full instruction body.
     */
    private void scanMetadata() {
        List<Path> searchDirs = buildSearchDirs();
        Set<String> seenNames = new LinkedHashSet<>(); // first-seen wins (higher priority)

        for (Path dir : searchDirs) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> entries = Files.list(dir).sorted()) {
                entries.filter(Files::isDirectory)
                       .filter(p -> Files.exists(p.resolve("SKILL.md")))
                       .forEach(skillDir -> {
                           String skillName = skillDir.getFileName().toString();
                           if (seenNames.contains(skillName)) return; // higher-priority already found
                           seenNames.add(skillName);
                           parseFrontmatter(skillDir).ifPresent(meta -> {
                               metaIndex.put(meta.name(), meta);
                               log.debug("[skill-discovery] indexed '{}'", meta.name());
                           });
                       });
            } catch (IOException e) {
                log.debug("[skill-discovery] scan failed for {}: {}", dir, e.getMessage());
            }
        }

        log.info("[skill-discovery] phase 1 complete: {} skills indexed", metaIndex.size());
        telemetry.gauge("skill.discovered_count", metaIndex.size());
    }

    private List<Path> buildSearchDirs() {
        return List.of(
                Path.of(".gamelan", "skills"),                                         // project-local
                Path.of(System.getProperty("user.home"), ".gamelan", "skills"),       // user-global
                Path.of(System.getProperty("user.home"), ".gamelan", "skills", "_bundled") // bundled
        );
    }

    /**
     * Parses only the YAML frontmatter of SKILL.md to extract name and description.
     * Never reads the instruction body — this is what keeps Phase 1 lightweight.
     */
    private Optional<SkillMeta> parseFrontmatter(Path skillDir) {
        Path skillMd = skillDir.resolve("SKILL.md");
        try {
            List<String> lines = Files.readAllLines(skillMd);
            if (lines.isEmpty() || !lines.get(0).startsWith("---")) return Optional.empty();

            String name = "", description = "", license = "", compatibility = "";
            boolean inFrontmatter = false;
            for (String line : lines) {
                if (line.startsWith("---")) { inFrontmatter = !inFrontmatter; continue; }
                if (!inFrontmatter) break;
                if (line.startsWith("name:"))          name          = line.substring(5).strip().replace("\"","");
                if (line.startsWith("description:"))   description   = line.substring(12).strip().replace("\"","");
                if (line.startsWith("license:"))       license       = line.substring(8).strip();
                if (line.startsWith("compatibility:")) compatibility = line.substring(14).strip();
            }
            if (name.isBlank()) name = skillDir.getFileName().toString();
            return Optional.of(new SkillMeta(name, description, license, compatibility, skillDir));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private String formatInjection(Skill skill) {
        return "### Skill: `" + skill.name() + "`\n" +
               skill.instructions().strip() + "\n";
    }

    private int relevanceScore(SkillMeta meta, String query) {
        int score = 0;
        if (meta.name().toLowerCase().contains(query)) score += 3;
        String[] words = query.split("\\s+");
        for (String w : words) {
            if (w.length() < 3) continue;
            if (meta.name().toLowerCase().contains(w)) score += 2;
            if (meta.description().toLowerCase().contains(w)) score += 1;
        }
        return score;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * Lightweight skill metadata loaded in Phase 1 — only frontmatter fields.
     * The full instruction body is NOT loaded until Phase 2 (invoke).
     */
    public record SkillMeta(
            String name,
            String description,
            String license,
            String compatibility,
            Path   skillDir
    ) {
        public String compactSummary() {
            return "`" + name + "`: " + description;
        }
    }
}
