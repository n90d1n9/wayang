package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import io.smallrye.mutiny.Uni;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * HITL (Human-in-the-Loop) skill executor.
 * 
 * Wraps skill execution with human feedback collection, allowing users to
 * provide corrections, refinements, and approval for skill-based actions.
 */
public class HITLSkillExecutor {

    private final SkillContext context;
    private final java.util.List<FeedbackHandler> feedbackHandlers;

    public HITLSkillExecutor(SkillContext context) {
        this.context = context;
        this.feedbackHandlers = new java.util.ArrayList<>();
    }

    /**
     * Register a feedback handler (e.g., user approval, corrections).
     */
    public HITLSkillExecutor addFeedbackHandler(FeedbackHandler handler) {
        feedbackHandlers.add(handler);
        return this;
    }

    /**
     * Collect human feedback on skill execution.
     */
    public Uni<FeedbackResult> collectFeedback(SkillResult result) {
        return Uni.combine().all()
            .unis(feedbackHandlers.stream()
                .map(handler -> handler.handle(context, result))
                .collect(java.util.stream.Collectors.toList()))
            .asList()
            .map(feedbacks -> mergeFeedback(feedbacks));
    }

    /**
     * Request human approval before skill execution.
     */
    public Uni<Boolean> requestApproval() {
        return Uni.createFrom().item(() -> {
            System.out.println("[HITL] Requesting approval for skill: " + context.skillId());
            return true; // In real impl, would block waiting for user input
        });
    }

    /**
     * Request human refinement of skill input/context.
     */
    public Uni<Map<String, Object>> requestRefinement(Map<String, Object> input) {
        return Uni.createFrom().item(() -> {
            System.out.println("[HITL] Requesting refinement for skill: " + context.skillId());
            return input; // In real impl, would allow user to modify
        });
    }

    private FeedbackResult mergeFeedback(java.util.List<?> feedbacks) {
        boolean approved = feedbacks.stream()
            .allMatch(f -> f instanceof Boolean && (Boolean) f);
        return new FeedbackResult(approved, new HashMap<>());
    }

    /**
     * Feedback result from human.
     */
    public record FeedbackResult(
        boolean approved,
        Map<String, Object> corrections
    ) {}

    /**
     * Feedback handler interface.
     */
    public interface FeedbackHandler {
        Uni<?> handle(SkillContext context, SkillResult result);
    }
}
