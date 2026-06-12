package tech.kayys.gamelan.context.prompt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * ConditionalPromptComposer — assembles system prompts from modular, priority-ordered sections.
 *
 * <h2>From the OPENDEV paper (§2.3.1)</h2>
 * A naive approach loads a single monolithic prompt containing every possible instruction. This
 * has two compounding costs: (1) sections irrelevant to the current session consume context-window
 * budget without contributing behavioral value; (2) irrelevant instructions dilute the sections
 * that do matter, making the agent's behavior noisier.
 *
 * <h2>Priority-ordered conditional composition (paper §2.3.1)</h2>
 * Behavioral instructions are factored into independent sections, each registered with:
 * - A condition predicate over a runtime context dictionary (null = always include)
 * - A priority integer controlling reading order (lower = earlier in the prompt)
 * - A cacheable flag for Anthropic prompt caching
 *
 * <h2>Four-step pipeline at initialization</h2>
 * <pre>
 * 1. FILTER — evaluate each section's predicate. Sections returning false are excluded
 *             before any file I/O occurs.
 * 2. SORT   — order surviving sections by ascending priority.
 * 3. LOAD   — read each markdown file, strip frontmatter, resolve ${VAR} placeholders.
 * 4. JOIN   — concatenate loaded sections into the complete system prompt.
 * </pre>
 *
 * <h2>Two-part caching (Anthropic prompt caching)</h2>
 * For providers supporting input caching, {@link #composeTwoPart} splits the assembled prompt
 * into a STABLE part (cacheable: base instructions, tool descriptions, safety policy) and a
 * DYNAMIC part (session-specific context). The stable portion typically comprises 80–90% of
 * the total, yielding ~88% reduction in input token cost for the cached portion.
 *
 * <h2>Two-tier fallback</h2>
 * If an individual section file is missing, the composer skips it and proceeds. If modular
 * composition fails wholesale, the builder falls back to a monolithic core template —
 * guaranteeing agent startup under partial-deployment conditions.
 */
@ApplicationScoped
public class ConditionalPromptComposer {

    private static final Logger log = LoggerFactory.getLogger(ConditionalPromptComposer.class);

    @Inject GamelanConfig  config;
    @Inject AgentTelemetry telemetry;

    private final List<SectionSpec>        sections    = new ArrayList<>();
    private final Map<String, String>      varRegistry = new ConcurrentHashMap<>();
    private volatile String                fallbackCore = null;
    private volatile Map<String, Object>   runtimeCtx  = new HashMap<>();

    // ── Registration API ───────────────────────────────────────────────────

    /**
     * Registers a prompt section.
     *
     * @param name       unique section identifier
     * @param content    the section content (or null to load from a file)
     * @param filePath   path to load content from (used if content is null)
     * @param condition  predicate over the runtime context; null = always include
     * @param priority   reading order (lower = earlier); sections with same priority
     *                   are sorted by name
     * @param cacheable  true if this section should go into the stable (cached) partition
     */
    public void register(String name, String content, Path filePath,
                         Predicate<Map<String, Object>> condition,
                         int priority, boolean cacheable) {
        sections.add(new SectionSpec(name, content, filePath, condition, priority, cacheable));
        log.debug("[prompt] registered section '{}' priority={} cacheable={}", name, priority, cacheable);
    }

    /** Registers a section with inline content (no file). */
    public void register(String name, String content, int priority, boolean cacheable) {
        register(name, content, null, null, priority, cacheable);
    }

    /** Registers a file-backed section. */
    public void registerFile(String name, Path file, int priority, boolean cacheable) {
        register(name, null, file, null, priority, cacheable);
    }

    /** Registers a conditional file-backed section. */
    public void registerConditional(String name, Path file,
                                    Predicate<Map<String, Object>> cond,
                                    int priority, boolean cacheable) {
        register(name, null, file, cond, priority, cacheable);
    }

    /** Registers a variable for ${VAR} placeholder resolution. */
    public void registerVar(String name, String value) {
        varRegistry.put(name, value);
    }

    /** Sets the fallback monolithic prompt (used if modular composition fails). */
    public void setFallback(String fallback) { this.fallbackCore = fallback; }

    // ── Composition API ────────────────────────────────────────────────────

    /**
     * Updates the runtime context dictionary used by condition predicates.
     * Call before composing to reflect the current session state.
     */
    public void updateContext(Map<String, Object> ctx) { this.runtimeCtx = new HashMap<>(ctx); }
    public void updateContext(String key, Object value) { this.runtimeCtx.put(key, value); }

    /**
     * Composes the full system prompt using the four-step pipeline:
     * FILTER → SORT → LOAD → JOIN
     *
     * @return the complete assembled system prompt
     */
    public String compose() {
        try {
            List<SectionSpec> active = filterAndSort();
            List<String> parts = loadSections(active);
            String prompt = String.join("\n\n", parts);
            prompt = resolveVars(addDynamicContext(prompt));
            telemetry.count("prompt.composed");
            telemetry.gauge("prompt.section_count", active.size());
            log.debug("[prompt] composed: {} sections, ~{}t",
                    active.size(), prompt.length() / 4);
            return prompt;
        } catch (Exception e) {
            log.warn("[prompt] composition failed, using fallback: {}", e.getMessage());
            telemetry.count("prompt.fallback_used");
            return fallbackCore != null ? fallbackCore : buildMinimalFallback();
        }
    }

    /**
     * Composes two prompt parts for Anthropic prompt caching.
     *
     * @return TwoPartPrompt where stablePart is cacheable and dynamicPart is not
     */
    public TwoPartPrompt composeTwoPart() {
        try {
            List<SectionSpec> active = filterAndSort();
            List<SectionSpec> stable  = active.stream().filter(SectionSpec::cacheable).toList();
            List<SectionSpec> dynamic = active.stream().filter(s -> !s.cacheable()).toList();

            String stablePart  = String.join("\n\n", loadSections(stable));
            String dynamicPart = String.join("\n\n", loadSections(dynamic));
            stablePart  = resolveVars(stablePart);
            dynamicPart = resolveVars(addDynamicContext(dynamicPart));

            double cacheRatio = stablePart.length() /
                    (double) Math.max(1, stablePart.length() + dynamicPart.length());
            telemetry.gauge("prompt.cache_ratio", cacheRatio);
            log.debug("[prompt] two-part: stable={}t dynamic={}t ratio={:.0f}%",
                    stablePart.length()/4, dynamicPart.length()/4, cacheRatio * 100);

            return new TwoPartPrompt(stablePart, dynamicPart, stable.size(), dynamic.size());
        } catch (Exception e) {
            String full = compose();
            return new TwoPartPrompt(full, "", sections.size(), 0);
        }
    }

    /** Returns the names of sections that will be included for the current context. */
    public List<String> activeSections() {
        return filterAndSort().stream().map(SectionSpec::name).toList();
    }

    /** Returns all registered sections (for introspection/debugging). */
    public List<SectionSpec> allSections() { return Collections.unmodifiableList(sections); }

    // ── Private ────────────────────────────────────────────────────────────

    private List<SectionSpec> filterAndSort() {
        return sections.stream()
                .filter(s -> s.condition() == null || s.condition().test(runtimeCtx))
                .sorted(Comparator.comparingInt(SectionSpec::priority)
                        .thenComparing(SectionSpec::name))
                .toList();
    }

    private List<String> loadSections(List<SectionSpec> active) {
        List<String> parts = new ArrayList<>();
        for (SectionSpec spec : active) {
            try {
                String content = loadContent(spec);
                if (content != null && !content.isBlank()) parts.add(content.strip());
            } catch (Exception e) {
                // Two-tier fallback: missing section → skip and continue
                log.debug("[prompt] skipping section '{}': {}", spec.name(), e.getMessage());
                telemetry.count("prompt.section_skipped");
            }
        }
        return parts;
    }

    private String loadContent(SectionSpec spec) throws IOException {
        if (spec.content() != null) return spec.content();
        if (spec.filePath() != null && Files.exists(spec.filePath())) {
            String raw = Files.readString(spec.filePath());
            return stripFrontmatter(raw);
        }
        return null;
    }

    private String stripFrontmatter(String content) {
        if (!content.startsWith("---")) return content;
        int end = content.indexOf("---", 3);
        return end < 0 ? content : content.substring(end + 3).strip();
    }

    private String resolveVars(String template) {
        for (Map.Entry<String, String> entry : varRegistry.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }

    private String addDynamicContext(String base) {
        String cwd = System.getProperty("user.dir", ".");
        String date = LocalDate.now().toString();
        return base.replace("${DATE}", date)
                   .replace("${CWD}",  cwd)
                   .replace("${MODEL}", config.defaultModel());
    }

    private String buildMinimalFallback() {
        return "You are Gamelan, an AI software engineering assistant. " +
               "Date: " + LocalDate.now() + ". CWD: " + System.getProperty("user.dir") + ". " +
               "Follow the user's instructions carefully. Read files before editing them.";
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record SectionSpec(
            String                        name,
            String                        content,
            Path                          filePath,
            Predicate<Map<String,Object>> condition,
            int                           priority,
            boolean                       cacheable
    ) {}

    public record TwoPartPrompt(
            String stablePart,
            String dynamicPart,
            int    stableSections,
            int    dynamicSections
    ) {
        public String combined()    { return stablePart + "\n\n" + dynamicPart; }
        public int    totalSections() { return stableSections + dynamicSections; }
        public double cacheRatio()  {
            int total = stablePart.length() + dynamicPart.length();
            return total == 0 ? 0 : (double) stablePart.length() / total;
        }
    }
}
