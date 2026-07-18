package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.ProductProfile;
import tech.kayys.wayang.gollek.sdk.ProductSurface;
import tech.kayys.wayang.gollek.sdk.ProductSurfacePolicy;

import java.util.List;
import java.util.Map;

/**
 * Text renderer for product surface and profile catalog responses shown by the Wayang CLI.
 */
final class WayangProductTextFormat {

    private static final String NL = System.lineSeparator();

    private WayangProductTextFormat() {
    }

    static String text(List<ProductSurface> surfaces) {
        return text(surfaces, List.of());
    }

    static String text(List<ProductSurface> surfaces, List<ProductSurfacePolicy> policies) {
        return text(surfaces, policies, List.of());
    }

    static String text(
            List<ProductSurface> surfaces,
            List<ProductSurfacePolicy> policies,
            List<ProductProfile> profiles) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang product surfaces").append(NL);
        output.append("Core engine: agents, skills, tools, MCP, RAG, memory, workflows, and harness checks.")
                .append(NL)
                .append(NL);
        Map<String, ProductSurfacePolicy> policyBySurface = WayangProductCatalogSupport.policiesBySurface(policies);
        Map<String, List<ProductProfile>> profilesBySurface = WayangProductCatalogSupport.profilesBySurface(profiles);
        for (ProductSurface surface : CliLists.copy(surfaces)) {
            output.append(surface.name())
                    .append(" (")
                    .append(surface.id())
                    .append(")")
                    .append(NL);
            output.append("  role: ").append(surface.role()).append(NL);
            output.append("  engine: ").append(CliText.commaSeparated(surface.engineCapabilities())).append(NL);
            output.append("  adapters: ").append(CliText.commaSeparated(surface.adapterBoundaries())).append(NL);
            ProductSurfacePolicy policy = policyBySurface.get(surface.id());
            if (policy != null) {
                output.append("  policy: ").append(policySummary(policy)).append(NL);
                output.append("  suggested skills: ").append(CliText.commaSeparated(policy.suggestedSkills())).append(NL);
                output.append("  routing: ").append(CliText.commaSeparated(policy.routingHints())).append(NL);
            }
            List<ProductProfile> surfaceProfiles = profilesBySurface.getOrDefault(surface.id(), List.of());
            if (!surfaceProfiles.isEmpty()) {
                output.append("  profiles: ")
                        .append(CliText.commaSeparated(WayangProductCatalogSupport.profileIds(surfaceProfiles)))
                        .append(NL);
            }
        }
        if (!CliLists.copy(profiles).isEmpty()) {
            output.append(NL).append("Wayang product profiles").append(NL);
            for (ProductProfile profile : CliLists.copy(profiles)) {
                output.append(profile.name())
                        .append(" (")
                        .append(profile.id())
                        .append(")")
                        .append(NL);
                output.append("  surface: ").append(profile.surfaceId()).append(NL);
                output.append("  skills: ").append(CliText.commaSeparated(profile.skills())).append(NL);
                output.append("  defaults: ").append(profileDefaults(profile)).append(NL);
                if (!profile.notes().isEmpty()) {
                    output.append("  notes: ").append(String.join(" ", profile.notes())).append(NL);
                }
            }
        }
        return output.toString();
    }

    private static String policySummary(ProductSurfacePolicy policy) {
        StringBuilder summary = new StringBuilder();
        CliText.appendCommaSeparatedTokenIf(summary, "memory", policy.memoryPreferred());
        CliText.appendCommaSeparatedTokenIf(summary, "workspace", policy.workspacePreferred());
        CliText.appendCommaSeparatedTokenIf(summary, "harness", policy.harnessPreferred());
        CliText.appendCommaSeparatedTokenIf(summary, "workflow", policy.workflowPreferred());
        return summary.length() == 0 ? "explicit-only" : summary.toString();
    }

    private static String profileDefaults(ProductProfile profile) {
        StringBuilder defaults = new StringBuilder();
        CliText.appendCommaSeparatedTokenIf(defaults, "memory", profile.memoryEnabled());
        CliText.appendCommaSeparatedTokenIf(defaults, "workspace", profile.workspaceEnabled());
        CliText.appendCommaSeparatedTokenIf(defaults, "harness", profile.harnessEnabled());
        CliText.appendCommaSeparatedTokenIf(defaults, "require-ready", profile.requireReady());
        if (!profile.workflowId().isBlank()) {
            CliText.appendCommaSeparatedToken(defaults, "workflow=" + profile.workflowId());
        }
        CliText.appendCommaSeparatedToken(defaults, "max-steps=" + profile.maxSteps());
        return defaults.toString();
    }
}
