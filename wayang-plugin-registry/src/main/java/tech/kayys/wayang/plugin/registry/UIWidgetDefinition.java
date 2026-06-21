/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * UI Widget Definition - What widgets support
 */
public class UIWidgetDefinition {
    public String widgetId; // "form.password", "node.standard"
    public String type; // "react", "vue", "webcomponent"
    public Set<String> supportedDataTypes = new HashSet<>();
    public Set<String> capabilities = new HashSet<>();
    public String entryPoint; // Module path or CDN URL
    public String version;
    public Map<String, Object> defaultProps = new HashMap<>();
}
