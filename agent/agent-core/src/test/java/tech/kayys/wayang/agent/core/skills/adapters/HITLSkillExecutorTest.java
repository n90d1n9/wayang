package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HITLSkillExecutor Tests")
class HITLSkillExecutorTest {

    private HITLSkillExecutor executor;
    private MockSkillRegistry mockRegistry;

    @BeforeEach
    void setUp() {
        mockRegistry = new MockSkillRegistry();
        executor = new HITLSkillExecutor(mockRegistry);
    }

    @Test
    @DisplayName("Should create approval request for skill execution")
    void testCreateApprovalRequest() {
        executor.initialize().await().indefinitely();
        
        Map<String, Object> executionPlan = Map.of(
            "skill_id", "skill-1",
            "input", Map.of("param", "value")
        );
        
        HITLSkillExecutor.ApprovalRequest request = 
            executor.createApprovalRequest("skill-1", executionPlan);

        assertNotNull(request);
        assertEquals("skill-1", request.skillId());
        assertFalse(request.executionPlan().isEmpty());
        assertFalse(request.requestId().isEmpty());
    }

    @Test
    @DisplayName("Should handle user approval of execution")
    void testHandleApprovalAccepted() {
        executor.initialize().await().indefinitely();
        
        Map<String, Object> executionPlan = Map.of("skill_id", "skill-1");
        HITLSkillExecutor.ApprovalRequest request = 
            executor.createApprovalRequest("skill-1", executionPlan);

        Map<String, Object> result = executor.executeWithApproval(
            request.requestId(), true, Map.of()
        ).await().indefinitely();

        assertNotNull(result);
        assertTrue((boolean) result.getOrDefault("approved", false));
    }

    @Test
    @DisplayName("Should handle user rejection of execution")
    void testHandleApprovalRejected() {
        executor.initialize().await().indefinitely();
        
        Map<String, Object> executionPlan = Map.of("skill_id", "skill-1");
        HITLSkillExecutor.ApprovalRequest request = 
            executor.createApprovalRequest("skill-1", executionPlan);

        Map<String, Object> result = executor.executeWithApproval(
            request.requestId(), false, Map.of("reason", "Not needed")
        ).await().indefinitely();

        assertNotNull(result);
        assertFalse((boolean) result.getOrDefault("approved", false));
    }

    @Test
    @DisplayName("Should collect user feedback on execution result")
    void testCollectUserFeedback() {
        executor.initialize().await().indefinitely();
        
        Map<String, Object> executionResult = Map.of(
            "skill_id", "skill-1",
            "status", "success",
            "result", "Execution completed"
        );
        
        HITLSkillExecutor.FeedbackRequest feedback = 
            executor.collectUserFeedback("skill-1", executionResult);

        assertNotNull(feedback);
        assertEquals("skill-1", feedback.skillId());
        assertFalse(feedback.executionResult().isEmpty());
    }

    @Test
    @DisplayName("Should handle user refinement request")
    void testHandleRefinementRequest() {
        executor.initialize().await().indefinitely();
        
        Map<String, Object> executionResult = Map.of("result", "initial");
        HITLSkillExecutor.FeedbackRequest feedback = 
            executor.collectUserFeedback("skill-1", executionResult);

        Map<String, Object> refined = executor.executeWithUserRefinement(
            feedback.feedbackId(),
            Map.of("adjustment", "more concise")
        ).await().indefinitely();

        assertNotNull(refined);
        assertTrue(refined.containsKey("refined_result"));
    }

    @Test
    @DisplayName("Should track approval history")
    void testApprovalHistory() {
        executor.initialize().await().indefinitely();
        
        Map<String, Object> executionPlan = Map.of("skill_id", "skill-1");
        HITLSkillExecutor.ApprovalRequest request1 = 
            executor.createApprovalRequest("skill-1", executionPlan);

        executor.executeWithApproval(request1.requestId(), true, Map.of())
            .await().indefinitely();

        List<HITLSkillExecutor.ApprovalRecord> history = 
            executor.getApprovalHistory("skill-1");

        assertNotNull(history);
        assertFalse(history.isEmpty());
    }

    @Test
    @DisplayName("Should support async approval workflow")
    void testAsyncApprovalWorkflow() {
        executor.initialize().await().indefinitely();
        
        Map<String, Object> executionPlan = Map.of("skill_id", "skill-1");
        HITLSkillExecutor.ApprovalRequest request = 
            executor.createApprovalRequest("skill-1", executionPlan);

        // Approval is async
        Map<String, Object> result = executor.executeWithApproval(
            request.requestId(), true, Map.of()
        ).await().indefinitely();

        assertTrue(result.containsKey("execution_id"));
    }

    @Test
    @DisplayName("Should generate approval request ID")
    void testApprovalRequestIdGeneration() {
        executor.initialize().await().indefinitely();
        
        Map<String, Object> executionPlan = Map.of("skill_id", "skill-1");
        HITLSkillExecutor.ApprovalRequest request = 
            executor.createApprovalRequest("skill-1", executionPlan);

        String requestId = request.requestId();
        
        assertNotNull(requestId);
        assertFalse(requestId.isEmpty());
        assertTrue(requestId.contains("apr-") || requestId.length() > 5);
    }

    // Mock skill registry for testing
    private static class MockSkillRegistry implements tech.kayys.wayang.agent.spi.skills.SkillRegistry {
        @Override
        public List<tech.kayys.wayang.agent.spi.skills.SkillDefinition> list() {
            return List.of(
                new MockSkillDefinition("skill-1", "Skill One", "First skill"),
                new MockSkillDefinition("skill-2", "Skill Two", "Second skill")
            );
        }

        @Override
        public io.smallrye.mutiny.Uni<tech.kayys.wayang.agent.spi.skills.SkillDefinition> get(String skillId) {
            return io.smallrye.mutiny.Uni.createFrom().item(
                new MockSkillDefinition(skillId, "Test Skill", "A test skill")
            );
        }

        @Override
        public io.smallrye.mutiny.Uni<tech.kayys.wayang.agent.spi.skills.SkillResult> executeSkill(
            String skillId,
            Map<String, Object> input) {
            return io.smallrye.mutiny.Uni.createFrom().item(
                new tech.kayys.wayang.agent.spi.skills.SkillResult(
                    skillId,
                    "invoc-123",
                    tech.kayys.wayang.agent.spi.skills.SkillResult.Status.SUCCESS,
                    "Executed: " + skillId,
                    true
                )
            );
        }
    }

    private static class MockSkillDefinition implements tech.kayys.wayang.agent.spi.skills.SkillDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final SkillMetadata metadata;

        MockSkillDefinition(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.metadata = new SkillMetadata(
                id, name, description, "1.0.0", "test",
                List.of("test"), null
            );
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public SkillMetadata metadata() {
            return metadata;
        }
    }
}
