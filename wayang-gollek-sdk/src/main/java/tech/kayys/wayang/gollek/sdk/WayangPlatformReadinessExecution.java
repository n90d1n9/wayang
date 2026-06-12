package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class WayangPlatformReadinessExecution {

    static final String INCLUDE_DURATION_PROPERTY = "wayang.readiness.includeDurationMillis";

    private static final String COMPLETED = "completed";
    private static final String FAILED = "failed";
    private static final String FAILURE_CODE = "platform_readiness_component_failed";
    private static final String FAILURE_SOURCE = "platform-readiness";

    private WayangPlatformReadinessExecution() {
    }

    static WayangReadinessReport assessSafely(
            WayangPlatformReadinessComponent component,
            WayangGollekSdk sdk) {
        return assessSafely(component, sdk, Boolean.getBoolean(INCLUDE_DURATION_PROPERTY));
    }

    static WayangReadinessReport assessSafely(
            WayangPlatformReadinessComponent component,
            WayangGollekSdk sdk,
            boolean includeDurationMillis) {
        long startedNanos = System.nanoTime();
        WayangReadinessReport report;
        try {
            report = component.assessUnchecked(sdk);
        } catch (RuntimeException failure) {
            return failureReport(component, failure, durationMillis(startedNanos), includeDurationMillis);
        }
        WayangReadinessReport validated = component.validateReport(report);
        if (!includeDurationMillis) {
            return validated;
        }
        return withExecutionDiagnostics(
                validated,
                component.readinessId(),
                COMPLETED,
                durationMillis(startedNanos));
    }

    private static WayangReadinessReport failureReport(
            WayangPlatformReadinessComponent component,
            RuntimeException failure,
            long durationMillis,
            boolean includeDurationMillis) {
        Map<String, Object> attributes = failureAttributes(
                component,
                failure,
                durationMillis,
                includeDurationMillis);
        return WayangReadinessReport.from(
                component.readinessId(),
                false,
                WayangReadinessReports.EXIT_FAILURE,
                1,
                List.of(WayangReadinessReports.probe(
                        component.readinessId() + ".execution",
                        true,
                        false,
                        1,
                        attributes)),
                List.of(WayangReadinessReports.issue(
                        FAILURE_CODE,
                        FAILURE_SOURCE,
                        "Platform readiness component failed during assessment.",
                        attributes)),
                attributes);
    }

    private static WayangReadinessReport withExecutionDiagnostics(
            WayangReadinessReport report,
            String componentReadinessId,
            String assessmentStatus,
            long durationMillis) {
        Map<String, Object> attributes = executionAttributes(
                report.attributes(),
                componentReadinessId,
                assessmentStatus,
                durationMillis,
                true);
        return WayangReadinessReport.from(
                report.readinessId(),
                report.ready(),
                report.exitCode(),
                report.issueCount(),
                report.probes(),
                report.issues(),
                attributes);
    }

    private static Map<String, Object> failureAttributes(
            WayangPlatformReadinessComponent component,
            RuntimeException failure,
            long durationMillis,
            boolean includeDurationMillis) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("exceptionType", failure.getClass().getName());
        values.put("exceptionMessage", SdkText.trimToDefault(
                failure.getMessage(),
                failure.getClass().getSimpleName()));
        return executionAttributes(
                values,
                component.readinessId(),
                FAILED,
                durationMillis,
                includeDurationMillis);
    }

    private static Map<String, Object> executionAttributes(
            Map<String, Object> attributes,
            String componentReadinessId,
            String assessmentStatus,
            long durationMillis,
            boolean includeDurationMillis) {
        Map<String, Object> values = new LinkedHashMap<>(WayangReportMaps.copyMap(attributes));
        values.put("componentReadinessId", componentReadinessId);
        values.put("assessmentStatus", assessmentStatus);
        if (includeDurationMillis) {
            values.put("durationMillis", Math.max(0, durationMillis));
        }
        return WayangReportMaps.copyMap(values);
    }

    private static long durationMillis(long startedNanos) {
        return Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
    }
}
