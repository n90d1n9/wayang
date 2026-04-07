package tech.kayys.wayang.hitl.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

class TaskAssignmentTest {

    @Test
    void shouldCreateTaskAssignmentSuccessfully() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("admin")
                .build();

        // When & Then
        assertEquals(AssigneeType.USER, assignment.getAssigneeType());
        assertEquals("user1", assignment.getAssigneeIdentifier());
        assertEquals("admin", assignment.getAssignedBy());
        assertNotNull(assignment.getAssignedAt());
    }

    @Test
    void shouldSetAssignedAtToCurrentTimeIfNotProvided() {
        // Given
        Instant before = Instant.now();
        
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("admin")
                .build();
        
        Instant after = Instant.now();

        // When & Then
        assertTrue(assignment.getAssignedAt().compareTo(before) >= 0);
        assertTrue(assignment.getAssignedAt().compareTo(after) <= 0);
    }

    @Test
    void shouldSetProvidedAssignedAt() {
        // Given
        Instant customTime = Instant.now().minusSeconds(100);
        
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("admin")
                .assignedAt(customTime)
                .build();

        // When & Then
        assertEquals(customTime, assignment.getAssignedAt());
    }

    @Test
    void shouldAllowUserToClaimTask() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("admin")
                .build();

        // When & Then
        assertTrue(assignment.canClaim("user1"));
        assertFalse(assignment.canClaim("user2"));
    }

    @Test
    void shouldAllowGroupMemberToClaimTask() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.GROUP)
                .assigneeIdentifier("group1")
                .assignedBy("admin")
                .build();

        // When & Then
        assertTrue(assignment.canClaim("anyUser"));
    }

    @Test
    void shouldAllowRoleMemberToClaimTask() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.ROLE)
                .assigneeIdentifier("role1")
                .assignedBy("admin")
                .build();

        // When & Then
        assertTrue(assignment.canClaim("anyUser"));
    }

    @Test
    void shouldSetDelegationReason() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("admin")
                .delegationReason("Going on vacation")
                .build();

        // When & Then
        assertEquals("Going on vacation", assignment.getDelegationReason());
    }
}