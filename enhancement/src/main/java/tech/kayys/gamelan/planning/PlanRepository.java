package tech.kayys.gamelan.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Persists, retrieves, and versions task plans (Section IX — Plan Versioning).
 *
 * <p>Plans are stored as JSON under {@code ~/.gamelan/plans/<project>/}.
 * Every plan execution is recorded; multiple plan versions for the same task
 * can be compared to see which strategy consistently works better.
 *
 * <h2>Versioning</h2>
 * Plans are keyed by a content hash of the task text, so re-planning the same
 * task produces a new version alongside the old one rather than overwriting it.
 * Callers can retrieve all versions and pick the one with the best historical
 * outcome.
 *
 * <h2>Usage</h2>
 * <pre>
 * gamelan plan list                    # show recent plans
 * gamelan plan show <id>               # show a plan's full detail
 * gamelan plan compare <id1> <id2>     # side-by-side comparison
 * </pre>
 */
@ApplicationScoped
public class PlanRepository {

    private static final Logger log = LoggerFactory.getLogger(PlanRepository.class);
    private static final ObjectMapper MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private Path planDir;

    @PostConstruct
    void init() {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        planDir = Path.of(System.getProperty("user.home"), ".gamelan", "plans", project);
        try { Files.createDirectories(planDir); }
        catch (IOException e) { log.warn("[plan-repo] cannot create dir: {}", e.getMessage()); }
    }

    // ── Save / Load ────────────────────────────────────────────────────────

    /** Saves a plan. Returns the file path written. */
    public Path save(TaskPlanner.Plan plan) {
        Path file = planDir.resolve(plan.id() + ".json");
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), plan);
            log.debug("[plan-repo] saved plan {} ({})", plan.id(), plan.goal());
        } catch (IOException e) {
            log.warn("[plan-repo] save failed: {}", e.getMessage());
        }
        return file;
    }

    /** Loads a plan by ID. */
    public Optional<TaskPlanner.Plan> load(String id) {
        Path file = planDir.resolve(id + ".json");
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(MAPPER.readValue(file.toFile(), TaskPlanner.Plan.class));
        } catch (IOException e) {
            log.warn("[plan-repo] load failed for {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    // ── Versioned lookups ──────────────────────────────────────────────────

    /**
     * Returns all plans whose original task contains the given keyword,
     * sorted newest-first.
     */
    public List<TaskPlanner.Plan> findByTask(String keyword) {
        return listAll().stream()
                .filter(p -> p.originalTask().toLowerCase()
                        .contains(keyword.toLowerCase()))
                .sorted(Comparator.comparing(p -> p.id(), Comparator.reverseOrder()))
                .toList();
    }

    /** Returns the N most recently saved plans. */
    public List<TaskPlanner.Plan> recent(int limit) {
        return listAll().stream().limit(limit).toList();
    }

    /** Returns all plans, newest first. */
    public List<TaskPlanner.Plan> listAll() {
        if (!Files.isDirectory(planDir)) return List.of();
        try (var stream = Files.list(planDir)) {
            List<TaskPlanner.Plan> plans = new ArrayList<>();
            stream.filter(p -> p.toString().endsWith(".json"))
                  .sorted(Comparator.reverseOrder())
                  .forEach(p -> {
                      try {
                          plans.add(MAPPER.readValue(p.toFile(), TaskPlanner.Plan.class));
                      } catch (IOException ignored) {}
                  });
            return Collections.unmodifiableList(plans);
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Deletes a plan by ID. Returns true if deleted. */
    public boolean delete(String id) {
        try {
            return Files.deleteIfExists(planDir.resolve(id + ".json"));
        } catch (IOException e) {
            return false;
        }
    }

    // ── Comparison ─────────────────────────────────────────────────────────

    /**
     * Produces a side-by-side text comparison of two plans.
     * Useful for understanding how strategy evolved between versions.
     */
    public String compare(TaskPlanner.Plan a, TaskPlanner.Plan b) {
        StringBuilder sb = new StringBuilder();
        sb.append("Plan A: ").append(a.id()).append("\n");
        sb.append("Plan B: ").append(b.id()).append("\n\n");

        sb.append("Task:\n  A: ").append(a.originalTask()).append("\n");
        sb.append("  B: ").append(b.originalTask()).append("\n\n");

        sb.append("Goal:\n  A: ").append(a.goal()).append("\n");
        sb.append("  B: ").append(b.goal()).append("\n\n");

        sb.append("Cost:\n  A: ").append(a.estimatedCost())
          .append("   B: ").append(b.estimatedCost()).append("\n\n");

        sb.append("Steps: A=").append(a.steps().size())
          .append("  B=").append(b.steps().size()).append("\n");

        int maxSteps = Math.max(a.steps().size(), b.steps().size());
        for (int i = 0; i < maxSteps; i++) {
            String aStep = i < a.steps().size() ? a.steps().get(i).description() : "(none)";
            String bStep = i < b.steps().size() ? b.steps().get(i).description() : "(none)";
            if (!aStep.equals(bStep)) {
                sb.append(String.format("  Step %d changed:%n    A: %s%n    B: %s%n", i+1, aStep, bStep));
            }
        }

        return sb.toString();
    }
}
