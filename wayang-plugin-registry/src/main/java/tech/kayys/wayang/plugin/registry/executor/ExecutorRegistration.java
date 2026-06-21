/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.executor;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import tech.kayys.wayang.plugin.CommunicationProtocol;

/**
 * Executor Registration
 */
public class ExecutorRegistration {
    public String executorId;
    public String executorType;
    public URI endpoint;
    public CommunicationProtocol protocol;
    public Set<String> capabilities = new HashSet<>();
    public Set<String> supportedNodes = new HashSet<>();
    public ExecutorStatus status = ExecutorStatus.PENDING;
    public ExecutorMetadata metadata = new ExecutorMetadata();
    public Instant registeredAt;
    public Instant lastHeartbeat;
    public boolean inProcess = false;
    public Object executorInstance;
}
