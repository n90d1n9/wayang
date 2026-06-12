package tech.kayys.gamelan.memory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.DirectCallOrchestrator;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;

/**
 * Extracts structured knowledge from episode outcomes via LLM (Section VIII — Semantic Memory).
 *
 * <h2>The problem with REMEMBER: lines</h2>
 * The existing REMEMBER protocol relies on the agent explicitly emitting
 * {@code REMEMBER: key = value} lines. This works for facts the agent
 * consciously decides to record, but misses implicit knowledge embedded
 * in its reasoning — patterns it uses without naming them.
 *
 * <h2>What this adds</h2>
 * After each episode, the extractor asks the LLM:
 * <em>"What facts about this project/codebase can you extract from this interaction?"</em>
 * The response is parsed into typed fact entries and fed into
 * {@link SemanticMemoryStore}. This enables:
 * <ul>
 *   <li>Automatic knowledge accumulation without explicit REMEMBER lines</li>
 *   <li>Fact deduplication — the LLM's extraction naturally deduplicates</li>
 *   <li>Cross-episode knowledge synthesis — facts from multiple episodes
 *       are merged into a coherent picture</li>
 * </ul>
 *
 * <h2>Output format</h2>
 * The LLM is instructed to produce lines in the form:
 * <pre>
 * FACT: build-tool = Maven; the project uses Maven with JDK 21
 * PREFERENCE: test-style = JUnit 5 with AssertJ assertions
 * DECISION: auth-pattern = JWT tokens stored in HttpOnly cookies
 * COMMAND: test-run = mvn test -pl auth-service -Dtest=AuthServiceTest
 * </pre>
 */
@ApplicationScoped
public class KnowledgeGraphExtractor {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphExtractor.class);

    private static final Pattern FACT_LINE = Pattern.compile(
            "^(FACT|PREFERENCE|DECISION|COMMAND|PROCEDURE):\\s*([^=]+?)\\s*=\\s*(.+)$",
            Pattern.MULTILINE);

    @Inject DirectCallOrchestrator llm;
    @Inject SemanticMemoryStore    semanticStore;

    private final ExecutorService extractor =
            Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Asynchronously extracts knowledge from an episode outcome.
     * Returns immediately; extraction happens in the background.
     *
     * @param episode  the completed episode to extract knowledge from
     * @param model    LLM model to use for extraction
     */
    public void extractAsync(MemoryHierarchy.Episode episode, String model) {
        extractor.submit(() -> extract(episode, model));
    }

    /**
     * Synchronous extraction (for testing and for manual invocation).
     *
     * @return list of facts extracted from the episode
     */
    public List<ExtractedFact> extract(MemoryHierarchy.Episode episode, String model) {
        if (episode == null || episode.outcome().isBlank()) return List.of();

        String extractionPrompt = buildExtractionPrompt(episode);
        try {
            var result = llm.execute(AgentRequest.builder(extractionPrompt)
                    .model(model).stream(false).build());

            List<ExtractedFact> facts = parse(result.answer());
            log.info("[knowledge-extractor] extracted {} facts from episode {}",
                    facts.size(), episode.id().substring(0, 8));

            // Store all extracted facts in the semantic memory
            String project = Path.of(java.nio.file.Path.of(".").toAbsolutePath()
                    .normalize().getFileName().toString()).toString();
            for (ExtractedFact fact : facts) {
                semanticStore.store(fact.topic(), fact.value(), fact.type(), project);
            }
            return facts;
        } catch (Exception e) {
            log.debug("[knowledge-extractor] extraction failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Parsing ────────────────────────────────────────────────────────────

    List<ExtractedFact> parse(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) return List.of();

        List<ExtractedFact> facts = new ArrayList<>();
        Matcher m = FACT_LINE.matcher(llmOutput);
        while (m.find()) {
            String type  = m.group(1).strip();
            String topic = m.group(2).strip().toLowerCase().replace(" ", "-");
            String value = m.group(3).strip();

            // Validate
            if (topic.isBlank() || value.isBlank() || topic.length() > 80) continue;
            if (value.length() > 500) value = value.substring(0, 500);

            facts.add(new ExtractedFact(type, topic, value));
        }
        return Collections.unmodifiableList(facts);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String buildExtractionPrompt(MemoryHierarchy.Episode episode) {
        return """
                Analyse the following completed agent task and extract factual knowledge
                about the project, codebase, or environment that was discovered.

                Only extract facts that would be USEFUL in future sessions.
                Do NOT extract generic advice — only project-specific facts.

                Output format (one per line, no prose, no explanation):
                FACT: <topic> = <concise fact about this project>
                PREFERENCE: <topic> = <a user/team preference discovered>
                DECISION: <topic> = <an architectural or design decision>
                COMMAND: <topic> = <an exact command that works for this project>
                PROCEDURE: <topic> = <a multi-step process that worked>

                If no project-specific facts were discovered, output: NO_FACTS

                TASK: %s
                OUTCOME: %s
                TOOLS USED: %s
                SUCCESS: %s
                """.formatted(
                        episode.task(),
                        episode.outcome().substring(0, Math.min(episode.outcome().length(), 800)),
                        String.join(", ", episode.toolsUsed()),
                        episode.success() ? "yes" : "no");
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public record ExtractedFact(String type, String topic, String value) {}
}
