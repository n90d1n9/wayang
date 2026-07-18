package tech.kayys.gollek.agent.skills.cli;

import tech.kayys.wayang.agent.core.skills.loader.DefaultSkillsLoaderService;
import tech.kayys.wayang.agent.core.skills.loader.SkillLoaderResult;
import tech.kayys.wayang.agent.core.skills.loader.SkillsLoaderService;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.gollek.agent.skills.store.ExternalSkillStore;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Handler for {@code golok skills} CLI subcommands.
 *
 * <p>
 * Supports the following operations:
 * 
 * <pre>
 * golok skills add https://github.com/samber/cc-skills [--skill '*']
 * golok skills remove cc-skills
 * golok skills list
 * golok skills update [repo-name]
 * golok skills info skill-name
 * </pre>
 *
 * <p>
 * This class is a pure handler — it prints to stdout and delegates to
 * {@link DefaultSkillsLoaderService} and {@link ExternalSkillStore} for actual work.
 * The Picocli CLI command in {@code golok-cli} creates an instance and
 * calls the appropriate method.
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
    private final ExternalSkillStore store;

    public SkillsCommandHandler(SkillsLoaderService loaderService, ExternalSkillStore store) {
        this.loaderService = loaderService;
        this.store = store;
    }

    /**
     * golok skills add &lt;url&gt; [--skill 'filter']
     */
    public void add(String source, String skillFilter) {
        System.out.printf("📦 Installing skills from %s%s%s...%n",
                CYAN, source, RESET);

        SkillLoaderResult result;
        if (java.nio.file.Files.isDirectory(Path.of(source))) {
            result = loaderService.installLocal(source, skillFilter);
        } else {
            result = loaderService.installGit(source, skillFilter);
        }

        if (result.isSuccess()) {
            String action = result.updated() ? "Updated" : "Installed";
            System.out.printf("%s✓ %s %d skill(s):%s%n",
                    GREEN, action, result.skillCount(), RESET);
            for (String skill : result.skillsInstalled()) {
                System.out.printf("  • %s%n", skill);
            }
            System.out.printf("%n%sLocation:%s %s%n", DIM, RESET, result.localPath());

            // Reload store
            store.loadAll();
        } else {
            System.out.printf("%s✗ Installation failed:%s%n", RED, RESET);
            for (String error : result.errors()) {
                System.out.printf("  %s%s%s%n", RED, error, RESET);
            }
        }
    }

    /**
     * golok skills remove &lt;name&gt;
     */
    public void remove(String repoName) {
        System.out.printf("🗑  Removing skill repo: %s%s%s%n", CYAN, repoName, RESET);

        boolean removed = loaderService.remove(repoName);
        if (removed) {
            store.unloadRepo(repoName);
            System.out.printf("%s✓ Removed successfully%s%n", GREEN, RESET);
        } else {
            System.out.printf("%s✗ Repo not found: %s%s%n", RED, repoName, RESET);
        }
    }

    /**
     * golok skills list
     */
    public void list() {
        store.loadAll();
        Map<String, List<String>> repos = store.listRepos();

        if (repos.isEmpty()) {
            System.out.println("No skills installed.");
            System.out.println();
            System.out.printf("%sTip:%s Install skills with:%n", DIM, RESET);
            System.out.println("  golok skills add https://github.com/samber/cc-skills");
            return;
        }

        System.out.printf("%s=== Installed Skills ===%s%n%n", BOLD, RESET);

        int totalSkills = 0;
        for (Map.Entry<String, List<String>> entry : repos.entrySet()) {
            String repoName = entry.getKey();
            List<String> skills = entry.getValue();
            totalSkills += skills.size();

            System.out.printf("  %s%s%s %s(%d skills)%s%n",
                    BOLD, repoName, RESET, DIM, skills.size(), RESET);

            for (String skill : skills) {
                store.findSkill(skill).ifPresent(m -> {
                    String emoji = m.getEmoji() != null ? m.getEmoji() + " " : "  ";
                    System.out.printf("    %s%-25s %sv%s%s%n",
                            emoji, m.getName(), DIM, m.getVersion(), RESET);
                });
            }
            System.out.println();
        }

        System.out.printf("%sTotal: %d skills from %d repos%s%n",
                DIM, totalSkills, repos.size(), RESET);
    }

    /**
     * golok skills update [repo-name]
     */
    public void update(String repoName) {
        if (repoName != null && !repoName.isBlank()) {
            // Update specific repo
            updateSingleRepo(repoName);
        } else {
            // Update all repos
            List<String> repos = loaderService.listInstalledRepos();
            if (repos.isEmpty()) {
                System.out.println("No skill repos to update.");
                return;
            }
            System.out.printf("🔄 Updating %d repo(s)...%n%n", repos.size());
            for (String repo : repos) {
                updateSingleRepo(repo);
            }
        }
        // Reload store
        store.loadAll();
    }

    /**
     * golok skills info &lt;skill-name&gt;
     */
    public void info(String skillName) {
        store.loadAll();
        store.findSkill(skillName).ifPresentOrElse(
                this::printSkillInfo,
                () -> {
                    System.out.printf("%s✗ Skill not found: %s%s%n", RED, skillName, RESET);
                    System.out.printf("%sTip:%s Use 'golok skills list' to see installed skills%n",
                            DIM, RESET);
                });
    }

    // ── Internal ──────────────────────────────────────────────────

    private void updateSingleRepo(String repoName) {
        System.out.printf("  🔄 %s...", repoName);

        SkillLoaderResult result = loaderService.update(repoName);

        if (result.isSuccess()) {
            System.out.printf(" %s✓ %d skills%s%n", GREEN, result.skillCount(), RESET);
        } else {
            System.out.printf(" %s✗ %s%s%n", RED,
                    result.errors().isEmpty() ? "unknown error" : result.errors().get(0), RESET);
        }
    }

    private void printSkillInfo(SkillManifest m) {
        System.out.printf("%s=== %s ===%s%n%n", BOLD, m.getName(), RESET);

        String emoji = m.getEmoji() != null ? m.getEmoji() + " " : "";
        System.out.printf("  %sName:%s        %s%s%n", DIM, RESET, emoji, m.getName());
        System.out.printf("  %sVersion:%s     v%s%n", DIM, RESET, m.getVersion());
        System.out.printf("  %sAuthor:%s      %s%n", DIM, RESET,
                m.getAuthor() != null ? m.getAuthor() : "unknown");
        System.out.printf("  %sLicense:%s     %s%n", DIM, RESET,
                m.getLicense() != null ? m.getLicense() : "unspecified");
        System.out.printf("  %sInvocable:%s   %s%n", DIM, RESET,
                m.isUserInvocable() ? GREEN + "yes (slash command)" + RESET : "no (contextual)");

        System.out.println();
        System.out.printf("  %sDescription:%s%n", DIM, RESET);
        System.out.printf("  %s%n", m.getDescription());

        if (!m.getAllowedTools().isEmpty()) {
            System.out.println();
            System.out.printf("  %sAllowed tools:%s %s%n", DIM, RESET,
                    String.join(", ", m.getAllowedTools()));
        }

        if (!m.getRequiredBins().isEmpty()) {
            System.out.printf("  %sRequired bins:%s %s%n", DIM, RESET,
                    String.join(", ", m.getRequiredBins()));
        }

        if (m.getCompatibility() != null) {
            System.out.printf("  %sCompatibility:%s %s%n", DIM, RESET, m.getCompatibility());
        }

        if (!m.getReferences().isEmpty()) {
            System.out.println();
            System.out.printf("  %sReferences:%s%n", DIM, RESET);
            for (String ref : m.getReferences().keySet()) {
                System.out.printf("    • %s%n", ref);
            }
        }

        System.out.println();
        System.out.printf("  %sBody tokens:%s ~%d tokens%n", DIM, RESET, m.estimateBodyTokens());
        System.out.printf("  %sDesc tokens:%s ~%d tokens%n", DIM, RESET, m.estimateDescriptionTokens());

        if (m.getSourceDirectory() != null) {
            System.out.printf("  %sLocation:%s   %s%n", DIM, RESET, m.getSourceDirectory());
        }
    }
}
