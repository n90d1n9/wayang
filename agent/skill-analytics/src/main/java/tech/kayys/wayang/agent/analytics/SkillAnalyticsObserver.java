package tech.kayys.wayang.agent.analytics;

import io.quarkus.arc.Arc;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Observer that listens to skill events and forwards them to analytics service.
 */
@ApplicationScoped
public class SkillAnalyticsObserver {
    
    private static final Logger LOG = Logger.getLogger(SkillAnalyticsObserver.class);
    
    private ExecutorService eventProcessor;
    private LinkedBlockingQueue<SkillUsageEvent> eventQueue;
    
    @PostConstruct
    void init() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        
        eventProcessor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        eventQueue = new LinkedBlockingQueue<>(1000);
        
        // Start background processor
        startEventProcessor();
        
        LOG.info("Skill analytics observer initialized");
    }
    
    private void startEventProcessor() {
        eventProcessor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SkillUsageEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        processEvent(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOG.error("Error processing analytics event", e);
                }
            }
        });
    }
    
    /**
     * Observe skill execution started event.
     */
    public void onExecutionStarted(String skillId, String tenantId, String userId, String eventId) {
        queueEvent(SkillUsageEvent.builder()
            .eventId(eventId)
            .skillId(skillId)
            .tenantId(tenantId)
            .userId(userId)
            .eventType(SkillUsageEvent.EventType.EXECUTION_STARTED)
            .build());
    }
    
    /**
     * Observe skill execution completed event.
     */
    public void onExecutionCompleted(String skillId, String tenantId, String userId, 
                                     String eventId, long durationMs) {
        queueEvent(SkillUsageEvent.builder()
            .eventId(eventId)
            .skillId(skillId)
            .tenantId(tenantId)
            .userId(userId)
            .eventType(SkillUsageEvent.EventType.EXECUTION_COMPLETED)
            .durationMs(durationMs)
            .successful(true)
            .build());
    }
    
    /**
     * Observe skill execution failed event.
     */
    public void onExecutionFailed(String skillId, String tenantId, String userId,
                                  String eventId, String errorMessage) {
        queueEvent(SkillUsageEvent.builder()
            .eventId(eventId)
            .skillId(skillId)
            .tenantId(tenantId)
            .userId(userId)
            .eventType(SkillUsageEvent.EventType.EXECUTION_FAILED)
            .successful(false)
            .errorMessage(errorMessage)
            .build());
    }
    
    /**
     * Observe cache hit event.
     */
    public void onCacheHit(String skillId, String tenantId) {
        queueEvent(SkillUsageEvent.builder()
            .skillId(skillId)
            .tenantId(tenantId)
            .eventType(SkillUsageEvent.EventType.CACHE_HIT)
            .build());
    }
    
    /**
     * Observe cache miss event.
     */
    public void onCacheMiss(String skillId, String tenantId) {
        queueEvent(SkillUsageEvent.builder()
            .skillId(skillId)
            .tenantId(tenantId)
            .eventType(SkillUsageEvent.EventType.CACHE_MISS)
            .build());
    }
    
    private void queueEvent(SkillUsageEvent event) {
        if (!eventQueue.offer(event)) {
            LOG.warnf("Analytics event queue full, dropping event: %s", event.eventId());
        }
    }
    
    private void processEvent(SkillUsageEvent event) {
        try {
            SkillAnalyticsService analytics = Arc.container()
                .select(SkillAnalyticsService.class)
                .get();
            
            analytics.recordEvent(event).subscribe().with(
                ignored -> LOG.debugf("Processed analytics event: %s", event.eventId()),
                error -> LOG.errorf(error, "Failed to process analytics event: %s", event.eventId())
            );
        } catch (Exception e) {
            LOG.errorf(e, "Error getting analytics service for event: %s", event.eventId());
        }
    }
    
    void destroy() {
        if (eventProcessor != null) {
            eventProcessor.shutdown();
            try {
                if (!eventProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                    eventProcessor.shutdownNow();
                }
            } catch (InterruptedException e) {
                eventProcessor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
