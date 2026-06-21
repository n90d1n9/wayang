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

package tech.kayys.wayang.plugin.multi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ============================================================================
 * MULTI-NODE PLUGIN SYSTEM - COMPLETE IMPLEMENTATION
 * ============================================================================
 * 
 * Handles multiple node definitions in a single JAR:
 * 
 * 1. Plugin Manifest approach (YAML/JSON)
 * 2. Annotation-based discovery
 * 3. Single executor handling multiple nodes
 * 4. Multiple executors in one plugin
 * 5. Node families (related nodes)
 * 6. Shared resources across nodes
 * 
 * Example Use Cases:
 * - AI Plugin: sentiment, classification, NER, summarization
 * - Database Plugin: query, insert, update, delete
 * - HTTP Plugin: GET, POST, PUT, DELETE
 * - Vector Store Plugin: insert, search, delete
 */

// ==================== PLUGIN ANNOTATIONS ====================

/**
 * Marks a plugin that contains multiple nodes
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiNodePlugin {

    /**
     * Plugin ID (reverse domain notation)
     */
    String id();

    /**
     * Plugin name
     */
    String name();

    /**
     * Plugin version
     */
    String version() default "1.0.0";

    /**
     * Node family/category
     */
    String family() default "";

    /**
     * Author
     */
    String author() default "";

    /**
     * Description
     */
    String description() default "";
}