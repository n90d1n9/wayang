# Skill Analytics Module

Track skill usage patterns, performance metrics, and identify optimization opportunities.

## Features

- **Usage Tracking**: Monitor skill execution counts, success rates, and tenant usage
- **Performance Metrics**: Track latency, throughput, and cache effectiveness
- **Top Skills Reports**: Identify most popular and frequently used skills
- **Real-time Analytics**: Asynchronous event processing with minimal overhead
- **Micrometer Integration**: Export metrics to monitoring systems

## Architecture

```
Skill Execution → Analytics Observer → Event Queue → Analytics Service → Metrics
                                                              ↓
                                                       In-Memory Cache
                                                              ↓
                                                       Top Skills Report
```

## Usage

### Record Events

The `SkillAnalyticsObserver` automatically captures events from skill executions:

```java
@Inject
SkillAnalyticsObserver observer;

// Automatically called during skill lifecycle
observer.onExecutionStarted(skillId, tenantId, userId, eventId);
observer.onExecutionCompleted(skillId, tenantId, userId, eventId, durationMs);
observer.onExecutionFailed(skillId, tenantId, userId, eventId, errorMessage);
observer.onCacheHit(skillId, tenantId);
observer.onCacheMiss(skillId, tenantId);
```

### Query Analytics

```java
@Inject
SkillAnalyticsService analytics;

// Get analytics for a specific skill
Uni<SkillAnalytics> skillStats = analytics.getSkillAnalytics("my-skill");

// Get top 10 most used skills
Uni<TopSkillsReport> topSkills = analytics.getTopSkills(10);

// Get tenant usage statistics
Uni<TenantUsageStats> tenantStats = analytics.getTenantUsage("tenant-123");
```

### Configuration

```yaml
wayang:
  analytics:
    enabled: true
    retention-period: 7d
    max-tracked-skills: 100
    export-metrics: true
    sampling-rate: 1.0
```

## Metrics Exported

| Metric Name | Type | Description |
|-------------|------|-------------|
| `wayang.skill.executions.total` | Counter | Total skill executions |
| `wayang.skill.executions.successful` | Counter | Successful executions |
| `wayang.skill.executions.failed` | Counter | Failed executions |
| `wayang.skill.execution.duration` | Timer | Execution duration |
| `wayang.skill.execution.latency` | Histogram | Latency distribution |

## Performance

- **Event Processing**: <5ms async overhead
- **Memory**: ~1KB per tracked skill
- **Throughput**: 10,000+ events/sec
- **Queue Size**: Configurable (default 1000)

## Integration Points

- **Skill Management**: Tracks skill lifecycle events
- **Skill Audit**: Complements audit logs with performance data
- **Agent Core**: Integrated into skill execution pipeline
- **Monitoring**: Exports to Prometheus, Grafana, etc.

## Files

- `SkillUsageEvent.java` - Event record for analytics
- `SkillAnalyticsService.java` - Core analytics service
- `SkillAnalyticsObserver.java` - Event observer and processor
- `SkillAnalytics.java` - Analytics data record
- `TopSkillsReport.java` - Top skills reporting
- `AnalyticsConfig.java` - Configuration interface

## License

Apache 2.0
