package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import tech.kayys.wayang.gollek.sdk.AgentRunPreview;
import tech.kayys.wayang.gollek.sdk.WayangProductCatalog;
import tech.kayys.wayang.gollek.sdk.WayangSpecApi;

import java.util.concurrent.Callable;

/**
 * CLI command module for portable Wayang run specs.
 *
 * <p>The module handles command parsing and text output while delegating spec
 * templates, validation, JSON envelopes, and persistence to {@link WayangSpecApi}.</p>
 */
final class WayangSpecCommands {

    private WayangSpecCommands() {
    }

    @Command(
            name = "spec",
            description = "Validate and template portable Wayang run specs.",
            mixinStandardHelpOptions = true,
            subcommands = {
                    SpecCommand.ValidateCommand.class,
                    SpecCommand.TemplateCommand.class
            })
    static final class SpecCommand implements Runnable {
        @ParentCommand
        WayangGollekCli parent;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }

        WayangCliContext context() {
            return parent.context();
        }

        @Command(name = "validate", description = "Validate a run spec without submitting a run.")
        static final class ValidateCommand implements Callable<Integer> {
            @ParentCommand
            SpecCommand parent;

            @Option(names = {"-p", "--path"}, required = true, description = "Run spec path.")
            String path;

            @Option(names = "--json", description = "Render validation as compact JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangSpecApi specs = context.client().specs();
                    AgentRunPreview preview = specs.validate(path);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> specs.validationJson(path, preview),
                            () -> WayangSpecTextFormat.validationText(path, preview));
                    return preview.ready() ? 0 : 1;
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }
        }

        @Command(name = "template", description = "Print a starter run spec for a product surface.")
        static final class TemplateCommand implements Callable<Integer> {
            @ParentCommand
            SpecCommand parent;

            @Option(
                    names = "--surface",
                    description = "Product surface id for the template.",
                    defaultValue = WayangProductCatalog.DEFAULT_SURFACE_ID)
            String surfaceId;

            @Option(names = "--profile", description = "Product profile id for the template.")
            String profileId;

            @Option(
                    names = {"-o", "--output"},
                    paramLabel = "<path>",
                    description = "Write template to a UTF-8 file instead of stdout.")
            String outputPath;

            @Option(names = "--force", description = "Allow --output to overwrite an existing file.")
            boolean forceOutput;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangSpecApi specs = context.client().specs();
                    WayangCliOutputTarget output = WayangCliOutputTarget.of(outputPath, forceOutput);
                    String template = profileId == null || profileId.isBlank()
                            ? specs.templateProperties(surfaceId)
                            : specs.profileTemplateProperties(profileId);
                    output.writeOrPrint(context.out(), specs::writeProperties, "Wayang run spec template", template);
                    return 0;
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }
        }
    }
}
