# ProgressiveTool - Complete Guide

## Overview

`ProgressiveTool` provides a comprehensive framework for executing long-running tools with real-time progress tracking, streaming results, cancellation support, and checkpoint/resume capabilities.

## Features

### 1. Progress Tracking
- Real-time progress updates (0-100%)
- Phase-based execution model
- Status messages and estimates
- Elapsed time and ETA calculation

### 2. Streaming Results
- Stream partial results as they become available
- Non-blocking result consumption
- Sequence numbering for ordering
- End-of-stream signaling

### 3. Cancellation Support
- Cancel at any point during execution
- Graceful shutdown with cleanup
- Resource deallocation
- Status notification

### 4. Checkpoint & Resume
- Create checkpoints during execution
- Restore from checkpoints
- Multiple checkpoint support
- Automatic checkpointing

### 5. Retry Mechanism
- Automatic retry on failure
- Exponential backoff
- Configurable retry attempts
- Retry tracking

### 6. Timeout Management
- Configurable timeouts
- Timeout detection
- Graceful timeout handling

### 7. Multiple Listeners
- Add/remove progress listeners dynamically
- Multiple listeners per execution
- Listener error isolation

## Usage Examples

### Basic Execution

```java
@Inject
ProgressiveTool progressiveTool;

// Simple execution with progress tracking
String executionId = progressiveTool.execute(
    "code_generation",  // tool ID
    params,             // Map<String, Object>
    context             // Map<String, Object>
);

// Get current progress
ProgressiveTool.ToolProgress progress = progressiveTool.getProgress(executionId);
System.out.println("Progress: " + progress.percent() + "%");
System.out.println("Status: " + progress.status());
System.out.println("Message: " + progress.message());
```

### With Progress Listener

```java
// Single listener
ProgressiveTool.ProgressListener listener = progress -> {
    System.out.printf("Progress: %.1f%% - %s: %s%n", 
        progress.percent(), 
        progress.status(), 
        progress.message()
    );
    
    if (progress.isCompleted()) {
        System.out.println("Execution completed!");
    }
};

String executionId = progressiveTool.execute(
    "data_processing", params, context, listener
);
```

### Multiple Listeners

```java
List<ProgressiveTool.ProgressListener> listeners = new ArrayList<>();

// Progress logger
listeners.add(progress -> 
    LOG.infof("Progress update: %.1f%%", progress.percent())
);

// Progress bar updater
listeners.add(progress -> 
    updateProgressBar(progress.percent())
);

// Completion notifier
listeners.add(progress -> {
    if (progress.isCompleted()) {
        sendNotification("Execution completed");
    }
});

String executionId = progressiveTool.execute(
    "long_running_task", params, context, listeners
);
```

### Dynamic Listener Management

```java
String executionId = progressiveTool.execute(
    "task", params, context
);

// Add listener during execution
ProgressiveTool.ProgressListener listener = progress -> {
    System.out.println("Progress: " + progress.percent());
};
progressiveTool.addProgressListener(executionId, listener);

// Remove listener when no longer needed
progressiveTool.removeProgressListener(executionId, listener);
```

### Cancellation

```java
// Cancel specific execution
boolean cancelled = progressiveTool.cancel(executionId);
if (cancelled) {
    System.out.println("Execution cancelled successfully");
}

// Cancel all active executions
int cancelledCount = progressiveTool.cancelAll();
System.out.println("Cancelled " + cancelledCount + " executions");

// Cancel based on progress
ProgressiveTool.ProgressListener listener = progress -> {
    if (progress.percent() > 50.0 && shouldStop()) {
        progressiveTool.cancel(progress.executionId());
    }
};
```

### Streaming Results

```java
String executionId = progressiveTool.execute(
    "streaming_tool", params, context
);

// Subscribe to result stream
progressiveTool.streamResults(executionId)
    .subscribe().with(
        partialResult -> {
            System.out.println("Partial result: " + partialResult.data());
            System.out.println("Sequence: " + partialResult.sequence());
            System.out.println("Is last: " + partialResult.isLast());
        },
        error -> System.err.println("Stream error: " + error),
        () -> System.out.println("Stream completed")
    );
```

### Await Completion

```java
// Wait for completion with timeout
Uni<SkillResult> completionUni = progressiveTool.awaitCompletion(
    executionId, 
    Duration.ofSeconds(30)
);

completionUni.subscribe().with(
    result -> System.out.println("Result: " + result.observation()),
    error -> System.err.println("Execution failed: " + error)
);
```

### Checkpoints

```java
// Create checkpoint during execution
progressiveTool.createCheckpoint(executionId, Map.of(
    "phase", "data_loaded",
    "records", 1000,
    "state", serializedState
));

// Get all checkpoints
List<ProgressiveTool.Checkpoint> checkpoints = 
    progressiveTool.getCheckpoints(executionId);

// Restore from checkpoint
boolean restored = progressiveTool.restoreFromCheckpoint(executionId, 0);

// Checkpoint information
checkpoints.forEach(checkpoint -> {
    System.out.println("Time: " + checkpoint.timestamp());
    System.out.println("Progress: " + checkpoint.progress() + "%");
    System.out.println("Data: " + checkpoint.data());
});
```

### Execution History

```java
// Get history for specific execution
ProgressiveTool.ExecutionHistory history = 
    progressiveTool.getHistory(executionId);

if (history != null) {
    System.out.println("Tool: " + history.toolId());
    System.out.println("Start: " + Instant.ofEpochMilli(history.startTime()));
    System.out.println("End: " + Instant.ofEpochMilli(history.endTime()));
    System.out.println("Success: " + history.success());
    System.out.println("Retries: " + history.retryAttempts());
}

// Get all history
Map<String, ProgressiveTool.ExecutionHistory> allHistory = 
    progressiveTool.getAllHistory();

// Clear old history
int cleared = progressiveTool.clearOldHistory(Duration.ofHours(1));
System.out.println("Cleared " + cleared + " old history entries");
```

### Retry on Failure

```java
// Execute with automatic retry
String executionId = progressiveTool.execute(
    "flaky_tool", params, context, progress -> {
        if (progress.status() == ProgressiveTool.ExecutionStatus.RETRYING) {
            System.out.println("Retrying...");
        }
    }
);

// Manual retry after failure
ProgressiveTool.ExecutionHistory history = progressiveTool.getHistory(executionId);
if (history != null && !history.success()) {
    String retryExecutionId = progressiveTool.retry(executionId);
    System.out.println("Retried as: " + retryExecutionId);
}
```

### Monitor All Active Executions

```java
// Get all active progress
Map<String, ProgressiveTool.ToolProgress> allProgress = 
    progressiveTool.getAllActiveProgress();

allProgress.forEach((id, progress) -> {
    System.out.printf("Execution %s: %.1f%% - %s%n", 
        id, progress.percent(), progress.status()
    );
});
```

## Progress Phases

The progressive execution follows these phases:

```
[Initializing] → [Validating] → [Preparing] → [Executing] → [Finalizing] → [Completed]
     0%              10%           25%           50%           75%          100%
```

### Phase Descriptions

1. **Initializing (0%)**: Setting up execution state
2. **Validating (10-25%)**: Validating parameters and context
3. **Preparing (25-50%)**: Loading resources, preparing environment
4. **Executing (50-75%)**: Running the actual tool logic
5. **Finalizing (75-100%)**: Post-processing, cleanup
6. **Completed (100%)**: Execution finished

## Execution Status

```java
public enum ExecutionStatus {
    INITIALIZING,   // Setting up
    RUNNING,        // Executing
    RETRYING,       // Retrying after failure
    COMPLETED,      // Successfully completed
    FAILED,         // Failed after retries
    CANCELLED       // Cancelled by user
}
```

## Progress Information

```java
public record ToolProgress(
    String executionId,      // Unique execution ID
    double percent,          // 0.0 to 100.0
    ExecutionStatus status,  // Current status
    String message,          // Status message
    Instant startTime,       // When execution started
    Instant endTime,         // When completed (null if running)
    List<Checkpoint> checkpoints  // List of checkpoints
) {
    boolean isCompleted();   // Check if execution is done
    Duration elapsed();      // Elapsed time
    Duration estimatedRemaining();  // ETA
}
```

### Usage

```java
ProgressiveTool.ToolProgress progress = progressiveTool.getProgress(executionId);

if (progress != null) {
    System.out.println("Progress: " + progress.percent() + "%");
    System.out.println("Status: " + progress.status());
    System.out.println("Elapsed: " + progress.elapsed());
    System.out.println("ETA: " + progress.estimatedRemaining());
    System.out.println("Checkpoints: " + progress.checkpoints().size());
}
```

## Partial Results

```java
public record PartialResult(
    Object data,      // Partial result data
    int sequence,     // Sequence number (0-based)
    boolean isLast    // True if last result
) {}
```

### Streaming Example

```java
progressiveTool.streamResults(executionId)
    .filter(result -> !result.isLast())  // Filter out last marker
    .map(result -> transform(result.data()))
    .subscribe().with(
        transformed -> process(transformed)
    );
```

## Checkpoints

```java
public record Checkpoint(
    Instant timestamp,           // When checkpoint was created
    double progress,             // Progress at checkpoint
    Map<String, Object> data     // Checkpoint data
) {}
```

### Checkpoint Strategy

```java
// Create checkpoint every 10 seconds
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    progressiveTool.createCheckpoint(executionId, Map.of(
        "timestamp", System.currentTimeMillis(),
        "state", serializeState()
    ));
}, 10, 10, TimeUnit.SECONDS);
```

## Execution History

```java
public record ExecutionHistory(
    String executionId,           // Unique execution ID
    String toolId,                // Tool that was executed
    Map<String, Object> params,   // Input parameters
    Map<String, Object> context,  // Execution context
    long startTime,               // Start timestamp
    long endTime,                 // End timestamp
    SkillResult result,           // Execution result
    boolean success,              // Whether it succeeded
    int retryAttempts,            // Number of retries
    List<Checkpoint> checkpoints  // Checkpoints created
) {}
```

## Error Handling

### ExecutionCancelledException

Thrown when execution is cancelled by user.

```java
try {
    progressiveTool.awaitCompletion(executionId, Duration.ofSeconds(30))
        .await().indefinitely();
} catch (ProgressiveTool.ExecutionCancelledException e) {
    System.out.println("Execution was cancelled: " + e.getMessage());
}
```

### ExecutionTimeoutException

Thrown when execution times out.

```java
try {
    progressiveTool.awaitCompletion(executionId, Duration.ofSeconds(10))
        .await().atMost(Duration.ofSeconds(10));
} catch (ProgressiveTool.ExecutionTimeoutException e) {
    System.out.println("Execution timed out: " + e.getMessage());
}
```

## Advanced Patterns

### Pattern 1: Progress Bar

```java
String executionId = progressiveTool.execute(
    "long_task", params, context, progress -> {
        int bars = (int)(progress.percent() / 5);
        String bar = "█".repeat(bars) + "░".repeat(20 - bars);
        System.out.printf("\r[%s] %.1f%%", bar, progress.percent());
    }
);
```

### Pattern 2: Timeout with Fallback

```java
Uni<SkillResult> result = progressiveTool.awaitCompletion(executionId, Duration.ofSeconds(30))
    .onFailure().recoverWithUni(error -> {
        if (error instanceof TimeoutException) {
            // Fallback to simpler tool
            return executeFallbackTool(params);
        }
        return Uni.createFrom().failure(error);
    });
```

### Pattern 3: Conditional Cancellation

```java
AtomicReference<String> executionIdRef = new AtomicReference<>();

ProgressiveTool.ProgressListener listener = progress -> {
    // Cancel if taking too long
    if (progress.elapsed().toSeconds() > 60) {
        progressiveTool.cancel(executionIdRef.get());
    }
    
    // Cancel if error detected
    if (progress.message().contains("ERROR")) {
        progressiveTool.cancel(executionIdRef.get());
    }
};

String executionId = progressiveTool.execute("task", params, context, listener);
executionIdRef.set(executionId);
```

### Pattern 4: Checkpoint Recovery

```java
public Uni<SkillResult> executeWithRecovery(String toolId, Map<String, Object> params) {
    String executionId = progressiveTool.execute(toolId, params, context);
    
    return progressiveTool.awaitCompletion(executionId, Duration.ofMinutes(5))
        .onFailure().recoverWithUni(error -> {
            // Try to restore from last checkpoint
            List<Checkpoint> checkpoints = progressiveTool.getCheckpoints(executionId);
            if (!checkpoints.isEmpty()) {
                Checkpoint last = checkpoints.get(checkpoints.size() - 1);
                progressiveTool.restoreFromCheckpoint(executionId, checkpoints.size() - 1);
                return progressiveTool.retry(executionId)
                    .flatMap(id -> progressiveTool.awaitCompletion(id, Duration.ofMinutes(5)));
            }
            return Uni.createFrom().failure(error);
        });
}
```

### Pattern 5: Parallel Executions with Progress Aggregation

```java
List<String> executionIds = new ArrayList<>();

// Start multiple executions
for (Map<String, Object> paramSet : paramSets) {
    String id = progressiveTool.execute("tool", paramSet, context, progress -> {
        // Update aggregated progress
        double avgProgress = executionIds.stream()
            .map(progressiveTool::getProgress)
            .filter(Objects::nonNull)
            .mapToDouble(ProgressiveTool.ToolProgress::percent)
            .average()
            .orElse(0.0);
        System.out.println("Average progress: " + avgProgress + "%");
    });
    executionIds.add(id);
}
```

## Configuration

### Timeouts

```java
// Default timeout is 300 seconds (5 minutes)
// Can be overridden per execution
Uni<SkillResult> result = progressiveTool.awaitCompletion(
    executionId, 
    Duration.ofMinutes(10)  // Custom timeout
);
```

### Retry Settings

```java
// Default: 3 retries with exponential backoff
// Backoff: 2s, 4s, 8s, ...
// Configured via constants in ProgressiveTool class
```

### Checkpoint Interval

```java
// Default: 10 seconds
// Create checkpoints periodically
scheduledExecutor.scheduleAtFixedRate(() -> {
    progressiveTool.createCheckpoint(executionId, state);
}, 10, 10, TimeUnit.SECONDS);
```

## Best Practices

### 1. Always Add Progress Listeners for Long Operations

```java
// Good
progressiveTool.execute("long_task", params, context, progress -> {
    LOG.infof("Progress: %.1f%%", progress.percent());
});

// Bad - no visibility into progress
progressiveTool.execute("long_task", params, context);
```

### 2. Handle Cancellation Gracefully

```java
ProgressiveTool.ProgressListener listener = progress -> {
    if (shouldCancel()) {
        progressiveTool.cancel(progress.executionId());
        return;
    }
    // Continue processing
};
```

### 3. Create Checkpoints for Long Operations

```java
// Create checkpoints at key milestones
progressiveTool.createCheckpoint(executionId, Map.of(
    "phase", "data_loaded",
    "records", recordCount
));
```

### 4. Clean Up Old History

```java
@Scheduled(every = "1h")
void cleanupHistory() {
    progressiveTool.clearOldHistory(Duration.ofHours(1));
}
```

### 5. Monitor Active Executions

```java
@Scheduled(every = "1m")
void monitorExecutions() {
    Map<String, ProgressiveTool.ToolProgress> all = 
        progressiveTool.getAllActiveProgress();
    
    all.forEach((id, progress) -> {
        if (progress.elapsed().toMinutes() > 10) {
            LOG.warnf("Long-running execution: %s", id);
        }
    });
}
```

## Testing

```java
@QuarkusTest
class ProgressiveToolTest {

    @Inject
    ProgressiveTool progressiveTool;

    @Test
    void testProgressTracking() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<Double> progressUpdates = new ArrayList<>();

        ProgressiveTool.ProgressListener listener = progress -> {
            progressUpdates.add(progress.percent());
            if (progress.isCompleted()) {
                latch.countDown();
            }
        };

        String executionId = progressiveTool.execute(
            "test_tool", params, context, listener
        );

        latch.await(10, TimeUnit.SECONDS);

        assertThat(progressUpdates).contains(0.0, 100.0);
    }

    @Test
    void testCancellation() {
        String executionId = progressiveTool.execute(
            "slow_tool", params, context
        );

        boolean cancelled = progressiveTool.cancel(executionId);

        assertThat(cancelled).isTrue();
        
        ProgressiveTool.ToolProgress progress = 
            progressiveTool.getProgress(executionId);
        assertThat(progress.status()).isEqualTo(
            ProgressiveTool.ExecutionStatus.CANCELLED
        );
    }
}
```

## Performance Considerations

### Memory Usage

- Active executions stored in memory
- History stored in memory (clear periodically)
- Checkpoints can accumulate (limit per execution)

### Recommendations

1. **Clear old history regularly**
   ```java
   progressiveTool.clearOldHistory(Duration.ofHours(1));
   ```

2. **Limit concurrent executions**
   ```java
   if (progressiveTool.getAllActiveProgress().size() > MAX_CONCURRENT) {
       throw new IllegalStateException("Too many active executions");
   }
   ```

3. **Use streaming for large results**
   ```java
   progressiveTool.streamResults(executionId)
       .subscribe().with(result -> processIncrementally(result));
   ```

## Troubleshooting

### Execution Not Progressing

**Symptoms**: Progress stuck at same percentage

**Solutions**:
- Check if execution is blocked waiting for resources
- Verify tool implementation is calling progress updates
- Check for deadlocks in tool logic

### Cancellation Not Working

**Symptoms**: Execution continues after cancel()

**Solutions**:
- Ensure tool checks for cancellation
- Implement proper cleanup in tool
- Use timeout as backup

### Memory Leak

**Symptoms**: Memory usage growing over time

**Solutions**:
- Clear old history: `clearOldHistory()`
- Limit checkpoint count
- Remove listeners when done

## Integration with Other Tools

### With ToolCacheManager

```java
@Inject
ProgressiveTool progressiveTool;

@Inject
ToolCacheManager cacheManager;

public Uni<SkillResult> executeWithCacheAndProgress(
        String toolId, 
        Map<String, Object> params) {
    
    return cacheManager.executeWithCache(
        toolId, params, context,
        () -> {
            String executionId = progressiveTool.execute(
                toolId, params, context, progress -> {
                    LOG.infof("Progress: %.1f%%", progress.percent());
                }
            );
            
            return progressiveTool.awaitCompletion(
                executionId, Duration.ofMinutes(5)
            ).await().indefinitely();
        }
    );
}
```

### With IntelligentToolSelector

```java
@Inject
ProgressiveTool progressiveTool;

@Inject
IntelligentToolSelector toolSelector;

public Uni<ToolChainResult> executeChainWithProgress(
        ProjectContext context, String task) {
    
    return toolSelector.selectTools(context, task)
        .flatMap(chain -> {
            List<Uni<ToolExecutionResult>> executions = new ArrayList<>();
            
            for (SelectedTool tool : chain.getTools()) {
                String executionId = progressiveTool.execute(
                    tool.toolId(),
                    tool.parameters(),
                    context,
                    progress -> LOG.infof("Tool %s: %.1f%%", 
                        tool.toolId(), progress.percent())
                );
                
                executions.add(
                    progressiveTool.awaitCompletion(
                        executionId, Duration.ofMinutes(5)
                    ).map(result -> new ToolExecutionResult(
                        tool.toolId(), true, 0, result.observation(), null
                    ))
                );
            }
            
            return Uni.combine().all().unis(executions)
                .combinedWith(results -> new ToolChainResult(results, false));
        });
}
```

## API Reference

### Main Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `execute(toolId, params, context)` | Execute tool | `String` execution ID |
| `execute(toolId, params, context, listener)` | Execute with listener | `String` execution ID |
| `getProgress(executionId)` | Get current progress | `ToolProgress` |
| `getAllActiveProgress()` | Get all active progress | `Map<String, ToolProgress>` |
| `cancel(executionId)` | Cancel execution | `boolean` |
| `cancelAll()` | Cancel all executions | `int` count |
| `streamResults(executionId)` | Stream partial results | `Multi<PartialResult>` |
| `awaitCompletion(executionId, timeout)` | Wait for completion | `Uni<SkillResult>` |
| `createCheckpoint(executionId, data)` | Create checkpoint | `void` |
| `getCheckpoints(executionId)` | Get checkpoints | `List<Checkpoint>` |
| `restoreFromCheckpoint(executionId, index)` | Restore from checkpoint | `boolean` |
| `getHistory(executionId)` | Get execution history | `ExecutionHistory` |
| `getAllHistory()` | Get all history | `Map<String, ExecutionHistory>` |
| `clearOldHistory(olderThan)` | Clear old history | `int` cleared |
| `retry(executionId)` | Retry failed execution | `String` new ID |

## More Resources

- Source Code: `ProgressiveTool.java`
- Tests: `ProgressiveToolTest.java`
- Tool Cache Guide: `TOOL_CACHE_SELECTOR_GUIDE.md`
- Quick Reference: `TOOL_QUICK_REFERENCE.md`
