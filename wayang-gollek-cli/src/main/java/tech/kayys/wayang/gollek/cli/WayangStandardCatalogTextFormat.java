package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangStandardCatalog;
import tech.kayys.wayang.gollek.sdk.WayangStandardCatalogEnvelopes;
import tech.kayys.wayang.gollek.sdk.WayangStandardDefinition;

/**
 * Text renderer for standards catalog responses shown by the Wayang CLI.
 */
final class WayangStandardCatalogTextFormat {

    private static final String NL = System.lineSeparator();

    private WayangStandardCatalogTextFormat() {
    }

    static String text(String productName, WayangStandardCatalog catalog) {
        WayangStandardCatalog model = WayangStandardCatalogEnvelopes.normalize(catalog);
        StringBuilder output = new StringBuilder("Wayang standards catalog").append(NL);
        output.append("product: ").append(productName).append(NL);
        output.append("standards: ").append(model.totalStandards()).append(NL);
        output.append("standardIds: ").append(model.standardIds()).append(NL);
        output.append("versions: ").append(model.versions()).append(NL);
        output.append("bindings: ").append(model.bindings()).append(NL);
        output.append("bindingCounts: ").append(model.bindingCounts()).append(NL);
        for (WayangStandardDefinition standard : model.standards()) {
            appendStandard(output, standard);
        }
        return output.toString();
    }

    private static void appendStandard(StringBuilder output, WayangStandardDefinition standard) {
        output.append("- ")
                .append(standard.standardId())
                .append(" ")
                .append(standard.name())
                .append(" ")
                .append(standard.version())
                .append(" [")
                .append(standard.binding())
                .append("]")
                .append(NL);
        output.append("  specUrl: ").append(standard.specUrl()).append(NL);
        output.append("  aliases: ").append(standard.aliases()).append(NL);
        if (!standard.attributes().isEmpty()) {
            output.append("  attributes: ").append(standard.attributes()).append(NL);
        }
    }

}
