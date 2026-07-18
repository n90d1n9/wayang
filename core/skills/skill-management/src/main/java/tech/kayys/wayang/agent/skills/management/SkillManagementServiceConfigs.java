package tech.kayys.wayang.agent.skills.management;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Parses deployable service-level skill-management persistence configuration.
 */
public final class SkillManagementServiceConfigs {

    private SkillManagementServiceConfigs() {
    }

    public static SkillManagementServiceConfig fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static SkillManagementServiceConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillManagementServiceConfig fromRuntime() {
        return microProfileConfigValues()
                .map(SkillManagementServiceConfigs::fromMap)
                .orElseGet(() -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.putAll(environmentValues(System.getenv()));
                    values.putAll(SkillStoreConfigValues.fromProperties(System.getProperties()));
                    return fromMap(values);
                });
    }

    public static SkillManagementServiceConfig fromProperties(Properties properties) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties));
    }

    public static SkillStoreConfigValidationResult validateProperties(Properties properties) {
        return validate(() -> fromProperties(properties));
    }

    public static SkillManagementServiceConfig fromMap(Map<String, ?> values) {
        Map<String, String> normalized = SkillStoreConfigValues.flattenAndNormalize(values);
        if (SkillManagementServiceProfiles.hasProfile(normalized)) {
            return fromProfiledMap(normalized);
        }
        return SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfigs.fromMap(values),
                SkillLifecycleStateStoreConfigs.fromMap(values),
                SkillManagementEventStoreConfigs.fromMap(values),
                SkillArtifactStoreConfigs.fromMap(values),
                SkillLifecycleStateReconcileConfigs.fromMap(values));
    }

    public static SkillStoreConfigValidationResult validateMap(Map<String, ?> values) {
        return validate(() -> fromMap(values));
    }

    public static SkillManagementServiceConfig fromEnvironment(Map<String, String> environment) {
        return fromMap(environmentValues(environment));
    }

    public static SkillStoreConfigValidationResult validateEnvironment(Map<String, String> environment) {
        return validate(() -> fromEnvironment(environment));
    }

    private static SkillStoreConfigValidationResult validate(Supplier<SkillManagementServiceConfig> parser) {
        try {
            return parser.get().validate();
        } catch (IllegalArgumentException error) {
            return SkillStoreConfigValidationResult.error(error.getMessage());
        }
    }

    private static Map<String, Object> environmentValues(Map<String, String> environment) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.putAll(SkillStoreConfigValues.fromEnvironment(
                environment,
                SkillDefinitionStoreConfigs.ENV_PREFIX,
                SkillDefinitionStoreConfigs.PREFIX));
        values.putAll(SkillStoreConfigValues.fromEnvironment(
                environment,
                SkillLifecycleStateStoreConfigs.ENV_PREFIX,
                SkillLifecycleStateStoreConfigs.PREFIX));
        values.putAll(SkillStoreConfigValues.fromEnvironment(
                environment,
                SkillManagementEventStoreConfigs.ENV_PREFIX,
                SkillManagementEventStoreConfigs.PREFIX));
        values.putAll(SkillStoreConfigValues.fromEnvironment(
                environment,
                SkillArtifactStoreConfigs.ENV_PREFIX,
                SkillArtifactStoreConfigs.PREFIX));
        values.putAll(SkillStoreConfigValues.fromEnvironment(
                environment,
                SkillLifecycleStateReconcileConfigs.ENV_PREFIX,
                SkillLifecycleStateReconcileConfigs.PREFIX));
        values.putAll(SkillManagementServiceProfiles.environmentValues(environment));
        return values;
    }

    private static SkillManagementServiceConfig fromProfiledMap(Map<String, String> values) {
        SkillManagementServiceConfig profiled = SkillManagementServiceProfiles.fromNormalizedMap(values);
        return SkillManagementServiceConfig.of(
                hasStoreSelector(values, SkillDefinitionStoreConfigs.PREFIX)
                        ? SkillDefinitionStoreConfigs.fromMap(values)
                        : profiled.definitionStore(),
                hasStoreSelector(values, SkillLifecycleStateStoreConfigs.PREFIX)
                        ? SkillLifecycleStateStoreConfigs.fromMap(values)
                        : profiled.lifecycleStateStore(),
                hasStoreSelector(values, SkillManagementEventStoreConfigs.PREFIX)
                        ? SkillManagementEventStoreConfigs.fromMap(values)
                        : profiled.eventStore(),
                hasStoreSelector(values, SkillArtifactStoreConfigs.PREFIX)
                        ? SkillArtifactStoreConfigs.fromMap(values)
                        : profiled.artifactStore(),
                hasConfiguredPrefix(values, SkillLifecycleStateReconcileConfigs.PREFIX)
                        ? SkillLifecycleStateReconcileConfigs.fromNormalizedMap(
                                values,
                                SkillStoreConfigValues.normalizePrefix(SkillLifecycleStateReconcileConfigs.PREFIX),
                                profiled.lifecycleStateReconcileOptions())
                        : profiled.lifecycleStateReconcileOptions());
    }

    private static boolean hasStoreSelector(Map<String, String> values, String prefix) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        return scoped.get("kind", "type", "backend").isPresent();
    }

    private static boolean hasConfiguredPrefix(Map<String, String> values, String prefix) {
        String normalizedPrefix = SkillStoreConfigValues.normalize(SkillStoreConfigValues.normalizePrefix(prefix));
        return values.keySet().stream().anyMatch(key -> key.startsWith(normalizedPrefix));
    }

    private static Optional<Map<String, Object>> microProfileConfigValues() {
        try {
            Class<?> providerClass = Class.forName("org.eclipse.microprofile.config.ConfigProvider");
            Object config = providerClass.getMethod("getConfig").invoke(null);
            Method propertyNamesMethod = config.getClass().getMethod("getPropertyNames");
            Method optionalValueMethod = config.getClass().getMethod("getOptionalValue", String.class, Class.class);
            Object propertyNames = propertyNamesMethod.invoke(config);
            if (!(propertyNames instanceof Iterable<?> names)) {
                return Optional.empty();
            }

            Map<String, Object> values = new LinkedHashMap<>();
            for (Object nameValue : names) {
                if (!(nameValue instanceof String name) || !isSkillManagementProperty(name)) {
                    continue;
                }
                Object optional = optionalValueMethod.invoke(config, name, String.class);
                if (optional instanceof Optional<?> value && value.isPresent()) {
                    Object resolved = value.orElseThrow();
                    if (isSkillManagementEnvironmentName(name)) {
                        values.putAll(environmentValues(Map.of(name, String.valueOf(resolved))));
                    } else {
                        values.put(name, resolved);
                    }
                }
            }
            return values.isEmpty() ? Optional.empty() : Optional.of(values);
        } catch (ReflectiveOperationException | RuntimeException error) {
            return Optional.empty();
        }
    }

    private static boolean isSkillManagementProperty(String name) {
        String normalized = SkillStoreConfigValues.normalize(name);
        return List.of(
                        SkillDefinitionStoreConfigs.PREFIX,
                        SkillLifecycleStateStoreConfigs.PREFIX,
                        SkillManagementEventStoreConfigs.PREFIX,
                        SkillArtifactStoreConfigs.PREFIX,
                        SkillLifecycleStateReconcileConfigs.PREFIX,
                        SkillManagementServiceProfiles.PROFILE_PREFIX,
                        SkillManagementServiceProfileOptions.PREFIX)
                .stream()
                .map(SkillStoreConfigValues::normalize)
                .anyMatch(normalized::startsWith)
                || isSkillManagementEnvironmentName(name);
    }

    private static boolean isSkillManagementEnvironmentName(String name) {
        return name.startsWith(SkillDefinitionStoreConfigs.ENV_PREFIX)
                || name.startsWith(SkillLifecycleStateStoreConfigs.ENV_PREFIX)
                || name.startsWith(SkillManagementEventStoreConfigs.ENV_PREFIX)
                || name.startsWith(SkillArtifactStoreConfigs.ENV_PREFIX)
                || name.startsWith(SkillLifecycleStateReconcileConfigs.ENV_PREFIX)
                || name.startsWith(SkillManagementServiceProfileOptions.ENV_PREFIX)
                || name.equals(SkillManagementServiceProfiles.PROFILE_ENV)
                || name.equals(SkillManagementServiceProfiles.SERVICE_PROFILE_ENV)
                || name.equals(SkillManagementServiceProfiles.PERSISTENCE_PROFILE_ENV);
    }
}
