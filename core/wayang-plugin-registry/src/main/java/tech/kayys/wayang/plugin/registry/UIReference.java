/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry;

import java.util.HashMap;
import java.util.Map;

/**
 * UI Reference - Points to widget, NEVER contains implementation
 */
public class UIReference {
    public String widgetId; // "form.password", "node.standard"
    public Map<String, Object> props = new HashMap<>();

    public UIReference() {
    }

    public UIReference(String widgetId) {
        this.widgetId = widgetId;
    }
}
