/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.executor;

import java.util.HashMap;
import java.util.Map;

import tech.kayys.wayang.plugin.CommunicationProtocol;
import tech.kayys.wayang.plugin.execution.ExecutionMode;

/**
 * Executor Binding - Indirect reference to executor
 */
public class ExecutorBinding {
    public String executorType;
    public String executorId;
    public ExecutionMode mode;
    public CommunicationProtocol protocol;
    public Map<String, Object> config = new HashMap<>();

    public ExecutorBinding() {
    }

    public ExecutorBinding(String executorId, ExecutionMode mode, CommunicationProtocol protocol) {
        this.executorId = executorId;
        this.mode = mode;
        this.protocol = protocol;
    }
}
