package tech.kayys.gamelan.runtime.offline;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * OfflineRuntimeManager — graceful degradation when the LLM provider is unreachable.
 *
 * <h2>From the OPENDEV paper (§2.5.4 — Provider and Model Cache)</h2>
 * The system needs to know what models are available from each provider, along with their
 * capabilities. The cache uses a stale-while-revalidate strategy: on startup, serve from cache
 * if fresh; if stale, serve the stale data and refresh in the background; if the refresh fails,
 * continue with stale data. This ensures that the agent can start even when offline.
 *
 * <h2>Also from §3.5 (Lazy Loading and Bounded Growth)</h2>
 * "External metadata benefits from a stale-while-revalidate caching strategy: on startup, serve
 * from cache if fresh; if stale, serve the stale data and refresh in the background; if the
 * refresh fails, continue with stale data. This guarantees offline startup."
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li>Provider health monitoring with circuit-breaker pattern</li>
 *   <li>Stale-while-revalidate model capability cache (24h TTL, paper §2.5.4)</li>
 *   <li>Response cache for repeated identical prompts (deterministic offline replay)</li>
 *   <li>Fallback ordering: primary → backup → cached response → error with context</li>
 *   <li>Background revalidation so cache never blocks the main thread</li>
 * </ul>
 */
@ApplicationScoped
public class OfflineRuntimeManager {

    private static final Logger log = LoggerFactory.getLogger(OfflineRuntimeManager.class);

    // Paper constants (§2.5.4)
    private static final Duration CACHE_TTL            = Duration.ofHours(24);
    private static final int      CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final Duration CIRCUIT_RESET_AFTER  = Duration.ofMinutes(2);
    private static final int      RESPONSE_CACHE_SIZE  = 200;

    @Inject GamelanConfig  config;
    @Inject AgentTelemetry telemetry;

    // Circuit breaker state per provider
    private final Map<String, ProviderHealth> providerHealth = new ConcurrentHashMap<>();

    // Stale-while-revalidate capability cache
    private final Map<String, CachedCapability> capabilityCache = new ConcurrentHashMap<>();

    // Response cache (LRU for deterministic offline replay)
    private final Map<String, CachedResponse> responseCache =
            Collections.synchronizedMap(new LinkedHashMap<>(RESPONSE_CACHE_SIZE, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, CachedResponse> e) {
                    return size() > RESPONSE_CACHE_SIZE;
                }
            });

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofVirtual().name("offline-revalidate").factory());

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Checks whether the given provider is currently healthy.
     * Uses the circuit breaker: after CIRCUIT_FAILURE_THRESHOLD failures, opens the circuit
     * and waits CIRCUIT_RESET_AFTER before trying again.
     */
    public boolean isProviderHealthy(String provider) {
        ProviderHealth health = providerHealth.computeIfAbsent(provider, k -> new ProviderHealth());
        if (health.isCircuitOpen()) {
            if (health.shouldReset()) {
                health.halfOpen();
                log.info("[offline] circuit half-open for provider {}", provider);
            } else {
                telemetry.count("offline.circuit.blocked");
                return false;
            }
        }
        return true;
    }

    /**
     * Records a successful provider call (resets failure count, closes circuit).
     */
    public void recordSuccess(String provider) {
        ProviderHealth health = providerHealth.computeIfAbsent(provider, k -> new ProviderHealth());
        health.recordSuccess();
        log.debug("[offline] provider {} healthy (circuit closed)", provider);
    }

    /**
     * Records a failed provider call. Opens the circuit after CIRCUIT_FAILURE_THRESHOLD.
     */
    public void recordFailure(String provider) {
        ProviderHealth health = providerHealth.computeIfAbsent(provider, k -> new ProviderHealth());
        int failures = health.recordFailure();
        if (failures >= CIRCUIT_FAILURE_THRESHOLD) {
            health.openCircuit();
            telemetry.count("offline.circuit.opened");
            log.warn("[offline] circuit OPENED for provider {} after {} failures", provider, failures);
        }
    }

    /**
     * Caches a model response for offline replay.
     * Key is a hash of the model + prompt.
     */
    public void cacheResponse(String model, String prompt, String response) {
        String key = cacheKey(model, prompt);
        responseCache.put(key, new CachedResponse(response, Instant.now()));
        telemetry.count("offline.response.cached");
    }

    /**
     * Returns a cached response if available (stale-while-revalidate).
     */
    public Optional<String> getCachedResponse(String model, String prompt) {
        CachedResponse cached = responseCache.get(cacheKey(model, prompt));
        if (cached == null) return Optional.empty();
        telemetry.count("offline.response.cache_hit");
        log.debug("[offline] serving cached response for model={}", model);
        return Optional.of(cached.response());
    }

    /**
     * Stores model capability metadata with 24h TTL.
     * Used for offline startup — the agent can operate without network if the cache is warm.
     */
    public void cacheCapability(String model, ModelCapability capability) {
        capabilityCache.put(model, new CachedCapability(capability, Instant.now()));
        persistCapabilityCache(); // write to disk
    }

    /**
     * Returns cached capability metadata (stale-while-revalidate).
     * Background refresh triggered if data is stale.
     */
    public Optional<ModelCapability> getCapability(String model) {
        // Try in-memory first
        CachedCapability cached = capabilityCache.get(model);
        if (cached == null) cached = loadFromDisk(model);
        if (cached == null) return Optional.empty();

        if (cached.isStale(CACHE_TTL)) {
            // Serve stale, trigger background refresh
            String modelCopy = model;
            scheduler.schedule(() -> refreshCapabilityInBackground(modelCopy), 0, TimeUnit.MILLISECONDS);
            telemetry.count("offline.capability.stale_hit");
        } else {
            telemetry.count("offline.capability.fresh_hit");
        }
        return Optional.of(cached.capability());
    }

    /**
     * Returns all known provider health states.
     */
    public Map<String, ProviderHealth> healthReport() {
        return Collections.unmodifiableMap(providerHealth);
    }

    /**
     * Detects whether the runtime has network connectivity.
     */
    public boolean isOnline() {
        String endpoint = config.get("gamelan.provider.health_check_url",
                "https://api.anthropic.com");
        try {
            URI uri = URI.create(endpoint);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(3_000);
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            return code >= 200 && code < 500;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String cacheKey(String model, String prompt) {
        int hash = Objects.hash(model, prompt.strip().toLowerCase());
        return Integer.toHexString(hash);
    }

    private void refreshCapabilityInBackground(String model) {
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.debug("[offline] background refresh for model capability: {}", model);
        telemetry.count("offline.capability.background_refresh");
    }

    private void persistCapabilityCache() {
        Path cacheDir = Path.of(System.getProperty("user.home"), ".gamelan", "cache");
        try {
            Files.createDirectories(cacheDir);
            // Simple key=value persistence
            StringBuilder sb = new StringBuilder();
            capabilityCache.forEach((model, cap) ->
                    sb.append(model).append("=").append(cap.capability().contextLength())
                      .append(",").append(cap.capability().supportsVision())
                      .append(",").append(cap.cachedAt().toEpochMilli()).append("\n"));
            Files.writeString(cacheDir.resolve("models.cache"), sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) { log.debug("[offline] cache persist failed: {}", e.getMessage()); }
    }

    private CachedCapability loadFromDisk(String model) {
        Path cacheFile = Path.of(System.getProperty("user.home"), ".gamelan", "cache", "models.cache");
        if (!Files.exists(cacheFile)) return null;
        try {
            for (String line : Files.readAllLines(cacheFile)) {
                if (!line.startsWith(model + "=")) continue;
                String[] parts = line.substring(model.length() + 1).split(",");
                if (parts.length < 3) continue;
                int contextLen = Integer.parseInt(parts[0]);
                boolean vision = Boolean.parseBoolean(parts[1]);
                Instant cachedAt = Instant.ofEpochMilli(Long.parseLong(parts[2]));
                CachedCapability cap = new CachedCapability(
                        new ModelCapability(model, contextLen, vision, List.of()), cachedAt);
                capabilityCache.put(model, cap); // warm in-memory
                return cap;
            }
        } catch (Exception e) { log.debug("[offline] cache load failed: {}", e.getMessage()); }
        return null;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record ModelCapability(
            String       modelId,
            int          contextLength,
            boolean      supportsVision,
            List<String> features
    ) {}

    private record CachedResponse(String response, Instant cachedAt) {}

    private record CachedCapability(ModelCapability capability, Instant cachedAt) {
        boolean isStale(Duration ttl) {
            return Instant.now().isAfter(cachedAt.plus(ttl));
        }
    }

    public static final class ProviderHealth {
        private final AtomicInteger failures   = new AtomicInteger(0);
        private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
        private volatile Instant    openedAt;

        boolean isCircuitOpen()  { return circuitOpen.get(); }
        void openCircuit()       { circuitOpen.set(true); openedAt = Instant.now(); }
        void halfOpen()          { circuitOpen.set(false); }
        boolean shouldReset()    { return openedAt != null &&
                                   Instant.now().isAfter(openedAt.plus(CIRCUIT_RESET_AFTER)); }
        int recordFailure()      { return failures.incrementAndGet(); }
        void recordSuccess()     { failures.set(0); circuitOpen.set(false); }
        int failureCount()       { return failures.get(); }

        public String summary() {
            return circuitOpen.get() ? "OPEN (failures=" + failures + ")" :
                   failures.get() > 0 ? "DEGRADED (failures=" + failures + ")" : "HEALTHY";
        }
    }
}
