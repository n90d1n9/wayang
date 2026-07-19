package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Properties;

/**
 * Shared source normalization for skill store configuration parsers.
 */
final class SkillStoreConfigParsing {

    private SkillStoreConfigParsing() {
    }

    static <T> T fromSystemProperties(String prefix, Parser<T> parser) {
        return fromProperties(System.getProperties(), prefix, parser);
    }

    static <T> T fromProperties(Properties properties, String prefix, Parser<T> parser) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties), prefix, parser);
    }

    static <T> T fromMap(Map<String, ?> values, String prefix, Parser<T> parser) {
        return parser.parse(
                SkillStoreConfigValues.flattenAndNormalize(values),
                SkillStoreConfigValues.normalizePrefix(prefix));
    }

    static <T> T fromEnvironment(
            Map<String, String> environment,
            String environmentPrefix,
            String prefix,
            Parser<T> parser) {
        return fromMap(
                SkillStoreConfigValues.fromEnvironment(environment, environmentPrefix, prefix),
                prefix,
                parser);
    }

    static <T> PrimaryFallback<T> primaryFallback(
            Map<String, String> values,
            String prefix,
            SkillStoreConfigValues.ScopedValues scoped,
            String missingConfigMessage,
            Parser<T> parser) {
        SkillStoreConfigKeys.requirePrimaryFallback(scoped, missingConfigMessage);
        return new PrimaryFallback<>(
                parser.parse(values, SkillStoreConfigKeys.childPrefix(prefix, "primary")),
                parser.parse(values, SkillStoreConfigKeys.childPrefix(prefix, "fallback")));
    }

    @FunctionalInterface
    interface Parser<T> {
        T parse(Map<String, String> values, String prefix);
    }

    record PrimaryFallback<T>(T primary, T fallback) {
    }
}
