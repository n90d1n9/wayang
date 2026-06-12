package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Persistence store that reads from a primary store and falls back to a second store.
 */
public final class HybridAgenticCommerceWayangPersistenceStore implements AgenticCommerceWayangPersistenceStore {

    public static final String STORAGE_KIND = "hybrid";

    private final AgenticCommerceWayangPersistenceStore primary;
    private final AgenticCommerceWayangPersistenceStore fallback;
    private final boolean mirrorWritesToFallback;

    public HybridAgenticCommerceWayangPersistenceStore(
            AgenticCommerceWayangPersistenceStore primary,
            AgenticCommerceWayangPersistenceStore fallback,
            boolean mirrorWritesToFallback) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.mirrorWritesToFallback = mirrorWritesToFallback;
    }

    public static HybridAgenticCommerceWayangPersistenceStore of(
            AgenticCommerceWayangPersistenceStore primary,
            AgenticCommerceWayangPersistenceStore fallback) {
        return new HybridAgenticCommerceWayangPersistenceStore(primary, fallback, true);
    }

    public static HybridAgenticCommerceWayangPersistenceStore configured(
            AgenticCommerceWayangPersistenceStore primary,
            AgenticCommerceWayangPersistenceStore fallback,
            boolean mirrorWritesToFallback) {
        return new HybridAgenticCommerceWayangPersistenceStore(primary, fallback, mirrorWritesToFallback);
    }

    public AgenticCommerceWayangPersistenceStore primary() {
        return primary;
    }

    public AgenticCommerceWayangPersistenceStore fallback() {
        return fallback;
    }

    public boolean mirrorWritesToFallback() {
        return mirrorWritesToFallback;
    }

    @Override
    public String storageKind() {
        return STORAGE_KIND;
    }

    @Override
    public Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
        return load(AgenticCommerceWayangPersistenceStore::loadRuntimeConfig);
    }

    @Override
    public void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        save(store -> store.saveRuntimeConfig(runtimeConfig));
    }

    @Override
    public Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
        return load(AgenticCommerceWayangPersistenceStore::loadBootstrapConfig);
    }

    @Override
    public void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        save(store -> store.saveBootstrapConfig(bootstrapConfig));
    }

    @Override
    public Optional<Map<String, Object>> loadBootstrapReport() {
        return load(AgenticCommerceWayangPersistenceStore::loadBootstrapReport);
    }

    @Override
    public void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport) {
        save(store -> store.saveBootstrapReport(bootstrapReport));
    }

    @Override
    public Optional<Map<String, Object>> loadManifest() {
        return load(AgenticCommerceWayangPersistenceStore::loadManifest);
    }

    @Override
    public void saveManifest(AgenticCommerceWayangManifest manifest) {
        save(store -> store.saveManifest(manifest));
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", storageKind());
        values.put("primaryStorageKind", primary.storageKind());
        values.put("fallbackStorageKind", fallback.storageKind());
        values.put("mirrorWritesToFallback", mirrorWritesToFallback);
        values.put("target", AgenticCommerceWayangPersistenceTargetDescriptor.fromStore(this).toMap());
        values.put("primary", primary.toMap());
        values.put("fallback", fallback.toMap());
        return Map.copyOf(values);
    }

    private <T> Optional<T> load(Function<AgenticCommerceWayangPersistenceStore, Optional<T>> loader) {
        try {
            Optional<T> primaryValue = loader.apply(primary);
            if (primaryValue.isPresent()) {
                return primaryValue;
            }
        } catch (RuntimeException ignored) {
            // Fall through to fallback store.
        }
        return loader.apply(fallback);
    }

    private void save(Consumer<AgenticCommerceWayangPersistenceStore> writer) {
        RuntimeException primaryFailure = null;
        try {
            writer.accept(primary);
        } catch (RuntimeException exception) {
            primaryFailure = exception;
        }
        if (primaryFailure == null && !mirrorWritesToFallback) {
            return;
        }
        try {
            writer.accept(fallback);
        } catch (RuntimeException fallbackFailure) {
            if (primaryFailure != null) {
                primaryFailure.addSuppressed(fallbackFailure);
                throw primaryFailure;
            }
            throw fallbackFailure;
        }
    }
}
