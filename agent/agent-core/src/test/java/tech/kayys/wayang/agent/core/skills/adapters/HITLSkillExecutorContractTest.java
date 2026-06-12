package tech.kayys.wayang.agent.core.skills.adapters;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.support.TestSkillContexts;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HITLSkillExecutorContractTest {

    @Test
    void createsApprovalRequestWithImmutableExecutionPlan() {
        HITLSkillExecutor executor = executor();
        Map<String, Object> plan = new LinkedHashMap<>();
        Map<String, Object> nestedStep = new LinkedHashMap<>();
        nestedStep.put("name", "draft");
        plan.put("input", "original");
        plan.put("steps", List.of(nestedStep));

        HITLSkillExecutor.ApprovalRequest request = executor.createApprovalRequest(" hitl-skill ", plan);
        plan.put("input", "mutated");
        nestedStep.put("name", "mutated");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) request.executionPlan().get("steps");

        assertTrue(request.requestId().startsWith("apr-"));
        assertEquals("hitl-skill", request.skillId());
        assertEquals("original", request.executionPlan().get("input"));
        assertEquals("draft", steps.get(0).get("name"));
        assertThrows(UnsupportedOperationException.class, () -> request.executionPlan().put("later", "nope"));
        assertThrows(UnsupportedOperationException.class, () -> steps.get(0).put("later", "nope"));
    }

    @Test
    void recordsApprovalDecisionAndFiltersHistory() {
        HITLSkillExecutor executor = executor();
        HITLSkillExecutor.ApprovalRequest request =
                executor.createApprovalRequest("hitl-skill", Map.of("input", "value"));

        Map<String, Object> response = executor.executeWithApproval(
                request.requestId(),
                true,
                Map.of("reviewer", "operator"))
                .await().indefinitely();

        assertEquals(request.requestId(), response.get("request_id"));
        assertEquals("hitl-skill", response.get("skill_id"));
        assertEquals(true, response.get("approved"));
        assertEquals("exec-" + request.requestId(), response.get("execution_id"));
        assertInstanceOf(Map.class, response.get("reviewer_input"));
        assertThrows(UnsupportedOperationException.class, () -> response.put("later", "nope"));

        List<HITLSkillExecutor.ApprovalRecord> matchingHistory = executor.getApprovalHistory("hitl-skill");
        assertEquals(1, matchingHistory.size());
        assertEquals("operator", matchingHistory.get(0).reviewerInput().get("reviewer"));
        assertThrows(UnsupportedOperationException.class, () -> matchingHistory.add(matchingHistory.get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> matchingHistory.get(0).reviewerInput().put("later", "nope"));
        assertTrue(executor.getApprovalHistory("other-skill").isEmpty());
    }

    @Test
    void returnsStructuredErrorForMissingApprovalRequest() {
        HITLSkillExecutor executor = executor();

        Map<String, Object> response = executor.executeWithApproval(" missing ", true, Map.of())
                .await().indefinitely();

        assertEquals("missing", response.get("request_id"));
        assertEquals(false, response.get("approved"));
        assertEquals("Approval request not found", response.get("error"));
    }

    @Test
    void collectsFeedbackFromHandlersAndMergesCorrections() {
        HITLSkillExecutor executor = executor()
                .addFeedbackHandler((context, result) -> Uni.createFrom().item(true))
                .addFeedbackHandler((context, result) -> Uni.createFrom().item(Map.of("tone", "short")))
                .addFeedbackHandler((context, result) -> Uni.createFrom()
                        .item(new HITLSkillExecutor.FeedbackResult(true, Map.of("approvedBy", "user"))));

        HITLSkillExecutor.FeedbackResult result = executor.collectFeedback(success()).await().indefinitely();

        assertTrue(result.approved());
        assertEquals("short", result.corrections().get("tone"));
        assertEquals("user", result.corrections().get("approvedBy"));
        assertThrows(UnsupportedOperationException.class, () -> result.corrections().put("later", "nope"));
    }

    @Test
    void failsClosedWhenFeedbackHandlerFailsOrReturnsFalse() {
        HITLSkillExecutor executor = executor()
                .addFeedbackHandler((context, result) -> Uni.createFrom().item(false))
                .addFeedbackHandler((context, result) -> null)
                .addFeedbackHandler((context, result) -> Uni.createFrom().failure(new IllegalStateException("boom")))
                .addFeedbackHandler((context, result) -> {
                    throw new IllegalArgumentException("bad handler");
                });

        HITLSkillExecutor.FeedbackResult result = executor.collectFeedback(success()).await().indefinitely();

        assertFalse(result.approved());
        assertTrue(result.corrections().isEmpty());
    }

    @Test
    void createsFeedbackRequestAndAppliesUserRefinement() {
        HITLSkillExecutor executor = executor();
        Map<String, Object> executionResult = new LinkedHashMap<>();
        executionResult.put("answer", "initial");

        HITLSkillExecutor.FeedbackRequest feedback =
                executor.collectUserFeedback(" hitl-skill ", executionResult);
        executionResult.put("answer", "mutated");

        Map<String, Object> refined = executor.executeWithUserRefinement(
                feedback.feedbackId(),
                Map.of("answer", "refined"))
                .await().indefinitely();

        assertTrue(feedback.feedbackId().startsWith("fbk-"));
        assertEquals("hitl-skill", feedback.skillId());
        assertEquals("initial", feedback.executionResult().get("answer"));
        assertEquals(feedback.feedbackId(), refined.get("feedback_id"));
        assertEquals(feedback.executionResult(), refined.get("original_result"));
        assertEquals(Map.of("answer", "refined"), refined.get("refined_result"));
        assertThrows(UnsupportedOperationException.class, () -> refined.put("later", "nope"));
    }

    @Test
    void returnsStructuredErrorForMissingFeedbackRequest() {
        HITLSkillExecutor executor = executor();

        Map<String, Object> response = executor.executeWithUserRefinement(" missing ", Map.of())
                .await().indefinitely();

        assertEquals("missing", response.get("feedback_id"));
        assertEquals("Feedback request not found", response.get("error"));
    }

    @Test
    void requestApprovalAndRequestRefinementRemainNonBlocking() {
        HITLSkillExecutor executor = executor();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("draft", "first");

        assertSame(executor, executor.initialize().await().indefinitely());
        assertTrue(executor.requestApproval().await().indefinitely());
        Map<String, Object> refinement = executor.requestRefinement(input).await().indefinitely();
        input.put("draft", "mutated");

        assertEquals("first", refinement.get("draft"));
        assertThrows(UnsupportedOperationException.class, () -> refinement.put("later", "nope"));
    }

    @Test
    void requiresContext() {
        assertThrows(NullPointerException.class, () -> new HITLSkillExecutor(null));
    }

    private static HITLSkillExecutor executor() {
        return new HITLSkillExecutor(TestSkillContexts.context("hitl-skill", null));
    }

    private static SkillResult success() {
        return SkillResult.success("hitl-skill", "completed");
    }
}
