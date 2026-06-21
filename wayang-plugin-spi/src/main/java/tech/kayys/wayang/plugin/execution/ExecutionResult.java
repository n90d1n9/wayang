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
import java.util.HashMap;
import java.util.Map;

/**
 * Execution Result - Response from executor
 */
public class ExecutionResult {
    public String executionId;
    public ExecutionStatus status;
    public Map<String, Object> outputs = new HashMap<>();
    public ExecutionError error;
    public ExecutionMetrics metrics;
    public Instant completedAt;
}