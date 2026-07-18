/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared resources across nodes
 */
public class SharedResources {
    public Map<String, String> schemas = new HashMap<>(); // name -> path
    public Map<String, String> widgets = new HashMap<>(); // name -> path
    public Map<String, String> scripts = new HashMap<>(); // name -> path
    public Map<String, Object> constants = new HashMap<>(); // Shared constants
}
