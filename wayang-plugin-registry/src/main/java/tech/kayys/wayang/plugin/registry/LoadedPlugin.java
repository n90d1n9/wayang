/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry;

import java.util.Map;

public class LoadedPlugin {
    public final ClassLoader classLoader;
    public final Map<String, Class<?>> classes;
    public final Map<String, Object> instances;
    public final Map<String, byte[]> resources;

    public LoadedPlugin(
            ClassLoader classLoader,
            Map<String, Class<?>> classes,
            Map<String, Object> instances,
            Map<String, byte[]> resources) {
        this.classLoader = classLoader;
        this.classes = classes;
        this.instances = instances;
        this.resources = resources;
    }
}
