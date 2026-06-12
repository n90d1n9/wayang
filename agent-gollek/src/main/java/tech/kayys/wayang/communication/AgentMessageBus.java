package tech.kayys.gamelan.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Agent Communication Protocol — Message Bus, Blackboard, and Conflict Resolution.
 *
 * <h2>Three Communication Primitives</h2>
 *
 * <h3>1. Message Bus (ACL — Agent Communication Language)</h3>
 * Structured messages with intent, constraints, and results:
 * <pre>
 * {
 *   "from":   "orchestrator",
 *   "to":     "security-agent",
 *   "intent": "ANALYZE",
 *   "topic":  "auth-service-security",
 *   "payload": { "path": "src/auth/", "depth": 3 },
 *   "constraints": { "maxTokens": 2000, "timeoutMs": 30000 }
 * }
 * </pre>
 *
 * <h3>2. Blackboard System</h3>
 * A shared, structured coordination memory space where agents post findings
 * and read each other's contributions without direct coupling.
 * Classic pattern from expert systems — proven for parallel agent coordination.
 *
 * <h3>3. Conflict Resolution</h3>
 * When agents produce contradictory findings:
 * <ul>
 *   <li>CONFIDENCE: highest-confidence agent wins</li>
 *   <li>VOTING: majority decision among N agents</li>
 *   <li>MERGE: both findings preserved, flagged as conflicting</li>
 *   <li>ESCALATE: conflict surfaced to orchestrator for resolution</li>
 * </ul>
 */
@ApplicationScoped
public class AgentMessageBus {

    private static final Logger log = LoggerFactory.getLogger(AgentMessageBus.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Subscriptions: topic → list of consumers
    private final Map<String, List<Consumer<AgentMessage>>> subscriptions = new ConcurrentHashMap<>();
    // Dead letter queue for undelivered messages
    private final List<AgentMessage> deadLetters = new CopyOnWriteArrayList<>();
    // Message ID sequence
    private final AtomicLong idSeq = new AtomicLong(1);
    // Blackboard
    private final Blackboard blackboard = new Blackboard();
    // Pending responses: correlationId → CompletableFuture
    private final Map<String, CompletableFuture<AgentMessage>> pendingResponses = new ConcurrentHashMap<>();

    // ── Publish/Subscribe ─────────────────────────────────────────────────

    /**
     * Publishes a message to a topic. All subscribers are notified asynchronously.
     *
     * @param message the message to publish
     * @return message ID
     */
    public long publish(AgentMessage message) {
        long id = idSeq.getAndIncrement();
        AgentMessage stamped = message.withId(id);
        log.debug("[bus] {} → [{}]: {} (id={})",
                message.from(), message.topic(), message.intent(), id);

        // Resolve CompletableFuture if this is a response
        if (message.correlationId() != null) {
            CompletableFuture<AgentMessage> future = pendingResponses.remove(message.correlationId());
            if (future != null) { future.complete(stamped); return id; }
        }

        List<Consumer<AgentMessage>> handlers = subscriptions.get(message.topic());
        if (handlers == null || handlers.isEmpty()) {
            deadLetters.add(stamped);
            log.debug("[bus] dead letter: no subscribers for topic '{}'", message.topic());
        } else {
            handlers.forEach(h -> Thread.ofVirtual().start(() -> {
                try { h.accept(stamped); }
                catch (Exception e) { log.warn("[bus] handler error: {}", e.getMessage()); }
            }));
        }
        return id;
    }

    /**
     * Subscribes to messages on a topic.
     */
    public void subscribe(String topic, Consumer<AgentMessage> handler) {
        subscriptions.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(handler);
        log.debug("[bus] subscribed to topic '{}'", topic);
    }

    /**
     * Sends a request and waits for a response with timeout.
     * Uses correlation IDs for request/response pairing.
     */
    public Optional<AgentMessage> request(AgentMessage message, long timeoutMs) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<AgentMessage> future = new CompletableFuture<>();
        pendingResponses.put(correlationId, future);

        publish(message.withCorrelationId(correlationId));

        try {
            return Optional.of(future.get(timeoutMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            pendingResponses.remove(correlationId);
            log.warn("[bus] request timeout on topic '{}' after {}ms", message.topic(), timeoutMs);
            return Optional.empty();
        } catch (Exception e) {
            pendingResponses.remove(correlationId);
            log.warn("[bus] request failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Returns the blackboard for direct access. */
    public Blackboard blackboard() { return blackboard; }

    /** Returns undelivered messages. */
    public List<AgentMessage> deadLetters() { return List.copyOf(deadLetters); }

    // ── Conflict Resolution ────────────────────────────────────────────────

    /**
     * Resolves conflicting findings from multiple agents.
     */
    public Resolution resolveConflict(List<AgentFinding> findings,
                                      ConflictResolutionStrategy strategy) {
        if (findings.isEmpty()) return Resolution.empty();
        if (findings.size() == 1) return Resolution.unanimous(findings.get(0));

        return switch (strategy) {
            case HIGHEST_CONFIDENCE -> resolveByConfidence(findings);
            case MAJORITY_VOTE      -> resolveByVoting(findings);
            case MERGE              -> resolveMerge(findings);
            case ESCALATE           -> Resolution.escalate(findings);
        };
    }

    private Resolution resolveByConfidence(List<AgentFinding> findings) {
        AgentFinding winner = findings.stream()
                .max(Comparator.comparingDouble(AgentFinding::confidence))
                .orElseThrow();
        return new Resolution(ConflictResolutionStrategy.HIGHEST_CONFIDENCE,
                List.of(winner), findings, false, null);
    }

    private Resolution resolveByVoting(List<AgentFinding> findings) {
        // Group by normalized finding content, pick majority
        Map<String, List<AgentFinding>> groups = new LinkedHashMap<>();
        findings.forEach(f -> groups.computeIfAbsent(
                normalize(f.finding()), k -> new ArrayList<>()).add(f));

        String majority = groups.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey).orElse("");
        List<AgentFinding> winners = groups.getOrDefault(majority, findings);
        return new Resolution(ConflictResolutionStrategy.MAJORITY_VOTE,
                winners, findings, findings.size() == winners.size(), null);
    }

    private Resolution resolveMerge(List<AgentFinding> findings) {
        return new Resolution(ConflictResolutionStrategy.MERGE,
                findings, findings, false,
                "All findings merged — review for contradictions");
    }

    private String normalize(String s) {
        return s.toLowerCase().replaceAll("\\s+", " ").strip();
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * A structured message in the Agent Communication Language (ACL).
     */
    public record AgentMessage(
            long                id,
            String              from,
            String              to,            // null = broadcast
            String              topic,
            String              intent,        // ANALYZE, EXECUTE, REPORT, REQUEST, RESPONSE
            Map<String, Object> payload,
            Map<String, Object> constraints,   // maxTokens, timeoutMs, etc.
            String              correlationId, // for request/response pairing
            Instant             sentAt
    ) {
        AgentMessage withId(long newId) {
            return new AgentMessage(newId, from, to, topic, intent, payload,
                    constraints, correlationId, sentAt);
        }

        AgentMessage withCorrelationId(String cid) {
            return new AgentMessage(id, from, to, topic, intent, payload,
                    constraints, cid, sentAt);
        }

        public static Builder builder(String from, String topic, String intent) {
            return new Builder(from, topic, intent);
        }

        public static class Builder {
            private final String from, topic, intent;
            private String to;
            private Map<String, Object> payload = Map.of();
            private Map<String, Object> constraints = Map.of();

            Builder(String from, String topic, String intent) {
                this.from = from; this.topic = topic; this.intent = intent;
            }
            public Builder to(String t)                    { this.to = t; return this; }
            public Builder payload(Map<String, Object> p)  { this.payload = p; return this; }
            public Builder constraint(String k, Object v)  {
                this.constraints = new HashMap<>(constraints);
                ((Map<String, Object>)this.constraints).put(k, v); return this; }

            public AgentMessage build() {
                return new AgentMessage(0, from, to, topic, intent, payload,
                        constraints, null, Instant.now());
            }
        }
    }

    /**
     * Shared blackboard for agent coordination.
     * Agents post findings here; the orchestrator reads and synthesises.
     */
    public static class Blackboard {
        private final Map<String, BlackboardEntry> entries = new ConcurrentHashMap<>();

        /** Posts a finding to the blackboard. */
        public void post(String key, String agentId, Object value, double confidence) {
            BlackboardEntry existing = entries.get(key);
            // Only update if new confidence is higher
            if (existing == null || confidence >= existing.confidence()) {
                entries.put(key, new BlackboardEntry(key, agentId, value, confidence, Instant.now()));
            }
        }

        /** Reads a finding. Returns empty if not posted. */
        public Optional<BlackboardEntry> read(String key) {
            return Optional.ofNullable(entries.get(key));
        }

        /** Returns all entries posted by a specific agent. */
        public List<BlackboardEntry> byAgent(String agentId) {
            return entries.values().stream()
                    .filter(e -> e.agentId().equals(agentId)).toList();
        }

        /** Returns all entries (sorted by confidence descending). */
        public List<BlackboardEntry> all() {
            return entries.values().stream()
                    .sorted(Comparator.comparingDouble(BlackboardEntry::confidence).reversed())
                    .toList();
        }

        /** Clears the blackboard (between workflow phases). */
        public void clear() { entries.clear(); }

        public int size() { return entries.size(); }
    }

    public record BlackboardEntry(
            String  key,
            String  agentId,
            Object  value,
            double  confidence,
            Instant postedAt
    ) {}

    public record AgentFinding(
            String agentId,
            String finding,
            double confidence,
            Instant producedAt
    ) {}

    public enum ConflictResolutionStrategy {
        HIGHEST_CONFIDENCE, MAJORITY_VOTE, MERGE, ESCALATE
    }

    public record Resolution(
            ConflictResolutionStrategy strategy,
            List<AgentFinding> selected,
            List<AgentFinding> allFindings,
            boolean unanimous,
            String warning
    ) {
        static Resolution empty() {
            return new Resolution(ConflictResolutionStrategy.MERGE,
                    List.of(), List.of(), true, null);
        }
        static Resolution unanimous(AgentFinding f) {
            return new Resolution(ConflictResolutionStrategy.MAJORITY_VOTE,
                    List.of(f), List.of(f), true, null);
        }
        static Resolution escalate(List<AgentFinding> f) {
            return new Resolution(ConflictResolutionStrategy.ESCALATE,
                    List.of(), f, false, "Conflict requires orchestrator resolution");
        }
    }
}
