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

package tech.kayys.wayang.plugin.executor;

import tech.kayys.wayang.plugin.WayangPlugin;
import java.util.Set;

/**
 * Plugin that provides one or more executors.
 */
public interface ExecutorPlugin extends WayangPlugin {
    /**
     * Returns the set of capabilities provided by this executor plugin.
     */
    Set<String> capabilities();
}
