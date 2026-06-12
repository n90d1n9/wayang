package tech.kayys.gamelan.agent.playbook;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * AcePlaybook — Agentic Context Engineering (ACE) adaptive memory system.
 *
 * <h2>From the OPENDEV paper (§2.3.6 — Adaptive Memory)</h2>
 * Agents working in a project over multiple sessions accumulate experience about which
 * approaches succeed and which do not. The ACE subsystem captures this experience as a
 * playbook: a collection of natural-language bullets, each tagged with effectiveness counters
 * (helpful, harmful, or neutral) and a creation timestamp.
 *
 * <h2>Four-stage pipeline (paper Figure 14)</h2>
 * <pre>
 * Stage 1 – BulletSelector   Ranks bullets by weighted score:
 *                              effectiveness(0.5) + recency_decay(0.3) + semantic_sim(0.2)
 *                              Top-K bullets injected into the Generator's system prompt.
 *
 * Stage 2 – Reflector        Every 5 messages, analyzes accumulated experience.
 *                              Produces: reasoning trace, error identification, root-cause,
 *                              correct approach. Tags bullets: HELPFUL/HARMFUL/NEUTRAL.
 *                              Never proposes structural changes — only effectiveness tags.
 *
 * Stage 3 – Curator          Reads reflection, plans concrete mutations:
 *                              ADD new bullets, UPDATE existing, TAG effectiveness, REMOVE stale.
 *                              Emits a DeltaBatch.
 *
 * Stage 4 – Persist          DeltaBatch applied to bullet table, saved to session-scoped JSON.
 * </pre>
 *
 * <h2>Cross-session learning</h2>
 * The playbook persists in {@code ~/.gamelan/playbook/<project>.json}, accumulating lessons
 * across sessions. The BulletSelector uses recency decay to ensure that old, stale strategies
 * don't crowd out recent, relevant ones.
 */
@ApplicationScoped
public class AcePlaybook {

    private static final Logger log = LoggerFactory.getLogger(AcePlaybook.class);

    // Paper constants
    private static final int    REFLECTOR_INTERVAL   = 5;   // messages between reflections
    private static final int    TOP_K_BULLETS        = 5;   // bullets injected per turn
    private static final double WEIGHT_EFFECTIVENESS = 0.50;
    private static final double WEIGHT_RECENCY       = 0.30;
    private static final double WEIGHT_SEMANTIC      = 0.20;
    private static final int    MAX_BULLETS          = 200; // cap to prevent unbounded growth

    @Inject GamelanConfig           config;
    @Inject SingleAgentOrchestrator orchestrator;
    @Inject AgentTelemetry          telemetry;

    private final Map<String, Bullet>  bullets       = new ConcurrentHashMap<>();
    private final AtomicInteger        msgCount      = new AtomicInteger(0);
    private final AtomicInteger        bulletIdSeq   = new AtomicInteger(1);
    private volatile String            projectKey    = "default";

    @PostConstruct
    void init() {
        projectKey = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        load();
        log.info("[ace] loaded {} bullets for project '{}'", bullets.size(), projectKey);
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Selects the top-K most relevant bullets for the current query.
     * Bullets are ranked by effectiveness + recency + semantic similarity.
     *
     * @param query the current user query / task description
     * @return formatted bullet text ready for injection into the system prompt
     */
    public String selectForPrompt(String query) {
        if (bullets.isEmpty()) return "";
        List<Bullet> ranked = rankBullets(query);
        String prompt = ranked.stream()
                .limit(TOP_K_BULLETS)
                .map(b -> "- " + b.content())
                .collect(Collectors.joining("\n"));
        telemetry.count("ace.bullets.selected");
        return "## Playbook Strategies (from past sessions)\n" + prompt;
    }

    /**
     * Called after each agent message. Triggers the Reflector pipeline every
     * REFLECTOR_INTERVAL messages.
     *
     * @param recentExchange the recent tool output or agent response to reflect on
     * @param taskContext    the overall task context
     */
    public void onMessage(String recentExchange, String taskContext) {
        int count = msgCount.incrementAndGet();
        if (count % REFLECTOR_INTERVAL == 0) {
            runReflectorAsync(recentExchange, taskContext);
        }
    }

    /**
     * Directly tags a bullet's effectiveness (call from user feedback or test result).
     */
    public void tagEffectiveness(String bulletId, EffectivenessTag tag) {
        Bullet b = bullets.get(bulletId);
        if (b != null) {
            Bullet updated = b.withTag(tag);
            bullets.put(bulletId, updated);
            persist();
            telemetry.count("ace.bullet.tagged." + tag.name().toLowerCase());
        }
    }

    /**
     * Manually adds a bullet to the playbook.
     */
    public String addBullet(String content, EffectivenessTag tag) {
        String id = "B" + String.format("%04d", bulletIdSeq.getAndIncrement());
        Bullet b = new Bullet(id, content, tag, Instant.now(), 0.5);
        bullets.put(id, b);
        enforceSizeLimit();
        persist();
        log.debug("[ace] added bullet {}: {}", id, truncate(content, 60));
        return id;
    }

    /**
     * Returns all bullets, sorted by score descending.
     */
    public List<Bullet> allBullets() {
        return rankBullets("").stream().toList();
    }

    /**
     * Returns all bullets with HARMFUL tag (for inspection/pruning).
     */
    public List<Bullet> harmfulBullets() {
        return bullets.values().stream()
                .filter(b -> b.tag() == EffectivenessTag.HARMFUL)
                .toList();
    }

    /** Clears all bullets (use with caution). */
    public void clear() {
        bullets.clear();
        msgCount.set(0);
        persist();
    }

    // ── Stage 2: Reflector ─────────────────────────────────────────────────

    private void runReflectorAsync(String exchange, String context) {
        Thread.ofVirtual().start(() -> {
            try {
                runReflector(exchange, context);
            } catch (Exception e) {
                log.warn("[ace] reflector failed: {}", e.getMessage());
            }
        });
    }

    private void runReflector(String exchange, String context) {
        String prompt = REFLECTOR_PROMPT.formatted(
                truncate(context, 400), truncate(exchange, 600));
        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, 2000))
                            .stream(false).maxSteps(1).build());
            if (!result.success()) return;
            // Run curator with the reflection
            runCurator(result.answer(), exchange);
            telemetry.count("ace.reflector.ran");
        } catch (Exception e) {
            log.debug("[ace] reflector error: {}", e.getMessage());
        }
    }

    // ── Stage 3: Curator ───────────────────────────────────────────────────

    private void runCurator(String reflection, String exchange) {
        String existingBullets = bullets.values().stream()
                .limit(20)
                .map(b -> b.id() + ": " + b.content() + " [" + b.tag() + "]")
                .collect(Collectors.joining("\n"));

        String prompt = CURATOR_PROMPT.formatted(reflection, existingBullets);
        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, 2000))
                            .stream(false).maxSteps(1).build());
            if (!result.success()) return;
            applyDeltaBatch(result.answer());
            telemetry.count("ace.curator.ran");
        } catch (Exception e) {
            log.debug("[ace] curator error: {}", e.getMessage());
        }
    }

    /** Parses curator output and applies mutations to the playbook. */
    private void applyDeltaBatch(String curatorOutput) {
        String[] lines = curatorOutput.split("\n");
        for (String line : lines) {
            line = line.strip();
            if (line.startsWith("ADD:")) {
                String content = line.substring(4).strip();
                if (!content.isBlank() && !isDuplicate(content)) addBullet(content, EffectivenessTag.NEUTRAL);
            } else if (line.startsWith("TAG_HELPFUL:")) {
                tagById(line.substring(12).strip(), EffectivenessTag.HELPFUL);
            } else if (line.startsWith("TAG_HARMFUL:")) {
                tagById(line.substring(12).strip(), EffectivenessTag.HARMFUL);
            } else if (line.startsWith("REMOVE:")) {
                String id = line.substring(7).strip();
                if (bullets.remove(id) != null) {
                    log.debug("[ace] removed bullet {}", id);
                    telemetry.count("ace.bullet.removed");
                }
            } else if (line.startsWith("UPDATE:")) {
                // UPDATE:B0001:new content
                String[] parts = line.substring(7).split(":", 2);
                if (parts.length == 2) {
                    Bullet b = bullets.get(parts[0].strip());
                    if (b != null) bullets.put(b.id(), b.withContent(parts[1].strip()));
                }
            }
        }
        enforceSizeLimit();
        persist();
    }

    private void tagById(String id, EffectivenessTag tag) {
        Bullet b = bullets.get(id);
        if (b != null) bullets.put(id, b.withTag(tag));
    }

    private boolean isDuplicate(String content) {
        String lower = content.toLowerCase();
        return bullets.values().stream()
                .anyMatch(b -> similarityScore(b.content().toLowerCase(), lower) > 0.85);
    }

    // ── Stage 1: BulletSelector scoring ───────────────────────────────────

    private List<Bullet> rankBullets(String query) {
        Instant now = Instant.now();
        return bullets.values().stream()
                .map(b -> {
                    double effectScore = b.tag() == EffectivenessTag.HELPFUL ? 1.0
                            : b.tag() == EffectivenessTag.HARMFUL ? 0.0 : 0.5;
                    // Recency decay: score = 1.0 for today, decays over 30 days
                    long daysSince = (now.toEpochMilli() - b.createdAt().toEpochMilli())
                            / (1000L * 60 * 60 * 24);
                    double recencyScore = Math.exp(-daysSince / 30.0);
                    // Semantic similarity (simple word overlap)
                    double semanticScore = query.isBlank() ? 0.5
                            : similarityScore(b.content().toLowerCase(), query.toLowerCase());
                    double total = WEIGHT_EFFECTIVENESS * effectScore
                            + WEIGHT_RECENCY       * recencyScore
                            + WEIGHT_SEMANTIC      * semanticScore;
                    return Map.entry(b, total);
                })
                // Filter out HARMFUL bullets
                .filter(e -> e.getKey().tag() != EffectivenessTag.HARMFUL)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Simple Jaccard word overlap for semantic similarity. */
    private double similarityScore(String a, String b) {
        Set<String> wa = Set.of(a.split("\\W+"));
        Set<String> wb = Set.of(b.split("\\W+"));
        Set<String> inter = new HashSet<>(wa); inter.retainAll(wb);
        Set<String> union = new HashSet<>(wa); union.addAll(wb);
        return union.isEmpty() ? 0 : (double) inter.size() / union.size();
    }

    private void enforceSizeLimit() {
        if (bullets.size() <= MAX_BULLETS) return;
        // Remove lowest-scoring NEUTRAL bullets first, then oldest
        List<String> toRemove = bullets.values().stream()
                .filter(b -> b.tag() == EffectivenessTag.NEUTRAL)
                .sorted(Comparator.comparing(Bullet::createdAt))
                .limit(bullets.size() - MAX_BULLETS)
                .map(Bullet::id)
                .toList();
        toRemove.forEach(bullets::remove);
    }

    // ── Persistence ────────────────────────────────────────────────────────

    private void persist() {
        Path dir = Path.of(System.getProperty("user.home"), ".gamelan", "playbook");
        try {
            Files.createDirectories(dir);
            StringBuilder sb = new StringBuilder("[\n");
            for (Bullet b : bullets.values()) {
                sb.append(String.format(
                        "  {\"id\":\"%s\",\"content\":\"%s\",\"tag\":\"%s\"," +
                        "\"createdAt\":\"%s\",\"score\":%.3f},\n",
                        b.id(), esc(b.content()), b.tag().name(),
                        b.createdAt().toString(), b.score()));
            }
            if (sb.toString().endsWith(",\n")) sb.setLength(sb.length() - 2);
            sb.append("\n]");
            Files.writeString(dir.resolve(projectKey + ".json"), sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) { log.debug("[ace] persist failed: {}", e.getMessage()); }
    }

    private void load() {
        Path file = Path.of(System.getProperty("user.home"), ".gamelan", "playbook",
                projectKey + ".json");
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            for (String entry : json.split("\\},\\s*\\{")) {
                try {
                    String id      = extract(entry, "id");
                    String content = extract(entry, "content");
                    String tagStr  = extract(entry, "tag");
                    String tsStr   = extract(entry, "createdAt");
                    if (id.isBlank() || content.isBlank()) continue;
                    EffectivenessTag tag = EffectivenessTag.valueOf(tagStr.toUpperCase());
                    Instant ts = tsStr.isBlank() ? Instant.now() : Instant.parse(tsStr);
                    bullets.put(id, new Bullet(id, content, tag, ts, 0.5));
                    int seqNum = 0;
                    try { seqNum = Integer.parseInt(id.substring(1)); } catch (Exception ignored) {}
                    bulletIdSeq.updateAndGet(cur -> Math.max(cur, seqNum + 1));
                } catch (Exception ignored) {}
            }
        } catch (IOException e) { log.debug("[ace] load failed: {}", e.getMessage()); }
    }

    private String extract(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }
    private String esc(String s) { return s == null ? "" : s.replace("\"","\\\"").replace("\n","\\n"); }
    private String truncate(String s, int max) { return s.length() > max ? s.substring(0,max)+"…" : s; }

    // ── Prompt templates ───────────────────────────────────────────────────

    private static final String REFLECTOR_PROMPT = """
            Analyze this agent exchange and produce bullet-level effectiveness tags.
            
            Task context: %s
            
            Recent exchange: %s
            
            Produce only the following outputs (one per line):
            - A reasoning trace about what worked and what didn't
            - For each existing bullet ID that was helpful: TAG_HELPFUL:<id>
            - For each existing bullet ID that was harmful: TAG_HARMFUL:<id>
            
            Do NOT propose new bullets or structural changes — only effectiveness tags.""";

    private static final String CURATOR_PROMPT = """
            You are a playbook curator. Based on this reflection, plan mutations for a strategy playbook.
            
            Reflection: %s
            
            Existing bullets:
            %s
            
            Produce only the following mutation commands (one per line):
            ADD: <concise strategy statement — start with an action verb>
            REMOVE: <bullet-id>
            UPDATE: <bullet-id>: <revised content>
            TAG_HELPFUL: <bullet-id>
            TAG_HARMFUL: <bullet-id>
            
            Rules:
            - ADD only genuinely new, transferable strategies (max 3 per reflection)
            - REMOVE only clearly outdated or wrong bullets
            - Strategies must be concrete and actionable (e.g. "Always read the file before editing it")
            - Do NOT add bullets that already exist in the playbook""";

    // ── Data types ─────────────────────────────────────────────────────────

    public enum EffectivenessTag { HELPFUL, NEUTRAL, HARMFUL }

    public record Bullet(
            String           id,
            String           content,
            EffectivenessTag tag,
            Instant          createdAt,
            double           score
    ) {
        Bullet withTag(EffectivenessTag newTag) {
            return new Bullet(id, content, newTag, createdAt, score);
        }
        Bullet withContent(String newContent) {
            return new Bullet(id, newContent, tag, createdAt, score);
        }
    }

    public record DeltaBatch(
            List<String> toAdd,
            List<String> toRemove,
            Map<String,EffectivenessTag> toTag
    ) {}
}
