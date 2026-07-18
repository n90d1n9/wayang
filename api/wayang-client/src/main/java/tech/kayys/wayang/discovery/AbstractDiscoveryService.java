package tech.kayys.wayang.discovery;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import tech.kayys.wayang.client.SdkLists;

/**
 * Abstract base class for discovery service implementations.
 * Provides common discovery patterns and template methods for discovering,
 * filtering, and searching through registry data.
 *
 * @param <R> Registry type for underlying discovery data
 * @param <T> Discovery result element type
 * @param <Q> Query type for filtering discovery results
 */
public abstract class AbstractDiscoveryService<R, T, Q> {

    private final DiscoveryCache<T> cache;

    protected AbstractDiscoveryService() {
        this.cache = new DiscoveryCache<>();
    }

    /**
     * Get the underlying registry for discovery.
     */
    protected abstract R getRegistry();

    /**
     * Get unique identifier for this discovery service.
     */
    protected abstract String getDiscoveryId();

    /**
     * Perform backend-specific discovery logic.
     */
    protected abstract List<T> doDiscover();

    /**
     * Apply filters to discovery results.
     */
    protected abstract List<T> doApplyFilter(List<T> results, Q filter);

    /**
     * Discover all items from registry without filtering.
     */
    public List<T> discover() {
        String cacheKey = getDiscoveryId() + ":all";
        return cache.getOrCompute(cacheKey, () -> SdkLists.copy(doDiscover()));
    }

    /**
     * Discover items matching the provided filter.
     */
    public List<T> discoverMatching(Q filter) {
        if (filter == null) {
            return discover();
        }
        String cacheKey = getDiscoveryId() + ":" + filter.hashCode();
        return cache.getOrCompute(cacheKey, () -> {
            List<T> all = doDiscover();
            return SdkLists.copy(doApplyFilter(all, filter));
        });
    }

    /**
     * Find the first item matching the provided predicate.
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
        return discover().stream()
                .filter(predicate)
                .findFirst();
    }

    /**
     * Find all items matching the provided predicate.
     */
    public List<T> findAll(Predicate<T> predicate) {
        return discover().stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Count total items in discovery.
     */
    public long count() {
        return discover().size();
    }

    /**
     * Count items matching the provided filter.
     */
    public long countMatching(Q filter) {
        return discoverMatching(filter).size();
    }

    /**
     * Clear the discovery cache.
     */
    public void clearCache() {
        cache.clear();
    }
}
