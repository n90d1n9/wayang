package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WayangProviderApi;
import tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityDiscovery;
import tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityQuery;

import java.util.concurrent.Callable;

final class WayangProviderCapabilityCommands {

    private WayangProviderCapabilityCommands() {
    }

    @Command(
            name = "providers",
            aliases = "provider-capabilities",
            description = "Discover provider capabilities across skills, MCP, RAG, storage, standards, and commerce.",
            mixinStandardHelpOptions = true,
            subcommands = {
                    ProvidersCommand.ListCommand.class,
                    ProvidersCommand.InspectCommand.class,
                    ProvidersCommand.SearchCommand.class
            })
    static final class ProvidersCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = "--json", description = "Render provider capabilities as compact JSON.")
        boolean json;

        @Mixin
        WayangProviderCapabilityQueryOptions query = new WayangProviderCapabilityQueryOptions();

        @Override
        public Integer call() {
            return renderList(query, "", json);
        }

        private Integer renderList(WayangProviderCapabilityQueryOptions options, String search, boolean json) {
            try {
                WayangCliContext context = parent.context();
                WayangProviderCapabilityQuery capabilityQuery = options.toQuery(null);
                WayangClient client = context.client();
                WayangProviderApi providers = client.providers();
                WayangProviderCapabilityDiscovery discovery = providers.discover(capabilityQuery, search);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> providers.discoveryJson(discovery),
                        () -> WayangProviderCapabilityTextFormat.text(client.productName(), discovery));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }

        @Command(name = "list", description = "List provider capabilities.")
        static final class ListCommand implements Callable<Integer> {
            @ParentCommand
            ProvidersCommand parent;

            @Option(names = "--json", description = "Render provider capabilities as compact JSON.")
            boolean json;

            @Mixin
            WayangProviderCapabilityQueryOptions query = new WayangProviderCapabilityQueryOptions();

            @Override
            public Integer call() {
                return parent.renderList(query, "", json);
            }
        }

        @Command(name = "inspect", description = "Show one provider capability by id.")
        static final class InspectCommand implements Callable<Integer> {
            @ParentCommand
            ProvidersCommand parent;

            @Parameters(index = "0", description = "Provider capability id to inspect.")
            String capabilityId;

            @Option(names = "--json", description = "Render provider capability as compact JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.parent.context();
                    WayangClient client = context.client();
                    WayangProviderApi providers = client.providers();
                    WayangProviderCapabilityDescriptor capability = providers.get(capabilityId);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> providers.detailJson(capability),
                            () -> WayangProviderCapabilityTextFormat.detailText(client.productName(), capability));
                    return 0;
                } catch (RuntimeException e) {
                    return parent.parent.context().commandFailure(e);
                }
            }
        }

        @Command(name = "search", description = "Search provider capabilities.")
        static final class SearchCommand implements Callable<Integer> {
            @ParentCommand
            ProvidersCommand parent;

            @Parameters(index = "0", description = "Search term matched against provider capability metadata.")
            String term;

            @Option(names = "--json", description = "Render matching provider capabilities as compact JSON.")
            boolean json;

            @Mixin
            WayangProviderCapabilityQueryOptions query = new WayangProviderCapabilityQueryOptions();

            @Override
            public Integer call() {
                return parent.renderList(query, term, json);
            }
        }
    }
}
