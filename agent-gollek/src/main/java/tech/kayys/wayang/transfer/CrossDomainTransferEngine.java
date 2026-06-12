package tech.kayys.gamelan.transfer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.memory.hierarchy.ProceduralMemory;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cross-Domain Transfer Engine.
 *
 * <h2>Problem</h2>
 * An agent that learns to fix null pointer exceptions in Java has no mechanism
 * to apply that knowledge to a NullReferenceException in C# or an AttributeError
 * in Python. Every language-domain combination is treated as a fresh problem,
 * even when the underlying pattern is identical.
 *
 * <h2>Solution: Abstract Pattern Extraction + Analogical Transfer</h2>
 * The engine extracts domain-agnostic patterns from successful episodes and
 * applies them across domains via analogical reasoning:
 *
 * <pre>
 * Source domain:  Java NPE fix
 * Pattern:        "null guard before object access → check → use Optional"
 * Target domain:  Python AttributeError
 * Transfer:       "check for None before attribute access → hasattr() / Optional chaining"
 * </pre>
 *
 * <h2>Transfer Maps</h2>
 * Pre-configured mappings between equivalent concepts across domains:
 * <pre>
 * Java NullPointerException  ↔  Python AttributeError / None
 * Java Optional<T>           ↔  Python Optional / Rust Option<T>
 * Java try-with-resources    ↔  Python with/contextmanager / C# using
 * Maven pom.xml              ↔  npm package.json / Cargo.toml / pyproject.toml
 * </pre>
 *
 * <h2>When Transfer is Triggered</h2>
 * <ol>
 *   <li>Agent encounters a task in domain B with low episodic coverage</li>
 *   <li>Semantic similarity search finds related patterns in domain A</li>
 *   <li>Transfer engine generates a domain-adapted strategy</li>
 *   <li>Adapted strategy is injected as context into the agent's system prompt</li>
 * </ol>
 */
@ApplicationScoped
public class CrossDomainTransferEngine {

    private static final Logger log = LoggerFactory.getLogger(CrossDomainTransferEngine.class);

    @Inject GollekSdk        sdk;
    @Inject EpisodicMemory   episodic;
    @Inject ProceduralMemory procedural;
    @Inject GamelanConfig    config;

    // Hard-coded concept transfer maps (loaded from config in production)
    private static final Map<String, Map<String, String>> TRANSFER_MAPS = Map.of(
            "java", Map.of(
                    "NullPointerException",  "null reference / None / nil",
                    "Optional<T>",           "Option<T> / Maybe / nullable",
                    "pom.xml",               "package.json / Cargo.toml / pyproject.toml",
                    "try-with-resources",    "with statement / using / RAII",
                    "interface",             "protocol / trait / ABC",
                    "CompletableFuture",     "Promise / async-await / Future",
                    "Stream.filter",         "list comprehension / iterator / LINQ"
            ),
            "python", Map.of(
                    "AttributeError",        "NullPointerException / nil error",
                    "requirements.txt",      "pom.xml / package.json / Cargo.toml",
                    "contextmanager",        "try-with-resources / using",
                    "dataclass",             "record / struct / POJO",
                    "asyncio",               "CompletableFuture / goroutine / async-await"
            ),
            "javascript", Map.of(
                    "undefined is not a function", "NullPointerException / AttributeError",
                    "Promise.all",           "CompletableFuture.allOf / asyncio.gather",
                    "package.json",          "pom.xml / Cargo.toml / pyproject.toml",
                    "async/await",           "CompletableFuture / coroutine"
            )
    );

    // Transfer history: source-domain → target-domain → list of transfers
    private final Map<String, List<TransferRecord>> history = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Detects if cross-domain transfer could improve handling of a task,
     * and returns adapted context if so.
     *
     * @param task        the current task
     * @param targetDomain the domain of the current task (java/python/js/etc.)
     * @return adapted context block, or empty if no transfer applicable
     */
    public Optional<TransferContext> transfer(String task, String targetDomain) {
        log.debug("[transfer] checking for applicable transfers to domain '{}'", targetDomain);

        // Find procedures from source domains
        List<ProceduralMemory.Procedure> allProcedures = procedural.all();
        if (allProcedures.isEmpty()) return Optional.empty();

        // Score procedures by relevance to the task (abstracted)
        String abstractTask = abstractConcepts(task, targetDomain);
        List<ProceduralMemory.Procedure> candidates = procedural.findApplicable(abstractTask, 3);

        if (candidates.isEmpty()) return Optional.empty();

        // For each candidate procedure, attempt cross-domain adaptation
        for (ProceduralMemory.Procedure proc : candidates) {
            String sourceDomain = inferDomain(proc.description());
            if (sourceDomain.equals(targetDomain)) continue; // same domain, no transfer needed

            Optional<String> adapted = adaptToTargetDomain(proc, sourceDomain, targetDomain, task);
            if (adapted.isPresent()) {
                TransferRecord record = new TransferRecord(
                        proc.name(), sourceDomain, targetDomain,
                        proc.description(), adapted.get(), Instant.now());
                history.computeIfAbsent(sourceDomain + "→" + targetDomain,
                        k -> new ArrayList<>()).add(record);

                log.info("[transfer] transferred '{}': {} → {}",
                        proc.name(), sourceDomain, targetDomain);
                return Optional.of(new TransferContext(sourceDomain, targetDomain,
                        proc, adapted.get(), record));
            }
        }
        return Optional.empty();
    }

    /**
     * Manually registers a transfer mapping between two domains.
     */
    public void registerTransferMap(String sourceDomain, String sourceConcept,
                                    String targetDomain, String targetConcept) {
        TRANSFER_MAPS.computeIfAbsent(sourceDomain, k -> new ConcurrentHashMap<>())
                .put(sourceConcept, targetConcept);
        log.info("[transfer] registered: {}/{} → {}/{}",
                sourceDomain, sourceConcept, targetDomain, targetConcept);
    }

    /**
     * Returns transfer statistics — useful for understanding which cross-domain
     * patterns are being discovered and reused.
     */
    public TransferStats stats() {
        int totalTransfers = history.values().stream()
                .mapToInt(List::size).sum();
        Map<String, Integer> byRoute = new LinkedHashMap<>();
        history.forEach((route, records) -> byRoute.put(route, records.size()));
        return new TransferStats(totalTransfers, byRoute);
    }

    // ── Private ────────────────────────────────────────────────────────────

    /**
     * Replaces domain-specific terms with abstract equivalents to enable
     * cross-domain matching.
     */
    private String abstractConcepts(String text, String domain) {
        Map<String, String> domainMap = TRANSFER_MAPS.getOrDefault(domain, Map.of());
        String result = text;
        for (Map.Entry<String, String> e : domainMap.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }
        return result;
    }

    /**
     * Infers the primary domain of a procedure description.
     */
    private String inferDomain(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("java") || lower.contains("maven") || lower.contains("spring")
                || lower.contains("pom.xml")) return "java";
        if (lower.contains("python") || lower.contains("pip") || lower.contains("django")
                || lower.contains("pytest")) return "python";
        if (lower.contains("javascript") || lower.contains("npm") || lower.contains("react")
                || lower.contains("node")) return "javascript";
        if (lower.contains("rust") || lower.contains("cargo")) return "rust";
        if (lower.contains("go") || lower.contains("golang")) return "go";
        return "generic";
    }

    /**
     * Uses LLM to adapt a procedure from the source domain to the target domain.
     */
    private Optional<String> adaptToTargetDomain(ProceduralMemory.Procedure proc,
                                                   String sourceDomain, String targetDomain,
                                                   String task) {
        // Build concept translation hints
        Map<String, String> sourceMap = TRANSFER_MAPS.getOrDefault(sourceDomain, Map.of());
        String hints = sourceMap.entrySet().stream()
                .map(e -> sourceDomain + " '" + e.getKey() + "' = " +
                        targetDomain + " '" + e.getValue() + "'")
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt = String.format("""
                Adapt this strategy from %s to %s:
                
                Source strategy (%s):
                %s
                
                Target task (%s):
                %s
                
                Concept translations:
                %s
                
                Rewrite the strategy for %s in 2-4 sentences. Be concrete and actionable.
                If this strategy is not applicable to the target domain, reply with: NOT_APPLICABLE
                """,
                sourceDomain, targetDomain,
                sourceDomain, proc.description(),
                targetDomain, task.length() > 200 ? task.substring(0, 200) : task,
                hints.isBlank() ? "(none available)" : hints,
                targetDomain);

        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You are a software engineering knowledge transfer expert.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.3)
                            .maxTokens(300)
                            .streaming(false)
                            .build());

            String content = resp.getContent();
            if (content == null || content.contains("NOT_APPLICABLE")) return Optional.empty();
            return Optional.of(content.strip());
        } catch (Exception e) {
            log.debug("[transfer] adaptation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record TransferContext(
            String                       sourceDomain,
            String                       targetDomain,
            ProceduralMemory.Procedure   sourceProcedure,
            String                       adaptedStrategy,
            TransferRecord               record
    ) {
        /** Returns a system-prompt injection block. */
        public String toPromptBlock() {
            return String.format("""
                    ## Cross-Domain Transfer
                    Relevant strategy transferred from %s experience:
                    
                    **Original (%s)**: %s
                    
                    **Adapted for %s**: %s
                    """,
                    sourceDomain, sourceDomain, sourceProcedure.description(),
                    targetDomain, adaptedStrategy);
        }
    }

    public record TransferRecord(
            String  procedureName,
            String  sourceDomain,
            String  targetDomain,
            String  originalStrategy,
            String  adaptedStrategy,
            Instant transferredAt
    ) {}

    public record TransferStats(
            int                  totalTransfers,
            Map<String, Integer> byRoute
    ) {
        public String summary() {
            if (totalTransfers == 0) return "No cross-domain transfers performed yet";
            return "Transfers: " + totalTransfers + " | Routes: " +
                    byRoute.entrySet().stream()
                            .map(e -> e.getKey() + "×" + e.getValue())
                            .reduce("", (a, b) -> a.isBlank() ? b : a + ", " + b);
        }
    }
}
