/*
 * PolyForm Noncommercial License 1.0.0
 *
 * Copyright (c) 2026 Kayys.tech
 *
 * This software is licensed for non-commercial use only.
 * You may use, modify, and distribute this software for personal,
 * educational, or research purposes.
 *
 * Commercial use, including SaaS or revenue-generating services,
 * requires a separate commercial license from Kayys.tech.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 *
 * @author Bhangun
 */

package tech.kayys.wayang.plugin.execution;

import java.time.Instant;
import java.util.Map;

import tech.kayys.wayang.plugin.TraceContext;
import tech.kayys.wayang.plugin.executor.ExecutorDescriptor;
// NodeDescriptor is the lightweight routing info embedded in each execution contract.
// NodeDefinition (from plugin-registry) is the full metadata kept in the node registry.

/**
 * Execution Contract - Language-neutral contract between Engine and Executor
 * This is THE critical artifact that enables:
 * - Replay
 * - Audit
 * - Retry
 * - Time-travel
 * - Multi-language executors
 */
public class ExecutionContract {
    // Identity
    public String executionId;
    public String workflowRunId;

    // Node information (lightweight routing descriptor, not the full
    // NodeDefinition)
    public String nodeType;
    public String nodeVersion;
    public String nodeInstanceId;

    // Executor information
    public ExecutorDescriptor executor;

    // Execution mode
    public ExecutionMode mode;

    // Data
    public Map<String, Object> inputs;
    public Map<String, Object> config;
    public ExecutionContext context;

    // Observability
    public TraceContext trace;

    // Timing
    public Instant createdAt;
    public Instant expiresAt;
}
