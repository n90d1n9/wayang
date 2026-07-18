package tech.kayys.wayang.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangStandardRegistryDriftIssue;
import tech.kayys.wayang.client.WayangStandardRegistryDriftMode;
import tech.kayys.wayang.client.WayangStandardRegistryDriftReport;

/**
 * JSON-ready multi-standard health payload for gateway and runtime endpoints.
 */
public record WayangStandardAlignmentHealthReport(
        String reportId,
        String status,
        boolean ready,
        WayangStandardAlignmentPortfolio portfolio,
        WayangStandardAlignmentPolicyAssessment policyAssessment,
        List<String> recommendations,
        WayangStandardRegistryDriftMode registryDriftMode,
        List<String> providerIds,
        List<WayangStandardAlignmentProviderSummary> providerSummaries,
        List<WayangStandardAlignmentProviderIssue> providerIssues,
        WayangStandardAlignmentProviderPolicyAssessment providerPolicyAssessment) {

    public static final String DEFAULT_REPORT_ID = "wayang.standard-alignment.health";

    public WayangStandardAlignmentHealthReport {
        reportId = SdkText.trimToDefault(reportId, DEFAULT_REPORT_ID);
        portfolio = portfolio == null ? WayangStandardAlignmentPortfolio.builder().build() : portfolio;
        policyAssessment = policyAssessment == null ? portfolio.assess(null) : policyAssessment;
        registryDriftMode = registryDriftMode == null
                ? WayangStandardRegistryDriftMode.IGNORE
                : registryDriftMode;
        WayangStandardAlignmentProviderDiagnostics providerDiagnostics =
                new WayangStandardAlignmentProviderDiagnostics(providerIds, providerSummaries, providerIssues);
        providerIds = providerDiagnostics.providerIds();
        providerSummaries = providerDiagnostics.providers();
        providerIssues = providerDiagnostics.issues();
        providerPolicyAssessment = providerPolicyAssessment == null
                ? WayangStandardAlignmentProviderPolicy.defaultPolicy().assess(providerDiagnostics)
                : providerPolicyAssessment;
        WayangStandardRegistryDriftReport registryDrift = portfolio.registryDrift();
        ready = policyAssessment.ready()
                && !driftBlocks(registryDrift, registryDriftMode)
                && providerPolicyAssessment.ready();
        status = SdkText.trimToDefault(
                status,
                status(policyAssessment, registryDrift, registryDriftMode, providerPolicyAssessment));
        recommendations = recommendations(
                policyAssessment,
                registryDrift,
                registryDriftMode,
                recommendations,
                providerPolicyAssessment);
    }

    public WayangStandardAlignmentHealthReport(
            String reportId,
            String status,
            boolean ready,
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyAssessment policyAssessment,
            List<String> recommendations,
            WayangStandardRegistryDriftMode registryDriftMode,
            List<String> providerIds,
            List<WayangStandardAlignmentProviderSummary> providerSummaries,
            List<WayangStandardAlignmentProviderIssue> providerIssues) {
        this(
                reportId,
                status,
                ready,
                portfolio,
                policyAssessment,
                recommendations,
                registryDriftMode,
                providerIds,
                providerSummaries,
                providerIssues,
                null);
    }

    public WayangStandardAlignmentHealthReport(
            String reportId,
            String status,
            boolean ready,
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyAssessment policyAssessment,
            List<String> recommendations,
            WayangStandardRegistryDriftMode registryDriftMode,
            List<String> providerIds,
            List<WayangStandardAlignmentProviderIssue> providerIssues) {
        this(
                reportId,
                status,
                ready,
                portfolio,
                policyAssessment,
                recommendations,
                registryDriftMode,
                providerIds,
                List.of(),
                providerIssues,
                null);
    }

    public WayangStandardAlignmentHealthReport(
            String reportId,
            String status,
            boolean ready,
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyAssessment policyAssessment,
            List<String> recommendations,
            WayangStandardRegistryDriftMode registryDriftMode) {
        this(
                reportId,
                status,
                ready,
                portfolio,
                policyAssessment,
                recommendations,
                registryDriftMode,
                List.of(),
                List.of(),
                List.of(),
                null);
    }

    public WayangStandardAlignmentHealthReport(
            String reportId,
            String status,
            boolean ready,
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyAssessment policyAssessment,
            List<String> recommendations) {
        this(
                reportId,
                status,
                ready,
                portfolio,
                policyAssessment,
                recommendations,
                WayangStandardRegistryDriftMode.IGNORE,
                List.of(),
                List.of(),
                List.of(),
                null);
    }

    public static WayangStandardAlignmentHealthReport from(WayangStandardAlignmentPortfolio portfolio) {
        return from(portfolio, null);
    }

    public static WayangStandardAlignmentHealthReport from(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicy policy) {
        WayangStandardAlignmentPortfolio resolved = portfolio == null
                ? WayangStandardAlignmentPortfolio.builder().build()
                : portfolio;
        return new WayangStandardAlignmentHealthReport(
                DEFAULT_REPORT_ID,
                "",
                false,
                resolved,
                resolved.assess(policy),
                List.of(),
                WayangStandardRegistryDriftMode.IGNORE,
                List.of(),
                List.of(),
                List.of(),
                null);
    }

    public static WayangStandardAlignmentHealthReport from(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicy policy,
            WayangStandardRegistryDriftMode registryDriftMode) {
        WayangStandardAlignmentPortfolio resolved = portfolio == null
                ? WayangStandardAlignmentPortfolio.builder().build()
                : portfolio;
        return new WayangStandardAlignmentHealthReport(
                DEFAULT_REPORT_ID,
                "",
                false,
                resolved,
                resolved.assess(policy),
                List.of(),
                registryDriftMode,
                List.of(),
                List.of(),
                List.of(),
                null);
    }

    public static WayangStandardAlignmentHealthReport fromConfiguredPolicy(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyConfig config) {
        return fromConfiguredPolicy(portfolio, config, List.of());
    }

    public static WayangStandardAlignmentHealthReport fromConfiguredPolicy(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyConfig config,
            List<WayangStandardAlignmentProviderIssue> providerIssues) {
        return fromConfiguredPolicy(portfolio, config, List.of(), providerIssues);
    }

    public static WayangStandardAlignmentHealthReport fromConfiguredPolicy(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyConfig config,
            List<String> providerIds,
            List<WayangStandardAlignmentProviderIssue> providerIssues) {
        return fromConfiguredPolicy(portfolio, config, providerIds, List.of(), providerIssues);
    }

    public static WayangStandardAlignmentHealthReport fromConfiguredPolicy(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyConfig config,
            WayangStandardAlignmentProviderDiagnostics providerDiagnostics) {
        WayangStandardAlignmentProviderDiagnostics resolvedDiagnostics =
                providerDiagnostics(providerDiagnostics);
        return fromConfiguredPolicy(
                portfolio,
                config,
                resolvedDiagnostics.providerIds(),
                resolvedDiagnostics.providers(),
                resolvedDiagnostics.issues());
    }

    public static WayangStandardAlignmentHealthReport fromConfiguredPolicy(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicyConfig config,
            List<String> providerIds,
            List<WayangStandardAlignmentProviderSummary> providerSummaries,
            List<WayangStandardAlignmentProviderIssue> providerIssues) {
        WayangStandardAlignmentPortfolio resolved = portfolio == null
                ? WayangStandardAlignmentPortfolio.builder().build()
                : portfolio;
        WayangStandardAlignmentPolicyConfig resolvedConfig = config == null
                ? WayangStandardAlignmentPolicyConfig.none()
                : config;
        WayangStandardAlignmentProviderDiagnostics providerDiagnostics =
                new WayangStandardAlignmentProviderDiagnostics(providerIds, providerSummaries, providerIssues);
        return from(
                resolved,
                resolvedConfig.toPolicy(),
                resolvedConfig.registryDriftMode(),
                providerDiagnostics.providerIds(),
                providerDiagnostics.providers(),
                providerDiagnostics.issues(),
                resolvedConfig.toProviderPolicy().assess(providerDiagnostics));
    }

    public static WayangStandardAlignmentHealthReport from(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicy policy,
            WayangStandardRegistryDriftMode registryDriftMode,
            List<WayangStandardAlignmentProviderIssue> providerIssues) {
        return from(portfolio, policy, registryDriftMode, List.of(), providerIssues);
    }

    public static WayangStandardAlignmentHealthReport from(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicy policy,
            WayangStandardRegistryDriftMode registryDriftMode,
            List<String> providerIds,
            List<WayangStandardAlignmentProviderIssue> providerIssues) {
        return from(portfolio, policy, registryDriftMode, providerIds, List.of(), providerIssues);
    }

    public static WayangStandardAlignmentHealthReport from(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicy policy,
            WayangStandardRegistryDriftMode registryDriftMode,
            WayangStandardAlignmentProviderDiagnostics providerDiagnostics) {
        WayangStandardAlignmentProviderDiagnostics resolvedDiagnostics =
                providerDiagnostics(providerDiagnostics);
        return from(
                portfolio,
                policy,
                registryDriftMode,
                resolvedDiagnostics.providerIds(),
                resolvedDiagnostics.providers(),
                resolvedDiagnostics.issues());
    }

    public static WayangStandardAlignmentHealthReport from(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicy policy,
            WayangStandardRegistryDriftMode registryDriftMode,
            List<String> providerIds,
            List<WayangStandardAlignmentProviderSummary> providerSummaries,
            List<WayangStandardAlignmentProviderIssue> providerIssues) {
        return from(
                portfolio,
                policy,
                registryDriftMode,
                providerIds,
                providerSummaries,
                providerIssues,
                null);
    }

    public static WayangStandardAlignmentHealthReport from(
            WayangStandardAlignmentPortfolio portfolio,
            WayangStandardAlignmentPolicy policy,
            WayangStandardRegistryDriftMode registryDriftMode,
            List<String> providerIds,
            List<WayangStandardAlignmentProviderSummary> providerSummaries,
            List<WayangStandardAlignmentProviderIssue> providerIssues,
            WayangStandardAlignmentProviderPolicyAssessment providerPolicyAssessment) {
        WayangStandardAlignmentPortfolio resolved = portfolio == null
                ? WayangStandardAlignmentPortfolio.builder().build()
                : portfolio;
        return new WayangStandardAlignmentHealthReport(
                DEFAULT_REPORT_ID,
                "",
                false,
                resolved,
                resolved.assess(policy),
                List.of(),
                registryDriftMode,
                providerIds,
                providerSummaries,
                providerIssues,
                providerPolicyAssessment);
    }

    public static WayangStandardAlignmentHealthReport fromPinnedKnownStandards(
            WayangStandardAlignmentPortfolio portfolio) {
        return from(portfolio, WayangStandardAlignmentPolicies.pinnedKnownStandards());
    }

    public static WayangStandardAlignmentHealthReport fromPinnedRegistry(
            WayangStandardAlignmentPortfolio portfolio,
            String... standardIds) {
        return from(portfolio, WayangStandardAlignmentPolicies.pinnedRegistry(standardIds));
    }

    @SafeVarargs
    public static WayangStandardAlignmentHealthReport fromReportMaps(
            WayangStandardAlignmentPolicy policy,
            Map<?, ?>... reports) {
        return fromReportMaps(policy, reports == null ? List.of() : Arrays.asList(reports));
    }

    public static WayangStandardAlignmentHealthReport fromReportMaps(
            WayangStandardAlignmentPolicy policy,
            List<? extends Map<?, ?>> reports) {
        return from(WayangStandardAlignmentPortfolio.fromReportMaps(reports), policy);
    }

    @SafeVarargs
    public static WayangStandardAlignmentHealthReport fromReportMapsConfiguredPolicy(
            WayangStandardAlignmentPolicyConfig config,
            Map<?, ?>... reports) {
        return fromReportMapsConfiguredPolicy(config, reports == null ? List.of() : Arrays.asList(reports));
    }

    public static WayangStandardAlignmentHealthReport fromReportMapsConfiguredPolicy(
            WayangStandardAlignmentPolicyConfig config,
            List<? extends Map<?, ?>> reports) {
        return fromConfiguredPolicy(WayangStandardAlignmentPortfolio.fromReportMaps(reports), config);
    }

    @SafeVarargs
    public static WayangStandardAlignmentHealthReport fromReportMapsPinnedKnownStandards(
            Map<?, ?>... reports) {
        return fromReportMapsPinnedKnownStandards(reports == null ? List.of() : Arrays.asList(reports));
    }

    public static WayangStandardAlignmentHealthReport fromReportMapsPinnedKnownStandards(
            List<? extends Map<?, ?>> reports) {
        return fromReportMaps(WayangStandardAlignmentPolicies.pinnedKnownStandards(), reports);
    }

    public static WayangStandardAlignmentHealthReport fromReportMapsPinnedRegistry(
            List<? extends Map<?, ?>> reports,
            String... standardIds) {
        return fromReportMaps(WayangStandardAlignmentPolicies.pinnedRegistry(standardIds), reports);
    }

    public boolean blocked() {
        return policyAssessment.hasFailures()
                || driftBlocks(registryDrift(), registryDriftMode)
                || providerPolicyAssessment.blocked();
    }

    public boolean warning() {
        return ready
                && (policyAssessment.hasWarnings()
                        || driftWarns(registryDrift(), registryDriftMode)
                        || providerPolicyAssessment.warning());
    }

    public boolean hasProviderIssues() {
        return providerDiagnostics().hasIssues();
    }

    public boolean hasProviders() {
        return providerDiagnostics().hasProviders();
    }

    public WayangStandardAlignmentProviderDiagnostics providerDiagnostics() {
        return new WayangStandardAlignmentProviderDiagnostics(providerIds, providerSummaries, providerIssues);
    }

    public WayangStandardRegistryDriftReport registryDrift() {
        return portfolio.registryDrift();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("reportId", reportId);
        values.put("status", status);
        values.put("ready", ready);
        values.put("aligned", portfolio.aligned());
        values.put("standardCount", portfolio.standardCount());
        values.put("gapCount", portfolio.gapCount());
        values.put("standardIds", portfolio.standardIds());
        values.put("gapStandardIds", portfolio.gapStandardIds());
        values.put("portfolio", portfolio.toMap());
        values.put("policyAssessment", policyAssessment.toMap());
        values.put("providerPolicyAssessment", providerPolicyAssessment.toMap());
        values.put("registryDrift", registryDrift().toMap());
        values.put("registryDriftMode", registryDriftMode.id());
        WayangStandardAlignmentProviderDiagnostics providerDiagnostics = providerDiagnostics();
        values.put("providerDiagnostics", providerDiagnostics.toMap());
        values.put("providerCount", providerDiagnostics.providerCount());
        values.put("providerIds", providerDiagnostics.providerIds());
        values.put("providers", providerDiagnostics.providerMaps());
        values.put("providerIssueCount", providerDiagnostics.issueCount());
        values.put("providerIssues", providerDiagnostics.issueMaps());
        values.put("recommendations", recommendations);
        return SdkMaps.orderedCopy(values);
    }

    private static String status(
            WayangStandardAlignmentPolicyAssessment assessment,
            WayangStandardRegistryDriftReport registryDrift,
            WayangStandardRegistryDriftMode registryDriftMode,
            WayangStandardAlignmentProviderPolicyAssessment providerPolicyAssessment) {
        if (assessment.hasFailures()
                || driftBlocks(registryDrift, registryDriftMode)
                || providerPolicyAssessment.blocked()) {
            return "blocked";
        }
        return assessment.hasWarnings()
                || driftWarns(registryDrift, registryDriftMode)
                || providerPolicyAssessment.warning()
                ? "warning"
                : "ready";
    }

    private static WayangStandardAlignmentProviderDiagnostics providerDiagnostics(
            WayangStandardAlignmentProviderDiagnostics providerDiagnostics) {
        return providerDiagnostics == null
                ? WayangStandardAlignmentProviderDiagnostics.empty()
                : providerDiagnostics;
    }

    private static boolean driftBlocks(
            WayangStandardRegistryDriftReport registryDrift,
            WayangStandardRegistryDriftMode registryDriftMode) {
        return registryDriftMode == WayangStandardRegistryDriftMode.BLOCK
                && registryDrift != null
                && registryDrift.hasDrift();
    }

    private static boolean driftWarns(
            WayangStandardRegistryDriftReport registryDrift,
            WayangStandardRegistryDriftMode registryDriftMode) {
        return registryDriftMode == WayangStandardRegistryDriftMode.WARN
                && registryDrift != null
                && registryDrift.hasDrift();
    }

    private static List<String> recommendations(
            WayangStandardAlignmentPolicyAssessment assessment,
            WayangStandardRegistryDriftReport registryDrift,
            WayangStandardRegistryDriftMode registryDriftMode,
            List<String> explicitRecommendations,
            WayangStandardAlignmentProviderPolicyAssessment providerPolicyAssessment) {
        List<String> values = explicitRecommendations == null || explicitRecommendations.isEmpty()
                ? new ArrayList<>(assessment.recommendations())
                : new ArrayList<>(SdkLists.copy(explicitRecommendations));
        if (registryDriftMode != WayangStandardRegistryDriftMode.IGNORE && registryDrift != null) {
            registryDrift.issues().stream()
                    .map(issue -> registryDriftRecommendation(issue, registryDriftMode))
                    .forEach(values::add);
        }
        values.addAll(providerPolicyAssessment.recommendations());
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    private static String registryDriftRecommendation(
            WayangStandardRegistryDriftIssue issue,
            WayangStandardRegistryDriftMode registryDriftMode) {
        String verb = registryDriftMode == WayangStandardRegistryDriftMode.BLOCK ? "Resolve" : "Review";
        return verb + " registry drift for standard "
                + issue.standardId()
                + " field "
                + issue.field()
                + ".";
    }
}
