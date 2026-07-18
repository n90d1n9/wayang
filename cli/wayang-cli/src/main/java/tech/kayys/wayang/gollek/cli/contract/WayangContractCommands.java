package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WayangContractApi;
import tech.kayys.wayang.gollek.sdk.WayangContractDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangContractDiscovery;
import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverageReport;
import tech.kayys.wayang.gollek.sdk.WayangContractIntegrityReport;
import tech.kayys.wayang.gollek.sdk.WayangContractQuery;

import java.util.concurrent.Callable;

final class WayangContractCommands {

    private WayangContractCommands() {
    }

    @Command(name = "contracts", aliases = "schemas", description = "List SDK-owned JSON contracts for product shells.")
    static final class ContractsCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Mixin
        WayangContractQueryOptions queryOptions = new WayangContractQueryOptions();

        @Option(names = "--json", description = "Render contract catalog as compact JSON.")
        boolean json;

        @Option(names = "--index", description = "Render only discovery metadata, facets, and command ids.")
        boolean index;

        @Option(names = "--schema-json", description = "Render JSON Schema for one matching contract envelope.")
        boolean schemaJson;

        @Option(names = "--schema-bundle-json", description = "Render JSON Schema documents for all matching contracts.")
        boolean schemaBundleJson;

        @Option(names = {"--check", "--validate"}, description = "Validate command/contract catalog links.")
        boolean check;

        @Option(names = "--coverage", description = "Render command coverage for all SDK-owned JSON contracts.")
        boolean coverage;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangClient client = context.client();
                WayangContractApi contracts = client.contracts();
                String productName = client.productName();
                if (check && coverage) {
                    throw new IllegalArgumentException(
                            "Use only one of --check or --coverage for contract diagnostics.");
                }
                if (check) {
                    WayangContractIntegrityReport report = contracts.integrity();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> contracts.integrityJson(report),
                            () -> WayangContractIntegrityTextFormat.text(productName, report));
                    return report.valid() ? 0 : 1;
                }
                if (coverage) {
                    WayangContractCommandCoverageReport report = contracts.coverage();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> contracts.coverageJson(report),
                            () -> WayangContractCoverageTextFormat.text(productName, report));
                    return report.incompleteContracts() == 0 ? 0 : 1;
                }
                WayangContractQuery query = queryOptions.toQuery();
                WayangContractDiscovery discovery = contracts.discover(query);
                if (schemaJson && schemaBundleJson) {
                    throw new IllegalArgumentException(
                            "Use only one of --schema-json or --schema-bundle-json for contract schema export.");
                }
                if (schemaJson) {
                    context.out().println(contracts.schemaJson(singleContractForSchema(discovery)));
                    return 0;
                }
                if (schemaBundleJson) {
                    context.out().println(contracts.schemaBundleJson(discovery));
                    return 0;
                }
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> index
                                ? contracts.indexJson(discovery)
                                : contracts.catalogJson(discovery),
                        () -> index
                                ? WayangContractCatalogTextFormat.indexText(productName, discovery)
                                : WayangContractCatalogTextFormat.text(productName, discovery));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }

        private static WayangContractDescriptor singleContractForSchema(WayangContractDiscovery discovery) {
            int matchingContracts = discovery == null ? 0 : discovery.matchingContracts();
            if (matchingContracts != 1) {
                throw new IllegalArgumentException("--schema-json requires exactly one matching contract; found "
                        + matchingContracts
                        + ". Add --schema, --envelope, --command-id, --domain, or --json-schema-id to narrow the contract query.");
            }
            return discovery.contracts().get(0);
        }
    }
}
