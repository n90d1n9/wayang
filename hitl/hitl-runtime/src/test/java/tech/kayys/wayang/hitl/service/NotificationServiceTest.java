package tech.kayys.wayang.hitl.service;

import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import tech.kayys.wayang.hitl.domain.*;
import tech.kayys.wayang.hitl.repository.HumanTaskEntity;
import tech.kayys.wayang.hitl.repository.HumanTaskRepository;

import java.time.Instant;
import java.util.List;

class NotificationServiceTest {

        @Mock
        private UserDirectoryService userDirectoryService;

        @Mock
        private HumanTaskRepository repository;

        @Mock
        private ReactiveMailer mailer;

        private NotificationService notificationService;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                notificationService = new NotificationService();
                // Use reflection to inject the mock services
                try {
                        java.lang.reflect.Field userDirField = NotificationService.class
                                        .getDeclaredField("userDirectory");
                        userDirField.setAccessible(true);
                        userDirField.set(notificationService, userDirectoryService);

                        java.lang.reflect.Field repoField = NotificationService.class.getDeclaredField("repository");
                        repoField.setAccessible(true);
                        repoField.set(notificationService, repository);

                        java.lang.reflect.Field mailerField = NotificationService.class.getDeclaredField("mailer");
                        mailerField.setAccessible(true);
                        mailerField.set(notificationService, mailer);
                } catch (Exception e) {
                        throw new RuntimeException("Failed to inject mock services", e);
                }

                when(mailer.send(any(io.quarkus.mailer.Mail[].class))).thenReturn(Uni.createFrom().voidItem());
        }

        @Test
        void shouldSendTaskAssignedNotificationSuccessfully() {
                // Given
                TaskAssignment assignment = TaskAssignment.builder()
                                .assigneeType(AssigneeType.USER)
                                .assigneeIdentifier("user1")
                                .assignedBy("system")
                                .build();

                HumanTask task = HumanTask.builder()
                                .workflowRunId("workflow-123")
                                .nodeId("node-456")
                                .tenantId("tenant-789")
                                .taskType("approval")
                                .title("Test Task")
                                .assignTo(assignment)
                                .build();

                when(userDirectoryService.getUserEmail(eq("user1")))
                                .thenReturn(Uni.createFrom().item("user1@company.com"));

                // When
                Uni<Void> result = notificationService.sendTaskAssignedNotification(task);

                // Then
                // Just verify that the method completes without exception
                result.await().indefinitely();
                verify(userDirectoryService, times(1)).getUserEmail(eq("user1"));
        }

        @Test
        void shouldHandleNullEmailInTaskAssignedNotification() {
                // Given
                TaskAssignment assignment = TaskAssignment.builder()
                                .assigneeType(AssigneeType.USER)
                                .assigneeIdentifier("unknown-user")
                                .assignedBy("system")
                                .build();

                HumanTask task = HumanTask.builder()
                                .workflowRunId("workflow-123")
                                .nodeId("node-456")
                                .tenantId("tenant-789")
                                .taskType("approval")
                                .title("Test Task")
                                .assignTo(assignment)
                                .build();

                when(userDirectoryService.getUserEmail(eq("unknown-user"))).thenReturn(Uni.createFrom().nullItem());

                // When
                Uni<Void> result = notificationService.sendTaskAssignedNotification(task);

                // Then
                result.await().indefinitely();
                verify(userDirectoryService, times(1)).getUserEmail(eq("unknown-user"));
        }

        @Test
        void shouldSendTaskRemindersSuccessfully() {
                // Given
                HumanTaskEntity taskEntity = new HumanTaskEntity();
                taskEntity.taskId = "TASK-123";
                taskEntity.title = "Test Task";
                taskEntity.assigneeIdentifier = "user1";
                taskEntity.dueDate = Instant.now().plusSeconds(100);
                taskEntity.priority = 3;

                when(repository.find(any(String.class), any(Instant.class), any(Instant.class), any(List.class)))
                                .thenReturn(mock(io.quarkus.hibernate.reactive.panache.PanacheQuery.class));

                io.quarkus.hibernate.reactive.panache.PanacheQuery<HumanTaskEntity> mockQuery = mock(
                                io.quarkus.hibernate.reactive.panache.PanacheQuery.class);
                when(mockQuery.list()).thenReturn(Uni.createFrom().item(List.of(taskEntity)));

                when(repository.find(any(String.class), any(Instant.class), any(Instant.class), any(List.class)))
                                .thenReturn(mockQuery);

                when(userDirectoryService.getUserEmail(eq("user1")))
                                .thenReturn(Uni.createFrom().item("user1@company.com"));

                // When
                Uni<Integer> result = notificationService.sendTaskReminders();

                // Then
                Integer count = result.await().indefinitely();
                assertNotNull(count);
        }

        @Test
        void shouldSendOverdueNotificationSuccessfully() {
                // Given
                TaskAssignment assignment = TaskAssignment.builder()
                                .assigneeType(AssigneeType.USER)
                                .assigneeIdentifier("user1")
                                .assignedBy("system")
                                .build();

                HumanTask task = HumanTask.builder()
                                .workflowRunId("workflow-123")
                                .nodeId("node-456")
                                .tenantId("tenant-789")
                                .taskType("approval")
                                .title("Test Task")
                                .dueDate(Instant.now().minusSeconds(100)) // Past due
                                .assignTo(assignment)
                                .build();

                when(userDirectoryService.getUserEmail(eq("user1")))
                                .thenReturn(Uni.createFrom().item("user1@company.com"));

                // When
                Uni<Void> result = notificationService.sendOverdueNotification(task);

                // Then
                result.await().indefinitely();
                verify(userDirectoryService, times(1)).getUserEmail(eq("user1"));
        }

        @Test
        void shouldHandleNullEmailInOverdueNotification() {
                // Given
                TaskAssignment assignment = TaskAssignment.builder()
                                .assigneeType(AssigneeType.USER)
                                .assigneeIdentifier("unknown-user")
                                .assignedBy("system")
                                .build();

                HumanTask task = HumanTask.builder()
                                .workflowRunId("workflow-123")
                                .nodeId("node-456")
                                .tenantId("tenant-789")
                                .taskType("approval")
                                .title("Test Task")
                                .dueDate(Instant.now().minusSeconds(100)) // Past due
                                .assignTo(assignment)
                                .build();

                when(userDirectoryService.getUserEmail(eq("unknown-user"))).thenReturn(Uni.createFrom().nullItem());

                // When
                Uni<Void> result = notificationService.sendOverdueNotification(task);

                // Then
                result.await().indefinitely();
                verify(userDirectoryService, times(1)).getUserEmail(eq("unknown-user"));
        }
}
