package tech.kayys.wayang.agent.skills.analytics;

import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory usage analytics for skill execution outcomes.
 */
public class UsageAnalyticsService {

    private final ConcurrentMap<String, MutableStats> statsBySkill = new ConcurrentHashMap<>();

    public Uni<SkillUsageMetrics> recordExecution(
            String skillId,
            long durationMs,
            boolean success) {
        return Uni.createFrom().item(() -> statsBySkill
                .computeIfAbsent(normalizeSkillId(skillId), MutableStats::new)
                .record(durationMs, success));
    }

    public Uni<SkillUsageMetrics> getSkillMetrics(String skillId) {
        return Uni.createFrom().item(() -> {
            MutableStats stats = statsBySkill.get(normalizeSkillId(skillId));
            return stats == null ? SkillUsageMetrics.empty(skillId) : stats.snapshot();
        });
    }

    public Uni<List<SkillUsageMetrics>> popularSkills(int limit) {
        return ranked(limit, Comparator.comparingLong(SkillUsageMetrics::totalExecutions).reversed());
    }

    public Uni<List<SkillUsageMetrics>> slowestSkills(int limit) {
        return ranked(limit, Comparator.comparingDouble(SkillUsageMetrics::averageDurationMs).reversed());
    }

    public Uni<List<SkillUsageMetrics>> highestFailureRateSkills(int limit) {
        return ranked(limit, Comparator.comparingDouble(SkillUsageMetrics::failureRate).reversed());
    }

    public Uni<Map<String, SkillUsageMetrics>> snapshot() {
        return Uni.createFrom().item(() -> statsBySkill.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().snapshot())));
    }

    public void clear() {
        statsBySkill.clear();
    }

    private Uni<List<SkillUsageMetrics>> ranked(int limit, Comparator<SkillUsageMetrics> comparator) {
        return Uni.createFrom().item(() -> statsBySkill.values().stream()
                .map(MutableStats::snapshot)
                .sorted(comparator.thenComparing(SkillUsageMetrics::skillId))
                .limit(Math.max(0, limit))
                .toList());
    }

    private String normalizeSkillId(String skillId) {
        return skillId == null || skillId.isBlank() ? "unknown" : skillId;
    }

    private static final class MutableStats {
        private final String skillId;
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private long totalDurationMs;
        private Instant firstExecutedAt;
        private Instant lastExecutedAt;

        private MutableStats(String skillId) {
            this.skillId = skillId;
        }

        private synchronized SkillUsageMetrics record(long durationMs, boolean success) {
            Instant now = Instant.now();
            totalExecutions++;
            if (success) {
                successfulExecutions++;
            } else {
                failedExecutions++;
            }
            totalDurationMs += Math.max(0, durationMs);
            firstExecutedAt = firstExecutedAt == null ? now : firstExecutedAt;
            lastExecutedAt = now;
            return snapshot();
        }

        private synchronized SkillUsageMetrics snapshot() {
            double average = totalExecutions == 0 ? 0.0 : (double) totalDurationMs / totalExecutions;
            return new SkillUsageMetrics(
                    skillId,
                    totalExecutions,
                    successfulExecutions,
                    failedExecutions,
                    average,
                    totalDurationMs,
                    firstExecutedAt,
                    lastExecutedAt);
        }
    }
}
