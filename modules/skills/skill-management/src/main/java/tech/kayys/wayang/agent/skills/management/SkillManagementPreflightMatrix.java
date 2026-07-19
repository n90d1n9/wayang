package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Operator-facing matrix joining preflight validation buckets with persistence contracts.
 */
public record SkillManagementPreflightMatrix(
        SkillManagementDeploymentConfig config,
        SkillManagementPreflightReport validation,
        List<Row> rows) {

    public SkillManagementPreflightMatrix {
        config = SkillManagementConfigResolution.deploymentConfig(config);
        validation = SkillManagementPreflightReport.orEmpty(validation);
        rows = rows == null
                ? List.of()
                : rows.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public static SkillManagementPreflightMatrix from(SkillManagementDeploymentPreflightReport report) {
        SkillManagementDeploymentPreflightReport resolved = SkillManagementDeploymentPreflightReport.orEmpty(report);
        return from(resolved.config(), resolved.validation());
    }

    public static SkillManagementPreflightMatrix from(
            SkillManagementDeploymentConfig config,
            SkillManagementPreflightReport validation) {
        SkillManagementDeploymentConfig resolvedConfig = SkillManagementConfigResolution.deploymentConfig(config);
        SkillManagementPreflightReport resolvedValidation =
                SkillManagementPreflightReport.orEmpty(validation);
        List<Row> rows = new ArrayList<>();
        rows.add(summary(Scope.CONFIGURATION, "configuration", resolvedValidation.configurationValidation()));
        rows.add(summary(Scope.TARGET_STORE, "target-stores", resolvedValidation.targetStoreValidation()));
        targetRows(resolvedConfig).forEach(row -> rows.add(contract(Scope.TARGET_STORE, "target.", false, row)));
        rows.add(summary(Scope.SOURCE_STORE, "source-stores", resolvedValidation.sourceStoreValidation()));
        sourceRows(resolvedConfig).forEach(row -> rows.add(contract(Scope.SOURCE_STORE, "source.", true, row)));
        rows.add(summary(Scope.CAPABILITY, "capabilities", resolvedValidation.capabilityValidation()));
        rows.add(eventPruneCapabilityRow(resolvedConfig, resolvedValidation.capabilityValidation()));
        return new SkillManagementPreflightMatrix(resolvedConfig, resolvedValidation, rows);
    }

    public boolean ready() {
        return validation.ready();
    }

    public List<Row> failedRows() {
        return rows.stream()
                .filter(row -> !row.valid())
                .toList();
    }

    public List<Row> rows(Scope scope) {
        return rows.stream()
                .filter(row -> row.scope() == scope)
                .toList();
    }

    public List<String> errors() {
        return validation.errors();
    }

    private static List<SkillPersistenceContractMatrix.Row> targetRows(
            SkillManagementDeploymentConfig config) {
        return SkillPersistenceContractMatrix.from(config.serviceConfig()).flattenedRows();
    }

    private static List<SkillPersistenceContractMatrix.Row> sourceRows(
            SkillManagementDeploymentConfig config) {
        SkillManagementMaintenanceSourceConfig source = config.maintenanceSource();
        List<SkillPersistenceContractMatrix.Row> rows = new ArrayList<>();
        if (source.hasDefinitionStore()) {
            rows.addAll(SkillPersistenceContractMatrix.from(SkillManagementServiceConfig.of(
                            source.definitionStore(),
                            SkillLifecycleStateStoreConfig.memory(),
                            SkillManagementEventStoreConfig.none(),
                            SkillArtifactStoreConfig.memory(),
                            SkillLifecycleStateReconcileOptions.inspectOnly()))
                    .row(SkillPersistenceContractMatrix.Role.DEFINITION)
                    .stream()
                    .flatMap(row -> flatten(row).stream())
                    .toList());
        }
        if (source.hasArtifactStore()) {
            rows.addAll(SkillPersistenceContractMatrix.from(SkillManagementServiceConfig.of(
                            SkillDefinitionStoreConfig.registry(),
                            SkillLifecycleStateStoreConfig.memory(),
                            SkillManagementEventStoreConfig.none(),
                            source.artifactStore(),
                            SkillLifecycleStateReconcileOptions.inspectOnly()))
                    .row(SkillPersistenceContractMatrix.Role.ARTIFACT)
                    .stream()
                    .flatMap(row -> flatten(row).stream())
                    .toList());
        }
        return List.copyOf(rows);
    }

    private static List<SkillPersistenceContractMatrix.Row> flatten(SkillPersistenceContractMatrix.Row row) {
        List<SkillPersistenceContractMatrix.Row> rows = new ArrayList<>();
        flatten(row, rows);
        return rows;
    }

    private static void flatten(
            SkillPersistenceContractMatrix.Row row,
            List<SkillPersistenceContractMatrix.Row> rows) {
        rows.add(row);
        row.children().forEach(child -> flatten(child, rows));
    }

    private static Row summary(
            Scope scope,
            String path,
            SkillStoreConfigValidationResult validation) {
        SkillStoreConfigValidationResult resolved =
                validation == null ? SkillStoreConfigValidationResult.valid() : validation;
        return new Row(
                scope,
                path,
                "validation",
                "validation",
                false,
                resolved.validConfiguration(),
                resolved.errors(),
                List.of(),
                false,
                false,
                false,
                false,
                false,
                false);
    }

    private static Row contract(
            Scope scope,
            String prefix,
            boolean required,
            SkillPersistenceContractMatrix.Row row) {
        return new Row(
                scope,
                prefix + row.path(),
                row.provider(),
                row.persistenceClassLabel(),
                required,
                true,
                List.of(),
                row.capabilities(),
                row.durable(),
                row.durableFallback(),
                row.external(),
                row.custom(),
                row.composite(),
                row.mirrored());
    }

    private static Row eventPruneCapabilityRow(
            SkillManagementDeploymentConfig config,
            SkillStoreConfigValidationResult capabilityValidation) {
        SkillPersistenceContractMatrix.Row eventStore = SkillPersistenceContractMatrix.from(config.serviceConfig())
                .row(SkillPersistenceContractMatrix.Role.EVENT_HISTORY)
                .orElseThrow();
        SkillStoreConfigValidationResult resolvedValidation =
                capabilityValidation == null ? SkillStoreConfigValidationResult.valid() : capabilityValidation;
        boolean required = config.maintenancePlan().eventPrunePolicy().enabled();
        return new Row(
                Scope.CAPABILITY,
                "capability.event-pruning",
                eventStore.provider(),
                eventStore.persistenceClassLabel(),
                required,
                !required || resolvedValidation.validConfiguration(),
                required ? resolvedValidation.errors() : List.of(),
                eventStore.capabilities(),
                eventStore.durable(),
                eventStore.durableFallback(),
                eventStore.external(),
                eventStore.custom(),
                eventStore.composite(),
                eventStore.mirrored());
    }

    public enum Scope {
        CONFIGURATION("configuration"),
        TARGET_STORE("target-store"),
        SOURCE_STORE("source-store"),
        CAPABILITY("capability");

        private final String label;

        Scope(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Row(
            Scope scope,
            String path,
            String provider,
            String persistenceClass,
            boolean required,
            boolean valid,
            List<String> errors,
            List<String> capabilities,
            boolean durable,
            boolean durableFallback,
            boolean external,
            boolean custom,
            boolean composite,
            boolean mirrored) {

        public Row {
            scope = Objects.requireNonNull(scope, "scope");
            path = nonBlank(path, "path");
            provider = nonBlank(provider, "provider");
            persistenceClass = nonBlank(persistenceClass, "persistenceClass");
            errors = SkillManagementValueSupport.compactStrings(errors);
            capabilities = SkillManagementValueSupport.compactStrings(capabilities);
        }

        public String scopeLabel() {
            return scope.label();
        }

        private static String nonBlank(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.toLowerCase(Locale.ROOT);
        }
    }
}
