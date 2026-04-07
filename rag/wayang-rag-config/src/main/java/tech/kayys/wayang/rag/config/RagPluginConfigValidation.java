package tech.kayys.wayang.rag.config;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class RagPluginConfigValidation {

    private RagPluginConfigValidation() {
    }

    static void validateTenantOverrideSyntax(
            String fieldName,
            String raw,
            boolean allowWildcard,
            ViolationFactory violationFactory) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        Set<String> seenTenants = new HashSet<>();
        for (String token : raw.split(";")) {
            String entry = token == null ? "" : token.trim();
            if (entry.isEmpty()) {
                continue;
            }

            int delimiter = entry.indexOf('=');
            if (delimiter <= 0 || delimiter >= entry.length() - 1) {
                throw violationFactory.create(
                        "invalid_entry",
                        fieldName,
                        null,
                        entry,
                        "Expected format `tenant=value`.");
            }

            String tenant = entry.substring(0, delimiter).trim();
            String value = entry.substring(delimiter + 1).trim();

            if (tenant.isEmpty() || value.isEmpty()) {
                throw violationFactory.create(
                        "empty_tenant_or_value",
                        fieldName,
                        tenant,
                        value,
                        "Tenant and value must be non-empty.");
            }

            String tenantKey = tenant.toLowerCase(Locale.ROOT);
            if (!seenTenants.add(tenantKey)) {
                throw violationFactory.create(
                        "duplicate_tenant",
                        fieldName,
                        tenant,
                        value,
                        "Duplicate tenant override.");
            }

            validatePluginCsv(fieldName, tenant, value, allowWildcard, violationFactory);
        }
    }

    private static void validatePluginCsv(
            String fieldName,
            String tenant,
            String value,
            boolean allowWildcard,
            ViolationFactory violationFactory) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw violationFactory.create(
                    "empty_value",
                    fieldName,
                    tenant,
                    value,
                    "Override value must not be empty.");
        }
        if ("*".equals(trimmed)) {
            if (!allowWildcard) {
                throw violationFactory.create(
                        "wildcard_not_allowed",
                        fieldName,
                        tenant,
                        value,
                        "Wildcard `*` is not allowed in this field.");
            }
            return;
        }

        Set<String> seen = new HashSet<>();
        for (String rawToken : trimmed.split(",")) {
            String pluginId = rawToken == null ? "" : rawToken.trim();
            if (pluginId.isEmpty()) {
                throw violationFactory.create(
                        "empty_plugin_id",
                        fieldName,
                        tenant,
                        value,
                        "Contains empty plugin id.");
            }
            if ("*".equals(pluginId)) {
                throw violationFactory.create(
                        "mixed_wildcard",
                        fieldName,
                        tenant,
                        value,
                        "Wildcard `*` must be used alone.");
            }
            String normalized = pluginId.toLowerCase(Locale.ROOT);
            if (!normalized.matches("[a-z0-9._-]+")) {
                throw violationFactory.create(
                        "invalid_plugin_id",
                        fieldName,
                        tenant,
                        pluginId,
                        "Plugin id must match `[a-z0-9._-]+`.");
            }
            if (!seen.add(normalized)) {
                throw violationFactory.create(
                        "duplicate_plugin_id",
                        fieldName,
                        tenant,
                        pluginId,
                        "Duplicate plugin id.");
            }
        }
    }

    @FunctionalInterface
    interface ViolationFactory {
        RuntimeException create(String code, String field, String tenant, String value, String detail);
    }
}
