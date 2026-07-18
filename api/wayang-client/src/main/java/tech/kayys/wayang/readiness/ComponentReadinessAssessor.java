package tech.kayys.wayang.readiness;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangReadinessReport;
import tech.kayys.wayang.client.WayangReadinessReports;
import tech.kayys.wayang.client.WayangReadinessAttributeMaps;

/**
 * Abstract template class for readiness assessors that consolidates common
 * patterns across different component readiness assessments. Implementations
 * must provide concrete implementations of getId, validate, and buildAttributes
 * methods while inheriting the common assess template method logic.
 */
public abstract class ComponentReadinessAssessor {

    /**
     * Get the unique readiness identifier for this assessor.
     * Example: "wayang.standard-alignment.readiness"
     */
    protected abstract String getId();

    /**
     * Get the probe source category name for issues and reports.
     * Example: "standards", "providers", "contracts"
     */
    protected abstract String getSource();

    /**
     * Validate the input object and return a list of issues.
     * Return empty list if validation passes (no issues).
     */
    protected abstract List<Map<String, Object>> validate(Object input);

    /**
     * Build attributes map containing detailed diagnostic information.
     */
    protected abstract Map<String, Object> buildAttributes(Object input);

    /**
     * Build probe list with detailed diagnostic information.
     * Default implementation creates a single probe. Override for custom behavior.
     */
    protected List<Map<String, Object>> buildProbes(Object input, List<Map<String, Object>> issues) {
        boolean ready = issues.isEmpty();
        return List.of(WayangReadinessReports.probe(
                buildProbeName(),
                true,
                ready,
                issues.size(),
                buildAttributes(input)));
    }

    /**
     * Get the probe name for the primary probe.
     * Example: "standards.alignment", "providers.capability_discovery"
     */
    protected abstract String buildProbeName();

    /**
     * Template method that orchestrates the readiness assessment.
     * Calls protected methods to validate, build attributes, and create report.
     */
    public WayangReadinessReport assess(Object input) {
        List<Map<String, Object>> issues = validate(input);
        boolean ready = issues.isEmpty();
        Map<String, Object> attributes = buildAttributes(input);
        List<Map<String, Object>> probes = buildProbes(input, issues);

        return WayangReadinessReport.from(
                getId(),
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                probes,
                issues,
                attributes);
    }

    /**
     * Helper method to create an issue report.
     */
    protected Map<String, Object> createIssue(String code, String message, Map<String, Object> fields) {
        return WayangReadinessReports.issue(code, getSource(), message, fields);
    }

    /**
     * Helper method to create an empty attributes map.
     */
    protected Map<String, Object> emptyAttributes() {
        return WayangReadinessAttributeMaps.ordered();
    }
}
