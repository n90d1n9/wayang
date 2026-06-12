package tech.kayys.wayang.agenticcommerce.wayang;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry for transfer audit sink/store providers.
 */
public final class AgenticCommerceWayangPersistenceTransferAuditStoreProviders {

    private static final AgenticCommerceWayangPersistenceTransferAuditStoreProviders DEFAULTS =
            builder()
                    .provider(new NoopProvider())
                    .provider(new InMemoryProvider())
                    .provider(new FileProvider())
                    .provider(new ObjectStoreProvider())
                    .provider(new DatabaseProvider())
                    .provider(new CompositeProvider())
                    .build();

    private final List<AgenticCommerceWayangPersistenceTransferAuditStoreProvider> providers;

    public AgenticCommerceWayangPersistenceTransferAuditStoreProviders(
            List<AgenticCommerceWayangPersistenceTransferAuditStoreProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            this.providers = List.of();
        } else {
            this.providers = providers.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    public static AgenticCommerceWayangPersistenceTransferAuditStoreProviders defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditStoreProvider> providers() {
        return providers;
    }

    public List<String> storageKinds() {
        return providers.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditStoreProvider::storageKind)
                .toList();
    }

    public Optional<AgenticCommerceWayangPersistenceTransferAuditStoreProvider> provider(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        AgenticCommerceWayangPersistenceTransferAuditConfig resolved = Objects.requireNonNull(config, "config");
        return providers.stream()
                .filter(provider -> provider.supports(resolved))
                .findFirst();
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink build(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return build(config, null);
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink build(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver) {
        return build(config, objectStoreClientResolver, null);
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink build(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        AgenticCommerceWayangPersistenceTransferAuditConfig resolved = Objects.requireNonNull(config, "config");
        return provider(resolved)
                .orElseThrow(() -> unsupported(resolved))
                .build(new AgenticCommerceWayangPersistenceTransferAuditProviderContext(
                        resolved,
                        objectStoreClientResolver,
                        databasePersistenceClientResolver,
                        this));
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerCount", providers.size());
        values.put("storageKinds", storageKinds());
        values.put("providers", providers.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditStoreProvider::toMap)
                .toList());
        return java.util.Map.copyOf(values);
    }

    private static IllegalArgumentException unsupported(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return new IllegalArgumentException(
                "Unsupported Agentic Commerce persistence transfer audit storage kind: " + config.storageKind());
    }

    public static final class Builder {

        private final List<AgenticCommerceWayangPersistenceTransferAuditStoreProvider> providers =
                new ArrayList<>();

        public Builder provider(AgenticCommerceWayangPersistenceTransferAuditStoreProvider provider) {
            providers.add(Objects.requireNonNull(provider, "provider"));
            return this;
        }

        public Builder providers(AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
            if (providers != null) {
                this.providers.addAll(providers.providers());
            }
            return this;
        }

        public AgenticCommerceWayangPersistenceTransferAuditStoreProviders build() {
            return new AgenticCommerceWayangPersistenceTransferAuditStoreProviders(providers);
        }
    }

    private static final class NoopProvider
            implements AgenticCommerceWayangPersistenceTransferAuditStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_NOOP;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_NOOP.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceTransferAuditSink build(
                AgenticCommerceWayangPersistenceTransferAuditProviderContext context) {
            return AgenticCommerceWayangPersistenceTransferAuditSink.noop();
        }
    }

    private static final class InMemoryProvider
            implements AgenticCommerceWayangPersistenceTransferAuditStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceTransferAuditSink build(
                AgenticCommerceWayangPersistenceTransferAuditProviderContext context) {
            return new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink(context.config().maxTrails());
        }
    }

    private static final class FileProvider
            implements AgenticCommerceWayangPersistenceTransferAuditStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceTransferAuditSink build(
                AgenticCommerceWayangPersistenceTransferAuditProviderContext context) {
            return new FileSystemAgenticCommerceWayangPersistenceTransferAuditStore(
                    Path.of(context.config().journalPath()),
                    context.config().retentionPolicy());
        }
    }

    private static final class CompositeProvider
            implements AgenticCommerceWayangPersistenceTransferAuditStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_COMPOSITE;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_COMPOSITE.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceTransferAuditSink build(
                AgenticCommerceWayangPersistenceTransferAuditProviderContext context) {
            return AgenticCommerceWayangPersistenceTransferAuditSink.composite(context.config().children().stream()
                    .map(context::buildNested)
                    .toList());
        }
    }

    private static final class ObjectStoreProvider
            implements AgenticCommerceWayangPersistenceTransferAuditStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceTransferAuditSink build(
                AgenticCommerceWayangPersistenceTransferAuditProviderContext context) {
            AgenticCommerceObjectStoreConfig objectStoreConfig = context.config().objectStoreConfig();
            return ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore.configured(
                    objectStoreConfig,
                    context.resolveObjectStore(objectStoreConfig),
                    context.config().journalPath(),
                    context.config().retentionPolicy());
        }
    }

    private static final class DatabaseProvider
            implements AgenticCommerceWayangPersistenceTransferAuditStoreProvider {

        @Override
        public String storageKind() {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE;
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
            return AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE.equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceTransferAuditSink build(
                AgenticCommerceWayangPersistenceTransferAuditProviderContext context) {
            AgenticCommerceDatabasePersistenceConfig databaseConfig = context.config().databaseConfig();
            return DatabaseAgenticCommerceWayangPersistenceTransferAuditStore.configured(
                    databaseConfig,
                    context.resolveDatabase(databaseConfig),
                    context.config().journalPath(),
                    context.config().retentionPolicy());
        }
    }
}
