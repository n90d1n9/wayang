package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProviderDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfigDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfigIssue;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryPreflightReport;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryResolution;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileSourceStatus;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileValidationIssue;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileValidationPolicyDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileValidationReport;

import java.util.List;
import java.util.Map;

/**
 * Text renderer for readiness profile catalog, validation, source, and config responses.
 */
final class WayangReadinessProfileTextFormat {

    private WayangReadinessProfileTextFormat() {
    }

    static String text(List<WayangPlatformReadinessProfileDescriptor> profiles) {
        StringBuilder output = new StringBuilder("Wayang readiness profiles\n");
        List<WayangPlatformReadinessProfileDescriptor> model = CliLists.copy(profiles);
        output.append("totalProfiles: ").append(model.size()).append('\n');
        for (WayangPlatformReadinessProfileDescriptor profile : model) {
            appendProfile(output, profile);
        }
        return output.append('\n').toString();
    }

    static String detailText(WayangPlatformReadinessProfileDescriptor profile) {
        StringBuilder output = new StringBuilder("Wayang readiness profile\n");
        appendProfile(output, profile);
        return output.append('\n').toString();
    }

    static String validationText(WayangPlatformReadinessProfileValidationReport report) {
        StringBuilder output = new StringBuilder("Wayang readiness profile validation\n");
        output.append("valid: ").append(report.valid()).append('\n');
        output.append("issues: ").append(report.issueCount()).append('\n');
        output.append("profiles: ").append(report.totalProfiles()).append('\n');
        output.append("profileIds: ").append(CliText.commaSeparated(report.profileIds())).append('\n');
        output.append("validationPolicy: ")
                .append("policyId=")
                .append(report.validationPolicy().policyId())
                .append(", ")
                .append("strict=")
                .append(report.validationPolicy().strict())
                .append(", knownReadinessCount=")
                .append(report.validationPolicy().knownReadinessCount())
                .append(", requireDefaultProfile=")
                .append(report.validationPolicy().requireDefaultProfile())
                .append(", requireProductionProfile=")
                .append(report.validationPolicy().requireProductionProfile())
                .append(", requireFullReadinessCoverage=")
                .append(report.validationPolicy().requireFullReadinessCoverage())
                .append('\n');
        output.append("defaultProfiles: ")
                .append(report.defaultProfileCount())
                .append(" [")
                .append(CliText.commaSeparated(report.defaultProfileIds()))
                .append("]")
                .append('\n');
        output.append("productionProfiles: ")
                .append(report.productionProfileCount())
                .append(" [")
                .append(CliText.commaSeparated(report.productionProfileIds()))
                .append("]")
                .append('\n');
        output.append("knownReadinessIds: ").append(CliText.commaSeparated(report.knownReadinessIds())).append('\n');
        output.append("coveredReadinessIds: ").append(CliText.commaSeparated(report.coveredReadinessIds())).append('\n');
        output.append("uncoveredReadinessIds: ").append(CliText.commaSeparated(report.uncoveredReadinessIds())).append('\n');
        if (!report.issues().isEmpty()) {
            output.append('\n').append("Issues").append('\n');
            for (WayangPlatformReadinessProfileValidationIssue issue : report.issues()) {
                output.append("  - ")
                        .append(issue.kind())
                        .append(": ")
                        .append(issue.message())
                        .append('\n');
            }
        }
        return output.toString();
    }

    static String validationPoliciesText(
            List<WayangPlatformReadinessProfileValidationPolicyDescriptor> policies) {
        StringBuilder output = new StringBuilder("Wayang readiness profile validation policies\n");
        List<WayangPlatformReadinessProfileValidationPolicyDescriptor> model = CliLists.copy(policies);
        output.append("totalPolicies: ").append(model.size()).append('\n');
        for (WayangPlatformReadinessProfileValidationPolicyDescriptor policy : model) {
            output.append('\n')
                    .append(policy.policyId())
                    .append('\n');
            output.append("  default: ").append(CliText.yesNo(policy.defaultPolicy())).append('\n');
            output.append("  strict: ").append(CliText.yesNo(policy.strict())).append('\n');
            output.append("  knownReadinessCount: ").append(policy.knownReadinessCount()).append('\n');
            output.append("  requireDefaultProfile: ").append(CliText.yesNo(policy.requireDefaultProfile())).append('\n');
            output.append("  requireProductionProfile: ")
                    .append(CliText.yesNo(policy.requireProductionProfile()))
                    .append('\n');
            output.append("  requireFullReadinessCoverage: ")
                    .append(CliText.yesNo(policy.requireFullReadinessCoverage()))
                    .append('\n');
            if (!policy.description().isBlank()) {
                output.append("  description: ").append(policy.description()).append('\n');
            }
        }
        return output.append('\n').toString();
    }

    static String registryResolutionText(WayangPlatformReadinessProfileRegistryResolution resolution) {
        StringBuilder output = new StringBuilder("Wayang readiness profile sources\n");
        output.append("valid: ").append(resolution.valid()).append('\n');
        output.append("activeSource: ")
                .append(resolution.activeSourceId())
                .append(" (")
                .append(resolution.activeSourceType())
                .append(")")
                .append('\n');
        output.append("fallbackUsed: ").append(CliText.yesNo(resolution.fallbackUsed())).append('\n');
        output.append("sources: ").append(resolution.sourceCount()).append('\n');
        output.append("profiles: ").append(resolution.totalProfiles()).append('\n');
        output.append("profileIds: ")
                .append(CliText.commaSeparated(resolution.profiles().stream()
                        .map(WayangPlatformReadinessProfileDescriptor::profileId)
                        .toList()))
                .append('\n');
        output.append("validationPolicy: ")
                .append(resolution.validation().validationPolicy().policyId())
                .append('\n');
        output.append('\n').append("Sources").append('\n');
        for (WayangPlatformReadinessProfileSourceStatus source : resolution.sources()) {
            output.append("  ")
                    .append(source.sourceId())
                    .append('\n');
            output.append("    type: ").append(source.sourceType()).append('\n');
            if (!source.location().isBlank()) {
                output.append("    location: ").append(source.location()).append('\n');
            }
            output.append("    selected: ").append(CliText.yesNo(source.selected())).append('\n');
            output.append("    fallback: ").append(CliText.yesNo(source.fallback())).append('\n');
            output.append("    available: ").append(CliText.yesNo(source.available())).append('\n');
            output.append("    valid: ").append(CliText.yesNo(source.valid())).append('\n');
            output.append("    profiles: ").append(source.profileCount()).append('\n');
            output.append("    issues: ").append(source.issueCount()).append('\n');
            if (!source.message().isBlank()) {
                output.append("    message: ").append(source.message()).append('\n');
            }
        }
        return output.append('\n').toString();
    }

    static String registryPreflightText(
            WayangPlatformReadinessProfileRegistryPreflightReport report) {
        StringBuilder output = new StringBuilder("Wayang readiness profile registry preflight\n");
        output.append("ready: ").append(CliText.yesNo(report.ready())).append('\n');
        output.append("issues: ").append(report.issueCount()).append('\n');
        output.append("warnings: ").append(report.warningCount()).append('\n');
        output.append("configValid: ").append(CliText.yesNo(report.configDiagnostics().valid())).append('\n');
        output.append("providerDiscoveryRequired: ")
                .append(CliText.yesNo(report.providerDiscovery().required()))
                .append('\n');
        output.append("providerDiscoveryReady: ")
                .append(CliText.yesNo(report.providerDiscovery().ready()))
                .append('\n');
        CliText.appendListLine(output, "requiredReaderTypes", report.providerDiscovery().requiredReaderTypes());
        CliText.appendListLine(output, "missingRequiredReaderTypes",
                report.providerDiscovery().missingRequiredReaderTypes());
        output.append("registryReady: ").append(CliText.yesNo(report.registryReady())).append('\n');
        output.append("activeSource: ")
                .append(report.registryResolution().activeSourceId())
                .append(" (")
                .append(report.registryResolution().activeSourceType())
                .append(")")
                .append('\n');
        output.append("fallbackToBuiltIn: ").append(CliText.yesNo(report.fallbackToBuiltIn())).append('\n');
        output.append("fallbackUsed: ").append(CliText.yesNo(report.fallbackUsed())).append('\n');
        output.append("profiles: ").append(report.registryResolution().totalProfiles()).append('\n');
        if (!report.message().isBlank()) {
            output.append("message: ").append(report.message()).append('\n');
        }
        appendIssueBlock(output, "Issues", report.issues());
        appendIssueBlock(output, "Warnings", report.warnings());
        return output.append('\n').toString();
    }

    static String externalReaderProviderDiscoveryText(
            WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport report) {
        StringBuilder output = new StringBuilder("Wayang readiness profile external reader providers\n");
        output.append("ready: ").append(CliText.yesNo(report.ready())).append('\n');
        output.append("required: ").append(CliText.yesNo(report.required())).append('\n');
        output.append("providers: ").append(report.providerCount()).append('\n');
        output.append("availableProviders: ").append(report.availableProviderCount()).append('\n');
        CliText.appendListLine(output, "requiredReaderTypes", report.requiredReaderTypes());
        CliText.appendListLine(output, "availableReaderTypes", report.availableReaderTypes());
        CliText.appendListLine(output, "missingRequiredReaderTypes", report.missingRequiredReaderTypes());
        if (!report.message().isBlank()) {
            output.append("message: ").append(report.message()).append('\n');
        }
        if (!report.providers().isEmpty()) {
            output.append('\n').append("Providers").append('\n');
            for (WayangPlatformReadinessProfileExternalReaderProviderDiagnostics provider : report.providers()) {
                output.append("  ")
                        .append(provider.providerId())
                        .append('\n');
                if (!provider.providerClass().isBlank()) {
                    output.append("    class: ").append(provider.providerClass()).append('\n');
                }
                output.append("    priority: ").append(provider.priority()).append('\n');
                output.append("    available: ").append(CliText.yesNo(provider.available())).append('\n');
                CliText.appendIndentedListLine(output, "readerTypes", provider.readerTypes());
                if (!provider.message().isBlank()) {
                    output.append("    message: ").append(provider.message()).append('\n');
                }
            }
        }
        return output.append('\n').toString();
    }

    private static void appendIssueBlock(
            StringBuilder output,
            String heading,
            List<Map<String, Object>> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }
        output.append('\n').append(heading).append('\n');
        for (Map<String, Object> issue : issues) {
            output.append("  - ")
                    .append(issue.getOrDefault("code", "issue"))
                    .append(": ")
                    .append(issue.getOrDefault("message", ""))
                    .append('\n');
        }
    }

    static String registryConfigDiagnosticsText(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics) {
        WayangPlatformReadinessProfileRegistryConfig config = diagnostics.config();
        Map<String, Object> redactedConfig = config.toMap();
        StringBuilder output = new StringBuilder("Wayang readiness profile registry config\n");
        output.append("valid: ").append(diagnostics.valid()).append('\n');
        output.append("issues: ").append(diagnostics.issueCount()).append('\n');
        output.append("mode: ").append(config.mode().name().toLowerCase()).append('\n');
        output.append("fallbackToBuiltIn: ").append(CliText.yesNo(config.fallbackToBuiltIn())).append('\n');
        output.append("validationPolicy: ").append(config.validationPolicyId()).append('\n');
        if (!config.filePath().isBlank()) {
            output.append("filePath: ").append(config.filePath()).append('\n');
        }
        if (!config.databaseUrl().isBlank()) {
            output.append("databaseUrl: ").append(redactedConfig.get("databaseUrl")).append('\n');
        }
        if (config.objectStorage().configured()) {
            output.append("objectStorage: ")
                    .append(config.objectStorage().provider())
                    .append('\n');
            if (!config.objectStorage().endpoint().isBlank()) {
                output.append("  endpoint: ").append(config.objectStorage().endpoint()).append('\n');
            }
            if (!config.objectStorage().bucket().isBlank()) {
                output.append("  bucket: ").append(config.objectStorage().bucket()).append('\n');
            }
            if (!config.objectStorage().keyPrefix().isBlank()) {
                output.append("  keyPrefix: ").append(config.objectStorage().keyPrefix()).append('\n');
            }
            output.append("  pathStyleAccess: ")
                    .append(CliText.yesNo(config.objectStorage().pathStyleAccess()))
                    .append('\n');
        }
        if (!diagnostics.issues().isEmpty()) {
            output.append('\n').append("Issues").append('\n');
            for (WayangPlatformReadinessProfileRegistryConfigIssue issue : diagnostics.issues()) {
                output.append("  - ")
                        .append(issue.code())
                        .append(" [")
                        .append(issue.field())
                        .append("]: ")
                        .append(issue.message())
                        .append('\n');
            }
        }
        return output.append('\n').toString();
    }

    private static void appendProfile(StringBuilder output, WayangPlatformReadinessProfileDescriptor profile) {
        output.append('\n')
                .append(profile.profileId())
                .append('\n');
        if (!profile.description().isBlank()) {
            output.append("  description: ").append(profile.description()).append('\n');
        }
        output.append("  default: ").append(CliText.yesNo(profile.defaultProfile())).append('\n');
        output.append("  production: ").append(CliText.yesNo(profile.productionProfile())).append('\n');
        output.append("  components: ").append(profile.componentCount()).append('\n');
        output.append("  readiness: ").append(CliText.commaSeparated(profile.readinessIds())).append('\n');
    }
}
