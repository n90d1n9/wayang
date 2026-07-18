/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.plugin.registry.SharedResources;
import tech.kayys.wayang.plugin.registry.executor.ExecutorManifest;
import tech.kayys.wayang.plugin.registry.node.NodeManifest;

/**
 * Complete plugin manifest for multiple nodes
 */
public class MultiNodePluginManifest {
    public String pluginId;
    public String name;
    public String version;
    public String family;
    public String author;
    public String description;
    public List<NodeManifest> nodes = new ArrayList<>();
    public List<ExecutorManifest> executors = new ArrayList<>();
    public SharedResources shared = new SharedResources();
    public Map<String, Object> config = new HashMap<>();
}
