package tech.kayys.wayang.alignment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;

/**
 * Discovery result for classpath standard-alignment providers.
 */
public record WayangStandardAlignmentProviderDiscovery(
        List<String> providerIds,
        List<WayangStandardAlignmentProviderSummary> providerSummaries,
        WayangStandardAlignmentPortfolio portfolio,
        List<WayangStandardAlignmentProviderIssue> issues) {

    public WayangStandardAlignmentProviderDiscovery(
            List<String> providerIds,
            WayangStandardAlignmentPortfolio portfolio,
            List<WayangStandardAlignmentProviderIssue> issues) {
        this(providerIds, List.of(), portfolio, issues);
    }

    public WayangStandardAlignmentProviderDiscovery {
        providerSummaries = WayangStandardAlignmentProviderSummaries.copy(providerSummaries);
        providerIds = WayangStandardAlignmentProviderSummaries.resolveProviderIds(providerIds, providerSummaries);
        portfolio = portfolio == null ? WayangStandardAlignmentPortfolio.builder().build() : portfolio;
        issues = WayangStandardAlignmentProviderIssues.copy(issues);
    }

    public boolean healthy() {
        return providerDiagnostics().healthy();
    }

    public WayangStandardAlignmentProviderDiagnostics providerDiagnostics() {
        return new WayangStandardAlignmentProviderDiagnostics(providerIds, providerSummaries, issues);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.putAll(providerDiagnostics().toMap());
        values.put("portfolio", portfolio.toMap());
        return SdkMaps.orderedCopy(values);
    }
}
