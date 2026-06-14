package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Readiness policy for standard-alignment provider discovery.
 */
public record WayangStandardAlignmentProviderPolicy(
        List<String> requiredProviderIds,
        int minimumProviderCount,
        WayangStandardAlignmentProviderIssueMode issueMode) {

    public WayangStandardAlignmentProviderPolicy {
        requiredProviderIds = normalizedProviderIds(requiredProviderIds);
        minimumProviderCount = Math.max(0, minimumProviderCount);
        issueMode = issueMode == null ? WayangStandardAlignmentProviderIssueMode.WARN : issueMode;
    }

    public static WayangStandardAlignmentProviderPolicy defaultPolicy() {
        return new WayangStandardAlignmentProviderPolicy(
                List.of(),
                0,
                WayangStandardAlignmentProviderIssueMode.WARN);
    }

    public WayangStandardAlignmentProviderPolicyAssessment assess(
            WayangStandardAlignmentProviderDiagnostics diagnostics) {
        WayangStandardAlignmentProviderDiagnostics resolved = diagnostics == null
                ? WayangStandardAlignmentProviderDiagnostics.empty()
                : diagnostics;
        List<String> present = resolved.providerIds();
        List<String> missing = requiredProviderIds.stream()
                .filter(required -> !present.contains(required))
                .toList();
        int issueCount = resolved.issueCount();
        List<String> recommendations = recommendations(present.size(), missing, resolved.issues());
        boolean ready = missing.isEmpty()
                && present.size() >= minimumProviderCount
                && !(issueMode == WayangStandardAlignmentProviderIssueMode.BLOCK && issueCount > 0);
        return new WayangStandardAlignmentProviderPolicyAssessment(
                ready,
                issueMode,
                minimumProviderCount,
                present.size(),
                requiredProviderIds,
                present,
                missing,
                issueCount,
                recommendations);
    }

    private List<String> recommendations(
            int providerCount,
            List<String> missingProviderIds,
            List<WayangStandardAlignmentProviderIssue> issues) {
        List<String> values = new ArrayList<>();
        if (providerCount < minimumProviderCount) {
            values.add("Register at least "
                    + minimumProviderCount
                    + " standard-alignment provider(s).");
        }
        missingProviderIds.forEach(providerId -> values.add(
                "Register required standard-alignment provider: " + providerId + "."));
        if (issueMode != WayangStandardAlignmentProviderIssueMode.IGNORE) {
            values.addAll(WayangStandardAlignmentProviderIssues.recommendations(issues));
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    private static List<String> normalizedProviderIds(List<String> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        providerIds.stream()
                .map(SdkText::trimToEmpty)
                .filter(value -> !value.isEmpty())
                .forEach(values::add);
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }
}
