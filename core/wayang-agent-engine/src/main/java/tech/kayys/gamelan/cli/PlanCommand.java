package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.planning.PlanRepository;
import tech.kayys.gamelan.planning.TaskPlanner;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.util.List;

/**
 * Plan management CLI — list, view, and compare saved task plans.
 *
 * <pre>
 * gamelan plan list                  # recent plans
 * gamelan plan show &lt;id&gt;            # full plan detail
 * gamelan plan search &lt;keyword&gt;     # find by task text
 * gamelan plan compare &lt;id1&gt; &lt;id2&gt; # side-by-side diff
 * gamelan plan delete &lt;id&gt;          # remove a plan
 * </pre>
 */
@Command(
    name = "plan",
    description = "Manage and compare saved task plans",
    mixinStandardHelpOptions = true,
    subcommands = {
        PlanCommand.ListCmd.class,
        PlanCommand.ShowCmd.class,
        PlanCommand.SearchCmd.class,
        PlanCommand.CompareCmd.class,
        PlanCommand.DeleteCmd.class
    }
)
public class PlanCommand implements Runnable {
    @Override public void run() { new CommandLine(this).usage(System.out); }

    @Command(name = "list", aliases = {"ls"}, description = "List recent plans")
    static class ListCmd implements Runnable {
        @Inject PlanRepository repo;
        @Inject GamelanConfig  config;
        @Option(names = {"-n"}, defaultValue = "10") int limit;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(config.color());
            List<TaskPlanner.Plan> plans = repo.recent(limit);
            if (plans.isEmpty()) { p.warn("No plans saved yet. Use: gamelan run --plan <task>"); return; }
            p.sectionHeader("Plans (" + plans.size() + ")");
            plans.forEach(plan -> p.listItem(
                    plan.id().substring(0, Math.min(8, plan.id().length())),
                    "[" + plan.estimatedCost() + "] " + plan.steps().size() + " steps — " + plan.goal()));
        }
    }

    @Command(name = "show", description = "Show a plan in full detail")
    static class ShowCmd implements Runnable {
        @Inject PlanRepository repo;
        @Inject GamelanConfig  config;
        @Parameters(index = "0") String id;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(config.color());
            repo.load(id).ifPresentOrElse(
                plan -> { p.sectionHeader("Plan: " + plan.id()); p.println(plan.summary()); },
                () -> p.error("Plan not found: " + id));
        }
    }

    @Command(name = "search", description = "Find plans by task keyword")
    static class SearchCmd implements Runnable {
        @Inject PlanRepository repo;
        @Inject GamelanConfig  config;
        @Parameters(index = "0") String keyword;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(config.color());
            var plans = repo.findByTask(keyword);
            if (plans.isEmpty()) { p.warn("No plans match: " + keyword); return; }
            p.sectionHeader("Plans matching '" + keyword + "' (" + plans.size() + ")");
            plans.forEach(plan -> p.listItem(plan.id().substring(0, 8), plan.goal()));
        }
    }

    @Command(name = "compare", description = "Side-by-side comparison of two plans")
    static class CompareCmd implements Runnable {
        @Inject PlanRepository repo;
        @Inject GamelanConfig  config;
        @Parameters(index = "0") String idA;
        @Parameters(index = "1") String idB;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(config.color());
            var a = repo.load(idA);
            var b = repo.load(idB);
            if (a.isEmpty()) { p.error("Plan not found: " + idA); return; }
            if (b.isEmpty()) { p.error("Plan not found: " + idB); return; }
            p.sectionHeader("Plan Comparison");
            p.println(repo.compare(a.get(), b.get()));
        }
    }

    @Command(name = "delete", aliases = {"rm"}, description = "Delete a saved plan")
    static class DeleteCmd implements Runnable {
        @Inject PlanRepository repo;
        @Inject GamelanConfig  config;
        @Parameters(index = "0") String id;

        @Override public void run() {
            AnsiPrinter p = new AnsiPrinter(config.color());
            if (repo.delete(id)) p.success("Deleted plan: " + id);
            else                 p.warn("Plan not found: " + id);
        }
    }
}
