package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WayangStandardCatalog;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardsApi;

import java.util.concurrent.Callable;

final class WayangStandardsCommands {

    private WayangStandardsCommands() {
    }

    @Command(
            name = "standards",
            aliases = {"standard-alignment", "alignment"},
            description = "Show SDK standard-alignment health for protocol adapter reports.")
    static final class StandardsCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Mixin
        WayangStandardPolicyOptions policyOptions = new WayangStandardPolicyOptions();

        @Option(names = "--json", description = "Render the selected standards view as compact JSON.")
        boolean json;

        @Option(
                names = {"--catalog", "--registry"},
                description = "Render the known standards registry instead of readiness health.")
        boolean catalog;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangClient client = context.client();
                WayangStandardsApi standards = client.standards();
                String productName = client.productName();
                if (catalog) {
                    WayangStandardCatalog standardsCatalog = standards.catalog();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> standards.catalogJson(standardsCatalog),
                            () -> WayangStandardCatalogTextFormat.text(productName, standardsCatalog));
                    return 0;
                }
                WayangStandardAlignmentHealthReport health =
                        standards.health(policyOptions.toConfig());
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> standards.healthJson(health),
                        () -> WayangStandardAlignmentHealthTextFormat.text(productName, health));
                return health.ready() ? 0 : 1;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }
}
