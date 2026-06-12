package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter-facing registry view over learned-skill persistence route backends.
 */
public final class HermesSkillPersistenceBackendRegistry {

    private final List<HermesSkillPersistenceBackendProfile> profiles;

    public HermesSkillPersistenceBackendRegistry(List<HermesSkillPersistenceBackendProfile> profiles) {
        this.profiles = profiles == null ? List.of() : profiles.stream()
                .filter(profile -> profile != null)
                .toList();
    }

    public static HermesSkillPersistenceBackendRegistry from(HermesSkillPersistencePlan plan) {
        HermesSkillPersistencePlan effective = plan == null
                ? HermesSkillPersistencePlan.from(null)
                : plan;
        return fromRoutes(effective.routes());
    }

    public static HermesSkillPersistenceBackendRegistry fromRoutes(
            List<HermesSkillPersistenceRoute> routes) {
        return new HermesSkillPersistenceBackendRegistry(
                (routes == null ? List.<HermesSkillPersistenceRoute>of() : routes).stream()
                        .map(HermesSkillPersistenceBackendProfile::from)
                        .toList());
    }

    public int routeCount() {
        return profiles.size();
    }

    public int backendCount() {
        return backendIds().size();
    }

    public List<HermesSkillPersistenceBackendProfile> profiles() {
        return profiles;
    }

    public List<String> backendIds() {
        return profiles.stream()
                .map(HermesSkillPersistenceBackendProfile::backendId)
                .distinct()
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> primaryProfiles() {
        return profiles.stream()
                .filter(profile -> !profile.fallback())
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> fallbackProfiles() {
        return profiles.stream()
                .filter(HermesSkillPersistenceBackendProfile::fallback)
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> profilesByRole(String role) {
        return profiles.stream()
                .filter(profile -> profile.roleIs(role))
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> profilesByStorageFamily(String storageFamily) {
        return profiles.stream()
                .filter(profile -> profile.storageFamilyIs(storageFamily))
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> readableProfiles() {
        return profiles.stream()
                .filter(profile -> profile.capabilities().readable())
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> writableProfiles() {
        return profiles.stream()
                .filter(profile -> profile.capabilities().writable())
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> durableProfiles() {
        return profiles.stream()
                .filter(profile -> profile.capabilities().durable())
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> credentialedProfiles() {
        return profiles.stream()
                .filter(profile -> profile.capabilities().requiresCredentials())
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> definitionCapableProfiles() {
        return profiles.stream()
                .filter(profile -> profile.capabilities().supportsDefinitions())
                .toList();
    }

    public List<HermesSkillPersistenceBackendProfile> artifactCapableProfiles() {
        return profiles.stream()
                .filter(profile -> profile.capabilities().supportsArtifacts())
                .toList();
    }

    public HermesSkillPersistenceTargetPlan targetPlan() {
        return HermesSkillPersistenceTargetPlan.from(this);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backendCount", backendCount());
        values.put("routeCount", routeCount());
        values.put("readableRouteCount", readableProfiles().size());
        values.put("writableRouteCount", writableProfiles().size());
        values.put("durableRouteCount", durableProfiles().size());
        values.put("credentialedRouteCount", credentialedProfiles().size());
        values.put("definitionCapableRouteCount", definitionCapableProfiles().size());
        values.put("artifactCapableRouteCount", artifactCapableProfiles().size());
        values.put("backendIds", backendIds());
        values.put("targetPlan", targetPlan().toMetadata());
        values.put("backendProfiles", profiles.stream()
                .map(HermesSkillPersistenceBackendProfile::toMetadata)
                .toList());
        return Map.copyOf(values);
    }
}
