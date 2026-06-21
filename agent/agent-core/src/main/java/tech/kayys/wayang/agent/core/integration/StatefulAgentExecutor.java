package tech.kayys.wayang.agent.core.integration;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.core.memory.AgentMemoryService;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-Turn Stateful Agent with Memory Consolidation
 *
 * This implementation demonstrates:
 * - Multi-turn conversation with persistent context
 * - Automatic memory consolidation (old → summary)
 * - Session-based memory management
 * - Adaptive behavior based on conversation history
 *
 * Usage:
 * {@code
 * @Inject
 * StatefulAgentExecutor executor;
 *
 * // Execute a multi-turn conversation
 * List<String> responses = executor.executeConversation(
 * "chatbot-agent",
 * "user-123",
 * "session-abc",
 * Arrays.asList(
 * "Hello, how are you?",
 * "What's the weather like?",
 * "Tell me a joke"
 * ))
 * .await().indefinitely();
 * }
 */
@ApplicationScoped
public class StatefulAgentExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(StatefulAgentExecutor.class);

    @Inject
    AgentMemoryService memoryService;

    @Inject
    Instance<AgentOrchestrator> agentOrchestrators;

    private AgentOrchestrator getReactOrchestrator() {
        return agentOrchestrators.stream()
            .filter(o -> "react".equals(o.strategyId()))
            .findFirst()
            .orElseGet(() -> agentOrchestrators.iterator().next());
    }

    /**
     * Execute a multi-turn conversation with persistent memory
     *
     * @param agentId   The agent ID
     * @param userId    The user ID
     * @param sessionId The session ID
     * @param prompts   List of user prompts in conversation order
     * @return Reactive list of agent responses
     */
    public Uni<List<String>> executeConversation(
            String agentId,
            String userId,
            String sessionId,
            List<String> prompts) {

        return Uni.join()
                .all(prompts.stream()
                        .map(prompt -> executeTurn(
                                agentId,
                                userId,
                                sessionId,
                                prompt))
                        .collect(Collectors.toList()))
                .andCollectFailures();
    }

    /**
     * Execute single turn with context retrieval and storage
     */
    private Uni<String> executeTurn(
            String agentId,
            String userId,
            String sessionId,
            String userPrompt) {

        LOG.info("Executing turn for agent {}, session {}", agentId, sessionId);

        // Step 1: Get conversation context
        return memoryService.getContextPrompt(agentId, 10)
                // Step 2: Build enhanced prompt
                .map(context -> buildEnhancedPrompt(userPrompt, context))
                // Step 3: Create agent request
                .map(enhancedPrompt -> AgentRequest.builder()
                        .requestId(UUID.randomUUID().toString())
                        .agentId(agentId)
                        .userId(userId)
                        .sessionId(sessionId)
                        .prompt(enhancedPrompt)
                        .strategy(OrchestrationStrategy.REACT)
                        .verbose(true)
                        .build())
                // Step 4: Execute agent
                .flatMap(request -> getReactOrchestrator().execute(request))
                // Step 5: Store interaction
                .flatMap(response -> {
                    return memoryService.storeInteraction(
                            agentId,
                            sessionId,
                            userId,
                            userPrompt,
                            response.content())
                            .map(__ -> response.content());
                })
                // Step 6: Check if consolidation needed (every 10 turns)
                .flatMap(responseContent -> {
                    return memoryService.getMemoryStats(agentId)
                            .flatMap(stats -> {
                                if (stats.totalMemories() > 20) {
                                    LOG.info("Consolidating memories for agent {}", agentId);
                                    return consolidateOldMemories(agentId)
                                            .map(__ -> responseContent);
                                }
                                return Uni.createFrom().item(responseContent);
                            });
                })
                .onFailure().invoke(ex -> {
                    LOG.error("Failed to execute turn: {}", ex.getMessage(), ex);
                });
    }

    /**
     * Consolidate old memories into summaries
     * (TIER 2 work - simplified version here)
     */
    private Uni<Void> consolidateOldMemories(String agentId) {
        LOG.info("Starting memory consolidation for agent {}", agentId);
        // In production, this would:
        // 1. Find memories older than 24 hours
        // 2. Generate summary using LLM
        // 3. Replace old memories with summary
        // For now, just log that consolidation would happen
        return Uni.createFrom().voidItem();
    }

    /**
     * Build enhanced prompt with conversation context
     */
    private String buildEnhancedPrompt(String userPrompt, String context) {
        return """
                You are a helpful, knowledgeable AI assistant engaged in a multi-turn conversation.

                Your behavior guidelines:
                1. Reference previous conversation points to show continuity
                2. Avoid repeating information already discussed
                3. Build upon previous responses
                4. Adapt your tone based on conversation history
                5. Remember user preferences and context

                Current user message: %s%s""".formatted(userPrompt, context);
    }

    /**
     * Get full conversation transcript
     */
    public Uni<String> getConversationTranscript(
            String agentId,
            String sessionId) {

        return memoryService.getSessionMemories(agentId, sessionId, 50)
                .map(memories -> memories.stream()
                        .sorted((a, b) -> a.getMetadata()
                                .get("timestamp")
                                .toString()
                                .compareTo(b.getMetadata()
                                        .get("timestamp")
                                        .toString()))
                        .map(this::formatMemoryEntry)
                        .collect(Collectors.joining("\n")));
    }

    /**
     * Get conversation summary
     */
    public Uni<String> getConversationSummary(
            String agentId,
            String sessionId) {

        return getConversationTranscript(agentId, sessionId)
                .flatMap(transcript -> {
                    // In production: Use LLM to generate summary
                    // For now, just extract key points
                    List<String> keyPoints = extractKeyPoints(transcript);

                    return Uni.createFrom().item(
                            "Session Summary:\n" +
                                    "- " + String.join("\n- ", keyPoints));
                });
    }

    /**
     * Extract key points from transcript
     */
    private List<String> extractKeyPoints(String transcript) {
        return Arrays.stream(transcript.split("\n"))
                .filter(line -> line.length() > 20)
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Determine conversation quality/relevance metrics
     */
    public Uni<ConversationMetrics> getConversationMetrics(
            String agentId,
            String sessionId) {

        return memoryService.getSessionMemories(agentId, sessionId, 50)
                .map(memories -> {
                    int totalTurns = memories.size() / 2; // User + agent pairs
                    double avgMemorySize = memories.stream()
                            .mapToInt(m -> m.getContent().length())
                            .average()
                            .orElse(0);

                    // Calculate conversation continuity
                    double continuity = calculateContinuity(memories);

                    // Calculate relevance to initial topic
                    double topicFocus = calculateTopicFocus(memories);

                    return new ConversationMetrics(
                            agentId,
                            sessionId,
                            totalTurns,
                            avgMemorySize,
                            continuity,
                            topicFocus,
                            Instant.now());
                });
    }

    /**
     * Calculate conversation continuity score (0-1)
     */
    private double calculateContinuity(List<MemoryEntry> memories) {
        if (memories.size() < 2)
            return 1.0;

        // Simple heuristic: check if responses reference previous messages
        int referencingResponses = 0;
        for (int i = 1; i < memories.size(); i++) {
            String current = memories.get(i).getContent();
            String previous = memories.get(i - 1).getContent();

            if (hasSemanticsOverlap(current, previous)) {
                referencingResponses++;
            }
        }

        return (double) referencingResponses / memories.size();
    }

    /**
     * Calculate topic focus score (0-1)
     * Higher score = better focus on initial topic
     */
    private double calculateTopicFocus(List<MemoryEntry> memories) {
        if (memories.isEmpty())
            return 0.0;

        String firstMessage = memories.get(0).getContent();
        int topicMatches = 0;

        for (MemoryEntry memory : memories) {
            if (hasSemanticsOverlap(memory.getContent(), firstMessage)) {
                topicMatches++;
            }
        }

        return (double) topicMatches / memories.size();
    }

    /**
     * Check if two texts have semantic overlap
     * (Simplified - in production use embedding similarity)
     */
    private boolean hasSemanticsOverlap(String text1, String text2) {
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");

        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));

        set1.retainAll(set2);
        return set1.size() >= 3; // At least 3 common words
    }

    /**
     * Format memory entry for display
     */
    private String formatMemoryEntry(MemoryEntry entry) {
        String type = (String) entry.getMetadata().getOrDefault("type", "message");
        String prefix = type.contains("user") ? "User: " : "Agent: ";
        return prefix + entry.getContent();
    }

    /**
     * Conversation metrics record
     */
    public record ConversationMetrics(
            String agentId,
            String sessionId,
            int totalTurns,
            double averageMemorySize,
            double continuityScore,
            double topicFocusScore,
            Instant generatedAt) {

        public boolean isHighQuality() {
            return continuityScore > 0.7 && topicFocusScore > 0.6;
        }

        public String getQualityAssessment() {
            if (continuityScore > 0.8 && topicFocusScore > 0.8) {
                return "Excellent - Highly coherent conversation with strong topic focus";
            } else if (continuityScore > 0.6 && topicFocusScore > 0.6) {
                return "Good - Generally coherent with reasonable topic focus";
            } else if (continuityScore > 0.4 || topicFocusScore > 0.4) {
                return "Fair - Some coherence issues or topic drift";
            } else {
                return "Poor - Low coherence and weak topic focus";
            }
        }
    }
}
