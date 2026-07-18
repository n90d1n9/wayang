package tech.kayys.wayang.agent.store;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import tech.kayys.wayang.agent.event.AgentRunEvent;
import tech.kayys.wayang.agent.run.AgentRunStatus;
import tech.kayys.wayang.client.SdkText;

/**
 * Abstract base class for AgentRunStore implementations, providing common
 * persistence logic, caching, and synchronization across file-based and
 * in-memory store implementations.
 *
 * Subclasses implement backend-specific storage operations through abstract
 * methods while benefiting from shared cache management and locking.
 */
public abstract class AbstractAgentRunStore implements AgentRunStore {

    protected final Map<String, AgentRunStatus> cache = new LinkedHashMap<>();
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected static final int DEFAULT_MAX_CACHE_SIZE = 1000;
    protected int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;

    /**
     * Template method for finding a single agent run.
     * Checks cache first, then delegates to backend implementation if not cached.
     */
    @Override
    public Optional<AgentRunStatus> find(String runId) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        
        lock.readLock().lock();
        try {
            if (cache.containsKey(normalizedRunId)) {
                AgentRunStatus cached = cache.get(normalizedRunId);
                return Optional.ofNullable(cached);
            }
        } finally {
            lock.readLock().unlock();
        }

        Optional<AgentRunStatus> result = doFind(normalizedRunId);
        
        if (result.isPresent()) {
            updateCache(normalizedRunId, result.get());
        }
        
        return result;
    }

    /**
     * Template method for finding all agent runs.
     * Delegates to backend implementation and updates cache.
     */
    @Override
    public List<AgentRunStatus> findAll() {
        List<AgentRunStatus> results = doFindAll();
        
        lock.writeLock().lock();
        try {
            invalidateCacheLocked();
            for (AgentRunStatus status : results) {
                cache.put(status.handle().runId(), status);
            }
        } finally {
            lock.writeLock().unlock();
        }
        
        return results;
    }

    /**
     * Template method for saving an agent run.
     * Validates input, delegates to backend, and updates cache.
     */
    @Override
    public AgentRunStatus save(AgentRunStatus status) {
        AgentRunStatus normalized = status == null
                ? AgentRunStatus.unknown("", "Cannot record a null run status.")
                : status;
        
        AgentRunStatus saved = doSave(normalized);
        updateCache(saved.handle().runId(), saved);
        
        return saved;
    }

    /**
     * Template method for removing an agent run by ID.
     * Delegates to backend and removes from cache.
     */
    @Override
    public boolean remove(String runId) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        boolean removed = doRemove(normalizedRunId);
        
        if (removed) {
            lock.writeLock().lock();
            try {
                cache.remove(normalizedRunId);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        return removed;
    }

    /**
     * Template method for getting count of agent runs.
     * Delegates to backend implementation.
     */
    public long count() {
        return doCount();
    }

    /**
     * Backend-specific find operation. Subclasses must implement.
     */
    protected abstract Optional<AgentRunStatus> doFind(String runId);

    /**
     * Backend-specific find all operation. Subclasses must implement.
     */
    protected abstract List<AgentRunStatus> doFindAll();

    /**
     * Backend-specific save operation. Subclasses must implement.
     */
    protected abstract AgentRunStatus doSave(AgentRunStatus status);

    /**
     * Backend-specific remove operation. Subclasses must implement.
     */
    protected abstract boolean doRemove(String runId);

    /**
     * Backend-specific count operation. Subclasses must implement.
     */
    protected abstract long doCount();

    /**
     * Helper method to update cache with thread-safe locking.
     */
    protected void updateCache(String id, AgentRunStatus run) {
        lock.writeLock().lock();
        try {
            if (cache.size() >= maxCacheSize) {
                cache.remove(cache.keySet().iterator().next());
            }
            cache.put(id, run);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Helper method to invalidate entire cache.
     */
    protected void invalidateCache() {
        lock.writeLock().lock();
        try {
            invalidateCacheLocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Internal helper for cache invalidation (assumes lock is held).
     */
    protected void invalidateCacheLocked() {
        cache.clear();
    }

    /**
     * Helper method to acquire write lock for batch operations.
     */
    protected Lock acquireWriteLock() {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        return writeLock;
    }

    /**
     * Helper method to release write lock after batch operations.
     */
    protected void releaseWriteLock(Lock writeLock) {
        if (writeLock != null) {
            writeLock.unlock();
        }
    }
}
