package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rollup view for multiple standard-alignment summaries.
 */
public record WayangStandardAlignmentPortfolio(
        List<WayangStandardAlignmentSummary> standards) {

    public WayangStandardAlignmentPortfolio {
        standards = WayangStandardAlignmentSummaries.mergeByStandardId(standards);
    }

    @SafeVarargs
    public static WayangStandardAlignmentPortfolio fromReportMaps(Map<?, ?>... reports) {
        return builder()
                .reportMaps(reports)
                .build();
    }

    public static WayangStandardAlignmentPortfolio fromReportMaps(List<? extends Map<?, ?>> reports) {
        return builder()
                .reportMaps(reports)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(WayangStandardAlignmentPortfolio portfolio) {
        return new Builder(portfolio);
    }

    public int standardCount() {
        return standards.size();
    }

    public boolean aligned() {
        return standards.stream().allMatch(WayangStandardAlignmentPortfolio::standardAligned);
    }

    public int alignedCount() {
        return (int) standards.stream()
                .filter(WayangStandardAlignmentPortfolio::standardAligned)
                .count();
    }

    public int gapCount() {
        return standards.stream()
                .mapToInt(WayangStandardAlignmentSummary::gapCount)
                .sum();
    }

    public List<String> standardIds() {
        return standards.stream()
                .map(WayangStandardAlignmentSummary::standardId)
                .toList();
    }

    public List<String> gapStandardIds() {
        return standards.stream()
                .filter(WayangStandardAlignmentSummary::hasGaps)
                .map(WayangStandardAlignmentSummary::standardId)
                .toList();
    }

    public WayangStandardAlignmentPolicyAssessment assess(WayangStandardAlignmentPolicy policy) {
        WayangStandardAlignmentPolicy resolved = policy == null
                ? WayangStandardAlignmentPolicy.builder().build()
                : policy;
        return resolved.assess(this);
    }

    public WayangStandardRegistryDriftReport registryDrift() {
        return WayangStandardRegistryDriftReport.from(this);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("aligned", aligned());
        values.put("standardCount", standardCount());
        values.put("alignedCount", alignedCount());
        values.put("gapCount", gapCount());
        values.put("standardIds", standardIds());
        values.put("gapStandardIds", gapStandardIds());
        values.put("standards", standards.stream()
                .map(WayangStandardAlignmentSummary::toMap)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    private static boolean standardAligned(WayangStandardAlignmentSummary summary) {
        return summary.aligned() && !summary.hasGaps();
    }

    public static final class Builder {
        private final List<WayangStandardAlignmentSummary> standards = new ArrayList<>();

        private Builder() {
        }

        private Builder(WayangStandardAlignmentPortfolio portfolio) {
            portfolio(portfolio);
        }

        public Builder portfolio(WayangStandardAlignmentPortfolio portfolio) {
            if (portfolio != null) {
                standards.addAll(portfolio.standards());
            }
            return this;
        }

        public Builder summary(WayangStandardAlignmentSummary summary) {
            if (summary != null) {
                standards.add(summary);
            }
            return this;
        }

        public Builder summaries(List<? extends WayangStandardAlignmentSummary> summaries) {
            if (summaries != null) {
                summaries.forEach(this::summary);
            }
            return this;
        }

        public Builder reportMap(Map<?, ?> report) {
            if (report != null) {
                WayangStandardAlignmentReportMaps.expand(report).stream()
                        .map(WayangStandardAlignmentSummary::fromReportMap)
                        .forEach(this::summary);
            }
            return this;
        }

        @SafeVarargs
        public final Builder reportMaps(Map<?, ?>... reports) {
            return reportMaps(reports == null ? List.of() : Arrays.asList(reports));
        }

        public Builder reportMaps(List<? extends Map<?, ?>> reports) {
            if (reports != null) {
                reports.forEach(this::reportMap);
            }
            return this;
        }

        public WayangStandardAlignmentPortfolio build() {
            return new WayangStandardAlignmentPortfolio(standards);
        }
    }
}
