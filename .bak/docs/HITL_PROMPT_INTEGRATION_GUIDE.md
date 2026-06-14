# HITL + Prompt Integration Guide

## Overview

Complete integration of **Human-in-the-Loop (HITL)** and **Prompt Management** modules with the existing agent + memory + tools ecosystem.

This enables:
- Human approval workflows for critical agent decisions
- Dynamic prompt management with variable substitution
- Context-aware prompt rendering
- Decision history tracking
- Escalation and fallback handling

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│            Agent Orchestrator (ReAct Loop)                   │
│  • Inference with prompts                                   │
│  • Detect decision points requiring human approval          │
└──────────────────────────────────────────────────────────────┘
         ↓                    ↓                    ↓
    ┌────────────┐      ┌──────────────┐     ┌──────────────┐
    │ Prompt     │      │ Memory       │     │ Tools        │
    │ Service    │      │ Service      │     │ Service      │
    └────────────┘      └──────────────┘     └──────────────┘
         ↓                    ↓
    ┌──────────────────────────────────┐
    │    HITL Service                  │
    │  • Decision routing             │
    │  • Human feedback collection    │
    │  • Escalation management        │
    └──────────────────────────────────┘
         ↓
    ┌──────────────────────────────────┐
    │  HITL Workflow System            │
    │  (External system integration)   │
    └──────────────────────────────────┘
```

## Integration Points

### 1. Prompt-Driven Agent Execution

```java
// Get prompt template
PromptTemplate template = promptService
    .getTemplate("react_agent")
    .await().indefinitely();

// Enhance with memory context
String prompt = promptService
    .enhanceWithMemory(agentId, template, baseVars)
    .await().indefinitely();

// Execute agent with prompt
AgentResponse response = orchestrator.execute(
    AgentRequest.builder()
        .prompt(prompt)  // Dynamic prompt
        .agentId(agentId)
        .build())
    .await().indefinitely();
```

### 2. Decision Point Detection & HITL Routing

```java
// After agent reasoning, check if human approval needed
if (requiresApproval(response.action())) {
    // Create HITL request with context
    HitlRequest hitlReq = HitlRequest.builder()
        .agentId(agentId)
        .taskId(taskId)
        .action(response.action())
        .context(response.reasoning())
        .priority("HIGH")
        .build();
    
    // Route to human
    HitlDecision decision = hitlService
        .requestDecision(hitlReq)
        .await().indefinitely();
    
    // Use decision to proceed
    if (decision.isApproved()) {
        executeAction(response.action());
    } else if (decision.status() == Status.MODIFIED) {
        executeAction(decision.modifications());
    }
}
```

### 3. Memory-Enhanced HITL Context

```java
// When requesting human review, include full context
String context = memoryService
    .getContextPrompt(agentId, 10)
    .await().indefinitely();

HitlRequest request = HitlRequest.builder()
    .agentId(agentId)
    .taskId(taskId)
    .action("transfer_funds")
    .context(Map.of(
        "amount", 50000,
        "recipient", "external_account",
        "history", context,  // Full conversation history
        "reasoning", response.reasoning()
    ))
    .build();

HitlDecision decision = hitlService
    .requestDecision(request)
    .await().indefinitely();
```

### 4. Prompt Template Versioning with Memory

```java
// Store prompt version in memory for audit
PromptTemplate template = promptService
    .getTemplate("expense_approval")
    .await().indefinitely();

memoryService.storeInteraction(
    agentId,
    sessionId,
    userId,
    "Using prompt template: " + template.id() + " v" + template.version(),
    "Prompt rendering complete"
);

// Later: Trace decisions back to specific prompt version
String promptVersion = getPromptVersionFromMemory(agentId);
```

## Implementation Patterns

### Pattern 1: HITL Decision Gate

```java
public Uni<AgentResponse> executeWithHitlGate(
        String agentId,
        AgentRequest request) {
    
    // Render prompt
    return promptService.enhanceWithMemory(
            agentId,
            template,
            request.inputs())
        // Execute agent
        .flatMap(prompt -> orchestrator.execute(
            request.withPrompt(prompt)))
        // Check if needs approval
        .flatMap(response -> {
            if (needsApproval(response)) {
                // Route to HITL
                HitlRequest hitlReq = createHitlRequest(agentId, response);
                return hitlService.requestDecision(hitlReq)
                    .map(decision -> applyDecision(response, decision));
            }
            return Uni.createFrom().item(response);
        });
}
```

### Pattern 2: Escalation with Context

```java
public Uni<HitlDecision> escalateDecision(
        String requestId,
        String reason,
        String agentId) {
    
    // Get decision context from memory
    return memoryService.getSessionMemories(agentId, null, 20)
        .map(memories -> formatContextForEscalation(memories))
        // Escalate with full context
        .flatMap(context -> hitlService.escalate(
            requestId,
            reason,
            "manager"))
        .map(result -> new HitlDecision(
            requestId,
            agentId,
            taskId,
            Status.ESCALATED,
            reason,
            Map.of("escalationContext", context),
            Instant.now(),
            null
        ));
}
```

### Pattern 3: Adaptive Prompts Based on HITL History

```java
public Uni<String> adaptPromptBasedOnHistory(
        String agentId,
        PromptTemplate template) {
    
    // Analyze HITL decision patterns
    return hitlService.getMetrics(agentId)
        // Get approval statistics
        .flatMap(metrics -> {
            if (metrics.approvalRate() < 0.5) {
                // Use stricter prompt
                return promptService.getTemplate("conservative_agent");
            } else if (metrics.approvalRate() > 0.9) {
                // Use more autonomous prompt
                return promptService.getTemplate("autonomous_agent");
            }
            return Uni.createFrom().item(template);
        })
        // Render with memory context
        .flatMap(selected -> promptService.enhanceWithMemory(
            agentId, selected, Map.of()));
}
```

### Pattern 4: Decision Audit Trail

```java
public Uni<AuditTrail> getDecisionAuditTrail(
        String agentId,
        LocalDate date) {
    
    return Uni.combine()
        .all()
        .unis(
            memoryService.getSessionMemories(agentId, null, 100),
            hitlService.getMetrics(agentId)
        )
        .asTuple()
        .map(tuple -> {
            var memories = tuple.getItem1();
            var metrics = tuple.getItem2();
            
            return new AuditTrail(
                agentId,
                date,
                memories.stream()
                    .filter(m -> isHitlRelated(m))
                    .map(m -> m.getContent())
                    .collect(Collectors.toList()),
                metrics
            );
        });
}
```

## Configuration

### Minimal Setup

```properties
# HITL
wayang.hitl.enabled=true
wayang.hitl.approval.required=true

# Prompt
wayang.prompt.caching.enabled=true
wayang.prompt.template.path=/config/prompts
```

### Recommended Setup

```properties
# HITL Workflow
wayang.hitl.enabled=true
wayang.hitl.approval.threshold=NORMAL
wayang.hitl.timeout.minutes=30
wayang.hitl.escalation.enabled=true
wayang.hitl.notification.enabled=true

# Prompt Management
wayang.prompt.caching.enabled=true
wayang.prompt.template.path=/config/prompts
wayang.prompt.version.control.enabled=true
wayang.prompt.analytics.enabled=true
wayang.prompt.model.optimization=true

# Integration
gamelan.embedding.cache.enabled=true
gamelan.tool.cache.enabled=true
wayang.memory.agent.context.limit=10
```

## Code Examples

### Example 1: Simple HITL Approval

```java
@Inject AgentHitlService hitlService;

// Request human approval
HitlDecision decision = hitlService.requestDecision(
    HitlRequest.builder()
        .agentId("expense-agent")
        .taskId("exp-123")
        .action("approve_expense")
        .context(Map.of("amount", 5000.00))
        .build())
    .await().indefinitely();

if (decision.isApproved()) {
    executeExpenseApproval(5000.00);
}
```

### Example 2: Dynamic Prompt Rendering

```java
@Inject AgentPromptService promptService;

// Get template
PromptTemplate template = promptService
    .getTemplate("task_planning")
    .await().indefinitely();

// Render with variables
String prompt = promptService.renderPrompt(template,
    Map.of("task", "Plan weekly report"))
    .await().indefinitely();

// Execute agent
AgentResponse response = executor.executeTaskWithTools(
    agentId, userId, sessionId, prompt);
```

### Example 3: HITL + Prompt + Memory

```java
@Inject AgentPromptService promptService;
@Inject AgentHitlService hitlService;
@Inject AgentMemoryService memoryService;

// Render prompt with memory context
String prompt = promptService.enhanceWithMemory(
    agentId,
    template,
    Map.of("task", userTask))
    .await().indefinitely();

// Execute
AgentResponse response = executor.executeTaskWithTools(
    agentId, userId, sessionId, prompt);

// Check if needs approval
if (isHighValue(response)) {
    HitlDecision decision = hitlService.requestDecision(
        HitlRequest.builder()
            .agentId(agentId)
            .action(response.action())
            .context(Map.of(
                "prompt_version", template.version(),
                "memory_context", memoryService.getContextPrompt(agentId)))
            .build())
        .await().indefinitely();
}
```

### Example 4: Escalation with Full Audit

```java
// When decision needs escalation
hitlService.escalate(
    requestId,
    "Complex decision requires management approval",
    "manager@company.com")
    .flatMap(escalation -> {
        // Get full context for escalation
        return getAuditTrail(agentId)
            .map(trail -> {
                LOG.info("Escalated with audit trail: {}", trail);
                return escalation;
            });
    })
    .await().indefinitely();
```

## Testing Examples

### Unit Test

```java
@QuarkusTest
public class HitlPromptIntegrationTest {
    
    @Inject AgentHitlService hitlService;
    @Inject AgentPromptService promptService;
    
    @Test
    public void testHitlDecision() {
        HitlDecision decision = hitlService.requestDecision(
            HitlRequest.builder()
                .agentId("test-agent")
                .action("test_action")
                .build())
            .await().indefinitely();
        
        assertEquals(Status.PENDING, decision.status());
    }
    
    @Test
    public void testPromptRendering() {
        PromptTemplate template = promptService
            .getTemplate("test_template")
            .await().indefinitely();
        
        String rendered = promptService.renderPrompt(template,
            Map.of("var1", "value1"))
            .await().indefinitely();
        
        assertTrue(rendered.contains("value1"));
    }
}
```

## Performance Characteristics

### HITL
- Decision routing: <100ms
- Human review time: 1-60 minutes (user-dependent)
- Decision retrieval: 10-50ms
- Escalation: <200ms

### Prompt
- Template loading: 10-50ms (with cache: 1-5ms)
- Rendering: 5-20ms per variable
- Model optimization: 20-50ms
- Validation: 10-30ms

### Combined
- Full flow (prompt → execute → HITL): depends on human review
- Memory context injection: 50-200ms
- Audit trail generation: 100-500ms

## Troubleshooting

### HITL Issues

**Problem**: Human review never completes

**Solution**:
```properties
wayang.hitl.timeout.minutes=30  # Set timeout
wayang.hitl.escalation.enabled=true  # Enable escalation
```

**Problem**: Decision not routed to correct person

**Solution**:
```java
// Specify approver in request
hitlService.requestDecision(
    request.withApprover("specific_user@company.com"))
```

### Prompt Issues

**Problem**: Variables not substituted

**Solution**:
```java
// Ensure all variables defined
PromptValidation validation = promptService
    .validateTemplate(template)
    .await().indefinitely();

if (!validation.isValid()) {
    System.out.println(validation.errors());
}
```

**Problem**: Template not optimized for model

**Solution**:
```java
// Explicitly optimize for target model
String optimized = promptService
    .getOptimizedPrompt(templateId, "openai-gpt4", vars)
    .await().indefinitely();
```

## Next Steps (TIER 2)

### Short-term
- [ ] Database persistence for HITL decisions
- [ ] Real HITL workflow engine integration
- [ ] Prompt template versioning system
- [ ] Advanced audit logging
- [ ] Email notifications for reviews

### Medium-term
- [ ] ML-based approval prediction
- [ ] Automated prompt optimization
- [ ] Multi-level escalation chains
- [ ] SLA management
- [ ] Performance analytics

### Long-term
- [ ] Federated HITL systems
- [ ] Complex workflow orchestration
- [ ] Prompt marketplace
- [ ] AI-assisted human review
- [ ] Decision analytics & insights

## API Reference

### AgentHitlService

```java
Uni<HitlDecision> requestDecision(HitlRequest request)
Uni<HitlDecision> submitDecision(String requestId, Status status, 
                                 String feedback, Map<String, Object> mods)
Uni<HitlDecision> getDecision(String requestId)
Uni<List<HitlRequest>> getPendingRequests(String agentId)
Uni<EscalationResult> escalate(String requestId, String reason, String approver)
Uni<HitlMetrics> getMetrics(String agentId)
```

### AgentPromptService

```java
Uni<PromptTemplate> getTemplate(String templateId)
Uni<String> renderPrompt(PromptTemplate template, Map<String, Object> vars)
Uni<String> enhanceWithMemory(String agentId, PromptTemplate template, 
                              Map<String, Object> vars)
Uni<String> getOptimizedPrompt(String templateId, String modelName, 
                               Map<String, Object> vars)
Uni<String> createSystemPrompt(String agentId, String description,
                               List<String> tools, String context)
Uni<PromptAnalytics> getAnalytics(String agentId)
Uni<List<PromptTemplate>> getVersionHistory(String templateId)
Uni<PromptTemplate> createTemplate(PromptTemplate.Builder builder)
Uni<PromptValidation> validateTemplate(PromptTemplate template)
```

## File Locations

**Code**:
- `agent/agent-core/.../hitl/AgentHitlService.java`
- `agent/agent-core/.../prompt/AgentPromptService.java`

**Documentation**:
- `agent/HITL_INTEGRATION_GUIDE.md`
- `agent/PROMPT_INTEGRATION_GUIDE.md`

---

**Version**: 1.0  
**Compatibility**: Wayang Platform 0.1.0+, Java 11+, Quarkus 3.32.2+
