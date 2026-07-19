package tech.kayys.wayang.agent.skills.cli.cli;

import tech.kayys.wayang.agent.core.skills.loader.SkillLoaderResult;
import tech.kayys.wayang.agent.core.skills.loader.SkillsLoaderService;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Handler for {@code wayang skills} CLI subcommands.
 *
 * <p>Delegates to {@link SkillsLoaderService} for loading and discovering
 * skills from local directories.
 *
 * <p>Supports:
 * <pre>
 * wayang skills add ./my-local-skill-dir
 * wayang skills remove skill-name
 * wayang skills list
 * wayang skills update [skill-name]
 * wayang skills info skill-name
 * </pre>
 *
 * @author Bhangun
 */
public class SkillsCommandHandler {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RED = "\u001B[31m";
    private static final String DIM = "\u001B[2m";
    private static final String BOLD = "\u001B[1m";

    private final SkillsLoaderService loaderService;

    public SkillsCommandHandler(SkillsLoaderService loaderService) {
        this.loaderService = loaderService;
    }

    /**
     * Install skills from a local directory.
     */
    public void add(String source, String skillFilter) {
        System.out.println(BOLD + "Adding skills from: " + RESET + source);
        try {
            SkillLoaderResult result;
            if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("git@")) {
                result = loaderService.installGit(source, skillFilter);
            } else {
                result = loaderService.installLocal(source, skillFilter);
            }

            if (result.isSuccess()) {
                System.out.printf(GREEN + "✓ Installed skills from %s%n" + RESET, source);
            } else {
                System.err.println(RED + "✗ Failed: " + result.getErrorMessage() + RESET);
            }
        } catch (Exception e) {
            System.err.println(RED + "✗ Error: " + e.getMessage() + RESET);
        }
    }

    /**
     * Remove an installed skill repo.
     */
    public void remove(String repoName) {
        System.out.println(BOLD + "Removing: " + RESET + repoName);
        try {
            boolean removed = loaderService.remove(repoName);
            if (removed) {
                System.out.printf(GREEN + "✓ Removed: %s%n" + RESET, repoName);
            } else {
                System.out.printf(YELLOW + "⚠ Not found: %s%n" + RESET, repoName);
            }
        } catch (Exception e) {
            System.err.println(RED + "✗ Error: " + e.getMessage() + RESET);
        }
    }

    /**
     * List all installed skill repos.
     */
    public void list() {
        System.out.println(BOLD + "Installed Skills" + RESET);
        System.out.println("─".repeat(50));

        try {
            List<String> repos = loaderService.listInstalledRepos();
            if (repos.isEmpty()) {
                System.out.println(DIM + "No skills installed." + RESET);
            } else {
                repos.forEach(repo -> System.out.printf("  %s%s%s%n", CYAN, repo, RESET));
                System.out.printf("%n%sTotal: %d repos%s%n", DIM, repos.size(), RESET);
            }
        } catch (Exception e) {
            System.err.println(RED + "✗ Error listing skills: " + e.getMessage() + RESET);
        }
    }

    /**
     * Update an installed skill repo or all repos.
     */
    public void update(String repoName) {
        if (repoName == null) {
            System.out.println(BOLD + "Updating all skills..." + RESET);
            List<String> repos = loaderService.listInstalledRepos();
            repos.forEach(repo -> {
                SkillLoaderResult result = loaderService.update(repo);
                System.out.printf("  %s %s%s%n",
                        result.isSuccess() ? GREEN + "✓" : RED + "✗",
                        repo, RESET);
            });
        } else {
            System.out.printf(BOLD + "Updating: %s%s%n" + RESET, repoName, RESET);
            SkillLoaderResult result = loaderService.update(repoName);
            if (result.isSuccess()) {
                System.out.printf(GREEN + "✓ Updated: %s%n" + RESET, repoName);
            } else {
                System.err.println(RED + "✗ Failed: " + result.getErrorMessage() + RESET);
            }
        }
    }

    /**
     * Show info for a specific skill from the installed repos.
     */
    public void info(String skillName) {
        System.out.printf(BOLD + "Skill Info: %s%s%n" + RESET, skillName, RESET);
        System.out.println("─".repeat(50));

        try {
            Path baseDir = loaderService.getSkillsBaseDir();
            List<SkillManifest> allSkills = loaderService.loadSkillsFromDirectory(baseDir);

            SkillManifest match = allSkills.stream()
                    .filter(s -> skillName.equalsIgnoreCase(s.getName()) || skillName.equalsIgnoreCase(s.getName()))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                System.out.printf(YELLOW + "⚠ Skill not found: %s%n" + RESET, skillName);
                return;
            }
            System.out.printf("  %sName:%s        %s%n", DIM, RESET, match.getName());
            System.out.printf("  %sVersion:%s     %s%n", DIM, RESET, match.getVersion());
            System.out.printf("  %sDescription:%s %s%n", DIM, RESET, match.getDescription());
        } catch (Exception e) {
            System.err.println(RED + "✗ Error: " + e.getMessage() + RESET);
        }
    }
}
