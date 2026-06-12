package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Route-scoped capability flags for a learned-skill persistence backend.
 */
public record HermesSkillPersistenceBackendCapabilities(
        boolean readable,
        boolean writable,
        boolean durable,
        boolean requiresCredentials,
        boolean fallbackOnly,
        boolean supportsDefinitions,
        boolean supportsArtifacts,
        boolean supplementalCloud) {

    public static HermesSkillPersistenceBackendCapabilities from(
            HermesSkillPersistenceBackendProfile profile) {
        if (profile == null || "none".equals(profile.storageFamily())) {
            return inactive();
        }
        String family = profile.storageFamily();
        boolean durable = durableFamily(family);
        boolean requiresCredentials = requiresCredentials(family);
        boolean supportsDefinitions = profile.roleIs(HermesSkillPersistenceRouteRoles.DEFINITIONS)
                || profile.roleIs(HermesSkillPersistenceRouteRoles.FALLBACK);
        boolean supportsArtifacts = profile.roleIs(HermesSkillPersistenceRouteRoles.ARTIFACTS)
                || profile.roleIs(HermesSkillPersistenceRouteRoles.CLOUD)
                || profile.roleIs(HermesSkillPersistenceRouteRoles.FALLBACK);
        boolean supplementalCloud = profile.roleIs(HermesSkillPersistenceRouteRoles.CLOUD);
        return new HermesSkillPersistenceBackendCapabilities(
                true,
                true,
                durable,
                requiresCredentials,
                profile.fallback(),
                supportsDefinitions,
                supportsArtifacts,
                supplementalCloud);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("readable", readable);
        values.put("writable", writable);
        values.put("durable", durable);
        values.put("requiresCredentials", requiresCredentials);
        values.put("fallbackOnly", fallbackOnly);
        values.put("supportsDefinitions", supportsDefinitions);
        values.put("supportsArtifacts", supportsArtifacts);
        values.put("supplementalCloud", supplementalCloud);
        return Map.copyOf(values);
    }

    private static HermesSkillPersistenceBackendCapabilities inactive() {
        return new HermesSkillPersistenceBackendCapabilities(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
    }

    private static boolean durableFamily(String family) {
        return switch (family) {
            case "database", "object-storage", "file-system", "skill-management", "hybrid" -> true;
            default -> false;
        };
    }

    private static boolean requiresCredentials(String family) {
        return switch (family) {
            case "database", "object-storage", "hybrid" -> true;
            default -> false;
        };
    }
}
