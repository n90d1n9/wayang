package tech.kayys.wayang.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for all Wayang catalogs.
 *
 * <p>Provides a unified template for catalog implementations with common storage,
 * indexing, and caching behavior. Concrete catalogs extend this class and implement
 * the abstract methods to define how items are identified and indexed.</p>
 *
 * @param <T> the type of items stored in this catalog
 * @param <I> the type of ID used to identify catalog items
 */
public abstract class AbstractCatalog<T, I> {

    private final String catalogId;
    private final Map<I, T> items;
    private final Map<String, Object> index;
    private volatile boolean indexBuilt;
    private final Object indexLock = new Object();

    /**
     * Constructs a new AbstractCatalog with the given catalog ID.
     *
     * @param catalogId unique identifier for this catalog
     */
    protected AbstractCatalog(String catalogId) {
        this.catalogId = catalogId;
        this.items = new ConcurrentHashMap<>();
        this.index = new ConcurrentHashMap<>();
        this.indexBuilt = false;
    }

    /**
     * Extracts the unique ID from a catalog item.
     *
     * @param item the item to extract ID from
     * @return the unique identifier for the item
     */
    protected abstract I getId(T item);

    /**
     * Converts an item to an index entry for lookup.
     *
     * @param item the item to convert
     * @return a map representation suitable for indexing
     */
    protected abstract Map<String, Object> toIndexEntry(T item);

    /**
     * Reconstructs an item from its index entry.
     *
     * @param entry the index entry
     * @return the reconstructed item, or empty if conversion fails
     */
    protected abstract Optional<T> fromIndexEntry(Map<String, Object> entry);

    /**
     * Finds a single item by its ID.
     *
     * @param id the item ID to look up
     * @return the item if found, or empty
     */
    public Optional<T> find(I id) {
        return Optional.ofNullable(items.get(id));
    }

    /**
     * Gets all items in the catalog.
     *
     * @return an unmodifiable list of all items
     */
    public List<T> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }

    /**
     * Adds a single item to the catalog.
     *
     * @param item the item to add
     */
    public void add(T item) {
        if (item == null) {
            return;
        }
        I id = getId(item);
        items.put(id, item);
        invalidateIndex();
    }

    /**
     * Adds multiple items to the catalog in bulk.
     *
     * @param itemsToAdd the collection of items to add
     */
    public void addAll(Collection<T> itemsToAdd) {
        if (itemsToAdd == null || itemsToAdd.isEmpty()) {
            return;
        }
        for (T item : itemsToAdd) {
            if (item != null) {
                I id = getId(item);
                items.put(id, item);
            }
        }
        invalidateIndex();
    }

    /**
     * Removes an item from the catalog by its ID.
     *
     * @param id the ID of the item to remove
     * @return true if an item was removed, false if not found
     */
    public boolean remove(I id) {
        boolean removed = items.remove(id) != null;
        if (removed) {
            invalidateIndex();
        }
        return removed;
    }

    /**
     * Gets the total number of items in the catalog.
     *
     * @return the count of items
     */
    public int size() {
        return items.size();
    }

    /**
     * Checks if the catalog contains an item with the given ID.
     *
     * @param id the ID to check
     * @return true if the item exists, false otherwise
     */
    public boolean contains(I id) {
        return items.containsKey(id);
    }

    /**
     * Gets the unique catalog ID.
     *
     * @return the catalog ID
     */
    public String getCatalogId() {
        return catalogId;
    }

    /**
     * Clears all items and index from the catalog.
     */
    public void clear() {
        items.clear();
        index.clear();
        indexBuilt = false;
    }

    /**
     * Builds or rebuilds the index from current items.
     * This is typically called after bulk modifications.
     */
    protected void buildIndex() {
        synchronized (indexLock) {
            index.clear();
            for (T item : items.values()) {
                Map<String, Object> entry = toIndexEntry(item);
                if (entry != null && !entry.isEmpty()) {
                    I id = getId(item);
                    index.put(String.valueOf(id), entry);
                }
            }
            indexBuilt = true;
        }
    }

    /**
     * Gets an index entry by ID, building index if needed.
     *
     * @param id the item ID
     * @return the index entry, or empty if not found
     */
    protected Optional<Map<String, Object>> getIndexEntry(I id) {
        ensureIndexBuilt();
        return Optional.ofNullable((Map<String, Object>) index.get(String.valueOf(id)));
    }

    /**
     * Ensures the index is built before use.
     */
    private void ensureIndexBuilt() {
        if (!indexBuilt) {
            synchronized (indexLock) {
                if (!indexBuilt) {
                    buildIndex();
                }
            }
        }
    }

    /**
     * Marks the index as invalid, requiring rebuild on next access.
     */
    protected void invalidateIndex() {
        indexBuilt = false;
    }

    /**
     * Gets the entire index map.
     *
     * @return an unmodifiable view of the index
     */
    protected Map<String, Object> getIndex() {
        ensureIndexBuilt();
        return Collections.unmodifiableMap(new LinkedHashMap<>(index));
    }
}
