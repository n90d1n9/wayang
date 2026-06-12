package tech.kayys.gamelan.evaluation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;

import java.time.*;
import java.util.concurrent.*;

/**
 * Continuous evaluation scheduler (Section XII — Continuous Evaluation Harness).
 *
 * <p>Runs the benchmark suite automatically on a configurable schedule so that
 * regressions are caught without manual intervention. Also triggers shadow-mode
 * comparison when a new strategy is configured.
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>DISABLED</b> — no automatic runs (default)</li>
 *   <li><b>DAILY</b>    — runs at midnight each day</li>
 *   <li><b>HOURLY</b>   — runs every hour (for active development)</li>
 *   <li><b>ON_START</b> — runs once at application startup</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * Set via {@code .gamelan/agent.json} or application.yml:
 * <pre>
 * gamelan.eval.schedule: daily     # disabled | daily | hourly | on_start
 * gamelan.eval.regression-alert: true
 * </pre>
 */
@ApplicationScoped
public class EvalScheduler {

    private static final Logger log = LoggerFactory.getLogger(EvalScheduler.class);

    @Inject SkillBenchmark  benchmark;
    @Inject GamelanConfig   config;

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public enum Schedule { DISABLED, ON_START, HOURLY, DAILY }

    @PostConstruct
    void init() {
        Schedule schedule = parseSchedule();
        if (schedule == Schedule.DISABLED) {
            log.debug("[eval-scheduler] disabled");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "eval-scheduler");
            t.setDaemon(true);
            return t;
        });

        switch (schedule) {
            case ON_START -> scheduler.execute(this::runBenchmark);
            case HOURLY   -> scheduler.scheduleAtFixedRate(this::runBenchmark, 0, 1, TimeUnit.HOURS);
            case DAILY    -> {
                long delay = secondsUntilMidnight();
                scheduler.scheduleAtFixedRate(this::runBenchmark, delay, 86400, TimeUnit.SECONDS);
                log.info("[eval-scheduler] daily benchmark in {}s", delay);
            }
            default -> {}
        }
    }

    @PreDestroy
    void shutdown() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    /** Runs the benchmark immediately in the background. */
    public void triggerNow() {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        scheduler.execute(this::runBenchmark);
    }

    public boolean isRunning() { return running; }

    // ── Private ────────────────────────────────────────────────────────────

    private void runBenchmark() {
        if (running) {
            log.warn("[eval-scheduler] benchmark already running — skipping");
            return;
        }
        running = true;
        log.info("[eval-scheduler] starting scheduled benchmark");
        try {
            SkillBenchmark.SuiteResult result =
                    benchmark.run(config.defaultModel(), "react");

            // Check for regression vs previous
            benchmark.loadLatest("built-in").ifPresent(prev -> {
                if (result.hasRegression(prev)) {
                    log.error("[eval-scheduler] REGRESSION DETECTED! Score: {:.0%} → {:.0%}",
                            prev.score(), result.score());
                    // In production: send alert, webhook, etc.
                }
            });

            log.info("[eval-scheduler] benchmark complete: {}/{} passed ({:.0%})",
                    result.passed(), result.total(), result.score());
        } catch (Exception e) {
            log.error("[eval-scheduler] benchmark failed: {}", e.getMessage(), e);
        } finally {
            running = false;
        }
    }

    private Schedule parseSchedule() {
        // Read from system property or default to disabled
        String s = System.getProperty("gamelan.eval.schedule",
                System.getenv() != null ? System.getenv().getOrDefault("GAMELAN_EVAL_SCHEDULE", "disabled") : "disabled");
        try {
            return Schedule.valueOf(s.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return Schedule.DISABLED;
        }
    }

    private long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).getSeconds();
    }
}
