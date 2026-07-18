package tech.kayys.wayang.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for constructing and initializing catalog instances.
 *
 * <p>Provides a convenient API for building catalogs with custom configuration,
 * items, and optional auto-registration in the CatalogRegistry.</p>
 */
public class CatalogBuilder {

    private String domainId;
    private final List<Object> items = new ArrayList<>();
    private Object dataSource;
    private boolean cacheEnabled = true;
    private boolean autoRegister = true;

    /**
     * Creates a new CatalogBuilder.
     */
    public CatalogBuilder() {
    }

    /**
     * Sets the domain ID for this catalog (e.g., "contract", "skill").
     *
     * @param domainId the domain identifier
     * @return this builder for method chaining
     */
    public CatalogBuilder forDomain(String domainId) {
        if (domainId == null || domainId.trim().isEmpty()) {
            throw new IllegalArgumentException("Domain ID cannot be null or empty");
        }
        this.domainId = domainId;
        return this;
    }

    /**
     * Adds items to the catalog.
     *
     * @param itemsToAdd collection of items to add
     * @return this builder for method chaining
     */
    public CatalogBuilder withItems(Collection<?> itemsToAdd) {
        if (itemsToAdd != null && !itemsToAdd.isEmpty()) {
            this.items.addAll(itemsToAdd);
        }
        return this;
    }

    /**
     * Adds a single item to the catalog.
     *
     * @param item the item to add
     * @return this builder for method chaining
     */
    public CatalogBuilder withItem(Object item) {
        if (item != null) {
            this.items.add(item);
        }
        return this;
    }

    /**
     * Sets a data source for the catalog (e.g., database, API, file).
     *
     * @param source the data source
     * @return this builder for method chaining
     */
    public CatalogBuilder withSource(Object source) {
        this.dataSource = source;
        return this;
    }

    /**
     * Enables or disables caching for this catalog.
     *
     * @param enabled true to enable cache, false to disable
     * @return this builder for method chaining
     */
    public CatalogBuilder withCache(boolean enabled) {
        this.cacheEnabled = enabled;
        return this;
    }

    /**
     * Sets whether this catalog should auto-register in CatalogRegistry.
     *
     * @param shouldAutoRegister true to auto-register, false to skip
     * @return this builder for method chaining
     */
    public CatalogBuilder autoRegister(boolean shouldAutoRegister) {
        this.autoRegister = shouldAutoRegister;
        return this;
    }

    /**
     * Builds the catalog with current configuration.
     * Note: Applications must provide a concrete implementation or factory.
     *
     * @return the constructed AbstractCatalog instance
     * @throws IllegalStateException if required configuration is missing
     */
    public <T, I> AbstractCatalog<T, I> build() {
        if (domainId == null || domainId.trim().isEmpty()) {
            throw new IllegalStateException("Domain ID must be set before building");
        }

        // Create a concrete implementation - this would typically be overridden
        // by subclasses or factory methods for specific catalog types.
        // For now, we provide a basic generic implementation.
        @SuppressWarnings("unchecked")
        AbstractCatalog<T, I> catalog = (AbstractCatalog<T, I>) new GenericCatalog(domainId);

        // Add items if provided
        if (!items.isEmpty()) {
            catalog.addAll((Collection<T>) items);
        }

        // Auto-register if enabled
        if (autoRegister) {
            CatalogRegistry.registerOrReplace(domainId, catalog);
        }

        return catalog;
    }

    /**
     * Gets the current domain ID.
     *
     * @return the domain ID
     */
    public String getDomainId() {
        return domainId;
    }

    /**
     * Gets the items to be added to the catalog.
     *
     * @return unmodifiable list of items
     */
    public List<Object> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Gets the configured data source.
     *
     * @return the data source or null
     */
    public Object getDataSource() {
        return dataSource;
    }

    /**
     * Gets the cache enabled setting.
     *
     * @return true if cache is enabled
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * A minimal generic catalog implementation for basic use cases.
     * Applications should provide specialized implementations.
     */
    private static class GenericCatalog extends AbstractCatalog<Object, String> {

        GenericCatalog(String catalogId) {
            super(catalogId);
        }

        @Override
        protected String getId(Object item) {
            if (item == null) {
                return null;
            }
            // Try common ID extraction patterns
            try {
                if (item instanceof Map) {
                    Object id = ((Map<?, ?>) item).get("id");
                    return id != null ? id.toString() : null;
                }
                // Could add reflection-based ID extraction here if needed
                return item.toString();
            } catch (Exception e) {
                return item.toString();
            }
        }

        @Override
        protected java.util.Map<String, Object> toIndexEntry(Object item) {
            if (item instanceof Map) {
                return new java.util.LinkedHashMap<>((Map<?, ?>) item);
            }
            return java.util.Map.of();
        }

        @Override
        protected java.util.Optional<Object> fromIndexEntry(java.util.Map<String, Object> entry) {
            return java.util.Optional.of(entry);
        }
    }
}
