package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Capability-aware targets concrete learned-skill persistence adapters can bind to.
 */
public record HermesSkillPersistenceTargetPlan(
        HermesSkillPersistenceTarget definitions,
        HermesSkillPersistenceTarget artifacts,
        List<HermesSkillPersistenceBackendProfile> supplementalCloudProfiles,
        List<HermesSkillPersistenceBackendProfile> fallbackProfiles) {

    public HermesSkillPersistenceTargetPlan {
        definitions = definitions == null
                ? emptyTarget(HermesSkillPersistenceRouteRoles.DEFINITIONS)
                : definitions;
        artifacts = artifacts == null
                ? emptyTarget(HermesSkillPersistenceRouteRoles.ARTIFACTS)
                : artifacts;
        supplementalCloudProfiles = HermesCollections.copyNonNull(supplementalCloudProfiles);
        fallbackProfiles = HermesCollections.copyNonNull(fallbackProfiles);
    }

    public static HermesSkillPersistenceTargetPlan from(
            HermesSkillPersistenceBackendRegistry registry) {
        HermesSkillPersistenceBackendRegistry effective = registry == null
                ? HermesSkillPersistenceBackendRegistry.from(null)
                : registry;
        List<HermesSkillPersistenceBackendProfile> supplementalCloud =
                effective.profilesByRole(HermesSkillPersistenceRouteRoles.CLOUD).stream()
                        .filter(profile -> profile.capabilities().supportsArtifacts())
                        .toList();
        List<HermesSkillPersistenceBackendProfile> fallbacks = effective.fallbackProfiles();
        return new HermesSkillPersistenceTargetPlan(
                new HermesSkillPersistenceTarget(
                        HermesSkillPersistenceRouteRoles.DEFINITIONS,
                        primaryProfiles(
                                effective,
                                HermesSkillPersistenceRouteRoles.DEFINITIONS,
                                capabilities -> capabilities.supportsDefinitions()),
                        List.of(),
                        fallbackProfiles(fallbacks, capabilities -> capabilities.supportsDefinitions())),
                new HermesSkillPersistenceTarget(
                        HermesSkillPersistenceRouteRoles.ARTIFACTS,
                        primaryProfiles(
                                effective,
                                HermesSkillPersistenceRouteRoles.ARTIFACTS,
                                capabilities -> capabilities.supportsArtifacts()),
                        supplementalCloud,
                        fallbackProfiles(fallbacks, capabilities -> capabilities.supportsArtifacts())),
                supplementalCloud,
                fallbacks);
    }

    public boolean ready() {
        return definitions.ready() && artifacts.ready();
    }

    public String targetSummary() {
        return "definitions=" + definitions.selectedBackendId().orElse("none")
                + ",artifacts=" + artifacts.selectedBackendId().orElse("none");
    }

    public List<String> supplementalCloudBackendIds() {
        return supplementalCloudProfiles.stream()
                .map(HermesSkillPersistenceBackendProfile::backendId)
                .distinct()
                .toList();
    }

    public List<String> fallbackBackendIds() {
        return fallbackProfiles.stream()
                .map(HermesSkillPersistenceBackendProfile::backendId)
                .distinct()
                .toList();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("targetSummary", targetSummary());
        values.put("supplementalCloudBackendIds", supplementalCloudBackendIds());
        values.put("fallbackBackendIds", fallbackBackendIds());
        values.put("definitions", definitions.toMetadata());
        values.put("artifacts", artifacts.toMetadata());
        return Map.copyOf(values);
    }

    private static HermesSkillPersistenceTarget emptyTarget(String purpose) {
        return new HermesSkillPersistenceTarget(purpose, List.of(), List.of(), List.of());
    }

    private static List<HermesSkillPersistenceBackendProfile> primaryProfiles(
            HermesSkillPersistenceBackendRegistry registry,
            String role,
            Predicate<HermesSkillPersistenceBackendCapabilities> capability) {
        return registry.primaryProfiles().stream()
                .filter(profile -> profile.roleIs(role))
                .filter(profile -> capability.test(profile.capabilities()))
                .toList();
    }

    private static List<HermesSkillPersistenceBackendProfile> fallbackProfiles(
            List<HermesSkillPersistenceBackendProfile> profiles,
            Predicate<HermesSkillPersistenceBackendCapabilities> capability) {
        return profiles.stream()
                .filter(profile -> capability.test(profile.capabilities()))
                .toList();
    }
}
