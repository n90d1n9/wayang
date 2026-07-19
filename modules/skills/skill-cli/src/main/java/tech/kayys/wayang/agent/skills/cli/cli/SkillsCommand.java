package tech.kayys.wayang.agent.skills.cli.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Picocli command for {@code wayang skills} subcommand.
 *
 * <p>
 * Provides CLI operations to manage agent skills — install, list, update, and remove.
 *
 * <h3>Usage:</h3>
 *
 * <pre>
 * wayang skills list
 * wayang skills add ./my-local-skill-dir
 * wayang skills remove skill-name
 * wayang skills info skill-name
 * </pre>
 *
 * @author Bhangun
 */
@Command(name = "skills", description = "Manage agent skills — install, list, update, and remove skill packs",
        mixinStandardHelpOptions = true, subcommands = {
        SkillsCommand.AddCommand.class,
        SkillsCommand.RemoveCommand.class,
        SkillsCommand.ListCommand.class,
        SkillsCommand.UpdateCommand.class,
        SkillsCommand.InfoCommand.class
})
public class SkillsCommand implements Runnable {

    @Override
    public void run() {
        // No subcommand specified — show help
        new picocli.CommandLine(this).usage(System.out);
    }

    // ── Subcommands ───────────────────────────────────────────────

    @Command(name = "add", description = "Install skills from a local directory or identifier",
            mixinStandardHelpOptions = true)
    static class AddCommand implements Runnable {

        @Parameters(index = "0", description = "Local directory path or skill identifier")
        String source;

        @Option(names = {"--skill", "-s"}, description = "Filter for specific skill name")
        String skillFilter;

        @Override
        public void run() {
            System.out.println("Installing skill: " + source + (skillFilter != null ? " (filter: " + skillFilter + ")" : ""));
            // TODO: integrate with SkillManagementService
        }
    }

    @Command(name = "remove", aliases = {"rm"}, description = "Remove an installed skill",
            mixinStandardHelpOptions = true)
    static class RemoveCommand implements Runnable {

        @Parameters(index = "0", description = "Name of the skill to remove")
        String skillName;

        @Override
        public void run() {
            System.out.println("Removing skill: " + skillName);
            // TODO: integrate with SkillManagementService
        }
    }

    @Command(name = "list", aliases = {"ls"}, description = "List all installed skills",
            mixinStandardHelpOptions = true)
    static class ListCommand implements Runnable {

        @Override
        public void run() {
            System.out.println("Listing skills...");
            // TODO: integrate with SkillManagementService
        }
    }

    @Command(name = "update", aliases = {"up"}, description = "Update installed skill repos",
            mixinStandardHelpOptions = true)
    static class UpdateCommand implements Runnable {

        @Parameters(index = "0", arity = "0..1", description = "Specific skill to update (default: all)")
        String skillName;

        @Override
        public void run() {
            System.out.println("Updating skills" + (skillName != null ? ": " + skillName : "..."));
            // TODO: integrate with SkillManagementService
        }
    }

    @Command(name = "info", description = "Show detailed information about a skill",
            mixinStandardHelpOptions = true)
    static class InfoCommand implements Runnable {

        @Parameters(index = "0", description = "Skill name")
        String skillName;

        @Override
        public void run() {
            System.out.println("Info for skill: " + skillName);
            // TODO: integrate with SkillManagementService
        }
    }
}
