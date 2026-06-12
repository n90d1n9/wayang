package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Machine-readable alignment snapshot for Wayang's pinned A2A v1.0 surface.
 */
public record WayangA2aSpecAlignmentReport(
        A2aHttpRouteCatalog routeCatalog,
        List<WayangA2aSpecAlignmentRequirement> requirements) {

    public static final String STANDARD_ID = "a2a";
    public static final String STANDARD_NAME = "Agent2Agent Protocol";
    public static final String SPEC_URL = "https://a2a-protocol.org/latest/specification/";

    public WayangA2aSpecAlignmentReport {
        routeCatalog = routeCatalog == null ? A2aHttpRouteCatalog.standard() : routeCatalog;
        requirements = requirements == null
                ? List.of()
                : requirements.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public static WayangA2aSpecAlignmentReport defaults() {
        return from(A2aHttpRouteCatalog.standard());
    }

    public static WayangA2aSpecAlignmentReport from(A2aHttpRouteCatalog routeCatalog) {
        A2aHttpRouteCatalog resolvedCatalog = routeCatalog == null
                ? A2aHttpRouteCatalog.standard()
                : routeCatalog;
        return new WayangA2aSpecAlignmentReport(
                resolvedCatalog,
                WayangA2aSpecAlignmentRequirements.from(resolvedCatalog));
    }

    public boolean aligned() {
        return requirementSummary().aligned();
    }

    public int requirementCount() {
        return requirementSummary().requirementCount();
    }

    public int alignedCount() {
        return requirementSummary().alignedCount();
    }

    public int gapCount() {
        return requirementSummary().gapCount();
    }

    public List<WayangA2aSpecAlignmentRequirement> gaps() {
        return requirementSummary().gaps();
    }

    public List<String> requirementIds() {
        return requirementSummary().requirementIds();
    }

    public List<String> gapIds() {
        return requirementSummary().gapIds();
    }

    public List<WayangA2aSpecAlignmentCategorySummary> categorySummaries() {
        return categorySummarySet().summaries();
    }

    public Optional<WayangA2aSpecAlignmentCategorySummary> categorySummary(String category) {
        return categorySummarySet().find(category);
    }

    public List<WayangA2aSpecAlignmentCategorySummary> gapCategorySummaries() {
        return categorySummarySet().gaps();
    }

    public List<String> gapCategories() {
        return categorySummarySet().gapCategories();
    }

    public Map<String, Object> toMap() {
        return WayangA2aSpecAlignmentReportProjection.report(this);
    }

    static Map<String, Object> standardDescriptor() {
        return WayangA2aSpecAlignmentStandardDescriptor.pinned().toMap();
    }

    private WayangA2aSpecAlignmentCategorySummaries categorySummarySet() {
        return WayangA2aSpecAlignmentCategorySummaries.fromRequirements(requirements);
    }

    private WayangA2aSpecAlignmentRequirementSummary requirementSummary() {
        return new WayangA2aSpecAlignmentRequirementSummary(requirements);
    }
}
