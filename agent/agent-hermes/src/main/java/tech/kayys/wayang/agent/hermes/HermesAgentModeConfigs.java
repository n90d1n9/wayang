package tech.kayys.wayang.agent.hermes;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Runtime configuration parser for Hermes mode.
 *
 * <p>Supported property prefix: {@code wayang.agent.hermes.}. Environment
 * variables can use {@code WAYANG_AGENT_HERMES_}. When MicroProfile Config is
 * present, it is used as the runtime source so Quarkus config priorities are
 * preserved without making this module directly depend on Quarkus.</p>
 */
public final class HermesAgentModeConfigs {

    public static final String PROPERTY_PREFIX = "wayang.agent.hermes.";
    public static final String ENVIRONMENT_PREFIX = "WAYANG_AGENT_HERMES_";

    private static final List<HermesConfigSection> CONFIG_SECTIONS = List.of(
            HermesAgentModeCoreConfigSection.INSTANCE,
            HermesRuntimeEventJournalConfigSection.INSTANCE,
            HermesSkillLineageRepairConfigSection.INSTANCE,
            HermesPersistenceHintsConfigSection.withPrefix(PROPERTY_PREFIX));

    private HermesAgentModeConfigs() {
    }

    public static HermesAgentModeConfig fromRuntime() {
        return microProfileConfigValues()
                .map(HermesAgentModeConfigs::fromMap)
                .orElseGet(() -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.putAll(environmentValues(System.getenv()));
                    values.putAll(propertiesValues(System.getProperties()));
                    return fromMap(values);
                });
    }

    public static HermesAgentModeConfig fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static HermesAgentModeConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static HermesAgentModeConfig fromProperties(Properties properties) {
        return fromMap(propertiesValues(properties));
    }

    public static HermesAgentModeConfig fromEnvironment(Map<String, String> environment) {
        return fromMap(environmentValues(environment));
    }

    public static HermesAgentModeConfig fromMap(Map<String, ?> values) {
        HermesConfigValues scoped = HermesConfigValues.from(values, PROPERTY_PREFIX);
        HermesAgentModeConfig.Builder builder = HermesAgentModeConfig.builder();

        CONFIG_SECTIONS.forEach(section -> section.apply(scoped, builder));

        return builder.build();
    }

    private static Map<String, Object> propertiesValues(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        Map<String, Object> values = new LinkedHashMap<>();
        properties.stringPropertyNames()
                .forEach(key -> values.put(key, properties.getProperty(key)));
        return values;
    }

    private static Map<String, Object> environmentValues(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        Map<String, Object> values = new LinkedHashMap<>();
        environment.forEach((key, value) -> {
            values.put(key, value);
            if (key.startsWith(ENVIRONMENT_PREFIX)) {
                String suffix = key.substring(ENVIRONMENT_PREFIX.length())
                        .toLowerCase(Locale.ROOT)
                        .replace('_', '.');
                values.put(PROPERTY_PREFIX + suffix, value);
            }
        });
        return values;
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
                if (!(nameValue instanceof String name) || !isHermesProperty(name)) {
                    continue;
                }
                Object optional = optionalValueMethod.invoke(config, name, String.class);
                if (optional instanceof Optional<?> value && value.isPresent()) {
                    Object resolved = value.orElseThrow();
                    if (name.startsWith(ENVIRONMENT_PREFIX)) {
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

    private static boolean isHermesProperty(String name) {
        return HermesConfigValues.normalizeKey(name).startsWith(HermesConfigValues.normalizeKey(PROPERTY_PREFIX))
                || name.startsWith(ENVIRONMENT_PREFIX);
    }
}
