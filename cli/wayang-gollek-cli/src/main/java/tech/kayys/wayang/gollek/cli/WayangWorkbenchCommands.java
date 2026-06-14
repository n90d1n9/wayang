package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandDiscovery;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;

import java.util.concurrent.Callable;

final class WayangWorkbenchCommands {

    private WayangWorkbenchCommands() {
    }

    @Command(name = "commands", aliases = "actions", description = "List SDK-owned workbench commands for agent shells.")
    static final class CommandsCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Mixin
        WayangCommandQueryOptions queryOptions = new WayangCommandQueryOptions();

        @Option(names = "--index", description = "Render only discovery metadata, categories, and command ids.")
        boolean index;

        @Option(names = "--json", description = "Render commands as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangClient client = context.client();
                WorkbenchCommandQuery query = queryOptions.toQuery();
                WorkbenchCommandDiscovery discovery = client.commands().discover(query);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> index
                                ? client.commands().indexJson(discovery)
                                : client.commands().discoveryJson(discovery),
                        () -> index
                                ? WayangCommandTextFormat.indexText(client.productName(), discovery)
                                : WayangCommandTextFormat.text(query, discovery.commands()));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }

    @Command(
            name = "workbench",
            aliases = "dashboard",
            description = "Render the SDK-owned agent workbench model without opening a TUI.")
    static final class WorkbenchCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = "--json", description = "Render workbench model as compact JSON.")
        boolean json;

        @Mixin
        WayangCommandQueryOptions queryOptions = new WayangCommandQueryOptions();

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangClient client = context.client();
                WorkbenchCommandQuery query = queryOptions.toQuery();
                WayangWorkbenchModel workbench = client.commands().workbench(query);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> client.commands().workbenchJson(workbench, query),
                        () -> new PlainWorkbenchRenderer().render(workbench));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }
}
