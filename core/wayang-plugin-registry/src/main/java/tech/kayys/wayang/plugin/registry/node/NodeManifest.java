/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.node;

import java.util.HashMap;
import java.util.Map;
import tech.kayys.wayang.schema.validator.SchemaReference;

/**
 * Node manifest entry
 */
public class NodeManifest {
    public String type;
    public String label;
    public String category;
    public String subCategory;
    public String description;
    public String icon;
    public String color;
    public SchemaReference configSchema;
    public SchemaReference inputSchema;
    public SchemaReference outputSchema;
    public String executorId;
    public String widgetId;
    public Map<String, Object> config = new HashMap<>();
}
