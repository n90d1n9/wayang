package tech.kayys.wayang.agenticcommerce.wayang;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry for persistence store providers.
 */
public final class AgenticCommerceWayangPersistenceStoreProviders {

    private static final AgenticCommerceWayangPersistenceStoreProviders DEFAULTS =
            builder()
                    .provider(new FileProvider())
                    .provider(new InMemoryProvider())
                    .provider(new HybridProvider())
                    .provider(new ObjectStoreProvider())
                    .provider(new DatabaseProvider())
                    .build();

    private final List<AgenticCommerceWayangPersistenceStoreProvider> providers;

    public AgenticCommerceWayangPersistenceStoreProviders(
            List<AgenticCommerceWayangPersistenceStoreProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            this.providers = List.of();
        } else {
            this.providers = providers.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    public static AgenticCommerceWayangPersistenceStoreProviders defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<AgenticCommerceWayangPersistenceStoreProvider> providers() {
        return providers;
    }

    public List<String> storageKinds() {
        return providers.stream()
                .map(AgenticCommerceWayangPersistenceStoreProvider::storageKind)
                .toList();
    }

    public Optional<AgenticCommerceWayangPersistenceStoreProvider> provider(
            AgenticCommerceWayangPersistenceConfig config) {
        AgenticCommerceWayangPersistenceConfig resolved = Objects.requireNonNull(config, "config");
        return providers.stream()
                .filter(provider -> provider.supports(resolved))
                .findFirst();
    }

    public AgenticCommerceWayangPersistenceStore build(AgenticCommerceWayangPersistenceConfig config) {
        return build(config, null);
    }

    public AgenticCommerceWayangPersistenceStore build(
            AgenticCommerceWayangPersistenceConfig config,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver) {
        return build(config, objectStoreClientResolver, null);
    }

    public AgenticCommerceWayangPersistenceStore build(
            AgenticCommerceWayangPersistenceConfig config,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        AgenticCommerceWayangPersistenceConfig resolved = Objects.requireNonNull(config, "config");
        return provider(resolved)
                .orElseThrow(() -> unsupported(resolved))
                .build(new AgenticCommerceWayangPersistenceProviderContext(
                        resolved,
                        objectStoreClientResolver,
                        databasePersistenceClientResolver,
                        this));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerCount", providers.size());
        values.put("storageKinds", storageKinds());
        values.put("providers", providers.stream()
                .map(AgenticCommerceWayangPersistenceStoreProvider::toMap)
                .toList());
        return Map.copyOf(values);
    }

    private static IllegalArgumentException unsupported(AgenticCommerceWayangPersistenceConfig config) {
        return new IllegalArgumentException(
                "Unsupported Agentic Commerce persistence storage kind: " + config.storageKind());
    }

    public static final class Builder {

        private final List<AgenticCommerceWayangPersistenceStoreProvider> providers = new ArrayList<>();

        public Builder provider(AgenticCommerceWayangPersistenceStoreProvider provider) {
            providers.add(Objects.requireNonNull(provider, "provider"));
            return this;
        }

        public Builder providers(AgenticCommerceWayangPersistenceStoreProviders providers) {
            if (providers != null) {
                this.providers.addAll(providers.providers());
            }
            return this;
        }

        public AgenticCommerceWayangPersistenceStoreProviders build() {
            return new AgenticCommerceWayangPersistenceStoreProviders(providers);
        }
    }

    private static final class FileProvider implements AgenticCommerceWayangPersistenceStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_FILE;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceConfig config) {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_FILE.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceStore build(AgenticCommerceWayangPersistenceProviderContext context) {
            return FileAgenticCommerceWayangPersistenceStore.at(Path.of(context.config().directory()));
        }
    }

    private static final class InMemoryProvider implements AgenticCommerceWayangPersistenceStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceConfig config) {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceStore build(AgenticCommerceWayangPersistenceProviderContext context) {
            return InMemoryAgenticCommerceWayangPersistenceStore.create();
        }
    }

    private static final class HybridProvider implements AgenticCommerceWayangPersistenceStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_HYBRID;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceConfig config) {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_HYBRID.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceStore build(AgenticCommerceWayangPersistenceProviderContext context) {
            AgenticCommerceWayangPersistenceConfig config = context.config();
            return HybridAgenticCommerceWayangPersistenceStore.configured(
                    context.buildNested(config.primary()),
                    context.buildNested(config.fallback()),
                    config.mirrorWritesToFallback());
        }
    }

    private static final class ObjectStoreProvider implements AgenticCommerceWayangPersistenceStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_OBJECT_STORE;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceConfig config) {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_OBJECT_STORE.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceStore build(AgenticCommerceWayangPersistenceProviderContext context) {
            AgenticCommerceObjectStoreConfig objectStoreConfig = context.config().objectStoreConfig();
            return ObjectStoreAgenticCommerceWayangPersistenceStore.configured(
                    objectStoreConfig,
                    context.resolveObjectStore(objectStoreConfig));
        }
    }

    private static final class DatabaseProvider implements AgenticCommerceWayangPersistenceStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_DATABASE;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceConfig config) {
            return AgenticCommerceWayangPersistenceConfig.STORAGE_DATABASE.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceStore build(AgenticCommerceWayangPersistenceProviderContext context) {
            AgenticCommerceDatabasePersistenceConfig databaseConfig = context.config().databaseConfig();
            return DatabaseAgenticCommerceWayangPersistenceStore.configured(
                    databaseConfig,
                    context.resolveDatabase(databaseConfig));
        }
    }
}
