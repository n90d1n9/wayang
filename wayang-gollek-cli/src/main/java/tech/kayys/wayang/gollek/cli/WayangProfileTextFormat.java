package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.ProductProfile;

import java.util.List;

/**
 * Text renderer for product profile catalog and detail responses shown by the Wayang CLI.
 */
final class WayangProfileTextFormat {

    private WayangProfileTextFormat() {
    }

    static String text(String productName, String surfaceId, List<ProductProfile> profiles) {
        StringBuilder output = new StringBuilder("Wayang product profiles\n");
        output.append("product: ").append(productName).append('\n');
        if (surfaceId != null && !surfaceId.isBlank()) {
            output.append("surface: ").append(surfaceId).append('\n');
        }
        output.append("totalProfiles: ").append(CliLists.copy(profiles).size()).append('\n');
        for (ProductProfile profile : CliLists.copy(profiles)) {
            appendProfile(output, profile);
        }
        return output.append('\n').toString();
    }

    static String detailText(String productName, ProductProfile profile) {
        StringBuilder output = new StringBuilder("Wayang product profile\n");
        output.append("product: ").append(productName).append('\n');
        appendProfile(output, profile);
        return output.append('\n').toString();
    }

    private static void appendProfile(StringBuilder output, ProductProfile profile) {
        output.append('\n')
                .append(profile.name())
                .append(" (")
                .append(profile.id())
                .append(")")
                .append('\n');
        output.append("  surface: ").append(profile.surfaceId()).append('\n');
        if (!profile.description().isBlank()) {
            output.append("  description: ").append(profile.description()).append('\n');
        }
        output.append("  starter: ").append(profile.starterPrompt()).append('\n');
        if (!profile.workflowId().isBlank()) {
            output.append("  workflow: ").append(profile.workflowId()).append('\n');
        }
        output.append("  skills: ").append(CliText.commaSeparated(profile.skills())).append('\n');
        output.append("  defaults: ").append(defaults(profile)).append('\n');
        if (!profile.context().isEmpty()) {
            output.append("  context: ").append(CliText.inlineKeyValueMap(profile.context())).append('\n');
        }
        if (!profile.notes().isEmpty()) {
            output.append("  notes: ").append(String.join(" ", profile.notes())).append('\n');
        }
    }

    private static String defaults(ProductProfile profile) {
        StringBuilder defaults = new StringBuilder();
        CliText.appendCommaSeparatedTokenIf(defaults, "memory", profile.memoryEnabled());
        CliText.appendCommaSeparatedTokenIf(defaults, "workspace", profile.workspaceEnabled());
        CliText.appendCommaSeparatedTokenIf(defaults, "harness", profile.harnessEnabled());
        CliText.appendCommaSeparatedTokenIf(defaults, "require-ready", profile.requireReady());
        CliText.appendCommaSeparatedToken(defaults, "max-steps=" + profile.maxSteps());
        return defaults.toString();
    }
}
