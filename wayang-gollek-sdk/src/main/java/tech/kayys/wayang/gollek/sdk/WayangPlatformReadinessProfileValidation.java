package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WayangPlatformReadinessProfileValidation {

    private WayangPlatformReadinessProfileValidation() {
    }

    public static WayangPlatformReadinessProfileValidationReport validateDefault() {
        return validate(
                WayangPlatformReadinessProfileCatalog.defaultProfiles(),
                WayangPlatformReadinessProfileValidationPolicy.defaultPolicy());
    }

    public static WayangPlatformReadinessProfileValidationReport validate(
            List<WayangPlatformReadinessProfileDescriptor> profiles) {
        return validate(profiles, WayangPlatformReadinessProfileValidationPolicy.defaultPolicy());
    }

    public static WayangPlatformReadinessProfileValidationReport validate(
            List<WayangPlatformReadinessProfileDescriptor> profiles,
            WayangPlatformReadinessProfileValidationPolicy policy) {
        WayangPlatformReadinessProfileValidationPolicy resolvedPolicy = policy == null
                ? WayangPlatformReadinessProfileValidationPolicy.defaultPolicy()
                : policy;
        List<WayangPlatformReadinessProfileDescriptor> model = profiles == null
                ? List.of()
                : profiles.stream()
                        .filter(profile -> profile != null)
                        .toList();
        List<String> knownReadinessIds = resolvedPolicy.knownReadinessIds();
        Set<String> knownReadinessIdSet = new LinkedHashSet<>(knownReadinessIds);
        List<WayangPlatformReadinessProfileValidationIssue> issues = new ArrayList<>();
        Set<String> profileIds = new LinkedHashSet<>();
        List<String> defaultProfileIds = new ArrayList<>();
        List<String> productionProfileIds = new ArrayList<>();
        Set<String> coveredReadinessIds = new LinkedHashSet<>();

        if (model.isEmpty()) {
            issues.add(issue(
                    "empty-profile-catalog",
                    "At least one platform readiness profile is required.",
                    "",
                    ""));
        }

        for (WayangPlatformReadinessProfileDescriptor profile : model) {
            if (!profileIds.add(profile.profileId())) {
                issues.add(issue(
                        "duplicate-profile",
                        "Duplicate platform readiness profile id '" + profile.profileId() + "'.",
                        profile.profileId(),
                        ""));
            }
            if (profile.defaultProfile()) {
                defaultProfileIds.add(profile.profileId());
            }
            if (profile.productionProfile()) {
                productionProfileIds.add(profile.profileId());
            }
            for (String readinessId : profile.readinessIds()) {
                coveredReadinessIds.add(readinessId);
                if (!knownReadinessIdSet.contains(readinessId)) {
                    issues.add(issue(
                            "unknown-readiness-component",
                            "Profile '" + profile.profileId()
                                    + "' references unknown readiness component id '" + readinessId + "'.",
                            profile.profileId(),
                            readinessId));
                }
            }
        }

        if (resolvedPolicy.requireDefaultProfile()) {
            validateProfileRole(
                    issues,
                    "default",
                    defaultProfileIds,
                    "missing-default-profile",
                    "multiple-default-profiles");
        }
        if (resolvedPolicy.requireProductionProfile()) {
            validateProfileRole(
                    issues,
                    "production",
                    productionProfileIds,
                    "missing-production-profile",
                    "multiple-production-profiles");
        }
        List<String> uncoveredReadinessIds = uncoveredReadinessIds(knownReadinessIds, coveredReadinessIds);
        if (resolvedPolicy.requireFullReadinessCoverage() && !model.isEmpty()) {
            for (String readinessId : uncoveredReadinessIds) {
                issues.add(issue(
                        "uncovered-readiness-component",
                        "No platform readiness profile references component id '" + readinessId + "'.",
                        "",
                        readinessId));
            }
        }

        return new WayangPlatformReadinessProfileValidationReport(
                model.size(),
                List.copyOf(profileIds),
                WayangPlatformReadinessProfileValidationPolicySummary.from(resolvedPolicy),
                defaultProfileIds,
                productionProfileIds,
                knownReadinessIds,
                List.copyOf(coveredReadinessIds),
                uncoveredReadinessIds,
                issues);
    }

    private static void validateProfileRole(
            List<WayangPlatformReadinessProfileValidationIssue> issues,
            String role,
            List<String> profileIds,
            String missingKind,
            String multipleKind) {
        if (profileIds.isEmpty()) {
            issues.add(issue(
                    missingKind,
                    "Exactly one " + role + " platform readiness profile is required.",
                    "",
                    ""));
            return;
        }
        if (profileIds.size() > 1) {
            issues.add(issue(
                    multipleKind,
                    "Expected one " + role + " platform readiness profile but found "
                            + profileIds.size() + ": " + String.join(", ", profileIds) + ".",
                    "",
                    ""));
        }
    }

    private static List<String> uncoveredReadinessIds(
            List<String> knownReadinessIds,
            Set<String> coveredReadinessIds) {
        List<String> uncovered = new ArrayList<>();
        for (String readinessId : knownReadinessIds) {
            if (!coveredReadinessIds.contains(readinessId)) {
                uncovered.add(readinessId);
            }
        }
        return List.copyOf(uncovered);
    }

    private static WayangPlatformReadinessProfileValidationIssue issue(
            String kind,
            String message,
            String profileId,
            String readinessId) {
        return new WayangPlatformReadinessProfileValidationIssue(kind, message, profileId, readinessId);
    }
}
