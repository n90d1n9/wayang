package tech.kayys.gamelan.collaboration.workspace;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * CollaborativeWorkspace — shared blackboard for multi-agent coordination.
 *
 * <h2>Motivation (OPENDEV paper §5, future directions)</h2>
 * Current subagents execute independently under main-agent coordination, communicating only
 * through completion markers. The paper identifies richer coordination patterns as a key future
 * direction: peer-to-peer communication between subagents, shared blackboard architectures for
 * collaborative problem-solving, and negotiation protocols for resolving conflicting tool outcomes.
 *
 * <h2>Architecture</h2>
 * <pre>
 * Blackboard — shared typed key-value store. Any agent can read/write. Versioned entries
 *              with conflict detection (last-writer-wins or fail-on-conflict policy).
 *
 * Message Bus — direct peer-to-peer messages between named agents. Agents subscribe by name.
 *              Non-blocking delivery; agents poll or register callbacks.
 *
 * Conflict Resolver — detects and resolves write conflicts on the same blackboard key.
 *                     Strategies: LAST_WINS, FAIL_FAST, MERGE_LIST, REQUIRE_LOCK.
 *
 * Lock Manager — named mutex locks for critical sections (file edits, shared state mutations).
 *                Agents acquire a lock before modifying shared artifacts, preventing race conditions.
 *                Per the paper: "task management tools are excluded from subagents so that only
 *                the main agent coordinates todo lists, preventing race conditions."
 * </pre>
 */
@ApplicationScoped
public class CollaborativeWorkspace {

    private static final Logger log = LoggerFactory.getLogger(CollaborativeWorkspace.class);

    @Inject AgentTelemetry telemetry;

    // Blackboard — versioned entries
    private final ConcurrentHashMap<String, BlackboardEntry> blackboard = new ConcurrentHashMap<>();
    private final AtomicLong                                 globalVersion = new AtomicLong(0);

    // Message bus — per-agent queues + callbacks
    private final ConcurrentHashMap<String, LinkedBlockingQueue<AgentMessage>> inboxes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Consumer<AgentMessage>>>      listeners = new ConcurrentHashMap<>();

    // Lock manager — named semaphores
    private final ConcurrentHashMap<String, String>      lockOwners   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Semaphore>   locks        = new ConcurrentHashMap<>();

    // ── Blackboard API ─────────────────────────────────────────────────────

    /**
     * Writes a value to the blackboard.
     *
     * @param key      blackboard key
     * @param value    the value (any serializable object)
     * @param authorId the writing agent's ID
     * @param policy   conflict resolution policy
     * @return the resulting BlackboardEntry
     */
    public BlackboardEntry write(String key, Object value, String authorId, ConflictPolicy policy) {
        long version = globalVersion.incrementAndGet();
        BlackboardEntry existing = blackboard.get(key);

        if (existing != null && policy == ConflictPolicy.FAIL_FAST) {
            throw new WorkspaceConflictException(
                    "Key '" + key + "' already set by " + existing.authorId() +
                    " at version " + existing.version());
        }

        if (existing != null && policy == ConflictPolicy.MERGE_LIST) {
            List<Object> merged = new ArrayList<>();
            if (existing.value() instanceof List<?> l) merged.addAll(l);
            else merged.add(existing.value());
            merged.add(value);
            value = merged;
        }

        BlackboardEntry entry = new BlackboardEntry(key, value, authorId, version, Instant.now());
        blackboard.put(key, entry);
        telemetry.count("workspace.blackboard.write");
        log.debug("[workspace] {} wrote '{}' (v{})", authorId, key, version);

        // Notify subscribers watching this key
        broadcast(AgentMessage.blackboardChange(key, value, authorId));
        return entry;
    }

    /** Reads a value from the blackboard. Returns empty if not set. */
    public Optional<BlackboardEntry> read(String key) { return Optional.ofNullable(blackboard.get(key)); }

    /** Returns all current blackboard entries. */
    public Map<String, BlackboardEntry> snapshot() { return Collections.unmodifiableMap(blackboard); }

    /** Returns all entries written by a specific agent. */
    public List<BlackboardEntry> entriesByAgent(String agentId) {
        return blackboard.values().stream()
                .filter(e -> agentId.equals(e.authorId()))
                .sorted(Comparator.comparing(BlackboardEntry::version))
                .toList();
    }

    // ── Message Bus API ────────────────────────────────────────────────────

    /**
     * Registers an agent to receive messages.
     * @param agentId   the agent's unique identifier
     * @param onMessage callback invoked when a message arrives (may be null for polling)
     */
    public void register(String agentId, Consumer<AgentMessage> onMessage) {
        inboxes.computeIfAbsent(agentId, k -> new LinkedBlockingQueue<>());
        if (onMessage != null) {
            listeners.computeIfAbsent(agentId, k -> new CopyOnWriteArrayList<>()).add(onMessage);
        }
        log.debug("[workspace] registered agent {}", agentId);
    }

    /**
     * Sends a direct message from one agent to another.
     */
    public void send(String fromAgent, String toAgent, String topic, Object payload) {
        AgentMessage msg = new AgentMessage(UUID.randomUUID().toString(), fromAgent, toAgent,
                topic, payload, Instant.now(), MessageType.DIRECT);
        LinkedBlockingQueue<AgentMessage> inbox = inboxes.get(toAgent);
        if (inbox == null) {
            log.warn("[workspace] no inbox for agent {} — message dropped", toAgent);
            return;
        }
        inbox.offer(msg);
        // Notify listeners synchronously
        List<Consumer<AgentMessage>> cbs = listeners.getOrDefault(toAgent, List.of());
        cbs.forEach(cb -> { try { cb.accept(msg); } catch (Exception e) { /* isolated */ } });
        telemetry.count("workspace.message.sent");
        log.debug("[workspace] {} → {} [{}]", fromAgent, toAgent, topic);
    }

    /**
     * Broadcasts a message to ALL registered agents (except the sender).
     */
    public void broadcast(AgentMessage message) {
        inboxes.forEach((agentId, inbox) -> {
            if (!agentId.equals(message.fromAgent())) {
                inbox.offer(message);
                listeners.getOrDefault(agentId, List.of()).forEach(cb -> {
                    try { cb.accept(message); } catch (Exception e) { /* isolated */ }
                });
            }
        });
    }

    /**
     * Polls pending messages for an agent (non-blocking).
     */
    public List<AgentMessage> poll(String agentId) {
        LinkedBlockingQueue<AgentMessage> inbox = inboxes.get(agentId);
        if (inbox == null) return List.of();
        List<AgentMessage> batch = new ArrayList<>();
        inbox.drainTo(batch);
        return batch;
    }

    /**
     * Waits for a message with the given topic (blocking, with timeout).
     */
    public Optional<AgentMessage> await(String agentId, String topic, long timeoutMs)
            throws InterruptedException {
        LinkedBlockingQueue<AgentMessage> inbox = inboxes.computeIfAbsent(agentId,
                k -> new LinkedBlockingQueue<>());
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            AgentMessage msg = inbox.poll(50, TimeUnit.MILLISECONDS);
            if (msg != null && topic.equals(msg.topic())) return Optional.of(msg);
            if (msg != null) inbox.offer(msg); // put back if wrong topic
        }
        return Optional.empty();
    }

    // ── Lock Manager API ───────────────────────────────────────────────────

    /**
     * Acquires an exclusive lock on a named resource.
     * Prevents multiple agents from concurrently modifying the same file/state.
     *
     * @param resource  the resource identifier (e.g., file path or "todos")
     * @param agentId   the requesting agent
     * @param timeoutMs max wait time in milliseconds
     * @return true if lock acquired, false if timeout
     */
    public boolean acquireLock(String resource, String agentId, long timeoutMs) {
        Semaphore sem = locks.computeIfAbsent(resource, k -> new Semaphore(1));
        try {
            boolean acquired = sem.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (acquired) {
                lockOwners.put(resource, agentId);
                telemetry.count("workspace.lock.acquired");
                log.debug("[workspace] lock '{}' acquired by {}", resource, agentId);
            } else {
                telemetry.count("workspace.lock.timeout");
                log.warn("[workspace] lock '{}' timeout for {} (held by {})",
                        resource, agentId, lockOwners.get(resource));
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Releases a lock held by an agent.
     * Only the lock owner may release it.
     */
    public boolean releaseLock(String resource, String agentId) {
        String owner = lockOwners.get(resource);
        if (!agentId.equals(owner)) {
            log.warn("[workspace] release rejected: {} doesn't hold lock '{}' (owner: {})",
                    agentId, resource, owner);
            return false;
        }
        lockOwners.remove(resource);
        Semaphore sem = locks.get(resource);
        if (sem != null) sem.release();
        telemetry.count("workspace.lock.released");
        return true;
    }

    /** Returns current lock owners. */
    public Map<String, String> lockState() { return Collections.unmodifiableMap(lockOwners); }

    /** Clears the entire workspace (call between sessions). */
    public void clear() {
        blackboard.clear();
        inboxes.clear();
        listeners.clear();
        lockOwners.clear();
        locks.clear();
        globalVersion.set(0);
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum ConflictPolicy { LAST_WINS, FAIL_FAST, MERGE_LIST }
    public enum MessageType    { DIRECT, BROADCAST, BLACKBOARD_CHANGE, COMPLETION, NEGOTIATION }

    public record BlackboardEntry(String key, Object value, String authorId, long version, Instant timestamp) {}

    public record AgentMessage(
            String      id,
            String      fromAgent,
            String      toAgent,
            String      topic,
            Object      payload,
            Instant     timestamp,
            MessageType type
    ) {
        static AgentMessage blackboardChange(String key, Object value, String author) {
            return new AgentMessage(UUID.randomUUID().toString(), author, "*",
                    "blackboard:" + key, value, Instant.now(), MessageType.BLACKBOARD_CHANGE);
        }
        static AgentMessage completion(String fromAgent, String toAgent, Object summary) {
            return new AgentMessage(UUID.randomUUID().toString(), fromAgent, toAgent,
                    "completion", summary, Instant.now(), MessageType.COMPLETION);
        }
    }

    public static final class WorkspaceConflictException extends RuntimeException {
        public WorkspaceConflictException(String msg) { super(msg); }
    }
}
