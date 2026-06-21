/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.node;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.networknt.schema.JsonSchema;

import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.kayys.wayang.plugin.registry.UIReference;
import tech.kayys.wayang.plugin.registry.executor.ExecutorBinding;

/**
 * Node definition metadata for runtime usage
 */
@RegisterForReflection
public class NodeDefinition {
    // Metadata
    public String type;
    public String label;
    public String category;
    public String subCategory;
    public String description;
    public String version;
    public String author;

    // Contract (JSON Schema - source of truth)
    public JsonSchema configSchema;
    public JsonSchema inputSchema;
    public JsonSchema outputSchema;

    // Presentation (UI reference, NOT implementation)
    public UIReference uiReference;

    // Behavior (executor binding, NOT executor)
    public ExecutorBinding executorBinding;

    // Additional metadata
    public Map<String, Object> metadata = new HashMap<>();
    public List<String> tags = new ArrayList<>();
    public Instant registeredAt;
}
