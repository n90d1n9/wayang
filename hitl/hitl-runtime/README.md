# Gamelan Human Task Executor - Complete Production Implementation

## 🎯 Overview

A **complete, production-ready** Human-in-the-Loop executor for the Gamelan workflow engine. This implementation includes:

✅ **Full Domain Model** with Event Sourcing  
✅ **Reactive Persistence** with PostgreSQL  
✅ **Complete REST API** with all CRUD operations  
✅ **Multi-channel Notifications** (Email, Slack, Teams)  
✅ **Automatic Escalation** based on SLA/priority  
✅ **Timeout Management** with scheduled jobs  
✅ **Complete Audit Trail** for compliance  
✅ **Multi-tenancy Support**  
✅ **Production-grade Error Handling**  

## 📁 Project Structure

```
tech.kayys.gamelan.executor.human/
├── domain/
│   ├── HumanTask.java              # Aggregate root
│   ├── HumanTaskId.java            # Value objects
│   ├── TaskAssignment.java
│   ├── HumanTaskStatus.java        # Enums
│   └── HumanTaskEvent.java         # Domain events
├── persistence/
│   ├── HumanTaskEntity.java        # JPA entities
│   ├── HumanTaskRepository.java    # Repository
│   └── HumanTaskQueryService.java  # Query service
├── api/
│   └── HumanTaskResource.java      # REST API
├── HumanTaskExecutor.java          # Main executor
├── HumanTaskService.java           # Application service
├── EscalationService.java          # Escalation logic
└── NotificationService.java        # Notifications
```

## 🚀 Quick Start

### 1. Prerequisites

- Java 17+
- Docker & Docker Compose
- PostgreSQL 15+ (or use Docker)
- Maven or Gradle

### 2. Start Database

```bash
docker-compose up -d postgres
```

### 3. Run Application

```bash
# Using Gradle
./gradlew quarkusDev

# Using Maven
mvn quarkus:dev
```

### 4. Access APIs

- Swagger UI: http://localhost:8080/swagger-ui
- Health Check: http://localhost:8080/q/health
- Metrics: http://localhost:8080/q/metrics

## 💡 Usage Examples

### Create a Task (via Workflow)

```java
// In workflow definition node config
Map<String, Object> humanTaskConfig = Map.of(
    "assignTo", "manager@company.com",
    "assigneeType", "USER",
    "taskType", "approval",
    "title", "Approve Budget Request",
    "description", "Please review Q4 budget request for $50,000",
    "priority", 4,
    "dueInHours", 48,
    "formData", Map.of(
        "amount", 50000,
        "department", "Engineering",
        "quarter", "Q4"
    ),
    "escalationConfig", Map.of(
        "escalateTo", "director@company.com",
        "escalateAfterHours", 24
    )
);
```

### API Examples

#### Get My Tasks

```bash
curl -X GET "http://localhost:8080/api/v1/tasks/my-tasks?status=ASSIGNED,IN_PROGRESS" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Response:
```json
[
  {
    "taskId": "TASK-abc123",
    "title": "Approve Budget Request",
    "priority": 4,
    "status": "ASSIGNED",
    "dueDate": "2024-01-15T18:00:00Z",
    "formData": {
      "amount": 50000,
      "department": "Engineering"
    }
  }
]
```

#### Claim a Task

```bash
curl -X POST "http://localhost:8080/api/v1/tasks/TASK-abc123/claim" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Approve a Task

```bash
curl -X POST "http://localhost:8080/api/v1/tasks/TASK-abc123/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "comments": "Budget approved with conditions",
    "data": {
      "approved": true,
      "conditions": ["Monthly reports required", "Cap at $45k"]
    }
  }'
```

#### Delegate a Task

```bash
curl -X POST "http://localhost:8080/api/v1/tasks/TASK-abc123/delegate" \
  -H "Content-Type: application/json" \
  -d '{
    "toUserId": "john.doe@company.com",
    "reason": "You have more context on this project"
  }'
```

#### Get Task History

```bash
curl -X GET "http://localhost:8080/api/v1/tasks/TASK-abc123/history"
```

Response:
```json
{
  "taskId": "TASK-abc123",
  "auditTrail": [
    {
      "action": "CREATED",
      "details": "Task created",
      "performedBy": "SYSTEM",
      "timestamp": "2024-01-13T10:00:00Z"
    },
    {
      "action": "CLAIMED",
      "details": "Task claimed",
      "performedBy": "manager@company.com",
      "timestamp": "2024-01-13T10:30:00Z"
    },
    {
      "action": "APPROVED",
      "details": "Task approved: Budget approved with conditions",
      "performedBy": "manager@company.com",
      "timestamp": "2024-01-13T11:00:00Z"
    }
  ],
  "assignmentHistory": [
    {
      "assigneeIdentifier": "manager@company.com",
      "assignedBy": "SYSTEM",
      "assignedAt": "2024-01-13T10:00:00Z"
    }
  ]
}
```

## 🎨 Features in Detail

### 1. Task Lifecycle

```
CREATED → ASSIGNED → IN_PROGRESS → COMPLETED
                ↓
           ESCALATED → IN_PROGRESS → COMPLETED
                ↓
           CANCELLED / EXPIRED
```

### 2. Escalation Rules

Tasks are automatically escalated based on priority:

| Priority | Escalation Time | Icon |
|----------|----------------|------|
| 5 (Critical) | 4 hours | 🔴 |
| 4 (High) | 12 hours | 🟠 |
| 3 (Medium) | 24 hours | 🟡 |
| 2 (Low) | 48 hours | 🟢 |
| 1 (Lowest) | 72 hours | ⚪ |

### 3. Notification Types

- **Task Assigned**: Email sent when task is assigned
- **Task Reminder**: 24 hours before due date
- **Task Overdue**: When task passes due date
- **Task Escalated**: When task is escalated to manager
- **New Comment**: When someone adds a comment

### 4. Security Features

- ✅ User can only complete tasks assigned to them
- ✅ Only assignee can delegate tasks
- ✅ Multi-tenant data isolation
- ✅ Audit trail for all actions
- ✅ Optimistic locking to prevent conflicts

## 📊 Database Schema

### Tables

1. **human_tasks** - Main task data
2. **human_task_assignments** - Assignment history
3. **human_task_audit** - Complete audit trail

### Example Queries

```sql
-- Get user's active tasks
SELECT * FROM human_tasks
WHERE assignee_identifier = 'user@company.com'
  AND status IN ('ASSIGNED', 'IN_PROGRESS')
ORDER BY priority DESC, created_at;

-- Get overdue tasks
SELECT * FROM human_tasks
WHERE due_date < NOW()
  AND status NOT IN ('COMPLETED', 'CANCELLED', 'EXPIRED');

-- Task statistics
SELECT 
    status,
    COUNT(*) as count,
    AVG(priority) as avg_priority
FROM human_tasks
GROUP BY status;
```

## ⚙️ Configuration

### Environment Variables

```bash
# Database
QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://localhost:5432/gamelan_db
QUARKUS_DATASOURCE_USERNAME=gamelan
QUARKUS_DATASOURCE_PASSWORD=your_password

# Email (Gmail example)
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Slack (optional)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK

# Teams (optional)
TEAMS_WEBHOOK_URL=https://outlook.office.com/webhook/YOUR/WEBHOOK
```

### Application Properties

```yaml
gamelan:
  notifications:
    email:
      enabled: true
    reminder:
      hours-before-due: 24
  executor:
    max-concurrent-tasks: 100
```

## 🧪 Testing

### Unit Tests

```java
@QuarkusTest
class HumanTaskTest {
    
    @Test
    void shouldCreateTask() {
        HumanTask task = HumanTask.builder()
            .workflowRunId("RUN-123")
            .nodeId("NODE-1")
            .tenantId("tenant-1")
            .taskType("approval")
            .title("Test Task")
            .priority(3)
            .build();
        
        assertEquals(HumanTaskStatus.CREATED, task.getStatus());
    }
    
    @Test
    void shouldClaimTask() {
        // Create and assign task
        TaskAssignment assignment = TaskAssignment.builder()
            .assigneeType(AssigneeType.USER)
            .assigneeIdentifier("user@test.com")
            .assignedBy("SYSTEM")
            .build();
        
        HumanTask task = HumanTask.builder()
            .workflowRunId("RUN-123")
            .nodeId("NODE-1")
            .tenantId("tenant-1")
            .taskType("approval")
            .title("Test Task")
            .assignTo(assignment)
            .build();
        
        // Claim task
        task.claim("user@test.com");
        
        assertEquals(HumanTaskStatus.IN_PROGRESS, task.getStatus());
        assertNotNull(task.getClaimedAt());
    }
    
    @Test
    void shouldNotAllowUnauthorizedClaim() {
        TaskAssignment assignment = TaskAssignment.builder()
            .assigneeType(AssigneeType.USER)
            .assigneeIdentifier("user1@test.com")
            .assignedBy("SYSTEM")
            .build();
        
        HumanTask task = HumanTask.builder()
            .workflowRunId("RUN-123")
            .nodeId("NODE-1")
            .tenantId("tenant-1")
            .taskType("approval")
            .title("Test Task")
            .assignTo(assignment)
            .build();
        
        // Try to claim with different user
        assertThrows(SecurityException.class, () -> {
            task.claim("user2@test.com");
        });
    }
}
```

### Integration Tests

```java
@QuarkusTest
class HumanTaskResourceTest {
    
    @Test
    void shouldGetMyTasks() {
        given()
            .when().get("/api/v1/tasks/my-tasks")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }
    
    @Test
    void shouldClaimTask() {
        given()
            .when().post("/api/v1/tasks/TASK-123/claim")
            .then()
            .statusCode(200)
            .body("status", equalTo("IN_PROGRESS"));
    }
    
    @Test
    void shouldApproveTask() {
        Map<String, Object> request = Map.of(
            "comments", "Approved",
            "data", Map.of("approved", true)
        );
        
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when().post("/api/v1/tasks/TASK-123/approve")
            .then()
            .statusCode(200);
    }
}
```

## 📈 Monitoring

### Health Checks

```bash
curl http://localhost:8080/q/health

# Response
{
  "status": "UP",
  "checks": [
    {
      "name": "Database connection health check",
      "status": "UP"
    },
    {
      "name": "SmtpHealthCheck",
      "status": "UP"
    }
  ]
}
```

### Metrics

```bash
curl http://localhost:8080/q/metrics

# Key metrics
human_tasks_created_total
human_tasks_completed_total
human_tasks_escalated_total
human_tasks_overdue_total
```

## 🔧 Troubleshooting

### Common Issues

1. **Email not sending**
   - Check SMTP credentials
   - Verify firewall rules
   - Enable "Less secure app access" for Gmail

2. **Database connection failed**
   - Verify PostgreSQL is running
   - Check connection string
   - Ensure database exists

3. **Tasks not escalating**
   - Check scheduler is enabled
   - Verify escalation configuration
   - Check logs for scheduler execution

### Logs

```bash
# View logs
docker-compose logs -f gamelan-app

# Filter by level
docker-compose logs -f gamelan-app | grep ERROR

# View specific service logs
docker-compose logs -f postgres
```

## 🚀 Deployment

### Docker Build

```bash
# Build native image
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true

# Build Docker image
docker build -f src/main/docker/Dockerfile.native -t gamelan-human-task:1.0.0 .

# Run container
docker run -p 8080:8080 gamelan-human-task:1.0.0
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gamelan-human-task
spec:
  replicas: 3
  selector:
    matchLabels:
      app: gamelan-human-task
  template:
    metadata:
      labels:
        app: gamelan-human-task
    spec:
      containers:
      - name: app
        image: gamelan-human-task:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: QUARKUS_DATASOURCE_REACTIVE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
```

## 📝 License

Copyright © 2024 Kayys Technology. All rights reserved.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Add tests
4. Submit pull request

## 📞 Support

- Email: support@kayys.tech
- Docs: https://docs.gamelan.kayys.tech
- Issues: https://github.com/kayys-tech/gamelan/issues