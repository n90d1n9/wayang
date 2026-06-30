package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.gollek.sdk.AgentSkillDiscovery;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.RegisteredSkill;
import tech.kayys.wayang.gollek.sdk.WayangSkillApi;

import java.util.concurrent.Callable;

final class WayangSkillCommands {

    private WayangSkillCommands() {
    }

    @Command(
            name = "skills",
            aliases = "capabilities",
            description = "Discover SDK-owned Wayang skill capabilities.",
            mixinStandardHelpOptions = true,
            subcommands = {
                    SkillsCommand.ListCommand.class,
                    SkillsCommand.InspectCommand.class,
                    SkillsCommand.SearchCommand.class
            })
    static final class SkillsCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = "--json", description = "Render skills as compact JSON.")
        boolean json;

        @Mixin
        WayangSkillQueryOptions query = new WayangSkillQueryOptions();

        @Override
        public Integer call() {
            return renderList(query, "", json);
        }

        private Integer renderList(WayangSkillQueryOptions options, String search, boolean json) {
            try {
                WayangCliContext context = parent.context();
                AgentSkillQuery skillQuery = options.toQuery(null);
                WayangClient client = context.client();
                WayangSkillApi skills = client.skills();
                AgentSkillDiscovery discovery = skills.discover(skillQuery, search);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> skills.discoveryJson(discovery),
                        () -> WayangSkillTextFormat.text(client.productName(), discovery));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }

        @Command(name = "list", description = "List SDK-owned Wayang skill capabilities.")
        static final class ListCommand implements Callable<Integer> {
            @ParentCommand
            SkillsCommand parent;

            @Option(names = "--json", description = "Render skills as compact JSON.")
            boolean json;

            @Mixin
            WayangSkillQueryOptions query = new WayangSkillQueryOptions();

            @Override
            public Integer call() {
                return parent.renderList(query, "", json);
            }
        }

        @Command(name = "inspect", description = "Show one skill capability contract by id or alias.")
        static final class InspectCommand implements Callable<Integer> {
            @ParentCommand
            SkillsCommand parent;

            @Parameters(index = "0", description = "Skill id or alias to inspect.")
            String skillId;

            @Option(names = "--json", description = "Render skill as compact JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.parent.context();
                    WayangClient client = context.client();
                    WayangSkillApi skills = client.skills();
                    RegisteredSkill skill = skills.get(skillId);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> skills.detailJson(skill),
                            () -> WayangSkillTextFormat.detailText(client.productName(), skill));
                    return 0;
                } catch (RuntimeException e) {
                    return parent.parent.context().commandFailure(e);
                }
            }
        }

        @Command(name = "search", description = "Search SDK-owned Wayang skill capabilities.")
        static final class SearchCommand implements Callable<Integer> {
            @ParentCommand
            SkillsCommand parent;

            @Parameters(index = "0", description = "Search term matched against id, name, description, tags, source, or aliases.")
            String term;

            @Option(names = "--json", description = "Render matching skills as compact JSON.")
            boolean json;

            @Mixin
            WayangSkillQueryOptions query = new WayangSkillQueryOptions();

            @Override
            public Integer call() {
                return parent.renderList(query, term, json);
            }
        }
    }
}
