package tech.kayys.wayang.agent.core.security;

import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for controlling request frequency per client.
 * Uses token bucket algorithm for fair rate limiting.
 */
public class RateLimiter {
    private static final Logger LOGGER = Logger.getLogger(RateLimiter.class);
    
    private final int requestsPerWindow;
    private final Duration window;
    private final Duration cleanupInterval;
    private final ConcurrentHashMap<String, ClientBucket> buckets;
    
    private volatile Instant lastCleanup = Instant.now();
    
    public RateLimiter(int requestsPerWindow, Duration window) {
        this.requestsPerWindow = requestsPerWindow;
        this.window = Objects.requireNonNull(window);
        this.cleanupInterval = Duration.ofMinutes(5);
        this.buckets = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if a client can proceed with a request.
     */
    public boolean allowRequest(String clientId) {
        cleanupIfNeeded();
        
        ClientBucket bucket = buckets.computeIfAbsent(clientId, k -> new ClientBucket(requestsPerWindow, window));
        return bucket.allowRequest();
    }
    
    /**
     * Get remaining requests for a client.
     */
    public int getRemainingRequests(String clientId) {
        ClientBucket bucket = buckets.get(clientId);
        return bucket != null ? bucket.getRemainingRequests() : requestsPerWindow;
    }
    
    /**
     * Get reset time for a client (when allowance resets).
     */
    public Instant getResetTime(String clientId) {
        ClientBucket bucket = buckets.get(clientId);
        return bucket != null ? bucket.getResetTime() : Instant.now();
    }
    
    /**
     * Reset limits for a specific client.
     */
    public void reset(String clientId) {
        buckets.remove(clientId);
    }
    
    /**
     * Reset all limits.
     */
    public void resetAll() {
        buckets.clear();
    }
    
    private void cleanupIfNeeded() {
        Instant now = Instant.now();
        if (now.isAfter(lastCleanup.plus(cleanupInterval))) {
            synchronized (this) {
                if (now.isAfter(lastCleanup.plus(cleanupInterval))) {
                    buckets.entrySet().removeIf(e -> e.getValue().isExpired());
                    lastCleanup = now;
                }
            }
        }
    }
    
    /**
     * Token bucket for individual client rate limiting.
     */
    private static class ClientBucket {
        private final int capacity;
        private final Duration window;
        private volatile int tokens;
        private volatile Instant windowStart;
        
        ClientBucket(int capacity, Duration window) {
            this.capacity = capacity;
            this.window = window;
            this.tokens = capacity;
            this.windowStart = Instant.now();
        }
        
        synchronized boolean allowRequest() {
            refillIfNeeded();
            
            if (tokens > 0) {
                tokens--;
                return true;
            }
            
            return false;
        }
        
        int getRemainingRequests() {
            refillIfNeeded();
            return tokens;
        }
        
        Instant getResetTime() {
            return windowStart.plus(window);
        }
        
        boolean isExpired() {
            return Instant.now().isAfter(windowStart.plus(window).plus(Duration.ofMinutes(1)));
        }
        
        private void refillIfNeeded() {
            Instant now = Instant.now();
            if (now.isAfter(windowStart.plus(window))) {
                tokens = capacity;
                windowStart = now;
            }
        }
    }
}
