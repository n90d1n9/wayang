/*
 * PolyForm Noncommercial License 1.0.0
 *
 * Copyright (c) 2026 Kayys.tech
 *
 * This software is licensed for non-commercial use only.
 * You may use, modify, and distribute this software for personal,
 * educational, or research purposes.
 *
 * Commercial use, including SaaS or revenue-generating services,
 * requires a separate commercial license from Kayys.tech.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 *
 * @author Bhangun
 */

package tech.kayys.wayang.plugin.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.plugin.registry.UIWidgetDefinition;

/**
 * UI Widget Registry - Independent from backend
 */
@ApplicationScoped
public class ControlPlaneWidgetRegistry {

    private static final Logger LOG = Logger.getLogger(ControlPlaneWidgetRegistry.class);

    private final Map<String, UIWidgetDefinition> widgetRegistry = new ConcurrentHashMap<>();

    public void register(UIWidgetDefinition widget) {
        widgetRegistry.put(widget.widgetId, widget);
        LOG.infof("Registered widget: %s (type: %s)", widget.widgetId, widget.type);
    }

    public void unregister(String widgetId) {
        widgetRegistry.remove(widgetId);
    }

    public UIWidgetDefinition get(String widgetId) {
        return widgetRegistry.get(widgetId);
    }

    public List<UIWidgetDefinition> getAll() {
        return new ArrayList<>(widgetRegistry.values());
    }
}
