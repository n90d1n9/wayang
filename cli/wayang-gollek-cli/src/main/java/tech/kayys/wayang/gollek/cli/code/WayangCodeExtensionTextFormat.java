package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangCodeAgentExtension;
import tech.kayys.wayang.gollek.sdk.WayangCodeAgentExtensionDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangCodeAgentExtensionDiscovery;

import java.io.PrintStream;

/**
 * Text renderer for coding-agent extension diagnostics.
 */
final class WayangCodeExtensionTextFormat {

    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    private WayangCodeExtensionTextFormat() {
    }

    static void render(
            PrintStream out,
            boolean color,
            WayangCodeAgentExtensionDiscovery discovery) {
        WayangCodeAgentExtensionDiscovery model = discovery == null
                ? WayangCodeAgentExtensionDiscovery.empty()
                : discovery;
        out.println();
        out.println(color ? BOLD + "  Coding-agent extensions:" + RESET : "  Coding-agent extensions:");
        out.println("    discovered: " + model.discoveredCount());
        out.println("    active:     " + model.activeCount());
        if (model.diagnostics().isEmpty()) {
            renderEmpty(out);
            return;
        }
        out.println();
        for (WayangCodeAgentExtensionDiagnostics diagnostic : model.diagnostics()) {
            renderDiagnostic(out, diagnostic);
        }
        if (!model.slashCommandHints().isEmpty()) {
            out.println();
            out.println("    extension commands:");
            model.slashCommandHints().forEach(hint -> out.println("      " + hint));
        }
        out.println();
    }

    private static void renderEmpty(PrintStream out) {
        out.println("    none");
        out.println();
        out.println("    Pro/enterprise features can be added by placing jars with");
        out.println("    META-INF/services/" + WayangCodeAgentExtension.class.getName());
        out.println("    on the Wayang classpath.");
        out.println();
    }

    private static void renderDiagnostic(
            PrintStream out,
            WayangCodeAgentExtensionDiagnostics diagnostic) {
        String marker = diagnostic.available() ? "active" : "inactive";
        out.println("    " + diagnostic.extensionId()
                + " [" + marker + ", " + diagnostic.edition() + ", priority=" + diagnostic.priority() + "]");
        out.println("      class: " + diagnostic.extensionClass());
        if (!diagnostic.capabilityTags().isEmpty()) {
            out.println("      capabilities: " + String.join(", ", diagnostic.capabilityTags()));
        }
        if (!diagnostic.message().isBlank()) {
            out.println("      message: " + diagnostic.message());
        }
    }
}
