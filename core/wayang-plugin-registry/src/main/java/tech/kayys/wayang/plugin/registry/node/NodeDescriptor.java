/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.node;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Lightweight runtime descriptor for a node within an execution contract.
 */
@RegisterForReflection
public class NodeDescriptor {
    public String type;
    public String version;
    public String instanceId;
}
