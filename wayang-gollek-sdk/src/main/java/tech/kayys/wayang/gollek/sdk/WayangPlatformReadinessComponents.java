package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WayangPlatformReadinessComponents {

    private static final List<WayangPlatformReadinessComponent> DEFAULT_COMPONENTS = components(
            component(WayangStorageReadiness.READINESS_ID, WayangGollekSdk::storageReadiness),
            component(WayangContractIntegrityReadiness.READINESS_ID,
                    sdk -> WayangContractIntegrityReadiness.assess(sdk.contractIntegrity())),
            component(WayangContractCoverageReadiness.READINESS_ID,
                    sdk -> WayangContractCoverageReadiness.assess(sdk.contractCommandCoverage())),
            component(WayangSkillCatalogReadiness.READINESS_ID,
                    sdk -> WayangSkillCatalogReadiness.assess(sdk.skillDiscovery(AgentSkillQuery.all()))),
            component(WayangProviderCapabilityReadiness.READINESS_ID,
                    sdk -> WayangProviderCapabilityReadiness.assess(
                            sdk.providerCapabilityDiscovery(WayangProviderCapabilityQuery.all()))),
            component(WayangStandardAlignmentReadiness.READINESS_ID,
                    sdk -> WayangStandardAlignmentReadiness.assess(sdk.standardAlignmentHealth())));

    private WayangPlatformReadinessComponents() {
    }

    public static List<WayangPlatformReadinessComponent> defaultComponents() {
        return DEFAULT_COMPONENTS;
    }

    public static List<String> defaultReadinessIds() {
        return DEFAULT_COMPONENTS.stream()
                .map(WayangPlatformReadinessComponent::readinessId)
                .toList();
    }

    public static List<WayangReadinessReport> assessDefault(WayangGollekSdk sdk) {
        return assess(DEFAULT_COMPONENTS, sdk);
    }

    public static List<WayangReadinessReport> assessProfile(
            WayangPlatformReadinessProfile profile,
            WayangGollekSdk sdk) {
        return assess(componentsFor(profile), sdk);
    }

    public static List<WayangReadinessReport> assess(
            List<WayangPlatformReadinessComponent> components,
            WayangGollekSdk sdk) {
        WayangGollekSdk resolved = sdk == null ? WayangGollekSdk.local() : sdk;
        return requiredComponents(components).stream()
                .map(component -> WayangPlatformReadinessExecution.assessSafely(component, resolved))
                .toList();
    }

    public static List<WayangPlatformReadinessComponent> componentsFor(
            WayangPlatformReadinessProfile profile) {
        WayangPlatformReadinessProfile resolved = profile == null
                ? WayangPlatformReadinessProfiles.defaultProfile()
                : profile;
        Map<String, WayangPlatformReadinessComponent> componentsById = componentsById(DEFAULT_COMPONENTS);
        return components(resolved.readinessIds().stream()
                .map(readinessId -> componentFor(resolved, componentsById, readinessId))
                .toList());
    }

    public static List<WayangPlatformReadinessComponent> components(
            WayangPlatformReadinessComponent... components) {
        return components(components == null ? List.of() : List.of(components));
    }

    public static List<WayangPlatformReadinessComponent> components(
            List<WayangPlatformReadinessComponent> components) {
        if (components == null || components.isEmpty()) {
            return List.of();
        }
        Set<String> readinessIds = new LinkedHashSet<>();
        for (WayangPlatformReadinessComponent component : components) {
            if (component == null) {
                throw new IllegalArgumentException("Platform readiness component is required.");
            }
            if (!readinessIds.add(component.readinessId())) {
                throw new IllegalArgumentException("Duplicate platform readiness component id '"
                        + component.readinessId() + "'.");
            }
        }
        return List.copyOf(components);
    }

    public static WayangPlatformReadinessComponent component(
            String readinessId,
            WayangPlatformReadinessAssessor assessor) {
        return new WayangPlatformReadinessComponent(readinessId, assessor);
    }

    private static List<WayangPlatformReadinessComponent> requiredComponents(
            List<WayangPlatformReadinessComponent> components) {
        List<WayangPlatformReadinessComponent> resolved = components(components);
        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("At least one platform readiness component is required.");
        }
        return resolved;
    }

    private static Map<String, WayangPlatformReadinessComponent> componentsById(
            List<WayangPlatformReadinessComponent> components) {
        Map<String, WayangPlatformReadinessComponent> values = new LinkedHashMap<>();
        for (WayangPlatformReadinessComponent component : components) {
            values.put(component.readinessId(), component);
        }
        return Map.copyOf(values);
    }

    private static WayangPlatformReadinessComponent componentFor(
            WayangPlatformReadinessProfile profile,
            Map<String, WayangPlatformReadinessComponent> componentsById,
            String readinessId) {
        WayangPlatformReadinessComponent component = componentsById.get(readinessId);
        if (component == null) {
            throw new IllegalArgumentException("Unknown platform readiness component id '"
                    + readinessId + "' for profile '" + profile.profileId() + "'.");
        }
        return component;
    }
}
