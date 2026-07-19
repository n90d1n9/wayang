package tech.kayys.wayang.hitl.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.hitl.domain.HumanTask;
import tech.kayys.wayang.hitl.domain.HumanTaskStatus;
import tech.kayys.wayang.hitl.repository.HumanTaskEntity;
import tech.kayys.wayang.hitl.repository.HumanTaskRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Multi-channel notification service for human tasks.
 * Supports: Email, Slack, Microsoft Teams, Webhooks
 */
@ApplicationScoped
public class NotificationService {

        private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

        @Inject
        ReactiveMailer mailer;

        @Inject
        HumanTaskRepository repository;

        @Inject
        UserDirectoryService userDirectory;

        @Inject
        Vertx vertx;

        private WebClient webClient;

        @ConfigProperty(name = "gamelan.notifications.email.enabled", defaultValue = "true")
        boolean emailEnabled;

        @ConfigProperty(name = "gamelan.notifications.slack.enabled", defaultValue = "false")
        boolean slackEnabled;

        @ConfigProperty(name = "gamelan.notifications.slack.webhook-url")
        Optional<String> slackWebhookUrl;

        @ConfigProperty(name = "gamelan.notifications.teams.enabled", defaultValue = "false")
        boolean teamsEnabled;

        @ConfigProperty(name = "gamelan.notifications.teams.webhook-url")
        Optional<String> teamsWebhookUrl;

        @ConfigProperty(name = "gamelan.app.base-url", defaultValue = "http://localhost:8080")
        String appBaseUrl;

        @ConfigProperty(name = "gamelan.notifications.reminder.hours-before-due", defaultValue = "24")
        int reminderHoursBeforeDue;

        @jakarta.annotation.PostConstruct
        void init() {
                webClient = WebClient.create(vertx);
        }

        // ==================== TASK ASSIGNMENT NOTIFICATIONS ====================

        /**
         * Send notification when task is assigned
         */
        public Uni<Void> sendTaskAssignedNotification(HumanTask task) {
                LOG.info("Sending task assigned notification for: {}", task.getId().value());

                return userDirectory.getUserEmail(task.getCurrentAssignment().getAssigneeIdentifier())
                                .flatMap(email -> {
                                        if (email == null) {
                                                LOG.warn("No email found for assignee: {}",
                                                                task.getCurrentAssignment().getAssigneeIdentifier());
                                                return Uni.createFrom().voidItem();
                                        }

                                        List<Uni<Void>> notifications = new ArrayList<>();

                                        if (emailEnabled) {
                                                notifications.add(sendAssignmentEmail(task, email));
                                        }

                                        if (slackEnabled && slackWebhookUrl.isPresent()) {
                                                notifications.add(sendSlackNotification(task));
                                        }

                                        if (teamsEnabled && teamsWebhookUrl.isPresent()) {
                                                notifications.add(sendTeamsNotification(task));
                                        }

                                        if (notifications.isEmpty()) {
                                                return Uni.createFrom().voidItem();
                                        }

                                        return Uni.join().all(notifications).andFailFast()
                                                        .replaceWithVoid();
                                })
                                .onFailure()
                                .invoke(error -> LOG.error("Failed to send task assigned notification", error));
        }

        private Uni<Void> sendAssignmentEmail(HumanTask task, String email) {
                String taskUrl = appBaseUrl + "/tasks/" + task.getId().value();

                String subject = String.format("[Gamelan] New Task Assigned: %s", task.getTitle());

                String body = buildEmailTemplate(
                                "Task Assigned",
                                task,
                                taskUrl,
                                "You have been assigned a new task. Please review and take action.");

                return mailer.send(
                                Mail.withHtml(email, subject, body)).replaceWithVoid()
                                .invoke(() -> LOG.debug("Assignment email sent to: {}", email))
                                .onFailure().invoke(error -> LOG.error("Failed to send assignment email", error));
        }

        // ==================== REMINDER NOTIFICATIONS ====================

        /**
         * Send reminders for tasks approaching due date
         */
        public Uni<Integer> sendTaskReminders() {
                LOG.debug("Sending task reminders");

                Instant reminderThreshold = Instant.now()
                                .plus(reminderHoursBeforeDue, ChronoUnit.HOURS);

                return repository.find(
                                "dueDate < ?1 and dueDate > ?2 and status in (?3)",
                                reminderThreshold,
                                Instant.now(),
                                List.of(HumanTaskStatus.ASSIGNED, HumanTaskStatus.IN_PROGRESS)).list()
                                .flatMap(tasks -> {
                                        if (tasks.isEmpty()) {
                                                return Uni.createFrom().item(0);
                                        }

                                        List<Uni<Void>> reminders = tasks.stream()
                                                        .map(this::sendReminderNotification)
                                                        .toList();

                                        return Uni.join().all(reminders).andFailFast()
                                                        .map(list -> list.size());
                                })
                                .onFailure().invoke(error -> LOG.error("Error sending task reminders", error));
        }

        private Uni<Void> sendReminderNotification(HumanTaskEntity taskEntity) {
                String taskUrl = appBaseUrl + "/tasks/" + taskEntity.taskId;

                return userDirectory.getUserEmail(taskEntity.assigneeIdentifier)
                                .flatMap(email -> {
                                        if (email == null) {
                                                return Uni.createFrom().voidItem();
                                        }

                                        Duration timeRemaining = Duration.between(
                                                        Instant.now(),
                                                        taskEntity.dueDate);

                                        String subject = String.format(
                                                        "[Gamelan] Task Reminder: %s (Due in %d hours)",
                                                        taskEntity.title,
                                                        timeRemaining.toHours());

                                        String body = String.format(
                                                        """
                                                                        <html>
                                                                        <body style="font-family: Arial, sans-serif;">
                                                                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                                                                <h2 style="color: #f59e0b;">⚠️ Task Reminder</h2>

                                                                                <p>This is a reminder that your task is due soon:</p>

                                                                                <div style="background: #f3f4f6; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                                                                    <h3 style="margin-top: 0;">%s</h3>
                                                                                    <p><strong>Due:</strong> %s</p>
                                                                                    <p><strong>Time Remaining:</strong> %d hours</p>
                                                                                    <p><strong>Priority:</strong> %d</p>
                                                                                </div>

                                                                                <p>
                                                                                    <a href="%s" style="background: #3b82f6; color: white; padding: 10px 20px;
                                                                                       text-decoration: none; border-radius: 5px; display: inline-block;">
                                                                                        View Task
                                                                                    </a>
                                                                                </p>

                                                                                <p style="color: #6b7280; font-size: 12px; margin-top: 30px;">
                                                                                    This is an automated reminder from Gamelan Workflow Engine.
                                                                                </p>
                                                                            </div>
                                                                        </body>
                                                                        </html>
                                                                        """,
                                                        taskEntity.title,
                                                        taskEntity.dueDate,
                                                        timeRemaining.toHours(),
                                                        taskEntity.priority,
                                                        taskUrl);

                                        return mailer.send(Mail.withHtml(email, subject, body))
                                                        .replaceWithVoid();
                                })
                                .invoke(() -> LOG.debug("Reminder sent for task: {}", taskEntity.taskId))
                                .onFailure()
                                .invoke(error -> LOG.error("Failed to send reminder for task: {}", taskEntity.taskId,
                                                error));
        }

        // ==================== OVERDUE NOTIFICATIONS ====================

        public Uni<Void> sendOverdueNotification(HumanTask task) {
                LOG.warn("Sending overdue notification for task: {}", task.getId().value());

                return userDirectory.getUserEmail(task.getCurrentAssignment().getAssigneeIdentifier())
                                .flatMap(email -> {
                                        if (email == null) {
                                                return Uni.createFrom().voidItem();
                                        }

                                        String taskUrl = appBaseUrl + "/tasks/" + task.getId().value();

                                        Duration overdueDuration = Duration.between(
                                                        task.getDueDate(),
                                                        Instant.now());

                                        String subject = String.format(
                                                        "[Gamelan] OVERDUE: %s (Overdue by %d hours)",
                                                        task.getTitle(),
                                                        overdueDuration.toHours());

                                        String body = buildEmailTemplate(
                                                        "Task Overdue",
                                                        task,
                                                        taskUrl,
                                                        String.format(
                                                                        "This task is now overdue by %d hours. Please complete it as soon as possible.",
                                                                        overdueDuration.toHours()),
                                                        "#dc2626" // Red color
                                        );

                                        return mailer.send(Mail.withHtml(email, subject, body))
                                                        .replaceWithVoid();
                                });
        }

        // ==================== ESCALATION NOTIFICATIONS ====================

        public Uni<Void> sendEscalationNotification(HumanTask task, String escalatedTo) {
                LOG.info("Sending escalation notification to: {}", escalatedTo);

                return userDirectory.getUserEmail(escalatedTo)
                                .flatMap(email -> {
                                        if (email == null) {
                                                return Uni.createFrom().voidItem();
                                        }

                                        String taskUrl = appBaseUrl + "/tasks/" + task.getId().value();

                                        String subject = String.format(
                                                        "[Gamelan] ESCALATED: %s",
                                                        task.getTitle());

                                        String body = buildEmailTemplate(
                                                        "Task Escalated to You",
                                                        task,
                                                        taskUrl,
                                                        "This task has been escalated to you for immediate attention.",
                                                        "#dc2626");

                                        List<Uni<Void>> notifications = new ArrayList<>();
                                        notifications.add(mailer.send(Mail.withHtml(email, subject, body))
                                                        .replaceWithVoid());

                                        // Also notify original assignee
                                        String originalAssignee = task.getEscalationState()
                                                        .originalAssignment()
                                                        .getAssigneeIdentifier();

                                        Uni<Void> originalNotification = userDirectory.getUserEmail(originalAssignee)
                                                        .flatMap(originalEmail -> {
                                                                if (originalEmail == null) {
                                                                        return Uni.createFrom().voidItem();
                                                                }

                                                                String originalSubject = String.format(
                                                                                "[Gamelan] Task Escalated: %s",
                                                                                task.getTitle());

                                                                String originalBody = String.format(
                                                                                """
                                                                                                <html>
                                                                                                <body style="font-family: Arial, sans-serif;">
                                                                                                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                                                                                        <h2 style="color: #dc2626;">Task Escalated</h2>

                                                                                                        <p>The following task assigned to you has been escalated to %s:</p>

                                                                                                        <div style="background: #f3f4f6; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                                                                                            <h3 style="margin-top: 0;">%s</h3>
                                                                                                            <p><strong>Escalation Reason:</strong> %s</p>
                                                                                                        </div>

                                                                                                        <p>
                                                                                                            <a href="%s" style="background: #3b82f6; color: white; padding: 10px 20px;
                                                                                                               text-decoration: none; border-radius: 5px; display: inline-block;">
                                                                                                                View Task
                                                                                                            </a>
                                                                                                        </p>
                                                                                                    </div>
                                                                                                </body>
                                                                                                </html>
                                                                                                """,
                                                                                escalatedTo,
                                                                                task.getTitle(),
                                                                                task.getEscalationState().reason(),
                                                                                taskUrl);

                                                                return mailer.send(Mail.withHtml(originalEmail,
                                                                                originalSubject, originalBody))
                                                                                .replaceWithVoid();
                                                        });

                                        notifications.add(originalNotification);

                                        return Uni.join().all(notifications).andFailFast()
                                                        .replaceWithVoid();
                                });
        }

        // ==================== COMMENT NOTIFICATIONS ====================

        public Uni<Void> sendTaskCommentNotification(
                        HumanTask task,
                        String commentBy,
                        String comment) {

                LOG.debug("Sending comment notification for task: {}", task.getId().value());

                // Notify assignee
                return userDirectory.getUserEmail(task.getCurrentAssignment().getAssigneeIdentifier())
                                .flatMap(email -> {
                                        if (email == null || task.getCurrentAssignment()
                                                        .getAssigneeIdentifier().equals(commentBy)) {
                                                return Uni.createFrom().voidItem();
                                        }

                                        String taskUrl = appBaseUrl + "/tasks/" + task.getId().value();

                                        String subject = String.format(
                                                        "[Gamelan] New Comment on: %s",
                                                        task.getTitle());

                                        String body = String.format(
                                                        """
                                                                        <html>
                                                                        <body style="font-family: Arial, sans-serif;">
                                                                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                                                                <h2>New Comment</h2>

                                                                                <p>%s added a comment to task:</p>

                                                                                <div style="background: #f3f4f6; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                                                                    <h3 style="margin-top: 0;">%s</h3>
                                                                                </div>

                                                                                <div style="background: #e5e7eb; padding: 15px; border-left: 4px solid #3b82f6; margin: 20px 0;">
                                                                                    <p style="margin: 0;"><em>"%s"</em></p>
                                                                                </div>

                                                                                <p>
                                                                                    <a href="%s" style="background: #3b82f6; color: white; padding: 10px 20px;
                                                                                       text-decoration: none; border-radius: 5px; display: inline-block;">
                                                                                        View Task
                                                                                    </a>
                                                                                </p>
                                                                            </div>
                                                                        </body>
                                                                        </html>
                                                                        """,
                                                        commentBy,
                                                        task.getTitle(),
                                                        comment,
                                                        taskUrl);

                                        return mailer.send(Mail.withHtml(email, subject, body))
                                                        .replaceWithVoid();
                                });
        }

        // ==================== SLACK NOTIFICATIONS ====================

        public Uni<Void> sendSlackNotification(HumanTask task) {
                if (!slackEnabled || slackWebhookUrl.isEmpty()) {
                        return Uni.createFrom().voidItem();
                }

                LOG.debug("Sending Slack notification for task: {}", task.getId().value());

                String taskUrl = appBaseUrl + "/tasks/" + task.getId().value();

                Map<String, Object> payload = Map.of(
                                "text", "New Task Assigned",
                                "blocks", List.of(
                                                Map.of(
                                                                "type", "header",
                                                                "text", Map.of(
                                                                                "type", "plain_text",
                                                                                "text", "📋 New Task Assigned")),
                                                Map.of(
                                                                "type", "section",
                                                                "fields", List.of(
                                                                                Map.of(
                                                                                                "type", "mrkdwn",
                                                                                                "text",
                                                                                                "*Task:*\n" + task
                                                                                                                .getTitle()),
                                                                                Map.of(
                                                                                                "type", "mrkdwn",
                                                                                                "text",
                                                                                                "*Priority:*\n" + getPriorityEmoji(
                                                                                                                task.getPriority())
                                                                                                                +
                                                                                                                " "
                                                                                                                + task.getPriority()),
                                                                                Map.of(
                                                                                                "type", "mrkdwn",
                                                                                                "text",
                                                                                                "*Due Date:*\n" +
                                                                                                                (task.getDueDate() != null
                                                                                                                                ? task.getDueDate()
                                                                                                                                : "Not set")))),
                                                Map.of(
                                                                "type", "section",
                                                                "text", Map.of(
                                                                                "type", "mrkdwn",
                                                                                "text",
                                                                                task.getDescription() != null
                                                                                                ? task.getDescription()
                                                                                                : "")),
                                                Map.of(
                                                                "type", "actions",
                                                                "elements", List.of(
                                                                                Map.of(
                                                                                                "type", "button",
                                                                                                "text", Map.of(
                                                                                                                "type",
                                                                                                                "plain_text",
                                                                                                                "text",
                                                                                                                "View Task"),
                                                                                                "url", taskUrl,
                                                                                                "style", "primary")))));

                return webClient.postAbs(slackWebhookUrl.get())
                                .sendJson(payload)
                                .replaceWithVoid()
                                .invoke(() -> LOG.debug("Slack notification sent"))
                                .onFailure().invoke(error -> LOG.error("Failed to send Slack notification", error));
        }

        // ==================== MICROSOFT TEAMS NOTIFICATIONS ====================

        public Uni<Void> sendTeamsNotification(HumanTask task) {
                if (!teamsEnabled || teamsWebhookUrl.isEmpty()) {
                        return Uni.createFrom().voidItem();
                }

                LOG.debug("Sending Teams notification for task: {}", task.getId().value());

                String taskUrl = appBaseUrl + "/tasks/" + task.getId().value();

                Map<String, Object> payload = Map.of(
                                "@type", "MessageCard",
                                "@context", "https://schema.org/extensions",
                                "summary", "New Task Assigned",
                                "themeColor", "0078D4",
                                "title", "📋 New Task Assigned",
                                "sections", List.of(
                                                Map.of(
                                                                "facts", List.of(
                                                                                Map.of("name", "Task", "value",
                                                                                                task.getTitle()),
                                                                                Map.of("name", "Priority", "value",
                                                                                                String.valueOf(task
                                                                                                                .getPriority())),
                                                                                Map.of("name", "Due Date", "value",
                                                                                                task.getDueDate() != null
                                                                                                                ? task.getDueDate()
                                                                                                                                .toString()
                                                                                                                : "Not set")),
                                                                "text",
                                                                task.getDescription() != null ? task.getDescription()
                                                                                : "")),
                                "potentialAction", List.of(
                                                Map.of(
                                                                "@type", "OpenUri",
                                                                "name", "View Task",
                                                                "targets", List.of(
                                                                                Map.of("os", "default", "uri",
                                                                                                taskUrl)))));

                return webClient.postAbs(teamsWebhookUrl.get())
                                .sendJson(payload)
                                .replaceWithVoid()
                                .invoke(() -> LOG.debug("Teams notification sent"))
                                .onFailure().invoke(error -> LOG.error("Failed to send Teams notification", error));
        }

        // ==================== HELPER METHODS ====================

        private String buildEmailTemplate(
                        String heading,
                        HumanTask task,
                        String taskUrl,
                        String message) {
                return buildEmailTemplate(heading, task, taskUrl, message, "#3b82f6");
        }

        private String buildEmailTemplate(
                        String heading,
                        HumanTask task,
                        String taskUrl,
                        String message,
                        String accentColor) {

                return String.format(
                                """
                                                <html>
                                                <body style="font-family: Arial, sans-serif;">
                                                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                                        <div style="background: %s; color: white; padding: 20px; border-radius: 5px 5px 0 0;">
                                                            <h1 style="margin: 0;">%s</h1>
                                                        </div>

                                                        <div style="border: 1px solid #e5e7eb; padding: 20px; border-radius: 0 0 5px 5px;">
                                                            <p>%s</p>

                                                            <div style="background: #f3f4f6; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                                                <h3 style="margin-top: 0; color: %s;">%s</h3>
                                                                <p style="margin: 5px 0;"><strong>Type:</strong> %s</p>
                                                                <p style="margin: 5px 0;"><strong>Priority:</strong> %s %s</p>
                                                                %s
                                                                %s
                                                            </div>

                                                            %s

                                                            <p>
                                                                <a href="%s" style="background: %s; color: white; padding: 12px 24px;
                                                                   text-decoration: none; border-radius: 5px; display: inline-block;
                                                                   font-weight: bold;">
                                                                    View Task
                                                                </a>
                                                            </p>

                                                            <p style="color: #6b7280; font-size: 12px; margin-top: 30px; border-top: 1px solid #e5e7eb; padding-top: 15px;">
                                                                This is an automated notification from Gamelan Workflow Engine.<br>
                                                                Task ID: %s
                                                            </p>
                                                        </div>
                                                    </div>
                                                </body>
                                                </html>
                                                """,
                                accentColor,
                                heading,
                                message,
                                accentColor,
                                task.getTitle(),
                                task.getTaskType(),
                                getPriorityEmoji(task.getPriority()),
                                task.getPriority(),
                                task.getDescription() != null
                                                ? "<p style=\"margin: 5px 0;\"><strong>Description:</strong> " +
                                                                task.getDescription() + "</p>"
                                                : "",
                                task.getDueDate() != null ? "<p style=\"margin: 5px 0;\"><strong>Due Date:</strong> " +
                                                task.getDueDate() + "</p>" : "",
                                task.getContext().containsKey("workflowName")
                                                ? "<p><em>Part of workflow: " + task.getContext().get("workflowName")
                                                                + "</em></p>"
                                                : "",
                                taskUrl,
                                accentColor,
                                task.getId().value());
        }

        private String getPriorityEmoji(int priority) {
                return switch (priority) {
                        case 5 -> "🔴";
                        case 4 -> "🟠";
                        case 3 -> "🟡";
                        case 2 -> "🟢";
                        case 1 -> "⚪";
                        default -> "⚫";
                };
        }
}