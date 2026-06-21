/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Resource loader for shared resources
 */
@ApplicationScoped
public class PluginResourceLoader {

    private static final Logger LOG = Logger.getLogger(PluginResourceLoader.class);

    private final Map<String, String> schemaCache = new HashMap<>();
    private final Map<String, byte[]> widgetCache = new HashMap<>();

    public void cacheSchema(String name, String schema) {
        schemaCache.put(name, schema);
    }

    public String getSchema(String name) {
        return schemaCache.get(name);
    }

    public void cacheWidget(String name, byte[] widget) {
        widgetCache.put(name, widget);
    }

    public byte[] getWidget(String name) {
        return widgetCache.get(name);
    }

    public String loadFromUrl(String url) {
        // Load schema from external URL
        // Implementation depends on HTTP client
        LOG.warnf("Loading from URL not yet implemented: %s", url);
        return null;
    }
}
