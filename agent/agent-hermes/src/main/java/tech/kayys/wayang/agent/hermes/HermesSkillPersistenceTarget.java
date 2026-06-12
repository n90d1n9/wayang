package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Capability-filtered backend target for one learned-skill persistence purpose.
 */
public record HermesSkillPersistenceTarget(
        String purpose,
        List<HermesSkillPersistenceBackendProfile> primaryProfiles,
        List<HermesSkillPersistenceBackendProfile> supplementalProfiles,
        List<HermesSkillPersistenceBackendProfile> fallbackProfiles) {

    public HermesSkillPersistenceTarget {
        purpose = HermesSkillPersistenceRouteRoles.normalize(purpose);
        primaryProfiles = HermesCollections.copyNonNull(primaryProfiles);
        supplementalProfiles = HermesCollections.copyNonNull(supplementalProfiles);
        fallbackProfiles = HermesCollections.copyNonNull(fallbackProfiles);
    }

    public Optional<HermesSkillPersistenceBackendProfile> selectedProfile() {
        return primaryProfiles.stream()
                .findFirst()
                .or(() -> fallbackProfiles.stream().findFirst());
    }

    public Optional<String> selectedBackendId() {
        return selectedProfile().map(HermesSkillPersistenceBackendProfile::backendId);
    }

    public boolean ready() {
        return selectedProfile().isPresent();
    }

    public boolean credentialed() {
        return activeProfiles().stream()
                .anyMatch(profile -> profile.capabilities().requiresCredentials());
    }

    public boolean durable() {
        return activeProfiles().stream()
                .anyMatch(profile -> profile.capabilities().durable());
    }

    public List<HermesSkillPersistenceBackendProfile> activeProfiles() {
        List<HermesSkillPersistenceBackendProfile> active = new ArrayList<>();
        active.addAll(primaryProfiles);
        active.addAll(supplementalProfiles);
        active.addAll(fallbackProfiles);
        return List.copyOf(active);
    }

    public List<String> primaryBackendIds() {
        return backendIds(primaryProfiles);
    }

    public List<String> supplementalBackendIds() {
        return backendIds(supplementalProfiles);
    }

    public List<String> fallbackBackendIds() {
        return backendIds(fallbackProfiles);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("purpose", purpose);
        values.put("ready", ready());
        values.put("credentialed", credentialed());
        values.put("durable", durable());
        values.put("selectedBackendId", selectedBackendId().orElse(""));
        values.put("primaryBackendIds", primaryBackendIds());
        values.put("supplementalBackendIds", supplementalBackendIds());
        values.put("fallbackBackendIds", fallbackBackendIds());
        values.put("primaryProfiles", primaryProfiles.stream()
                .map(HermesSkillPersistenceBackendProfile::toMetadata)
                .toList());
        values.put("supplementalProfiles", supplementalProfiles.stream()
                .map(HermesSkillPersistenceBackendProfile::toMetadata)
                .toList());
        values.put("fallbackProfiles", fallbackProfiles.stream()
                .map(HermesSkillPersistenceBackendProfile::toMetadata)
                .toList());
        return Map.copyOf(values);
    }

    private static List<String> backendIds(List<HermesSkillPersistenceBackendProfile> profiles) {
        return profiles.stream()
                .map(HermesSkillPersistenceBackendProfile::backendId)
                .distinct()
                .toList();
    }
}
