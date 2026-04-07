package tech.kayys.wayang.hitl.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.hitl.domain.*;
import tech.kayys.wayang.hitl.repository.HumanTaskEntity;
import tech.kayys.wayang.hitl.repository.HumanTaskRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for handling task escalations
 */
@ApplicationScoped
public class EscalationService {

    private static final Logger LOG = LoggerFactory.getLogger(EscalationService.class);

    @Inject
    HumanTaskRepository repository;

    // Escalation rules by priority
    private static final Map<Integer, Duration> ESCALATION_THRESHOLDS = Map.of(
        5, Duration.ofHours(4),   // Critical - 4 hours
        4, Duration.ofHours(12),  // High - 12 hours
        3, Duration.ofHours(24),  // Medium - 24 hours
        2, Duration.ofHours(48),  // Low - 48 hours
        1, Duration.ofHours(72)   // Lowest - 72 hours
    );

    public Uni<Void> scheduleEscalation(
            HumanTaskId taskId,
            String escalateTo,
            Duration after) {

        LOG.info("Scheduling escalation for task {} to {} after {}",
            taskId.value(), escalateTo, after);

        // In production, schedule using Quartz or similar
        // For now, we'll check during scheduled job
        return Uni.createFrom().voidItem();
    }

    public Uni<Integer> processEscalations() {
        LOG.debug("Processing task escalations");

        List<Uni<Void>> escalations = new ArrayList<>();

        // Check each priority level
        for (Map.Entry<Integer, Duration> entry : ESCALATION_THRESHOLDS.entrySet()) {
            int priority = entry.getKey();
            Duration threshold = entry.getValue();

            Instant escalationTime = Instant.now().minus(threshold);

            Uni<Void> escalation = repository.findTasksForEscalation("*", escalationTime)
                .flatMap(tasks -> {
                    List<Uni<Void>> taskEscalations = tasks.stream()
                        .filter(t -> t.priority == priority)
                        .map(this::escalateTask)
                        .toList();

                    return Uni.join().all(taskEscalations).andFailFast()
                        .replaceWithVoid();
                });

            escalations.add(escalation);
        }

        return Uni.join().all(escalations).andFailFast()
            .map(list -> list.size());
    }

    private Uni<Void> escalateTask(HumanTaskEntity taskEntity) {
        LOG.info("Escalating task: {} - {}", taskEntity.taskId, taskEntity.title);

        // Determine escalation target
        String escalateTo = determineEscalationTarget(taskEntity);

        return new HumanTaskService().getTask(HumanTaskId.of(taskEntity.taskId))
            .flatMap(task -> {
                task.escalate(EscalationReason.TIMEOUT, escalateTo);
                return repository.save(task);
            })
            .replaceWithVoid();
    }

    private String determineEscalationTarget(HumanTaskEntity task) {
        // In production, look up manager/escalation chain
        // For now, return system admin
        return "admin@company.com";
    }
}