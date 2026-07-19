package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Operator-facing summary of the configured skill persistence strategy.
 */
public record SkillPersistenceStrategySummary(
        StrategyKind kind,
        List<RoleStrategy> roles,
        List<String> warnings) {

    public SkillPersistenceStrategySummary {
        kind = kind == null ? StrategyKind.MIXED : kind;
        roles = roles == null
                ? List.of()
                : roles.stream()
                        .filter(Objects::nonNull)
                        .toList();
        warnings = SkillManagementValueSupport.compactStrings(warnings);
    }

    public static SkillPersistenceStrategySummary from(SkillManagementServiceConfig config) {
        return from(SkillPersistenceContractMatrix.from(config));
    }

    public static SkillPersistenceStrategySummary from(SkillPersistenceContractMatrix matrix) {
        SkillPersistenceContractMatrix resolved = matrix == null
                ? SkillPersistenceContractMatrix.from(null)
                : matrix;
        List<RoleStrategy> roles = resolved.rows().stream()
                .map(RoleStrategy::from)
                .toList();
        return new SkillPersistenceStrategySummary(classify(roles), roles, warnings(roles));
    }

    public Optional<RoleStrategy> role(SkillPersistenceContractMatrix.Role role) {
        return roles.stream()
                .filter(strategy -> strategy.role() == role)
                .findFirst();
    }

    public List<RoleStrategy> flattenedRoles() {
        List<RoleStrategy> flattened = new ArrayList<>();
        roles.forEach(role -> flatten(role, flattened));
        return List.copyOf(flattened);
    }

    public List<RoleStrategy> enabledRoles() {
        return rolesMatching(role -> !role.disabled());
    }

    public List<RoleStrategy> durableRoles() {
        return rolesMatching(RoleStrategy::durable);
    }

    public List<RoleStrategy> ephemeralRoles() {
        return rolesMatching(RoleStrategy::ephemeral);
    }

    public List<RoleStrategy> disabledRoles() {
        return rolesMatching(RoleStrategy::disabled);
    }

    public List<RoleStrategy> customRoles() {
        return rolesMatching(RoleStrategy::custom);
    }

    public boolean fullyDurable() {
        List<RoleStrategy> enabled = enabledRoles();
        return !enabled.isEmpty() && enabled.stream().allMatch(RoleStrategy::durable);
    }

    public boolean hasEphemeralRole() {
        return roles.stream().anyMatch(RoleStrategy::ephemeral);
    }

    public boolean hasDurableFallback() {
        return roles.stream().anyMatch(RoleStrategy::durableFallback);
    }

    public boolean hasExternalProvider() {
        return roles.stream().anyMatch(RoleStrategy::external);
    }

    public boolean hasCustomProvider() {
        return roles.stream().anyMatch(RoleStrategy::custom);
    }

    public boolean hasCompositeProvider() {
        return roles.stream().anyMatch(RoleStrategy::composite);
    }

    public boolean hasMirroredProvider() {
        return roles.stream().anyMatch(RoleStrategy::mirrored);
    }

    public String kindLabel() {
        return kind.label();
    }

    private List<RoleStrategy> rolesMatching(Predicate<RoleStrategy> predicate) {
        return roles.stream()
                .filter(predicate)
                .toList();
    }

    private static void flatten(RoleStrategy role, List<RoleStrategy> flattened) {
        flattened.add(role);
        role.children().forEach(child -> flatten(child, flattened));
    }

    private static StrategyKind classify(List<RoleStrategy> roles) {
        List<RoleStrategy> enabled = roles.stream()
                .filter(role -> !role.disabled())
                .toList();
        if (enabled.isEmpty()) {
            return StrategyKind.DISABLED;
        }
        if (enabled.stream().anyMatch(RoleStrategy::custom)) {
            return StrategyKind.CUSTOM;
        }
        if (enabled.stream().anyMatch(RoleStrategy::mirrored)) {
            return StrategyKind.MIRRORED;
        }
        if (enabled.stream().anyMatch(RoleStrategy::durableFallback)) {
            return StrategyKind.HYBRID_FALLBACK;
        }
        if (enabled.stream().allMatch(RoleStrategy::ephemeral)) {
            return StrategyKind.EPHEMERAL;
        }
        if (enabled.stream().allMatch(role -> role.kind() == StrategyKind.LOCAL_FILESYSTEM)) {
            return StrategyKind.LOCAL_FILESYSTEM;
        }
        if (enabled.stream().allMatch(role -> role.kind() == StrategyKind.OBJECT_STORAGE)) {
            return StrategyKind.OBJECT_STORAGE;
        }
        if (enabled.stream().allMatch(role -> role.kind() == StrategyKind.DATABASE)) {
            return StrategyKind.DATABASE;
        }
        if (enabled.stream().allMatch(RoleStrategy::durable)) {
            return StrategyKind.MIXED_DURABLE;
        }
        return StrategyKind.MIXED;
    }

    private static List<String> warnings(List<RoleStrategy> roles) {
        List<String> warnings = new ArrayList<>();
        List<RoleStrategy> disabled = roles.stream()
                .filter(RoleStrategy::disabled)
                .toList();
        if (!disabled.isEmpty()) {
            warnings.add("Disabled skill persistence roles: " + labels(disabled));
        }

        List<RoleStrategy> ephemeral = roles.stream()
                .filter(RoleStrategy::ephemeral)
                .toList();
        if (!ephemeral.isEmpty()) {
            warnings.add("Ephemeral skill persistence roles: " + labels(ephemeral));
        }

        List<RoleStrategy> custom = roles.stream()
                .filter(RoleStrategy::custom)
                .toList();
        if (!custom.isEmpty()) {
            warnings.add("Custom skill persistence roles need an externally declared durability contract: "
                    + labels(custom));
        }

        List<RoleStrategy> compositesWithoutDurableFallback = roles.stream()
                .filter(role -> role.composite() && role.durable() && !role.durableFallback())
                .toList();
        if (!compositesWithoutDurableFallback.isEmpty()) {
            warnings.add("Composite skill persistence roles without a durable fallback branch: "
                    + labels(compositesWithoutDurableFallback));
        }
        return List.copyOf(warnings);
    }

    private static String labels(List<RoleStrategy> roles) {
        return String.join(", ", roles.stream()
                .map(RoleStrategy::roleLabel)
                .distinct()
                .toList());
    }

    public enum StrategyKind {
        DISABLED("disabled"),
        EPHEMERAL("ephemeral"),
        LOCAL_FILESYSTEM("local-filesystem"),
        OBJECT_STORAGE("object-storage"),
        DATABASE("database"),
        HYBRID_FALLBACK("hybrid-fallback"),
        MIRRORED("mirrored"),
        CUSTOM("custom"),
        MIXED_DURABLE("mixed-durable"),
        MIXED("mixed");

        private final String label;

        StrategyKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record RoleStrategy(
            SkillPersistenceContractMatrix.Role role,
            String path,
            String provider,
            SkillPersistenceContractMatrix.PersistenceClass persistenceClass,
            StrategyKind kind,
            boolean durable,
            boolean durableFallback,
            boolean external,
            boolean custom,
            boolean composite,
            boolean mirrored,
            List<String> capabilities,
            List<RoleStrategy> children) {

        public RoleStrategy {
            role = Objects.requireNonNull(role, "role");
            path = nonBlank(path, "path");
            provider = nonBlank(provider, "provider");
            persistenceClass = persistenceClass == null
                    ? SkillPersistenceContractMatrix.PersistenceClass.CUSTOM
                    : persistenceClass;
            kind = kind == null ? StrategyKind.MIXED : kind;
            capabilities = SkillManagementValueSupport.compactStrings(capabilities);
            children = children == null
                    ? List.of()
                    : children.stream()
                            .filter(Objects::nonNull)
                            .toList();
        }

        static RoleStrategy from(SkillPersistenceContractMatrix.Row row) {
            Objects.requireNonNull(row, "row");
            List<RoleStrategy> children = row.children().stream()
                    .map(RoleStrategy::from)
                    .toList();
            return new RoleStrategy(
                    row.role(),
                    row.path(),
                    row.provider(),
                    row.persistenceClass(),
                    classify(row),
                    row.durable(),
                    row.durableFallback(),
                    row.external(),
                    row.custom(),
                    row.composite(),
                    row.mirrored(),
                    row.capabilities(),
                    children);
        }

        public String roleLabel() {
            return role.label();
        }

        public String persistenceClassLabel() {
            return persistenceClass.label();
        }

        public String kindLabel() {
            return kind.label();
        }

        public boolean disabled() {
            return kind == StrategyKind.DISABLED;
        }

        public boolean ephemeral() {
            return kind == StrategyKind.EPHEMERAL || (!disabled() && !durable && !durableFallback);
        }

        public boolean supports(SkillStoreCapability capability) {
            return capability != null && capabilities.contains(capability.label());
        }

        private static StrategyKind classify(SkillPersistenceContractMatrix.Row row) {
            if (row.disabled()) {
                return StrategyKind.DISABLED;
            }
            if (row.custom()) {
                return StrategyKind.CUSTOM;
            }
            if (row.mirrored()) {
                return StrategyKind.MIRRORED;
            }
            if (row.composite()) {
                return row.durableFallback()
                        ? StrategyKind.HYBRID_FALLBACK
                        : row.durable() ? StrategyKind.MIXED_DURABLE : StrategyKind.MIXED;
            }
            return switch (row.persistenceClass()) {
                case FILESYSTEM -> StrategyKind.LOCAL_FILESYSTEM;
                case OBJECT_STORAGE -> StrategyKind.OBJECT_STORAGE;
                case DATABASE -> StrategyKind.DATABASE;
                case RUNTIME_REGISTRY, MEMORY -> StrategyKind.EPHEMERAL;
                case CUSTOM -> StrategyKind.CUSTOM;
                case DISABLED -> StrategyKind.DISABLED;
                case COMPOSED -> row.durable() ? StrategyKind.MIXED_DURABLE : StrategyKind.MIXED;
            };
        }

        private static String nonBlank(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.trim();
        }
    }
}
