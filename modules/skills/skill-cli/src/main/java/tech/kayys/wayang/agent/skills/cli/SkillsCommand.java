package tech.kayys.wayang.agent.skills.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
        name = "skills",
        description = "Manage Wayang agent skill definitions.",
        mixinStandardHelpOptions = true,
        subcommands = {
                SkillsCommand.RegisterCommand.class,
                SkillsCommand.ListCommand.class,
                SkillsCommand.InfoCommand.class,
                SkillsCommand.ProfilesCommand.class,
                SkillsCommand.ProfileCommand.class,
                SkillsCommand.ConfigCommand.class,
                SkillsCommand.StatusCommand.class,
                SkillsCommand.ValidateCommand.class,
                SkillsCommand.EnableCommand.class,
                SkillsCommand.DisableCommand.class
        })
public class SkillsCommand implements Runnable {

    private final SkillsCommandHandler handler;

    public SkillsCommand() {
        this(SkillsCommandHandler.inMemory(System.out, System.err));
    }

    public SkillsCommand(SkillsCommandHandler handler) {
        this.handler = handler;
    }

    public static int execute(String... args) {
        return new CommandLine(new SkillsCommand()).execute(args);
    }

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    @Command(name = "register", description = "Register a data-driven skill definition.")
    static final class RegisterCommand implements Callable<Integer> {
        @ParentCommand
        SkillsCommand parent;

        @Parameters(index = "0", description = "Skill id.")
        String id;

        @Option(names = {"-n", "--name"}, description = "Display name.")
        String name;

        @Option(names = {"-d", "--description"}, description = "Description.")
        String description;

        @Option(names = {"-c", "--category"}, description = "Skill category.", defaultValue = "custom")
        String category;

        @Option(names = {"-p", "--system-prompt"}, description = "System prompt.", required = true)
        String systemPrompt;

        @Override
        public Integer call() {
            return parent.handler.register(SkillsDefinitionRequest.fromOptions(
                    id,
                    name,
                    description,
                    category,
                    systemPrompt));
        }
    }

    @Command(name = "list", description = "List skill definitions.")
    static final class ListCommand implements Callable<Integer> {
        @ParentCommand
        SkillsCommand parent;

        @Option(names = {"-c", "--category"}, description = "Filter by category.")
        String category;

        @Option(names = {"--all"}, description = "Include disabled and deprecated skills.")
        boolean includeDisabled;

        @Override
        public Integer call() {
            return parent.handler.list(SkillsDefinitionListRequest.fromOptions(category, includeDisabled));
        }
    }

    @Command(name = "info", description = "Show a skill definition.")
    static final class InfoCommand implements Callable<Integer> {
        @ParentCommand
        SkillsCommand parent;

        @Parameters(index = "0", description = "Skill id.")
        String id;

        @Override
        public Integer call() {
            return parent.handler.info(SkillsDefinitionInfoRequest.fromOptions(id));
        }
    }

    @Command(name = "profiles", description = "List named skill-management persistence profiles.")
    static final class ProfilesCommand implements Callable<Integer> {
        @ParentCommand
        SkillsCommand parent;

        @Option(names = {"--json"}, description = "Render profile catalog as machine-readable JSON.")
        boolean json;

        @Override
        public Integer call() {
            return parent.handler.profiles(json);
        }
    }

    @Command(
            name = "config",
            description = "Explain skill-management runtime config.",
            mixinStandardHelpOptions = true,
            subcommands = {
                    ConfigCommand.ExplainCommand.class,
                    ConfigCommand.GroupsCommand.class,
                    ConfigCommand.ResolveCommand.class,
                    ConfigCommand.SamplesCommand.class,
                    ConfigCommand.SampleCommand.class,
                    ConfigCommand.ValidateCommand.class
            })
    static final class ConfigCommand implements Runnable {
        @ParentCommand
        SkillsCommand parent;

        @Override
        public void run() {
            new CommandLine(this).usage(System.out);
        }

        @Command(name = "explain", description = "Explain runtime config keys, environment names, and defaults.")
        static final class ExplainCommand implements Callable<Integer> {
            @ParentCommand
            ConfigCommand parent;

            @Option(names = {"--json"}, description = "Render runtime config hints as machine-readable JSON.")
            boolean json;

            @Option(names = {"--group"}, description = "Only render one hint group by name.")
            String group;

            @Override
            public Integer call() {
                return parent.parent.handler.configExplain(
                        SkillsConfigCatalogRequest.forGroup(group),
                        json);
            }
        }

        @Command(name = "groups", description = "List runtime config hint groups.")
        static final class GroupsCommand implements Callable<Integer> {
            @ParentCommand
            ConfigCommand parent;

            @Option(names = {"--json"}, description = "Render runtime config groups as machine-readable JSON.")
            boolean json;

            @Override
            public Integer call() {
                return parent.parent.handler.configGroups(json);
            }
        }

        @Command(name = "resolve", description = "Show effective resolved skill-management persistence config.")
        static final class ResolveCommand implements Callable<Integer> {
            @ParentCommand
            ConfigCommand parent;

            @Option(names = {"--profile"}, description = "Resolve a named persistence profile.")
            String profile;

            @Option(names = {"--runtime"}, description = "Resolve runtime properties/environment config.")
            boolean runtime;

            @Option(names = {"--json"}, description = "Render resolved config as machine-readable JSON.")
            boolean json;

            @Override
            public Integer call() {
                return parent.parent.handler.configResolve(
                        SkillsPersistenceConfigResolveRequest.fromOptions(profile, runtime),
                        json);
            }
        }

        @Command(name = "sample", description = "Render a starter runtime config for a persistence profile.")
        static final class SampleCommand implements Callable<Integer> {
            @ParentCommand
            ConfigCommand parent;

            @Parameters(index = "0", description = "Persistence profile name or sample alias. Use `config samples` to list choices.")
            String profile;

            @Option(
                    names = {"--format"},
                    defaultValue = "properties",
                    description = "Output format: properties or env.")
            String format;

            @Option(names = {"--json"}, description = "Render sample as machine-readable JSON.")
            boolean json;

            @Override
            public Integer call() {
                return parent.parent.handler.configSample(
                        SkillsConfigSampleRequest.fromOptions(profile, format),
                        json);
            }
        }

        @Command(name = "samples", description = "List starter runtime config samples and aliases.")
        static final class SamplesCommand implements Callable<Integer> {
            @ParentCommand
            ConfigCommand parent;

            @Option(names = {"--json"}, description = "Render runtime config sample catalog as machine-readable JSON.")
            boolean json;

            @Override
            public Integer call() {
                return parent.parent.handler.configSamples(json);
            }
        }

        @Command(name = "validate", description = "Validate resolved skill-management persistence config.")
        static final class ValidateCommand implements Callable<Integer> {
            @ParentCommand
            ConfigCommand parent;

            @Option(names = {"--profile"}, description = "Validate a named persistence profile.")
            String profile;

            @Option(names = {"--runtime"}, description = "Validate runtime properties/environment config.")
            boolean runtime;

            @Option(names = {"--require-durable"}, description = "Fail unless all persistence roles are durable.")
            boolean requireDurable;

            @Option(names = {"--json"}, description = "Render validation result as machine-readable JSON.")
            boolean json;

            @Override
            public Integer call() {
                return parent.parent.handler.configValidate(profile, runtime, requireDurable, json);
            }
        }
    }

    @Command(
            name = "profile",
            description = "Inspect one named skill-management persistence profile.",
            mixinStandardHelpOptions = true,
            subcommands = {
                    ProfileCommand.InspectCommand.class
            })
    static final class ProfileCommand implements Runnable {
        @ParentCommand
        SkillsCommand parent;

        @Override
        public void run() {
            new CommandLine(this).usage(System.out);
        }

        @Command(name = "inspect", description = "Inspect a named persistence profile.")
        static final class InspectCommand implements Callable<Integer> {
            @ParentCommand
            ProfileCommand parent;

            @Parameters(index = "0", description = "Persistence profile name or alias.")
            String profile;

            @Option(names = {"--preflight"}, description = "Include deployment preflight readiness.")
            boolean preflight;

            @Option(names = {"--diagnostics"}, description = "Include resolved persistence config diagnostics.")
            boolean diagnostics;

            @Option(names = {"--json"}, description = "Render profile inspection as machine-readable JSON.")
            boolean json;

            @Override
            public Integer call() {
                return parent.parent.handler.profileInspect(
                        SkillsPersistenceProfileInspectRequest.fromOptions(profile, preflight, diagnostics),
                        json);
            }
        }
    }

    @Command(name = "status", description = "Show skill-management persistence status.")
    static final class StatusCommand implements Callable<Integer> {
        @ParentCommand
        SkillsCommand parent;

        @Option(
                names = {"--profile"},
                description = "Preview a named persistence profile.")
        String profile;

        @Option(names = {"--runtime"}, description = "Read persistence config from runtime properties/environment.")
        boolean runtimeConfig;

        @Option(names = {"--preflight"}, description = "Include deployment preflight readiness.")
        boolean preflight;

        @Option(names = {"--diagnostics"}, description = "Include resolved persistence config diagnostics.")
        boolean diagnostics;

        @Option(names = {"--json"}, description = "Render persistence status as machine-readable JSON.")
        boolean json;

        @Override
        public Integer call() {
            return parent.handler.status(
                    SkillsPersistenceStatusRequest.fromOptions(
                            profile,
                            runtimeConfig,
                            preflight,
                            diagnostics),
                    json);
        }
    }

    @Command(name = "validate", description = "Validate a skill definition payload.")
    static final class ValidateCommand implements Callable<Integer> {
        @ParentCommand
        SkillsCommand parent;

        @Parameters(index = "0", description = "Skill id.")
        String id;

        @Option(names = {"-n", "--name"}, description = "Display name.")
        String name;

        @Option(names = {"-d", "--description"}, description = "Description.")
        String description;

        @Option(names = {"-c", "--category"}, description = "Skill category.", defaultValue = "custom")
        String category;

        @Option(names = {"-p", "--system-prompt"}, description = "System prompt.", required = true)
        String systemPrompt;

        @Override
        public Integer call() {
            return parent.handler.validate(SkillsDefinitionRequest.fromOptions(
                    id,
                    name,
                    description,
                    category,
                    systemPrompt));
        }
    }

    @Command(name = "enable", description = "Enable a skill definition.")
    static final class EnableCommand implements Callable<Integer> {
        @ParentCommand
        SkillsCommand parent;

        @Parameters(index = "0", description = "Skill id.")
        String id;

        @Override
        public Integer call() {
            return parent.handler.enable(SkillsLifecycleCommandRequest.enable(id));
        }
    }

    @Command(name = "disable", description = "Disable a skill definition.")
    static final class DisableCommand implements Callable<Integer> {
        @ParentCommand
        SkillsCommand parent;

        @Parameters(index = "0", description = "Skill id.")
        String id;

        @Override
        public Integer call() {
            return parent.handler.disable(SkillsLifecycleCommandRequest.disable(id));
        }
    }
}
