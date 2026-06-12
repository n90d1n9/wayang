package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Read-only operational view of queryable skill-management event history.
 */
public record SkillManagementEventStoreInspection(
        String name,
        String storeType,
        SkillManagementEventStoreHealthStatus status,
        int matchedEvents,
        int returnedEvents,
        boolean truncated,
        SkillManagementEventSummary summary,
        String failure,
        List<SkillManagementEventStoreInspection> children,
        SkillStoreCapabilities capabilities) {

    public SkillManagementEventStoreInspection(
            String name,
            String storeType,
            SkillManagementEventStoreHealthStatus status,
            int matchedEvents,
            int returnedEvents,
            boolean truncated,
            SkillManagementEventSummary summary,
            String failure,
            List<SkillManagementEventStoreInspection> children) {
        this(name, storeType, status, matchedEvents, returnedEvents, truncated, summary, failure, children,
                SkillStoreCapabilities.of(SkillStoreCapability.QUERY_EVENTS));
    }

    public SkillManagementEventStoreInspection {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (storeType == null || storeType.isBlank()) {
            throw new IllegalArgumentException("storeType must not be blank");
        }
        status = Objects.requireNonNull(status, "status");
        summary = summary == null ? SkillManagementEventSummary.empty() : summary;
        returnedEvents = SkillStoreInspectionSupport.count(returnedEvents);
        matchedEvents = SkillStoreInspectionSupport.countAtLeast(matchedEvents, returnedEvents);
        truncated = truncated || matchedEvents > returnedEvents;
        failure = SkillStoreInspectionSupport.text(failure);
        children = SkillStoreInspectionSupport.children(children);
        capabilities = capabilities == null ? SkillStoreCapabilities.none() : capabilities;
    }

    public boolean ready() {
        return status == SkillManagementEventStoreHealthStatus.READY;
    }

    public static SkillManagementEventStoreInspection ready(
            String name,
            String storeType,
            SkillManagementEventPage page,
            List<SkillManagementEventStoreInspection> children) {
        return ready(
                name,
                storeType,
                page,
                children,
                SkillStoreCapabilities.of(SkillStoreCapability.QUERY_EVENTS));
    }

    public static SkillManagementEventStoreInspection ready(
            String name,
            String storeType,
            SkillManagementEventPage page,
            List<SkillManagementEventStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        SkillManagementEventPage resolvedPage = Objects.requireNonNull(page, "page");
        return ready(
                name,
                storeType,
                resolvedPage.matchedEvents(),
                resolvedPage.returnedEvents(),
                resolvedPage.truncated(),
                resolvedPage.summary(),
                children,
                capabilities);
    }

    public static SkillManagementEventStoreInspection ready(
            String name,
            String storeType,
            int matchedEvents,
            int returnedEvents,
            boolean truncated,
            SkillManagementEventSummary summary,
            List<SkillManagementEventStoreInspection> children) {
        return ready(name, storeType, matchedEvents, returnedEvents, truncated, summary, children,
                SkillStoreCapabilities.of(SkillStoreCapability.QUERY_EVENTS));
    }

    public static SkillManagementEventStoreInspection ready(
            String name,
            String storeType,
            int matchedEvents,
            int returnedEvents,
            boolean truncated,
            SkillManagementEventSummary summary,
            List<SkillManagementEventStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        return new SkillManagementEventStoreInspection(
                name,
                storeType,
                SkillManagementEventStoreHealthStatus.READY,
                matchedEvents,
                returnedEvents,
                truncated,
                summary,
                "",
                children,
                capabilities);
    }

    public static SkillManagementEventStoreInspection unavailable(
            String name,
            String storeType,
            String failure,
            List<SkillManagementEventStoreInspection> children) {
        return unavailable(
                name,
                storeType,
                failure,
                children,
                SkillStoreCapabilities.of(SkillStoreCapability.QUERY_EVENTS));
    }

    public static SkillManagementEventStoreInspection unavailable(
            String name,
            String storeType,
            String failure,
            List<SkillManagementEventStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        return new SkillManagementEventStoreInspection(
                name,
                storeType,
                SkillManagementEventStoreHealthStatus.UNAVAILABLE,
                0,
                0,
                false,
                SkillManagementEventSummary.empty(),
                failure,
                children,
                capabilities);
    }
}
