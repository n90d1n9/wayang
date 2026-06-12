package tech.kayys.gollek.agent.skills.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import tech.kayys.gollek.agent.skills.agent.AgentSkillUtil;
import tech.kayys.gollek.agent.skills.agent.AgentSkillConfig;

/**
 * Picocli command for {@code golok skills} subcommand.
 *
 * <p>
 * Integrates with the existing golok CLI ({@code golokCommand}) as a
 * subcommand. Add to the {@code @Command.subcommands} list in
 * {@code golokCommand.java}:
 * 
 * <pre>{@code
 * &#64;Command(name = "golok", subcommands = {
 *     ...
 *     SkillsCommand.class
 * })
 * }</pre>
 *
 * <h3>Usage:</h3>
 * 
 * <pre>
 * golok skills add https://github.com/samber/cc-skills
 * golok skills add ./my-local-skill-dir
 * golok skills add https://github.com/samber/cc-skills --skill 'golang-*'
 * golok skills list
 * golok skills info conventional-git
 * golok skills update
 * golok skills remove cc-skills
 * </pre>
 *
 * @author Bhangun
 */
@Command(name = "skills", description = "Manage external agent skills — install, list, update, and remove skill packs", mixinStandardHelpOptions = true, subcommands = {
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

    // ── Shared agent instance ─────────────────────────────────────

    private static AgentSkillUtil getAgent() {
        return AgentSkillUtil.create(AgentSkillConfig.fromEnvironment());
    }

    // ── Subcommands ───────────────────────────────────────────────

    @Command(name = "add", description = "Install skills from a git repository or local directory", mixinStandardHelpOptions = true)
    static class AddCommand implements Runnable {

        @Parameters(index = "0", description = "Git repository URL (e.g. https://github.com/samber/cc-skills) or local directory path")
        String source;

        @Option(names = { "--skill",
                "-s" }, description = "Glob filter for specific skills (default: '*' = all). Example: 'golang-*'")
        String skillFilter;

        @Override
        public void run() {
            getAgent().getCommandHandler().add(source, skillFilter);
        }
    }

    @Command(name = "remove", aliases = {
            "rm" }, description = "Remove an installed skill repository", mixinStandardHelpOptions = true)
    static class RemoveCommand implements Runnable {

        @Parameters(index = "0", description = "Name of the installed skill repo (e.g. cc-skills)")
        String repoName;

        @Override
        public void run() {
            getAgent().getCommandHandler().remove(repoName);
        }
    }

    @Command(name = "list", aliases = {
            "ls" }, description = "List all installed skills", mixinStandardHelpOptions = true)
    static class ListCommand implements Runnable {

        @Override
        public void run() {
            getAgent().getCommandHandler().list();
        }
    }

    @Command(name = "update", aliases = {
            "up" }, description = "Update installed skill repos (git pull)", mixinStandardHelpOptions = true)
    static class UpdateCommand implements Runnable {

        @Parameters(index = "0", arity = "0..1", description = "Specific repo to update (default: all repos)")
        String repoName;

        @Override
        public void run() {
            getAgent().getCommandHandler().update(repoName);
        }
    }

    @Command(name = "info", description = "Show detailed information about a skill", mixinStandardHelpOptions = true)
    static class InfoCommand implements Runnable {

        @Parameters(index = "0", description = "Skill name (e.g. conventional-git)")
        String skillName;

        @Override
        public void run() {
            getAgent().getCommandHandler().info(skillName);
        }
    }
}
