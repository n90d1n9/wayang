package tech.kayys.wayang.agent.analytics;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

/**
 * Configuration for skill analytics.
 */
@ConfigMapping(prefix = "wayang.analytics")
public interface AnalyticsConfig {
    
    /**
     * Whether analytics is enabled.
     */
    @WithDefault("true")
    boolean enabled();
    
    /**
     * Retention period for analytics data.
     */
    @WithDefault("7d")
    Duration retentionPeriod();
    
    /**
     * Maximum number of skills to track in top skills report.
     */
    @WithDefault("100")
    int maxTrackedSkills();
    
    /**
     * Whether to export metrics to Micrometer.
     */
    @WithDefault("true")
    boolean exportMetrics();
    
    /**
     * Sampling rate for detailed tracing (0.0 to 1.0).
     */
    @WithDefault("1.0")
    double samplingRate();
}
