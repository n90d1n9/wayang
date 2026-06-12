package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Config-derived persistence contract matrix for skill-management stores.
 */
public record SkillPersistenceContractMatrix(List<Row> rows) {

    public SkillPersistenceContractMatrix {
        rows = rows == null
                ? List.of()
                : rows.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public static SkillPersistenceContractMatrix from(SkillManagementServiceConfig config) {
        SkillManagementServiceConfig resolved = SkillManagementConfigResolution.serviceConfig(config);
        return new SkillPersistenceContractMatrix(List.of(
                definition(Role.DEFINITION, Role.DEFINITION.label(), resolved.definitionStore()),
                lifecycle(Role.LIFECYCLE_STATE, Role.LIFECYCLE_STATE.label(), resolved.lifecycleStateStore()),
                event(Role.EVENT_HISTORY, Role.EVENT_HISTORY.label(), resolved.eventStore()),
                artifact(Role.ARTIFACT, Role.ARTIFACT.label(), resolved.artifactStore())));
    }

    public SkillPersistenceStrategySummary strategy() {
        return SkillPersistenceStrategySummary.from(this);
    }

    public Optional<Row> row(Role role) {
        return rows.stream()
                .filter(row -> row.role() == role)
                .findFirst();
    }

    public List<Row> flattenedRows() {
        List<Row> flattened = new ArrayList<>();
        rows.forEach(row -> flatten(row, flattened));
        return List.copyOf(flattened);
    }

    public List<Row> durableRows() {
        return flattenedRows().stream()
                .filter(Row::durable)
                .toList();
    }

    public List<Row> customRows() {
        return flattenedRows().stream()
                .filter(Row::custom)
                .toList();
    }

    public List<Row> ephemeralRows() {
        return flattenedRows().stream()
                .filter(Row::ephemeral)
                .toList();
    }

    private static void flatten(Row row, List<Row> flattened) {
        flattened.add(row);
        row.children().forEach(child -> flatten(child, flattened));
    }

    private static Row definition(
            Role role,
            String path,
            SkillDefinitionStoreConfig config) {
        SkillDefinitionStoreConfig resolved = config == null ? SkillDefinitionStoreConfig.registry() : config;
        return switch (resolved.kind()) {
            case REGISTRY -> leaf(
                    role,
                    path,
                    "registry",
                    PersistenceClass.RUNTIME_REGISTRY,
                    false,
                    false,
                    false,
                    false,
                    definitionCapabilities());
            case FILESYSTEM -> leaf(
                    role,
                    path,
                    "filesystem",
                    PersistenceClass.FILESYSTEM,
                    true,
                    false,
                    false,
                    false,
                    definitionCapabilities());
            case OBJECT_STORAGE -> leaf(
                    role,
                    path,
                    "object-storage",
                    PersistenceClass.OBJECT_STORAGE,
                    true,
                    true,
                    false,
                    false,
                    definitionCapabilities());
            case JDBC -> leaf(
                    role,
                    path,
                    "jdbc",
                    PersistenceClass.DATABASE,
                    true,
                    true,
                    false,
                    false,
                    definitionCapabilities(SkillStoreCapability.TRANSACTIONAL));
            case CUSTOM -> leaf(
                    role,
                    path,
                    "custom",
                    PersistenceClass.CUSTOM,
                    false,
                    false,
                    true,
                    false,
                    definitionCapabilities());
            case HYBRID -> composed(
                    role,
                    path,
                    "hybrid",
                    false,
                    definition(role, childPath(path, "primary"), resolved.primary()),
                    definition(role, childPath(path, "fallback"), resolved.fallback()),
                    definitionCapabilities(SkillStoreCapability.PRIMARY_FALLBACK));
            case MIRRORED -> composed(
                    role,
                    path,
                    "mirrored",
                    true,
                    definition(role, childPath(path, "primary"), resolved.primary()),
                    definition(role, childPath(path, "fallback"), resolved.fallback()),
                    definitionCapabilities(
                            SkillStoreCapability.PRIMARY_FALLBACK,
                            SkillStoreCapability.MIRROR_WRITE));
        };
    }

    private static Row lifecycle(
            Role role,
            String path,
            SkillLifecycleStateStoreConfig config) {
        SkillLifecycleStateStoreConfig resolved = config == null ? SkillLifecycleStateStoreConfig.memory() : config;
        return switch (resolved.kind()) {
            case MEMORY -> leaf(
                    role,
                    path,
                    "memory",
                    PersistenceClass.MEMORY,
                    false,
                    false,
                    false,
                    false,
                    stateStoreCapabilities());
            case FILESYSTEM -> leaf(
                    role,
                    path,
                    "filesystem",
                    PersistenceClass.FILESYSTEM,
                    true,
                    false,
                    false,
                    false,
                    stateStoreCapabilities());
            case OBJECT_STORAGE -> leaf(
                    role,
                    path,
                    "object-storage",
                    PersistenceClass.OBJECT_STORAGE,
                    true,
                    true,
                    false,
                    false,
                    stateStoreCapabilities());
            case JDBC -> leaf(
                    role,
                    path,
                    "jdbc",
                    PersistenceClass.DATABASE,
                    true,
                    true,
                    false,
                    false,
                    stateStoreCapabilities(SkillStoreCapability.TRANSACTIONAL));
            case CUSTOM -> leaf(
                    role,
                    path,
                    "custom",
                    PersistenceClass.CUSTOM,
                    false,
                    false,
                    true,
                    false,
                    stateStoreCapabilities());
            case HYBRID -> composed(
                    role,
                    path,
                    "hybrid",
                    false,
                    lifecycle(role, childPath(path, "primary"), resolved.primary()),
                    lifecycle(role, childPath(path, "fallback"), resolved.fallback()),
                    stateStoreCapabilities(SkillStoreCapability.PRIMARY_FALLBACK));
            case MIRRORED -> composed(
                    role,
                    path,
                    "mirrored",
                    true,
                    lifecycle(role, childPath(path, "primary"), resolved.primary()),
                    lifecycle(role, childPath(path, "fallback"), resolved.fallback()),
                    stateStoreCapabilities(
                            SkillStoreCapability.PRIMARY_FALLBACK,
                            SkillStoreCapability.MIRROR_WRITE));
        };
    }

    private static Row artifact(
            Role role,
            String path,
            SkillArtifactStoreConfig config) {
        SkillArtifactStoreConfig resolved = config == null ? SkillArtifactStoreConfig.memory() : config;
        return switch (resolved.kind()) {
            case MEMORY -> leaf(
                    role,
                    path,
                    "memory",
                    PersistenceClass.MEMORY,
                    false,
                    false,
                    false,
                    false,
                    stateStoreCapabilities());
            case FILESYSTEM -> leaf(
                    role,
                    path,
                    "filesystem",
                    PersistenceClass.FILESYSTEM,
                    true,
                    false,
                    false,
                    false,
                    stateStoreCapabilities());
            case OBJECT_STORAGE -> leaf(
                    role,
                    path,
                    "object-storage",
                    PersistenceClass.OBJECT_STORAGE,
                    true,
                    true,
                    false,
                    false,
                    stateStoreCapabilities());
            case JDBC -> leaf(
                    role,
                    path,
                    "jdbc",
                    PersistenceClass.DATABASE,
                    true,
                    true,
                    false,
                    false,
                    stateStoreCapabilities(SkillStoreCapability.TRANSACTIONAL));
            case CUSTOM -> leaf(
                    role,
                    path,
                    "custom",
                    PersistenceClass.CUSTOM,
                    false,
                    false,
                    true,
                    false,
                    stateStoreCapabilities());
            case HYBRID -> composed(
                    role,
                    path,
                    "hybrid",
                    false,
                    artifact(role, childPath(path, "primary"), resolved.primary()),
                    artifact(role, childPath(path, "fallback"), resolved.fallback()),
                    stateStoreCapabilities(SkillStoreCapability.PRIMARY_FALLBACK));
            case MIRRORED -> composed(
                    role,
                    path,
                    "mirrored",
                    true,
                    artifact(role, childPath(path, "primary"), resolved.primary()),
                    artifact(role, childPath(path, "fallback"), resolved.fallback()),
                    stateStoreCapabilities(
                            SkillStoreCapability.PRIMARY_FALLBACK,
                            SkillStoreCapability.MIRROR_WRITE));
        };
    }

    private static Row event(
            Role role,
            String path,
            SkillManagementEventStoreConfig config) {
        SkillManagementEventStoreConfig resolved = config == null ? SkillManagementEventStoreConfig.none() : config;
        return switch (resolved.kind()) {
            case NONE -> leaf(
                    role,
                    path,
                    "none",
                    PersistenceClass.DISABLED,
                    false,
                    false,
                    false,
                    false,
                    List.of());
            case MEMORY -> leaf(
                    role,
                    path,
                    "memory",
                    PersistenceClass.MEMORY,
                    false,
                    false,
                    false,
                    false,
                    eventStoreCapabilities());
            case FILESYSTEM -> leaf(
                    role,
                    path,
                    "filesystem",
                    PersistenceClass.FILESYSTEM,
                    true,
                    false,
                    false,
                    false,
                    eventStoreCapabilities());
            case OBJECT_STORAGE -> leaf(
                    role,
                    path,
                    "object-storage",
                    PersistenceClass.OBJECT_STORAGE,
                    true,
                    true,
                    false,
                    false,
                    eventStoreCapabilities());
            case JDBC -> leaf(
                    role,
                    path,
                    "jdbc",
                    PersistenceClass.DATABASE,
                    true,
                    true,
                    false,
                    false,
                    eventStoreCapabilities(SkillStoreCapability.TRANSACTIONAL));
            case CUSTOM -> leaf(
                    role,
                    path,
                    "custom",
                    PersistenceClass.CUSTOM,
                    false,
                    false,
                    true,
                    false,
                    capabilities(SkillStoreCapability.WRITE));
            case HYBRID -> eventComposed(
                    role,
                    path,
                    "hybrid",
                    false,
                    event(role, childPath(path, "primary"), resolved.primary()),
                    event(role, childPath(path, "fallback"), resolved.fallback()));
            case MIRRORED -> eventComposed(
                    role,
                    path,
                    "mirrored",
                    true,
                    event(role, childPath(path, "primary"), resolved.primary()),
                    event(role, childPath(path, "fallback"), resolved.fallback()));
        };
    }

    private static Row leaf(
            Role role,
            String path,
            String provider,
            PersistenceClass persistenceClass,
            boolean durable,
            boolean external,
            boolean custom,
            boolean mirrored,
            List<String> capabilities) {
        return new Row(
                role,
                path,
                provider,
                persistenceClass,
                durable,
                false,
                external,
                custom,
                false,
                mirrored,
                capabilities,
                List.of());
    }

    private static Row composed(
            Role role,
            String path,
            String provider,
            boolean mirrored,
            Row primary,
            Row fallback,
            List<String> capabilities) {
        return new Row(
                role,
                path,
                provider,
                PersistenceClass.COMPOSED,
                mirrored ? primary.durable() || fallback.durable() : primary.durable(),
                fallback.durable() || fallback.durableFallback(),
                primary.external() || fallback.external(),
                primary.custom() || fallback.custom(),
                true,
                mirrored,
                capabilities,
                List.of(primary, fallback));
    }

    private static Row eventComposed(
            Role role,
            String path,
            String provider,
            boolean mirrored,
            Row primary,
            Row fallback) {
        EnumSet<SkillStoreCapability> capabilities = EnumSet.of(SkillStoreCapability.WRITE);
        if (mirrored) {
            capabilities.add(SkillStoreCapability.PRIMARY_FALLBACK);
            capabilities.add(SkillStoreCapability.MIRROR_WRITE);
        } else {
            capabilities.add(SkillStoreCapability.COMPOSITE);
        }
        if (primary.queryable() || fallback.queryable()) {
            capabilities.add(SkillStoreCapability.QUERY_EVENTS);
        }
        if (primary.prunable() && fallback.prunable()) {
            capabilities.add(SkillStoreCapability.PRUNE_EVENTS);
        }
        return composed(
                role,
                path,
                provider,
                mirrored,
                primary,
                fallback,
                capabilities(capabilities));
    }

    private static List<String> definitionCapabilities(SkillStoreCapability... extra) {
        return capabilities(
                SkillStoreCapability.READ,
                SkillStoreCapability.WRITE,
                SkillStoreCapability.DELETE,
                SkillStoreCapability.LIST,
                extra);
    }

    private static List<String> stateStoreCapabilities(SkillStoreCapability... extra) {
        return capabilities(
                SkillStoreCapability.READ,
                SkillStoreCapability.WRITE,
                SkillStoreCapability.DELETE,
                SkillStoreCapability.LIST,
                extra);
    }

    private static List<String> eventStoreCapabilities(SkillStoreCapability... extra) {
        return capabilities(
                SkillStoreCapability.WRITE,
                SkillStoreCapability.QUERY_EVENTS,
                SkillStoreCapability.PRUNE_EVENTS,
                extra);
    }

    private static List<String> capabilities(
            SkillStoreCapability first,
            SkillStoreCapability second,
            SkillStoreCapability third,
            SkillStoreCapability fourth,
            SkillStoreCapability... extra) {
        EnumSet<SkillStoreCapability> capabilities = EnumSet.of(first, second, third, fourth);
        addAll(capabilities, extra);
        return capabilities(capabilities);
    }

    private static List<String> capabilities(
            SkillStoreCapability first,
            SkillStoreCapability second,
            SkillStoreCapability third,
            SkillStoreCapability... extra) {
        EnumSet<SkillStoreCapability> capabilities = EnumSet.of(first, second, third);
        addAll(capabilities, extra);
        return capabilities(capabilities);
    }

    private static List<String> capabilities(SkillStoreCapability capability) {
        return capabilities(EnumSet.of(capability));
    }

    private static List<String> capabilities(EnumSet<SkillStoreCapability> capabilities) {
        return SkillStoreCapabilities.of(capabilities.toArray(SkillStoreCapability[]::new)).names();
    }

    private static void addAll(
            EnumSet<SkillStoreCapability> capabilities,
            SkillStoreCapability... extra) {
        if (extra == null) {
            return;
        }
        for (SkillStoreCapability capability : extra) {
            if (capability != null) {
                capabilities.add(capability);
            }
        }
    }

    private static String childPath(String path, String child) {
        return path + "." + child;
    }

    public enum Role {
        DEFINITION("definition"),
        LIFECYCLE_STATE("lifecycle-state"),
        EVENT_HISTORY("event-history"),
        ARTIFACT("artifact");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum PersistenceClass {
        DISABLED("disabled"),
        RUNTIME_REGISTRY("runtime-registry"),
        MEMORY("memory"),
        FILESYSTEM("filesystem"),
        OBJECT_STORAGE("object-storage"),
        DATABASE("database"),
        CUSTOM("custom"),
        COMPOSED("composed");

        private final String label;

        PersistenceClass(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Row(
            Role role,
            String path,
            String provider,
            PersistenceClass persistenceClass,
            boolean durable,
            boolean durableFallback,
            boolean external,
            boolean custom,
            boolean composite,
            boolean mirrored,
            List<String> capabilities,
            List<Row> children) {

        public Row {
            role = Objects.requireNonNull(role, "role");
            path = nonBlank(path, "path");
            provider = nonBlank(provider, "provider");
            persistenceClass = persistenceClass == null ? PersistenceClass.CUSTOM : persistenceClass;
            capabilities = SkillManagementValueSupport.compactStrings(capabilities);
            children = children == null
                    ? List.of()
                    : children.stream()
                            .filter(Objects::nonNull)
                            .toList();
        }

        public String roleLabel() {
            return role.label();
        }

        public String persistenceClassLabel() {
            return persistenceClass.label();
        }

        public boolean disabled() {
            return persistenceClass == PersistenceClass.DISABLED;
        }

        public boolean ephemeral() {
            return !disabled() && !durable && !durableFallback;
        }

        public boolean readable() {
            return supports(SkillStoreCapability.READ);
        }

        public boolean writable() {
            return supports(SkillStoreCapability.WRITE);
        }

        public boolean deletable() {
            return supports(SkillStoreCapability.DELETE);
        }

        public boolean listable() {
            return supports(SkillStoreCapability.LIST);
        }

        public boolean queryable() {
            return supports(SkillStoreCapability.QUERY_EVENTS);
        }

        public boolean prunable() {
            return supports(SkillStoreCapability.PRUNE_EVENTS);
        }

        public boolean transactional() {
            return supports(SkillStoreCapability.TRANSACTIONAL);
        }

        public boolean supports(SkillStoreCapability capability) {
            return capability != null && capabilities.contains(capability.label());
        }

        private static String nonBlank(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.toLowerCase(Locale.ROOT);
        }
    }
}
