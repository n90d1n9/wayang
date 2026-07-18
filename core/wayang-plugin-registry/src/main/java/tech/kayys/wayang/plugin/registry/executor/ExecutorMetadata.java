/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.executor;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor Metadata
 */
public class ExecutorMetadata {
    public String version;
    public String language;
    public Map<String, String> labels = new HashMap<>();
    public Map<String, Object> config = new HashMap<>();
    public int maxConcurrency;
    public long timeoutMs;
}
