package tech.kayys.wayang.rag.runtime;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class RagPluginSelectionConfig {

    static final String WILDCARD_PLUGIN_ID = "*";

    private RagPluginSelectionConfig() {
    }

    static String normalizeTenant(String tenantId) {
        return RagRuntimeText.trimToEmpty(tenantId);
    }

    static String normalizeStrategyId(String strategyId) {
        return RagRuntimeText.trimToLowerEmpty(strategyId);
    }

    static Map<String, String> parseTenantOverrides(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String token : raw.split(";")) {
            String entry = RagRuntimeText.trimToEmpty(token);
            if (entry.isEmpty()) {
                continue;
            }
            int delimiter = entry.indexOf('=');
            if (delimiter <= 0 || delimiter == entry.length() - 1) {
                continue;
            }
            String tenant = normalizeTenant(entry.substring(0, delimiter));
            String value = RagRuntimeText.trimToEmpty(entry.substring(delimiter + 1));
            if (!tenant.isEmpty() && !value.isEmpty()) {
                parsed.put(tenant, value);
            }
        }
        return RagRuntimeMetadata.copyStrings(parsed);
    }

    static Set<String> parseEnabledPluginIds(String raw) {
        Set<String> parsed = parsePluginIdSet(raw);
        if (parsed.isEmpty()) {
            return Set.of(WILDCARD_PLUGIN_ID);
        }
        return parsed;
    }

    static List<String> parsePluginOrder(String raw) {
        return normalizedPluginTokens(raw);
    }

    static boolean allPluginsEnabled(Set<String> enabledPluginIds) {
        return enabledPluginIds == null
                || enabledPluginIds.isEmpty()
                || enabledPluginIds.contains(WILDCARD_PLUGIN_ID);
    }

    private static Set<String> parsePluginIdSet(String raw) {
        return normalizedPluginTokens(raw).stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<String> normalizedPluginTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return raw.lines()
                .flatMap(line -> Arrays.stream(line.split(",")))
                .map(RagRuntimeText::trimToEmpty)
                .filter(token -> !token.isEmpty())
                .map(RagPluginCatalog::normalizeId)
                .toList();
    }
}
